package com.majeur.hmaputils;

import com.majeur.hmaputils.ComputeMapsTask.Callbacks;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.toHexString;

public class MainFrame extends JFrame implements ActionListener {

    private static final int RG_MAP = 0;
    private static final int RELIEF_MAP = 1;
    private static final int RGB_MASK = 2;
    private static final int CUSTOM = 3;

    private static final NumberFormat DECIMAL_FORMAT;
    private static final MaskFormatter HEX_FORMAT;

    static {
        DECIMAL_FORMAT = NumberFormat.getNumberInstance(Locale.US);
        DECIMAL_FORMAT.setMinimumFractionDigits(2);
        try {
            HEX_FORMAT = new MaskFormatter("******");
            HEX_FORMAT.setValidCharacters("0123456789abcdefABCDEF");
            HEX_FORMAT.setPlaceholderCharacter('0');
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private File sourceFile;
    private File trackMaskFile;

    private JButton fileButton;
    private JTextField filePathText;
    private JButton startButton;
    private JCheckBox[] checkBoxes = new JCheckBox[Strings.CHECKBOX_LABELS.length];
    private JProgressBar progressBar;
    private JFormattedTextField reliefMultiplierText;
    private JFormattedTextField lowerRgbmBoundText, upperRgbmBoundText;
    private JButton trackMaskFileButton;
    private JTextField trackMaskPathText;
    private JFormattedTextField redColorText, greenColorText, blueColorText, blackColorText;

    public MainFrame() {
        super("Heightmap Utilities");
        setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(createInputPane());
        contentPanel.add(createOutputPane());
        contentPanel.add(createBottomPane());
        setContentPane(contentPanel);
        setSize(530, 650);
        setLocationRelativeTo(null);

        File[] files = HeightmapUtilities.getWorkingDir().listFiles();
        if (files != null)
        for (File file : files) {
            if (!file.isDirectory() && (file.getName().equals("heightmap.png")
                    || file.getName().equals("hmap.png"))) {
                sourceFile = file;
                filePathText.setText(file.getName() + " (Autodetected)");
                break;
            }
        }

        readParams();
    }

    private JPanel createInputPane() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Input"));
        JLabel label = new JLabel("Heightmap:");
        filePathText = new JTextField("", 20);
        label.setLabelFor(filePathText);
        panel.add(label);
        filePathText.setEditable(false);
        panel.add(filePathText);
        fileButton = new JButton("Choose File");
        fileButton.addActionListener(this);
        panel.add(fileButton);
        return panel;
    }

    private JPanel createOutputPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Output"));

        for (int which = 0; which < Strings.CHECKBOX_LABELS.length; which++) {
            JCheckBox checkBox = new JCheckBox(Strings.CHECKBOX_LABELS[which], which == RG_MAP);
            checkBoxes[which] = checkBox;
            checkBox.addActionListener(this);
            checkBox.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(checkBox);

            if (which == RELIEF_MAP) {
                JPanel subPanel = new JPanel();
                subPanel.setAlignmentX(LEFT_ALIGNMENT);
                JLabel label = new JLabel("Multiplier:");
                reliefMultiplierText = buildDoubleTextField(1.0, false);
                label.setLabelFor(reliefMultiplierText);
                subPanel.add(label);
                subPanel.add(reliefMultiplierText);
                panel.add(subPanel);
            } else if (which == RGB_MASK) {
                JPanel subPanel = new JPanel();
                subPanel.setAlignmentX(LEFT_ALIGNMENT);
                lowerRgbmBoundText = buildDoubleTextField(0.3, false);
                JLabel label = new JLabel("Lower bound:");
                label.setLabelFor(lowerRgbmBoundText);
                subPanel.add(label);
                subPanel.add(lowerRgbmBoundText);
                upperRgbmBoundText = buildDoubleTextField(0.6, false);
                label = new JLabel("Upper bound:");
                label.setLabelFor(upperRgbmBoundText);
                subPanel.add(label);
                subPanel.add(upperRgbmBoundText);
                label = new JLabel("Track mask:");
                trackMaskPathText = new JTextField("None", 12);
                trackMaskPathText.setEditable(false);
                trackMaskPathText.setEnabled(false);
                label.setLabelFor(trackMaskPathText);
                subPanel.add(label);
                subPanel.add(trackMaskPathText);
                trackMaskFileButton = new JButton("Choose");
                trackMaskFileButton.addActionListener(this);
                trackMaskFileButton.setEnabled(false);
                subPanel.add(trackMaskFileButton);
                panel.add(subPanel);
            } else if (which == CUSTOM) {
                JPanel subPanel = new JPanel();
                subPanel.setAlignmentX(LEFT_ALIGNMENT);

                redColorText = buildHexTextField("ff0000", false);
                JLabel label = new JLabel("Red:");
                label.setLabelFor(redColorText);
                subPanel.add(label);
                subPanel.add(redColorText);

                greenColorText = buildHexTextField("00ff00", false);
                label = new JLabel("Green:");
                label.setLabelFor(greenColorText);
                subPanel.add(label);
                subPanel.add(greenColorText);

                blueColorText = buildHexTextField("0000ff", false);
                label = new JLabel("Blue:");
                label.setLabelFor(blueColorText);
                subPanel.add(label);
                subPanel.add(blueColorText);

                blackColorText = buildHexTextField("000000", false);
                label = new JLabel("Black:");
                label.setLabelFor(blackColorText);
                subPanel.add(label);
                subPanel.add(blackColorText);

                panel.add(subPanel);
            }

            JTextArea descLabel = new JTextArea(Strings.CHECKBOX_DESCRS[which]);
            descLabel.setEditable(false);
            descLabel.setLineWrap(true);
            descLabel.setWrapStyleWord(true);
            descLabel.setFont(checkBox.getFont());
            descLabel.setBackground(panel.getBackground());
            descLabel.setAlignmentX(LEFT_ALIGNMENT);
            descLabel.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 0));
            panel.add(descLabel);
        }
        panel.setAlignmentX(CENTER_ALIGNMENT);
        return panel;
    }

    private JFormattedTextField buildDoubleTextField(double defValue, boolean enabled) {
        JFormattedTextField jftf = new JFormattedTextField(DECIMAL_FORMAT);
        jftf.setValue(defValue);
        jftf.setColumns(4);
        jftf.setEnabled(enabled);
        return jftf;
    }

    private JFormattedTextField buildHexTextField(String defValue, boolean enabled) {
        JFormattedTextField jftf = new JFormattedTextField(HEX_FORMAT);
        jftf.setValue(defValue);
        jftf.setColumns(6);
        jftf.setEnabled(enabled);
        return jftf;
    }

    private JPanel createBottomPane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        progressBar = new JProgressBar();
        progressBar.setEnabled(false);
        panel.add(progressBar, BorderLayout.CENTER);
        startButton = new JButton("Go !");
        startButton.addActionListener(this);
        panel.add(startButton, BorderLayout.EAST);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == fileButton) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setCurrentDirectory(sourceFile == null ? HeightmapUtilities.getWorkingDir() : sourceFile.getParentFile());
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                sourceFile = chooser.getSelectedFile();
                filePathText.setText(sourceFile.getName());
            }
        } else if (event.getSource() == checkBoxes[RELIEF_MAP]) {
            reliefMultiplierText.setEnabled(checkBoxes[RELIEF_MAP].isSelected());

        } else if (event.getSource() == checkBoxes[RGB_MASK]) {
            boolean checked = checkBoxes[RGB_MASK].isSelected();
            lowerRgbmBoundText.setEnabled(checked);
            upperRgbmBoundText.setEnabled(checked);
            trackMaskPathText.setEnabled(checked);
            trackMaskFileButton.setEnabled(checked);

            if (!checkBoxes[RELIEF_MAP].isSelected())
                checkBoxes[RELIEF_MAP].doClick();
            checkBoxes[RELIEF_MAP].setEnabled(!checkBoxes[RGB_MASK].isSelected());

        } else if (event.getSource() == trackMaskFileButton) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setCurrentDirectory(sourceFile == null ? HeightmapUtilities.getWorkingDir() : sourceFile.getParentFile());
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                trackMaskFile = chooser.getSelectedFile();
                trackMaskPathText.setText(trackMaskFile.getName());
            }
        } else if (event.getSource() == checkBoxes[CUSTOM]) {
            boolean checked = checkBoxes[CUSTOM].isSelected();
            redColorText.setEnabled(checked);
            greenColorText.setEnabled(checked);
            blueColorText.setEnabled(checked);
			blackColorText.setEnabled(checked);

            if (!checkBoxes[RGB_MASK].isSelected())
                checkBoxes[RGB_MASK].doClick();
            checkBoxes[RGB_MASK].setEnabled(!checkBoxes[CUSTOM].isSelected());

        } else if (event.getSource() == startButton) {
            startComputation(sourceFile, checkBoxes[RG_MAP].isSelected(),
                    checkBoxes[RELIEF_MAP].isSelected(),
                    parseDouble(reliefMultiplierText.getText()),
                    checkBoxes[RGB_MASK].isSelected(),
                    parseDouble(lowerRgbmBoundText.getText()),
                    parseDouble(upperRgbmBoundText.getText()),
                    trackMaskFile,
                    checkBoxes[CUSTOM].isSelected(),
                    parseInt(redColorText.getText(), 16),
                    parseInt(greenColorText.getText(), 16),
                    parseInt(blueColorText.getText(), 16),
                    parseInt(blackColorText.getText(), 16));

        }
    }

    private void startComputation(File src, boolean rgmap, boolean relief, double multiplier,
                                  boolean rgbm, double lbound, double ubound, File trackMask, boolean custom, int red,
                                  int green, int blue, int black) {
        saveParams(src, multiplier, lbound, ubound, trackMask, red, green, blue, black);
        ComputeMapsTask worker = new ComputeMapsTask(src, rgmap, relief, multiplier, rgbm, lbound, ubound, trackMask, custom,
                new int[]{red, green, blue, black});
        worker.setCallbacks(new Callbacks() {

            @Override
            public void onResult(String[] arr) {
                setWindowProgressCompat(false, false);
                String text = "Successfully generated:";
                for (String name : arr)
                    if (name != null) text += System.lineSeparator() + name;
                progressBar.setValue(progressBar.getMaximum());
                JOptionPane.showMessageDialog(MainFrame.this, text, "Done", JOptionPane.INFORMATION_MESSAGE);
                startButton.setEnabled(true);
                progressBar.setValue(0);
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void onProgress(int p, int total) {
                progressBar.setMaximum(total);
                progressBar.setValue(p);
            }

            @Override
            public void onError(String reason) {
                setWindowProgressCompat(true, true);
                JOptionPane.showMessageDialog(MainFrame.this, reason, "Error", JOptionPane.ERROR_MESSAGE);
                startButton.setEnabled(true);
                progressBar.setValue(0);
                setCursor(Cursor.getDefaultCursor());
                setWindowProgressCompat(false, false);
            }

        });
        progressBar.setEnabled(true);
        startButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setWindowProgressCompat(true, false);
        worker.execute();
    }

    public void saveParams(File srcFile, double rmultiplier, double lbound, double ubound, File trackMask,
						   int red, int green, int blue, int black) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                JSONObject json = new JSONObject();
                json.put("sourcehmap", srcFile != null ? srcFile.getAbsolutePath() : "");
                json.put("rmultiplier", rmultiplier);
                json.put("lbound", lbound);
                json.put("ubound", ubound);
                json.put("trackmask", trackMask != null ? trackMask.getAbsolutePath() : "");
                json.put("customred", to6Hex(red));
                json.put("customgreen", to6Hex(green));
                json.put("customblue", to6Hex(blue));
                json.put("customblack", to6Hex(black));
                File jsonFile = new File(HeightmapUtilities.getWorkingDir(), "config.json");
                try (FileWriter file = new FileWriter(jsonFile)) {
                    file.write(json.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            private String to6Hex(int c) {
            	String hex = toHexString(c);
            	return "000000".substring(hex.length()) + hex;
			}

            @Override
            public void done() {
                try {
                    get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    public void readParams() {
        SwingWorker<JSONObject, Void> worker = new SwingWorker<JSONObject, Void>() {

            @Override
            protected JSONObject doInBackground() throws Exception {
                JSONParser parser = new JSONParser();
                File jsonFile = new File(HeightmapUtilities.getWorkingDir(), "config.json");
                try (Reader reader = new FileReader(jsonFile)) {
                    return (JSONObject) parser.parse(reader);
                } catch (IOException | ParseException e) {
                    // Ignore
                    return null;
                }
            }

            @Override
            public void done() {
                try {
                    JSONObject json = get();
                    if (json == null) return;
                    String srcPath = (String) json.get("sourcehmap");
                    if (sourceFile == null) {
                        File lastFile = new File(srcPath);
                        if (lastFile.exists() && !lastFile.isDirectory()) {
                            sourceFile = lastFile;
                            filePathText.setText(lastFile.getName() + " (Last used)");
                        }
                    }
                    double rmultiplier = (Double) json.get("rmultiplier");
                    reliefMultiplierText.setText(Double.toString(rmultiplier));
                    double lbound = (Double) json.get("lbound");
                    lowerRgbmBoundText.setText(Double.toString(lbound));
                    double ubound = (Double) json.get("ubound");
                    upperRgbmBoundText.setText(Double.toString(ubound));
					String maskPath = (String) json.get("trackmask");
					if (trackMaskFile == null) {
						File lastFile = new File(maskPath);
						if (lastFile.exists() && !lastFile.isDirectory()) {
							trackMaskFile = lastFile;
							trackMaskPathText.setText(lastFile.getName());
						}
					}
                    String customred = (String) json.get("customred");
                    redColorText.setText(customred);
                    String customgreen = (String) json.get("customgreen");
					greenColorText.setText(customgreen);
                    String customblue = (String) json.get("customblue");
					blueColorText.setText(customblue);
                    String customblack = (String) json.get("customblack");
					blackColorText.setText(customblack);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void setWindowProgressCompat(boolean on, boolean error) {
        String valueName;
        if (!on) valueName = "OFF";
        else if (error) valueName = "ERROR";
        else valueName = "INDETERMINATE";

        try {
            Class<?> taskBarClass = Class.forName("java.awt.Taskbar");
            Class taskBarStateClass = Class.forName("java.awt.Taskbar$State");
            boolean isSupported = (boolean) taskBarClass.getMethod("isTaskbarSupported").invoke(null);
            if (!isSupported) return;
            Object taskBar = taskBarClass.getMethod("getTaskbar").invoke(null);
            Object param = Enum.valueOf(taskBarStateClass, valueName);
            taskBarClass.getMethod("setWindowProgressState", Window.class, taskBarStateClass)
                    .invoke(taskBar, MainFrame.this, param);
        } catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            // Silent ignore
        }
    }

}
