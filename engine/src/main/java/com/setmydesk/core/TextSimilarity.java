package com.setmydesk.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * TF-IDF cosine similarity. Works on any Unicode script (English, Telugu, Hindi...), so ideas
 * sent in local languages are compared fairly too.
 */
public final class TextSimilarity {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "of", "to", "in", "on", "for", "with", "at", "by",
            "from", "is", "are", "was", "were", "be", "been", "it", "its", "this", "that", "these", "those",
            "i", "me", "my", "we", "our", "you", "your", "they", "them", "their", "he", "she", "his", "her",
            "so", "as", "do", "does", "did", "can", "will", "would", "should", "could", "have", "has", "had",
            "not", "no", "yes", "all", "any", "some", "more", "most", "very", "just", "also", "than", "then",
            "there", "here", "what", "which", "who", "when", "where", "why", "how", "about", "into", "out",
            "up", "down", "over", "one", "like", "want", "idea", "get", "make", "use", "using");

    private TextSimilarity() {
    }

    public static List<String> tokens(String text) {
        if (text == null) return List.of();
        String norm = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String raw : norm.split("[^\\p{L}\\p{M}\\p{N}]+")) {
            if (raw.length() < 2 || STOPWORDS.contains(raw)) continue;
            out.add(stem(raw));
        }
        return out;
    }

    /** Light suffix stripping so "teaching"/"teaches"/"teach" meet. Only touches Latin words. */
    static String stem(String w) {
        if (!w.chars().allMatch(c -> c < 128)) return w;
        for (String suffix : new String[]{"ings", "ing", "ies", "ers", "er", "ed", "es", "s"}) {
            if (w.endsWith(suffix) && w.length() - suffix.length() >= 3) {
                String base = w.substring(0, w.length() - suffix.length());
                return suffix.equals("ies") ? base + "y" : base;
            }
        }
        return w;
    }

    /**
     * Returns the similarity (0..1) of {@code query} to each document, in document order:
     * the average of TF-IDF cosine (do they talk about the same things, weighted by rarity) and
     * containment (what share of the idea's vocabulary the project already covers). Containment
     * keeps results stable when only a handful of projects exist and IDF is unreliable.
     */
    public static double[] similarities(String query, List<String> documents) {
        List<List<String>> docTokens = documents.stream().map(TextSimilarity::tokens).toList();
        List<String> qTokens = tokens(query);

        Map<String, Integer> df = new HashMap<>();
        for (List<String> d : docTokens) for (String t : new HashSet<>(d)) df.merge(t, 1, Integer::sum);
        int n = documents.size();

        Map<String, Double> q = tfidf(qTokens, df, n);
        Set<String> qSet = new HashSet<>(qTokens);
        double[] out = new double[documents.size()];
        for (int i = 0; i < docTokens.size(); i++) {
            double cosine = cosine(q, tfidf(docTokens.get(i), df, n));
            out[i] = (cosine + containment(qSet, new HashSet<>(docTokens.get(i)))) / 2;
        }
        return out;
    }

    private static double containment(Set<String> query, Set<String> doc) {
        if (query.isEmpty()) return 0;
        long shared = query.stream().filter(doc::contains).count();
        return (double) shared / query.size();
    }

    private static Map<String, Double> tfidf(List<String> tokens, Map<String, Integer> df, int n) {
        Map<String, Double> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1.0, Double::sum);
        tf.replaceAll((t, c) -> (1 + Math.log(c)) * (Math.log((n + 1.0) / (df.getOrDefault(t, 0) + 1.0)) + 1));
        return tf;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        double dot = 0;
        for (var e : a.entrySet()) dot += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        double na = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double nb = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        return Math.min(1.0, dot / (na * nb));
    }
}
