package com.majeur.hmaputils;

public class Strings {

    public static final String RG_LABEL = "Compute RG heightmap";
    public static final String RG_DESCR =
            ""
                    + "Create a 16bit heightmap using red and green 8bit channels. "
                    + "Allows 65536 levels of height instead of 256, "
                    + "which will prevent 'stairs' effect seen on heightmaps with high elevation.";

    public static final String RELIEF_LABEL = "Compute relief map";
    public static final String RELIEF_DESCR =
            ""
                    + "Create a map representing elevation variations where black is flat (or min slope) and white is max slope. "
                    + "Useful as a layer to view your track from the top, and paint various masks (textures, zones...)."
                    + System.lineSeparator()
                    + "If different than 1.00, multiplier will be applied to relief map to adjust its 'brightness'.";

    public static final String RGBM_LABEL = "Compute RGB mask";
    public static final String RGBM_DESCR =
            ""
                    + "Create an RGB mask from the relief map. High elevation variations will be set to blue channel, "
                    + "medium ones to red, and low ones to green. Typically you would then assign blue channel to a cliff texture, "
                    + "red to a hill texture and green to a regular flat ground texture."
                    + System.lineSeparator()
                    + "Warning: Alpha channel is set to 0 by default so image might not be visible. Load it in DE "
                    + "to see actual RGB mask."
                    + System.lineSeparator()
                    + "Lower bound and upper bound modify at which variation values whether blue, red or green are assigned. "
                    + "(Ex: if lower=0.3 and upper=0.6 -> 0.0 - green- 0.3 -red- 0.6 -blue- 1.0)"
                    + System.lineSeparator()
                    + "If a track mask is provided, black will be set accordingly to track mask red information. Alpha (wetness) "
                    + "will be set accordingly to track mask green information.";

    public static final String CUSTOMMAP_LABEL = "Compute custom color map";
    public static final String CUSTOMMAP_DESCR =
            ""
                    + "Remap RGB mask colors to custom colors."
                    + System.lineSeparator()
                    + "Useful to create various maps such as softness map or zone maps.";

    public static final String[] CHECKBOX_LABELS = {RG_LABEL, RELIEF_LABEL, RGBM_LABEL, CUSTOMMAP_LABEL};
    public static final String[] CHECKBOX_DESCRS = {RG_DESCR, RELIEF_DESCR, RGBM_DESCR, CUSTOMMAP_DESCR};
}
