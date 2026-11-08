package com.setmydesk.desk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DeskModels {
    private DeskModels() {
    }

    public enum Handedness { RIGHT, LEFT }

    /**
     * One item to place. Give {@code x}/{@code z} (cm from desk centre; +z towards you) to pin it to an
     * exact spot — the solver respects pins and arranges everything else around them.
     */
    public record ItemRequest(@NotNull DeskItemType type, Double x, Double z, Boolean rotated) {
        public boolean pinned() {
            return x != null && z != null;
        }
    }

    public record LayoutRequest(
            @DecimalMin("60") @DecimalMax("400") double deskWidthCm,
            @DecimalMin("40") @DecimalMax("200") double deskDepthCm,
            Handedness handedness,
            @NotEmpty @Size(max = 30) List<@Valid ItemRequest> items) {
        public Handedness hand() {
            return handedness == null ? Handedness.RIGHT : handedness;
        }
    }

    public record Placement(int index, DeskItemType type, String label, double x, double z,
                            double widthCm, double depthCm, double heightCm, boolean rotated, boolean pinned, String color) {
    }

    public record Layout(double deskWidthCm, double deskDepthCm, Handedness handedness, List<Placement> placements,
                         List<String> unplaced, List<String> warnings, int ergonomicScore, double usedAreaPercent) {
    }

    public record CatalogItem(DeskItemType type, String label, int widthCm, int depthCm, int heightCm, String color) {
    }
}
