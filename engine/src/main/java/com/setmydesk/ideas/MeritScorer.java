package com.setmydesk.ideas;

import com.setmydesk.core.TextSimilarity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Scores the idea itself — never who sent it, when, or how. Deterministic: the same words always
 * earn the same score, so nobody gets more for being early or less for being late.
 */
@Component
public class MeritScorer {

    static final double FLOOR = 0.2;

    private static final Set<String> PROBLEM = stems("problem", "issue", "struggle", "difficult", "hard", "pain",
            "need", "cannot", "can't", "lack", "missing", "unable", "expensive", "slow", "unsafe", "waste", "challenge");
    private static final Set<String> SOLUTION = stems("app", "build", "create", "platform", "tool", "service", "system",
            "solution", "help", "allow", "enable", "connect", "track", "automate", "provide", "offer", "share", "teach", "learn");
    private static final Set<String> AUDIENCE = stems("women", "woman", "mother", "mothers", "student", "students", "user",
            "users", "people", "customer", "customers", "farmer", "farmers", "village", "teacher", "teachers", "elderly",
            "children", "family", "families", "worker", "workers", "business", "community", "girls", "patients");

    public MeritScore score(String text) {
        List<String> tokens = TextSimilarity.tokens(text);
        long words = text.trim().split("\\s+").length;
        long sentences = Math.max(1, text.split("[.!?।\\n]+\\s*").length);

        double detail = Math.min(words / 80.0, 1.0);
        double problem = tokens.stream().anyMatch(PROBLEM::contains) || text.toLowerCase().contains("can't") ? 1 : 0;
        double solution = tokens.stream().anyMatch(SOLUTION::contains) ? 1 : 0;
        double audience = tokens.stream().anyMatch(AUDIENCE::contains) ? 1 : 0;
        double structure = Math.min(sentences / 3.0, 1.0);

        double weighted = 0.30 * detail + 0.25 * problem + 0.20 * solution + 0.15 * audience + 0.10 * structure;
        double total = FLOOR + (1 - FLOOR) * weighted;
        return new MeritScore(r(detail), problem, solution, audience, r(structure), r(total));
    }

    private static Set<String> stems(String... words) {
        return Set.copyOf(java.util.Arrays.stream(words).flatMap(w -> TextSimilarity.tokens(w).stream()).toList());
    }

    private static double r(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
