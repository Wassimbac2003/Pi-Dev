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
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matching V2 wrapper: tries external semantic API, falls back to local TF-IDF matcher.
 */
public final class HybridMatchingService {

    private static final Pattern SCORE_PATTERN = Pattern.compile("\"score\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final double MIX_EXTERNAL_WEIGHT = 0.70d;
    private static final double MIX_LOCAL_WEIGHT = 0.30d;

    private final MatchingService localMatcher = new MatchingService();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String endpoint = System.getenv("AI_MATCHING_API_URL");

    public List<MatchingService.MatchResult> rankAnnoncesForDonationText(String donationText, List<Annonce> annonces, int topK) {
        List<MatchingService.MatchResult> local = localMatcher.rankAnnoncesForDonationText(donationText, annonces, topK <= 0 ? 1000 : topK);
        if (endpoint == null || endpoint.isBlank() || annonces == null || annonces.isEmpty()) {
            return topK(local, topK);
        }

        List<MatchingService.MatchResult> merged = new ArrayList<>();
        for (Annonce annonce : annonces) {
            double localScore = findScore(local, annonce);
            OptionalDouble ext = fetchSemanticScore(donationText, annonce);
            double score = ext.isPresent()
                    ? clamp(MIX_EXTERNAL_WEIGHT * ext.getAsDouble() + MIX_LOCAL_WEIGHT * localScore)
                    : localScore;
            if (score > 0d) {
                merged.add(new MatchingService.MatchResult(annonce, score));
            }
        }
        merged.sort(Comparator.comparingDouble(MatchingService.MatchResult::score).reversed());
        return topK(merged, topK);
    }

    private OptionalDouble fetchSemanticScore(String donationText, Annonce annonce) {
        try {
            String payload = "{\"query\":\"" + json(donationText)
                    + "\",\"candidate\":\"" + json((annonce == null ? "" : (safe(annonce.getTitreAnnonce()) + " " + safe(annonce.getDescription()))))
                    + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return OptionalDouble.empty();
            }
            Matcher m = SCORE_PATTERN.matcher(response.body());
            if (!m.find()) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(clamp(Double.parseDouble(m.group(1))));
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            return OptionalDouble.empty();
        }
    }

    private static double findScore(List<MatchingService.MatchResult> local, Annonce annonce) {
        if (local == null || annonce == null) {
            return 0d;
        }
        for (MatchingService.MatchResult r : local) {
            if (r.annonce() != null && annonce.getId() != null && annonce.getId().equals(r.annonce().getId())) {
                return r.score();
            }
        }
        return 0d;
    }

    private static List<MatchingService.MatchResult> topK(List<MatchingService.MatchResult> all, int topK) {
        if (all == null || all.isEmpty() || topK <= 0 || all.size() <= topK) {
            return all == null ? List.of() : all;
        }
        return new ArrayList<>(all.subList(0, topK));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String json(String s) {
        return safe(s).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static double clamp(double value) {
        if (value < 0d) {
            return 0d;
        }
        return Math.min(1d, value);
    }
}
