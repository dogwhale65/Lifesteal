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
    public static final int CHEST_COLUMNS   = 9;

    // Heads occupy rows 1-4, columns 1-7: a filler row along the top and a filler column down each
    // side. The bottom row is the navigation row, so no filler sits between it and the last heads.
    public static final int HEAD_FIRST_ROW = 1;
    public static final int HEAD_LAST_ROW  = 4;
    public static final int HEAD_FIRST_COL = 1;
    public static final int HEAD_LAST_COL  = 7;

    public static final int HEAD_ROWS = HEAD_LAST_ROW - HEAD_FIRST_ROW + 1;
    public static final int HEAD_COLS = HEAD_LAST_COL - HEAD_FIRST_COL + 1;

    public static final int ITEMS_PER_PAGE = HEAD_ROWS * HEAD_COLS;

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
