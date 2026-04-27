package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fraud scoring V2 wrapper: combines heuristic score + optional external ML score.
 */
public final class HybridFraudDetectionService {

    private static final Pattern SCORE_PATTERN = Pattern.compile("\"risk_score\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern REASONS_PATTERN = Pattern.compile("\"reasons\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern REASON_ITEM_PATTERN = Pattern.compile("\"(.*?)\"");
    private static final double HEURISTIC_WEIGHT = 0.55d;
    private static final double ML_WEIGHT = 0.45d;

    private final FraudDetectionService heuristicService = new FraudDetectionService();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String endpoint = System.getenv("AI_FRAUD_API_URL");

    public FraudDetectionService.FraudReport analyze(Annonce candidate, List<Annonce> existing) {
        FraudDetectionService.FraudReport heuristic = heuristicService.analyze(candidate, existing);
        FraudDetectionService.FraudReport ml = fetchMlReport(candidate);
        if (ml == null) {
            return heuristic;
        }

        double combined = clamp(HEURISTIC_WEIGHT * heuristic.riskScore() + ML_WEIGHT * ml.riskScore());
        List<String> reasons = new ArrayList<>();
        reasons.addAll(heuristic.reasons() == null ? List.of() : heuristic.reasons());
        if (ml.reasons() != null) {
            for (String r : ml.reasons()) {
                reasons.add("[ML] " + r);
            }
        }
        boolean suspicious = heuristic.suspicious() || ml.suspicious() || combined >= 0.35d;
        return new FraudDetectionService.FraudReport(combined, reasons, suspicious);
    }

    private FraudDetectionService.FraudReport fetchMlReport(Annonce candidate) {
        if (endpoint == null || endpoint.isBlank() || candidate == null) {
            return null;
        }
        try {
            String payload = "{\"title\":\"" + json(candidate.getTitreAnnonce())
                    + "\",\"description\":\"" + json(candidate.getDescription()) + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            String body = response.body();
            Matcher scoreMatcher = SCORE_PATTERN.matcher(body);
            if (!scoreMatcher.find()) {
                return null;
            }
            double score = clamp(Double.parseDouble(scoreMatcher.group(1)));
            List<String> reasons = parseReasons(body);
            return new FraudDetectionService.FraudReport(score, reasons, score >= 0.50d);
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            return null;
        }
    }

    private static List<String> parseReasons(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        Matcher reasonsBlock = REASONS_PATTERN.matcher(body);
        if (!reasonsBlock.find()) {
            return List.of();
        }
        String listBody = reasonsBlock.group(1);
        Matcher item = REASON_ITEM_PATTERN.matcher(listBody);
        List<String> out = new ArrayList<>();
        while (item.find()) {
            String reason = item.group(1);
            if (reason != null && !reason.isBlank()) {
                out.add(reason);
            }
        }
        return out;
    }

    private static String json(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static double clamp(double value) {
        if (value < 0d) {
            return 0d;
        }
        return Math.min(1d, value);
    }
}
