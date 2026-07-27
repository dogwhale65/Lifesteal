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

    // The revive list is inset by one slot on the left, right and top. The bottom edge is the
    // navigation row itself, so the heads run right up against it with no gap.
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
