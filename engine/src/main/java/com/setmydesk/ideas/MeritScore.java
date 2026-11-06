package com.setmydesk.ideas;

/**
 * Transparent breakdown of how good an idea's description is. Every component is 0..1 and
 * published at /api/rules so contributors can see exactly why they received what they did.
 */
public record MeritScore(double detail, double problem, double solution, double audience, double structure, double total) {
}
