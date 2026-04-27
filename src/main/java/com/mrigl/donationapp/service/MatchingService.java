package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight text-embedding matcher (TF-IDF + cosine similarity) to suggest annonces for a donation text.
 */
public final class MatchingService {

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "du", "des", "le", "la", "les", "un", "une", "et", "ou", "pour", "par",
            "sur", "dans", "en", "a", "au", "aux", "avec", "sans", "the", "and", "for");

    public record MatchResult(Annonce annonce, double score) {}

    public List<MatchResult> rankAnnoncesForDonationText(String donationText, List<Annonce> annonces, int topK) {
        if (annonces == null || annonces.isEmpty()) {
            return List.of();
        }
        List<String> donationTokens = tokenize(donationText);
        if (donationTokens.isEmpty()) {
            return List.of();
        }

        List<List<String>> docs = new ArrayList<>();
        docs.add(donationTokens);
        for (Annonce annonce : annonces) {
            docs.add(tokenize(annonceText(annonce)));
        }
        Map<String, Double> idf = computeIdf(docs);

        Map<String, Double> donationVec = tfidf(donationTokens, idf);
        if (donationVec.isEmpty()) {
            return List.of();
        }

        List<MatchResult> results = new ArrayList<>();
        for (Annonce annonce : annonces) {
            Map<String, Double> annonceVec = tfidf(tokenize(annonceText(annonce)), idf);
            double score = cosineSimilarity(donationVec, annonceVec);
            if (score > 0d) {
                // urgency weight can slightly promote urgent annonces with similar content
                score *= urgencyBoost(annonce);
                results.add(new MatchResult(annonce, Math.min(score, 1.0d)));
            }
        }
        results.sort(Comparator.comparingDouble(MatchResult::score).reversed());
        if (topK > 0 && results.size() > topK) {
            return new ArrayList<>(results.subList(0, topK));
        }
        return results;
    }

    private static double urgencyBoost(Annonce annonce) {
        if (annonce == null || annonce.getUrgence() == null) {
            return 1.0d;
        }
        return switch (annonce.getUrgence()) {
            case "élevée" -> 1.10d;
            case "moyenne" -> 1.05d;
            default -> 1.0d;
        };
    }

    private static String annonceText(Annonce annonce) {
        if (annonce == null) {
            return "";
        }
        String title = annonce.getTitreAnnonce() == null ? "" : annonce.getTitreAnnonce();
        String desc = annonce.getDescription() == null ? "" : annonce.getDescription();
        return title + " " + desc;
    }

    private static Map<String, Double> computeIdf(List<List<String>> docs) {
        int n = docs.size();
        Map<String, Integer> df = new HashMap<>();
        for (List<String> doc : docs) {
            Set<String> seen = new HashSet<>(doc);
            for (String token : seen) {
                df.merge(token, 1, Integer::sum);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            double value = Math.log((1.0d + n) / (1.0d + e.getValue())) + 1.0d;
            idf.put(e.getKey(), value);
        }
        return idf;
    }

    private static Map<String, Double> tfidf(List<String> tokens, Map<String, Double> idf) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        int total = tokens.size();
        Map<String, Double> vec = new HashMap<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            double tf = (double) e.getValue() / (double) total;
            double idfValue = idf.getOrDefault(e.getKey(), 1.0d);
            vec.put(e.getKey(), tf * idfValue);
        }
        return vec;
    }

    private static double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0d;
        }
        double dot = 0d;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bv = b.get(e.getKey());
            if (bv != null) {
                dot += e.getValue() * bv;
            }
        }
        double normA = norm(a.values());
        double normB = norm(b.values());
        if (normA == 0d || normB == 0d) {
            return 0d;
        }
        return dot / (normA * normB);
    }

    private static double norm(Collection<Double> values) {
        double sum = 0d;
        for (double v : values) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ");
        String[] raw = normalized.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String token : raw) {
            if (token.length() < 2) {
                continue;
            }
            if (STOP_WORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }
}

