package com.majeur.hmaputils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ComputeMapsTask extends SwingWorker<String[], Void> {

    public interface Callbacks {

        void onResult(String[] arr);

        void onProgress(int p, int total);

        void onError(String reason);
    }

    private static final String ERR_TAG = "error";

    private final File srcFile;
    private final boolean rgmap, relief, rgbm, custom;
    private final double rmultiplier, lbound, ubound;
    private final File trackMask;
    private final int[] replaceColors;

    private Callbacks callbacks;

    private int progress;

    public ComputeMapsTask(File srcFile, boolean rgmap, boolean relief, double rmultiplier,
                           boolean rgbm, double lbound, double ubound, File trackMask, boolean custom, int[] replaceColors) {
        super();
        this.srcFile = srcFile;
        this.rgmap = rgmap;
        this.relief = relief;
        this.rmultiplier = rmultiplier;
        this.rgbm = rgbm;
        this.lbound = lbound;
        this.ubound = ubound;
        this.trackMask = trackMask;
        this.custom = custom;
        this.replaceColors = replaceColors;
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    private void postProgress() {
        if (callbacks == null)
            throw new IllegalStateException("Callbacks must be bounded before executing worker.");
        int total = 2;
        if (rgmap) total += 1;
        if (relief) total += 1;
        if (rgbm && relief) total += 1;
        else total += 2;
        if (rgbm && trackMask != null) total += 1;
        if (custom) total += 1;
        final int ftotal = total;
        final int fprogress = ++progress;
        EventQueue.invokeLater(() -> callbacks.onProgress(fprogress, ftotal));
    }

    @Override
    public String[] doInBackground() {
        if (srcFile == null)
            return new String[]{ERR_TAG, "No input file."};

        BufferedImage sourceImage = readImage(srcFile);
        if (sourceImage == null)
            return new String[]{ERR_TAG, "Unable to read " + srcFile.getName()};

        if (sourceImage.getType() != BufferedImage.TYPE_USHORT_GRAY)
            return new String[]{ERR_TAG, "Input image must be a 16bit grayscaled no-alpha png."};

        String[] result = new String[4];
        short[] data = ((DataBufferUShort) sourceImage.getRaster().getDataBuffer()).getData();
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        HeightmapMath map = new HeightmapMath(width, height, width, data);
        postProgress();

        if (rgmap) {
            BufferedImage rgImage = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_RGB);
            WritableRaster raster = rgImage.getRaster();
            map.calculateRGImage();
            raster.setDataElements(0, 0, map.width, map.height, map.rgImageData);
            result[0] = writeImage("heightmap_rg.png", rgImage);
            rgImage.flush();
            map.releaseRGImageRef();
            postProgress();
        }

        if (relief || rgbm) {
            map.calculateGradients();
            map.calculateGradientNorm(rmultiplier);
            map.releaseGradientsRef();
            if (relief) {
                BufferedImage reliefImage = new BufferedImage(map.width, map.height, BufferedImage.TYPE_USHORT_GRAY);
                WritableRaster raster = reliefImage.getRaster();
                raster.setDataElements(0, 0, map.width, map.height, map.gradientNorm);
                result[1] = writeImage("heightmap_relief.png", reliefImage);
                reliefImage.flush();
            }
            postProgress();
        }

        if (rgbm) {
            if (ubound < lbound)
                return new String[]{ERR_TAG, "Upper bound cannot be lower than lower bound."};
            BufferedImage rgbmImage = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_ARGB);
            WritableRaster raster = rgbmImage.getRaster();
            map.calculateRGBMask(lbound, ubound);
            if (trackMask != null) {
                BufferedImage trackMaskImage = readImage(trackMask);
                if (trackMaskImage != null) {
                    if (trackMaskImage.getWidth() != map.width || trackMaskImage.getHeight() != map.height)
                        return new String[]{ERR_TAG, "Track mask size must be same as heightmap."};
                    map.applyTrackMask(trackMaskImage);
                    trackMaskImage.flush();
                }
                postProgress();
            }
            raster.setDataElements(0, 0, map.width, map.height, map.rgbMaskData);
            result[2] = writeImage("rgb_mask.png", rgbmImage);
            rgbmImage.flush();
            postProgress();
        }

        if (custom) {
            BufferedImage customImage = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_RGB);
            WritableRaster raster = customImage.getRaster();
            map.alterRgbMask(replaceColors[0], replaceColors[1], replaceColors[2], replaceColors[3]);
            raster.setDataElements(0, 0, map.width, map.height, map.rgbMaskData);
            result[3] = writeImage("custom_color_map.png", customImage);
            customImage.flush();
            postProgress();
        }

        return result;
    }

    private BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String writeImage(String fileName, BufferedImage im) {
        try {
            File file = new File(srcFile.getParent(), fileName);
            if (ImageIO.write(im, "png", file))
                return fileName;
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void done() {
        if (callbacks == null)
            throw new IllegalStateException("Callbacks must be bounded before executing worker.");

        String[] result;
        try {
            result = get();
        } catch (InterruptedException | ExecutionException e) {
            result = new String[]{ERR_TAG, e.getClass().getSimpleName() + System.lineSeparator() + e.getMessage()};
            e.printStackTrace();
        }

        if (ERR_TAG.equals(result[0])) {
            callbacks.onError(result[1]);
        } else {
            callbacks.onResult(result);
        }

        System.gc(); // Only way to make memory usage getting back to normal after a computation
    }

}
