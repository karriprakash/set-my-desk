package com.setmydesk.shares;

import java.util.List;

/** Who holds what share of a project. Percentages always add up to 100. */
public record CapTable(Long projectId, double totalPoints, List<Holder> holders) {
    public record Holder(Long contributorId, String name, double points, double percent, int contributions) {
    }
}
