package me.av306.keybindsgaloreplus;

import java.util.ArrayList;

public class Configurations
{
    public static boolean DEBUG = false;

    // Performance

    public static boolean LAZY_CONFLICT_CHECK = true;
    public static int CIRCLE_VERTICES = 120;
    public static boolean PIE_MENU_BLEND = false;
    public static boolean DARKENED_BACKGROUND = true;
    public static boolean LABEL_TEXT_SHADOW = false;

    // Behaviour

    // Impl as list of ints to make config easier
    public static ArrayList<Integer> SKIPPED_KEYS = new ArrayList<>();
    public static boolean USE_KEYBIND_FIX = true;


    // Pie menu customisation

    public static float EXPANSION_FACTOR_WHEN_SELECTED = 0;
    public static int PIE_MENU_MARGIN = 0;
    public static float PIE_MENU_SCALE = 0.6f;
    public static float CANCEL_ZONE_SCALE = 0.25f;
    public static short PIE_MENU_COLOR = 0x40;
    //public static short PIE_MENU_COLOR_RED = 0x40;
    //public static short PIE_MENU_COLOR_GREEN = 0x40;
    //public static short PIE_MENU_COLOR_BLUE = 0x40;
    public static short PIE_MENU_HIGHLIGHT_COLOR = 0xFF;
    public static short PIE_MENU_COLOR_LIGHTEN_FACTOR = 0x19;
    public static short PIE_MENU_ALPHA = 0x60;
    public static boolean SECTOR_GRADATION = true;
    public static int LABEL_TEXT_INSET = 6;
    public static boolean ANIMATE_PIE_MENU = true;
}
