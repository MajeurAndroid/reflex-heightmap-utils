package com.majeur.hmaputils;

import java.awt.image.BufferedImage;

public class HeightmapMath {

    private static final int USHORT_MAX_VALUE = 0xffff;

    public double[][] gradientX; // Variable to communicate the gradient in X direction with calling class
    public double[][] gradientY; // Variable to communicate the gradient in Y direction with calling class
    public short[] gradientNorm; // Variable to communicate the gradient norm of both directions with calling class
    public int[] rgImageData; // Variable to communicate the gradient in Y direction with calling class
    public int[] rgbMaskData; // Variable to communicate the gradient in Y direction with calling class


    public final short[] data; // Variable to store hmap data
    public final int width, height;
    private final int scansize;

    private int val; // temporary variable to store the height value of a pixel


    public HeightmapMath(int width, int height, int scansize, short[] data) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.scansize = scansize;
    }

    /**
     * Function that encode current heightmap values to R and G channels of an RGB 24bits image.
     * Stores results in {@link #rgImageData}.
     */
    public void calculateRGImage() {
        rgImageData = new int[height * width];

        for (int i = 0; i < height; ++i) {

            for (int j = 1; j < width; ++j) {

                extractValue(i, j);

                rgImageData[i * scansize + j] = val << 8;
            }
        }
    }

    public void releaseRGImageRef() {
        rgImageData = null;
    }

    /**
     * Function that calculates the gradient from the image given in argument using the central difference method.
     * Stores results in {@link #gradientX} and {@link #gradientY}.
     */
    public void calculateGradients() {
        gradientX = new double[height][width];
        gradientY = new double[height][width];

        int[] row = new int[height];
        int[] column = new int[width];

        for (int i = 0; i < height; ++i) {
            row[i] = i + 1;
        }

        for (int i = 0; i < width; ++i) {
            column[i] = i + 1;
        }

        if (height > 1) {
			/*
			  Calculate gradient for boundary pixels.
			 */
            for (int j = 0; j < width; ++j) {
                extractValue(1, j);
                int v2 = val;

                extractValue(0, j);
                int v1 = val;

                gradientX[0][j] = (double) -(v2 - v1) / (row[1] - row[0]);

                extractValue(height - 1, j);
                int vn = val;

                extractValue(height - 2, j);
                int vn1 = val;

                gradientX[height - 1][j] = (double) -(vn - vn1) / (row[height - 1] - row[height - 2]);
            }
        }

        if (height > 2) {
			/*
			  Calculate the gradient for central pixels.
			 */
            for (int i = 1; i < height - 1; ++i) {
                for (int j = 0; j < width; ++j) {
                    extractValue(i + 1, j);
                    int v2 = val;

                    extractValue(i - 1, j);
                    int v1 = val;

                    gradientX[i][j] = (double) -(v2 - v1) / (row[i + 1] - row[i - 1]);
                }
            }
        }

        if (width > 1) {
			/*
			  Calculate gradient for boundary pixels.
			 */
            for (int j = 0; j < height; ++j) {
                extractValue(j, 1);
                int v2 = val;

                extractValue(j, 0);
                int v1 = val;

                gradientY[j][0] = (double) (v2 - v1) / (column[1] - column[0]);

                extractValue(j, width - 1);
                int vn = val;

                extractValue(j, width - 2);
                int vn1 = val;

                gradientY[j][width - 1] = (double) (vn - vn1) / (column[width - 1] - column[width - 2]);
            }
        }

        if (width > 2) {
			/*
			  Calculate gradient for central pixels.
			 */
            for (int i = 0; i < height; ++i) {
                for (int j = 1; j < width - 1; ++j) {

                    extractValue(i, j + 1);
                    int v2 = val;

                    extractValue(i, j - 1);
                    int v1 = val;

                    gradientY[i][j] = (double) (v2 - v1) / (column[j + 1] - column[j - 1]);
                }
            }
        }
    }

    /**
     * Reads heightmap value for a given i (y) and a given j (x) and stores result
     * in {@link #val}.
     *
     * @param i Row index.
     * @param j Column index.
     */
    private void extractValue(int i, int j) {
        val = data[i * scansize + j] & 0xffff; // get unsigned short as int
    }

    public void releaseGradientsRef() {
        gradientX = null;
        gradientY = null;
    }

    /**
     * Function that calculates the euclidean norm of Gx(x, y) and Gy(x, y)
     * for each (x, y) scaled to a short.
     *
     * @param multiplier A multiplier that will be applied to computed norm of each point.
     */
    public void calculateGradientNorm(double multiplier) {
        gradientNorm = new short[width * height];

        double[][] arr = new double[width][height];
        double max = 0, min = 0;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
				/*
				  Calculate euclidean norm and extremums for scaling.
				 */
                arr[x][y] = Math.hypot(gradientY[y][x], gradientX[y][x]);

                if (arr[x][y] > max) {
                    max = arr[x][y];
                } else if (arr[x][y] < min) {
                    min = arr[x][y];
                }
            }
        }

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
				/*
				  Scaling into 0-65535 (unsigned short).
				 */

                gradientNorm[y * scansize + x] = (short) Math.min(
                        (arr[x][y] - min) / (max - min) * USHORT_MAX_VALUE * multiplier,
                        USHORT_MAX_VALUE
                );

            }
        }
    }

    /**
     * Function that calculates the RGB mask from relief map values.
     *
     * @param lowerBound
     * @param upperBound
     */
    public void calculateRGBMask(double lowerBound, double upperBound) {
        rgbMaskData = new int[width * height];
        int up = (int) Math.round(USHORT_MAX_VALUE * upperBound);
        int low = (int) Math.round(USHORT_MAX_VALUE * lowerBound);

        for (int y = 0; y < height; ++y) {

            for (int x = 0; x < width; ++x) {

                val = gradientNorm[y * scansize + x] & 0xffff; // get unsigned short as int

                if (val < low)
                    rgbMaskData[y * scansize + x] = 0x0000ff00; // green
                else if (val < up)
                    rgbMaskData[y * scansize + x] = 0x00ff0000; // red
                else
                    rgbMaskData[y * scansize + x] = 0x000000ff; // blue
            }
        }
    }

    /**
     * Function that set rgb mask pixel to black using red information, and alpha value using green
     * information from the track mask image.
     * <p>
     * This can be heavily optimized by accessing buffers, but getRGB()
     * is used to support any kind of image.
     *
     * @param im Track mask image
     */
    public void applyTrackMask(BufferedImage im) {
        int mask_val;
        for (int y = 0; y < height; ++y) {

            for (int x = 0; x < width; ++x) {
                mask_val = im.getRGB(x, y);

                if ((mask_val & 0x00ff0000) >> 16 > 0) { // if has red
                    mask_val = (mask_val & 0x00ff0000) >> 16; // extract red
                    mask_val = 255 - mask_val; // invert

                    val = rgbMaskData[y * scansize + x] & 0x00ffffff;

                    if (val == 0xff0000) // if red, set our inverted value to red channel and 0 to others
                        rgbMaskData[y * scansize + x] = mask_val << 16;
                    else if (val == 0x00ff00)
                        rgbMaskData[y * scansize + x] = mask_val << 8; // same if green
                    else if (val == 0x0000ff)
                        rgbMaskData[y * scansize + x] = mask_val; // same if blue

                } else if ((mask_val & 0x0000ff00) >> 8 > 0) { // if has green
                    mask_val = (mask_val & 0x0000ff00) >> 8; // extract green

                    rgbMaskData[y * scansize + x] |= mask_val << 24; // set our mask value to the alpha channel

                }

            }
        }
    }

    public void alterRgbMask(int red, int green, int blue, int black) {

        for (int y = 0; y < height; ++y) {

            for (int x = 0; x < width; ++x) {

                val = rgbMaskData[y * scansize + x];

                if (val == 0x00ff0000) {
                    rgbMaskData[y * scansize + x] = red;
                } else if (val == 0x0000ff00) {
                    rgbMaskData[y * scansize + x] = green;
                } else if (val == 0x000000ff) {
                    rgbMaskData[y * scansize + x] = blue;
                } else {
                    rgbMaskData[y * scansize + x] = black;
                }

            }
        }

    }

    @Override
    public String toString() {
        return "ImageMath [width=" + width + ", height=" + height + "]";
    }


}
