package com.setmydesk.desk;

import com.setmydesk.desk.DeskModels.Handedness;
import com.setmydesk.desk.DeskModels.ItemRequest;
import com.setmydesk.desk.DeskModels.Layout;
import com.setmydesk.desk.DeskModels.LayoutRequest;
import com.setmydesk.desk.DeskModels.Placement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.setmydesk.desk.DeskItemType.*;
import static org.assertj.core.api.Assertions.assertThat;

class DeskLayoutSolverTest {

    DeskLayoutSolver solver = new DeskLayoutSolver();

    static List<ItemRequest> items(DeskItemType... types) {
        return Arrays.stream(types).map(t -> new ItemRequest(t, null, null, null)).toList();
    }

    static Placement find(Layout l, DeskItemType t) {
        return l.placements().stream().filter(p -> p.type() == t).findFirst().orElseThrow();
    }

    @Test
    void everythingInsideTheDeskAndNothingOverlaps() {
        Layout l = solver.solve(new LayoutRequest(160, 80, Handedness.RIGHT,
                items(MONITOR, MONITOR, KEYBOARD, MOUSE, DESK_LAMP, PLANT, MUG, NOTEBOOK, SPEAKER, SPEAKER, PHONE_STAND, HEADPHONE_STAND)));

        assertThat(l.unplaced()).isEmpty();
        for (Placement p : l.placements()) {
            assertThat(p.x() - p.widthCm() / 2).isGreaterThanOrEqualTo(-80);
            assertThat(p.x() + p.widthCm() / 2).isLessThanOrEqualTo(80);
            assertThat(p.z() - p.depthCm() / 2).isGreaterThanOrEqualTo(-40);
            assertThat(p.z() + p.depthCm() / 2).isLessThanOrEqualTo(40);
            for (Placement q : l.placements()) {
                if (p == q) continue;
                boolean overlap = Math.abs(p.x() - q.x()) < (p.widthCm() + q.widthCm()) / 2
                        && Math.abs(p.z() - q.z()) < (p.depthCm() + q.depthCm()) / 2;
                assertThat(overlap).as(p.label() + " vs " + q.label()).isFalse();
            }
        }
    }

    @Test
    void ergonomicsFollowHandedness() {
        Layout right = solver.solve(new LayoutRequest(140, 70, Handedness.RIGHT, items(MONITOR, KEYBOARD, MOUSE, DESK_LAMP)));
        Layout left = solver.solve(new LayoutRequest(140, 70, Handedness.LEFT, items(MONITOR, KEYBOARD, MOUSE, DESK_LAMP)));

        assertThat(find(right, MONITOR).x()).isEqualTo(0);
        assertThat(find(right, MONITOR).z()).isLessThan(find(right, KEYBOARD).z());
        assertThat(find(right, MOUSE).x()).isGreaterThan(find(right, KEYBOARD).x());
        assertThat(find(left, MOUSE).x()).isLessThan(find(left, KEYBOARD).x());
        assertThat(find(right, DESK_LAMP).x()).isLessThan(0);
        assertThat(find(left, DESK_LAMP).x()).isGreaterThan(0);
    }

    @Test
    void dualMonitorsAreCentredAsAGroup() {
        Layout l = solver.solve(new LayoutRequest(180, 80, Handedness.RIGHT, items(MONITOR, MONITOR, KEYBOARD)));
        double sum = l.placements().stream().filter(p -> p.type() == MONITOR).mapToDouble(Placement::x).sum();
        assertThat(Math.abs(sum)).isLessThanOrEqualTo(1.0);
    }

    @Test
    void pinnedItemStaysExactlyWherePinned() {
        var req = new LayoutRequest(140, 70, Handedness.RIGHT, List.of(
                new ItemRequest(PLANT, -55.0, 20.0, null), new ItemRequest(MONITOR, null, null, null)));
        Layout l = solver.solve(req);
        Placement plant = find(l, PLANT);
        assertThat(plant.pinned()).isTrue();
        assertThat(plant.x()).isEqualTo(-55.0);
        assertThat(plant.z()).isEqualTo(20.0);
    }

    @Test
    void tinyDeskReportsWhatDoesNotFit() {
        Layout l = solver.solve(new LayoutRequest(60, 40, Handedness.RIGHT, items(MONITOR, MONITOR, KEYBOARD, DOCUMENT_TRAY)));
        assertThat(l.unplaced()).isNotEmpty();
        assertThat(l.ergonomicScore()).isLessThan(100);
    }

    @Test
    void sameInputSameLayout() {
        var req = new LayoutRequest(150, 75, Handedness.RIGHT, items(LAPTOP, KEYBOARD, MOUSE, MUG, PLANT));
        assertThat(solver.solve(req)).isEqualTo(solver.solve(req));
    }
}
