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
     * The revive list keeps a one-slot border of filler on the top, left, and right; the navigation
     * row doubles as the bottom border, so heads stop one row short of it with no gap in between.
     */
    public static final int GRID_FIRST_ROW  = 1;
    public static final int GRID_FIRST_COL  = 1;
    public static final int GRID_ROWS       = 4;
    public static final int GRID_COLS       = 7;
    public static final int ITEMS_PER_PAGE  = GRID_ROWS * GRID_COLS;

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
