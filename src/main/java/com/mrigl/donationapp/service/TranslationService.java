package com.mrigl.donationapp.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight translator service for JavaFX forms.
 * Uses a public Google translate endpoint and keeps API shape ready for future provider swap.
 */
public final class TranslationService {

    private static final Pattern FIRST_TRANSLATED_SEGMENT =
            Pattern.compile("\\[\\[\\[\\\"((?:\\\\.|[^\\\\\"])*)\\\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String sl = normalizeLang(sourceLang, "auto");
        String tl = normalizeLang(targetLang, "fr");
        if (sl.equals(tl)) {
            return text;
        }

        try {
            String query = "client=gtx"
                    + "&sl=" + URLEncoder.encode(sl, StandardCharsets.UTF_8)
                    + "&tl=" + URLEncoder.encode(tl, StandardCharsets.UTF_8)
                    + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            URI uri = URI.create("https://translate.googleapis.com/translate_a/single?" + query);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Erreur HTTP traduction: " + response.statusCode());
            }
            String translated = parseTranslatedText(response.body());
            return translated == null || translated.isBlank() ? text : translated;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Traduction indisponible pour le moment.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Traduction indisponible pour le moment.", ex);
        }
    }

    private static String parseTranslatedText(String body) {
        Matcher matcher = FIRST_TRANSLATED_SEGMENT.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        return raw
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private static String normalizeLang(String lang, String fallback) {
        if (lang == null || lang.isBlank()) {
            return fallback;
        }
        return lang.trim().toLowerCase(Locale.ROOT);
    }
}

