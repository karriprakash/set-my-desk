package com.setmydesk.shares;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShareService {

    private final ShareEntryRepository ledger;

    public ShareService(ShareEntryRepository ledger) {
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public CapTable capTable(Long projectId) {
        List<ShareEntry> entries = ledger.findByProjectIdOrderByCreatedAtAsc(projectId);
        double total = entries.stream().mapToDouble(ShareEntry::getPoints).sum();

        record Acc(String name, double points, int count) {}
        Map<Long, Acc> byContributor = new LinkedHashMap<>();
        for (ShareEntry e : entries) {
            byContributor.merge(e.getContributor().getId(),
                    new Acc(e.getContributor().getDisplayName(), e.getPoints(), 1),
                    (a, b) -> new Acc(a.name(), a.points() + b.points(), a.count() + 1));
        }
        List<CapTable.Holder> holders = byContributor.entrySet().stream()
                .map(en -> new CapTable.Holder(en.getKey(), en.getValue().name(), round(en.getValue().points()),
                        total == 0 ? 0 : round(100 * en.getValue().points() / total), en.getValue().count()))
                .sorted(Comparator.comparingDouble(CapTable.Holder::points).reversed())
                .toList();
        return new CapTable(projectId, round(total), holders);
    }

    static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
