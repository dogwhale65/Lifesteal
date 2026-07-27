package nightfallmods.lifesteal;

public final class Constants {
    private Constants() {}

    public static final double HEART_VALUE       = 2.0;
    public static final int    DEFAULT_HEARTS     = 10;
    public static final int    MAX_HEARTS         = 20;
    public static final int    CRAFTED_HEART_CAP  = 10;
    public static final int    EGA_HEART_THRESHOLD = 12;
    public static final int    REVIVE_HEARTS      = 3;

    public static final int CHEST_3X9_SIZE  = 27;
    public static final int CHEST_6X9_SIZE  = 54;

    /**
     * The revive list is inset by one row on top and one column on each side, so the heads sit in
     * a 4x7 block over rows 1-4. Row 5 is the navigation row and doubles as the bottom padding —
     * there is deliberately no blank row between the heads and it.
     */
    public static final int GRID_ROWS       = 4;
    public static final int GRID_COLS       = 7;
    public static final int GRID_ROW_OFFSET = 1;
    public static final int GRID_COL_OFFSET = 1;
    public static final int ITEMS_PER_PAGE  = GRID_ROWS * GRID_COLS;

    /** Container slot backing the {@code index}-th head on a page (0 .. ITEMS_PER_PAGE - 1). */
    public static int gridSlot(int index) {
        return (GRID_ROW_OFFSET + index / GRID_COLS) * 9 + GRID_COL_OFFSET + index % GRID_COLS;
    }

    public static final int SLOT_YES_BUTTON = 11;
    public static final int SLOT_CONFIRM_HEAD = 13;
    public static final int SLOT_NO_BUTTON  = 15;

    public static final int SLOT_PREV_PAGE  = 45;
    public static final int SLOT_PAGE_INFO  = 49;
    public static final int SLOT_SORT       = 51;
    public static final int SLOT_NEXT_PAGE  = 53;

    public static final String SOUND_ELIMINATION = "minecraft:entity.wither.spawn";
    public static final String SOUND_HEART_EQUIP = "minecraft:block.respawn_anchor.charge";
    public static final String SOUND_HEART_DEATH = "minecraft:block.respawn_anchor.deplete";

    public static final String MOD_ID = "lifesteal";
}
