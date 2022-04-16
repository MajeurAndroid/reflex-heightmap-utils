package com.majeur.hmaputils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class HeightmapUtilities implements Runnable {

    public static void main(String... args) {
        EventQueue.invokeLater(new HeightmapUtilities());
    }

    public static File getWorkingDir() {
        try {
            URI uri = HeightmapUtilities.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return new File(uri).getParentFile();
        } catch (URISyntaxException e) {
            // This should not append
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            // Ignore
        }
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }
}
