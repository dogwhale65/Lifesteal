package nightfallmods.lifesteal;

public final class Constants {
    private Constants() {}

    public static final double HEART_VALUE       = 2.0;
    public static final int    DEFAULT_HEARTS     = 10;
    public static final int    MAX_HEARTS         = 20;
    public static final int    MIN_HEARTS         = 1;
    public static final int    CRAFTED_HEART_CAP  = 10;
    public static final int    EGA_HEART_THRESHOLD = 12;
    public static final int    REVIVE_HEARTS      = 3;

    public static final int GRACE_PERIOD_SECONDS = 1800;

    public static final int CHEST_3X9_SIZE  = 27;
    public static final int CHEST_6X9_SIZE  = 54;
    public static final int CHEST_COLUMNS   = 9;

    /**
     * The revive menu keeps a one-slot border of filler around the player heads: row 0 on top,
     * columns 0 and 8 on the sides, and the navigation row (row 5) closing off the bottom.
     */
    public static final int CONTENT_FIRST_ROW = 1;
    public static final int CONTENT_FIRST_COL = 1;
    public static final int CONTENT_ROWS      = 4;
    public static final int CONTENT_COLS      = 7;
    public static final int ITEMS_PER_PAGE    = CONTENT_ROWS * CONTENT_COLS;

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

    /** Maps a 0-based index on the current page to its slot in the 6x9 container. */
    public static int contentSlot(int index) {
        int row = CONTENT_FIRST_ROW + index / CONTENT_COLS;
        int col = CONTENT_FIRST_COL + index % CONTENT_COLS;
        return row * CHEST_COLUMNS + col;
    }
}
