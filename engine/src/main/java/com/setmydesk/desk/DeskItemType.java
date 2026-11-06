package com.setmydesk.desk;

/**
 * Catalogue of desk items with real-world footprints (cm). Declaration order is the placement order:
 * screens first, then input devices, then everything else. Anchors are for a right-handed person
 * (x: -1 left … +1 right, z: -1 back … +1 front) and are mirrored for left-handed users.
 */
public enum DeskItemType {
    MONITOR("Monitor (24\")", 61, 20, 45, "#1f2937", 0, -1),
    LAPTOP("Laptop", 32, 22, 2, "#9ca3af", -0.7, -0.4),
    KEYBOARD("Keyboard", 44, 14, 3, "#374151", 0, 1),
    MOUSE("Mouse", 7, 12, 4, "#4b5563", 0.4, 0.6),
    SPEAKER("Speaker", 10, 15, 20, "#111827", 0, -1),
    DESK_LAMP("Desk lamp", 15, 15, 45, "#f59e0b", -1, -1),
    TABLET("Tablet", 25, 18, 1, "#6b7280", -0.6, 0.1),
    NOTEBOOK("Notebook", 21, 30, 2, "#2563eb", -0.85, 0.5),
    PHONE_STAND("Phone stand", 8, 10, 12, "#a855f7", -0.75, -0.3),
    DOCUMENT_TRAY("Document tray", 25, 33, 8, "#78716c", -1, -0.6),
    HEADPHONE_STAND("Headphone stand", 12, 12, 25, "#0f766e", 0.95, -0.95),
    PLANT("Plant", 15, 15, 30, "#16a34a", 1, -1),
    MUG("Mug", 9, 9, 10, "#dc2626", 0.85, 0.3);

    public final String label;
    public final int widthCm;
    public final int depthCm;
    public final int heightCm;
    public final String color;
    public final double anchorX;
    public final double anchorZ;

    DeskItemType(String label, int widthCm, int depthCm, int heightCm, String color, double anchorX, double anchorZ) {
        this.label = label;
        this.widthCm = widthCm;
        this.depthCm = depthCm;
        this.heightCm = heightCm;
        this.color = color;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
    }
}
