package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lightweight anti-spam / anti-fraud heuristics for annonces.
 */
public final class FraudDetectionService {

    public record FraudReport(double riskScore, List<String> reasons, boolean suspicious) {}

    private static final Set<String> SUSPICIOUS_KEYWORDS = Set.of(
            "argent", "virement", "cash", "urgent argent", "gagnez", "loterie", "promo",
            "bitcoin", "crypto", "western union", "moneygram", "cadeau", "prize", "win",
            "lottery", "lotter", "dollar", "dollars", "donate", "kidney", "rein");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\b\\d{3,}(?:[\\s,.]\\d{3})*\\b");

    public FraudReport analyze(Annonce candidate, List<Annonce> existing) {
        if (candidate == null) {
            return new FraudReport(0d, List.of(), false);
        }
        List<String> reasons = new ArrayList<>();
        double score = 0d;

        String title = safe(candidate.getTitreAnnonce());
        String desc = safe(candidate.getDescription());
        String joined = (title + " " + desc).trim();
        String normalized = normalize(joined);

        if (joined.length() < 12) {
            score += 0.20d;
            reasons.add("Texte très court, contexte insuffisant.");
        }
        if (looksLikeShouting(joined)) {
            score += 0.15d;
            reasons.add("Usage excessif de majuscules.");
        }
        if (hasRepeatedCharacters(joined)) {
            score += 0.15d;
            reasons.add("Répétitions anormales de caractères.");
        }
        if (hasTooManySpecialChars(joined)) {
            score += 0.10d;
            reasons.add("Trop de caractères spéciaux.");
        }
        if (containsSuspiciousKeywords(normalized)) {
            score += 0.25d;
            reasons.add("Mots-clés souvent liés aux annonces frauduleuses.");
        }
        if (containsMoneyAmount(normalized)) {
            score += 0.25d;
            reasons.add("Montant d'argent détecté dans le texte.");
        }
        if (containsPrizeScamPattern(normalized)) {
            score += 0.30d;
            reasons.add("Pattern gain/récompense détecté (ex: 'win/gagnez' + argent).");
        }
        if (containsOrganTradePattern(normalized)) {
            score += 0.40d;
            reasons.add("Pattern sensible détecté (don d'organe contre argent).");
        }

        double duplicateScore = bestDuplicateSimilarity(candidate, existing);
        if (duplicateScore >= 0.90d) {
            score += 0.35d;
            reasons.add("Annonce presque identique à une annonce existante.");
        } else if (duplicateScore >= 0.75d) {
            score += 0.20d;
            reasons.add("Annonce très similaire à une annonce existante.");
        }

        score = Math.min(1.0d, score);
        boolean suspicious = score >= 0.35d;
        return new FraudReport(score, reasons, suspicious);
    }

    private static boolean containsMoneyAmount(String normalized) {
        return MONEY_PATTERN.matcher(normalized).find();
    }

    private static boolean containsPrizeScamPattern(String normalized) {
        boolean hasPrizeWord = normalized.contains("win")
                || normalized.contains("gagnez")
                || normalized.contains("prize")
                || normalized.contains("loterie")
                || normalized.contains("lottery")
                || normalized.contains("lotter");
        boolean hasMoneyWord = normalized.contains("dollar")
                || normalized.contains("dollars")
                || normalized.contains("euro")
                || normalized.contains("euros")
                || normalized.contains("tnd")
                || normalized.contains("argent")
                || containsMoneyAmount(normalized);
        return hasPrizeWord && hasMoneyWord;
    }

    private static boolean containsOrganTradePattern(String normalized) {
        boolean hasOrganWord = normalized.contains("kidney")
                || normalized.contains("rein")
                || normalized.contains("organe")
                || normalized.contains("organ");
        boolean hasDonationVerb = normalized.contains("don")
                || normalized.contains("donate")
                || normalized.contains("vendre")
                || normalized.contains("sell");
        boolean hasMoneyWord = normalized.contains("argent")
                || normalized.contains("money")
                || normalized.contains("dollar")
                || normalized.contains("dollars")
                || normalized.contains("euro")
                || normalized.contains("euros")
                || normalized.contains("tnd")
                || containsMoneyAmount(normalized);
        return hasOrganWord && (hasDonationVerb || hasMoneyWord) && hasMoneyWord;
    }

    private static boolean containsSuspiciousKeywords(String normalized) {
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeShouting(String text) {
        int letters = 0;
        int upper = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        if (letters < 10) {
            return false;
        }
        return ((double) upper / (double) letters) > 0.60d;
    }

    private static boolean hasRepeatedCharacters(String text) {
        int run = 1;
        char prev = 0;
        for (char c : text.toCharArray()) {
            if (c == prev) {
                run++;
                if (run >= 5) {
                    return true;
                }
            } else {
                run = 1;
            }
            prev = c;
        }
        return false;
    }

    private static boolean hasTooManySpecialChars(String text) {
        if (text.isBlank()) {
            return false;
        }
        int special = 0;
        for (char c : text.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                special++;
            }
        }
        return ((double) special / (double) text.length()) > 0.20d;
    }

    private static double bestDuplicateSimilarity(Annonce candidate, List<Annonce> existing) {
        if (existing == null || existing.isEmpty()) {
            return 0d;
        }
        Set<String> cTokens = tokenSet(candidate);
        if (cTokens.isEmpty()) {
            return 0d;
        }
        double best = 0d;
        for (Annonce other : existing) {
            if (other == null) {
                continue;
            }
            if (candidate.getId() != null && candidate.getId().equals(other.getId())) {
                continue;
            }
            Set<String> oTokens = tokenSet(other);
            if (oTokens.isEmpty()) {
                continue;
            }
            double sim = jaccard(cTokens, oTokens);
            if (sim > best) {
                best = sim;
            }
        }
        return best;
    }

    private static Set<String> tokenSet(Annonce a) {
        String text = safe(a.getTitreAnnonce()) + " " + safe(a.getDescription());
        String normalized = normalize(text);
        String[] parts = normalized.split("\\s+");
        Set<String> set = new HashSet<>();
        for (String p : parts) {
            if (p.length() >= 2) {
                set.add(p);
            }
        }
        return set;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0d;
        }
        int intersection = 0;
        for (String s : a) {
            if (b.contains(s)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0d : (double) intersection / (double) union;
    }

    private static String normalize(String text) {
        String base = Normalizer.normalize(safe(text), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ");
        return base.replaceAll("\\s+", " ").trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

