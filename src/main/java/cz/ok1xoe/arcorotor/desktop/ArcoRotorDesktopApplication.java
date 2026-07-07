package cz.ok1xoe.arcorotor.desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

public class ArcoRotorDesktopApplication {

    private static final Color APP_BACKGROUND = new Color(229, 236, 238);
    private static final Color PANEL_BACKGROUND = new Color(247, 250, 250);
    private static final Color TEXT_PRIMARY = new Color(25, 34, 40);
    private static final Color TEXT_SECONDARY = new Color(82, 96, 106);
    private static final Color BORDER_COLOR = new Color(188, 202, 208);
    private static final Color TARGET_BLUE = new Color(38, 112, 206);
    private static final Color HEADING_RED = new Color(214, 70, 52);
    private static final Color ACTION_GREEN = new Color(40, 132, 97);
    private static final Color WARNING_RED = new Color(178, 54, 50);
    private static final Color BUTTON_BACKGROUND = new Color(252, 253, 253);
    private static final float UI_FONT_SIZE = 15f;
    private static final float BUTTON_FONT_SIZE = 15f;
    private static final long POLL_INTERVAL_MS = 150;
    private static final int MAX_TARGET_AZIMUTH = 360;
    private static final int MAX_SCAN_HOSTS = 1024;
    private static final int SCAN_THREAD_COUNT = 16;
    private static final int MAX_COMMUNICATION_LOG_ENTRIES = 80;
    private static final int MIN_AZIMUTH_MAP_DISTANCE_KM = 1000;
    private static final int MAX_AZIMUTH_MAP_DISTANCE_KM = 20000;
    private static final int AZIMUTH_MAP_DISTANCE_STEP_KM = 500;
    private static final int DEFAULT_AZIMUTH_MAP_DISTANCE_KM = 17000;
    private static final double EARTH_HALF_CIRCUMFERENCE_KM = 20015.0868;
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String OLD_PREFERENCES_NODE = "/cz/ok1xoe/macrotor/desktop";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ArcoRotorFrame frame = new ArcoRotorFrame();
            frame.setVisible(true);
        });
    }

    private static final class ArcoRotorFrame extends JFrame {

        private final JComboBox<String> hostCombo = new JComboBox<>();
        private final JTextField portField = new JTextField("4001", 6);
        private final JComboBox<Language> languageCombo = new JComboBox<>(Language.values());
        private final JButton scanButton = new JButton("Scan");
        private final JButton connectButton = new JButton("Pripojit");
        private final JLabel statusLabel = new JLabel("");
        private final JLabel ipLabel = createFormLabel("");
        private final JLabel tcpPortLabel = createFormLabel("");
        private final JLabel languageLabel = createFormLabel("");
        private final JLabel locatorLabel = createFormLabel("");
        private final JTextField locatorField = new JTextField(10);
        private final JCheckBox graylineCheckBox = new JCheckBox();
        private final JLabel mapTypeLabel = createFormLabel("");
        private final JComboBox<MapStyle> mapTypeCombo = new JComboBox<>(MapStyle.values());
        private final JLabel mapDistanceLabel = createFormLabel("");
        private final JSpinner mapDistanceSpinner = new JSpinner(new SpinnerNumberModel(
                DEFAULT_AZIMUTH_MAP_DISTANCE_KM,
                MIN_AZIMUTH_MAP_DISTANCE_KM,
                MAX_AZIMUTH_MAP_DISTANCE_KM,
                AZIMUTH_MAP_DISTANCE_STEP_KM
        ));
        private final JButton generateMapButton = new JButton();
        private final JButton clearMapButton = new JButton();
        private final JLabel captionLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel hintLabel = new JLabel("");
        private final JTextArea communicationLogArea = new JTextArea(5, 80);
        private final JMenu settingsMenu = new JMenu();
        private final JMenuItem connectionSettingsMenuItem = new JMenuItem();
        private final JMenu windowsMenu = new JMenu();
        private final JMenuItem tcpCommunicationMenuItem = new JMenuItem();
        private final JLabel headingLabel = new JLabel("---", SwingConstants.CENTER);
        private final JTextField headingEditField = new JTextField(4);
        private final JPanel headingCards = new JPanel(new CardLayout());
        private final CompassPanel compassPanel = new CompassPanel(this::moveToClickedAzimuth);
        private final JButton ccwButton = new JButton("CCW");
        private final JButton cwButton = new JButton("CW");
        private final JButton stopButton = new JButton("STOP");
        private final JLabel speedLabel = createFormLabel("");
        private final JSlider speedSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 4, 4);
        private final JLabel targetAzimuthValueLabel = new JLabel("---", SwingConstants.CENTER);
        private final PresetStore presetStore = new PresetStore();
        private final PresetButton[] presetButtons = new PresetButton[10];
        private final List<String> communicationLogEntries = new ArrayList<>();
        private final TitledBorder connectionBorder = BorderFactory.createTitledBorder("");
        private final TitledBorder currentAzimuthBorder = BorderFactory.createTitledBorder("");
        private final TitledBorder targetAzimuthBorder = BorderFactory.createTitledBorder("");
        private final TitledBorder manualControlBorder = BorderFactory.createTitledBorder("");
        private final TitledBorder presetsBorder = BorderFactory.createTitledBorder("");

        private ScheduledExecutorService executor;
        private ScheduledFuture<?> pollingTask;
        private TcpRotorClient client;
        private final Preferences windowPreferences = preferencesNode("window");
        private I18n i18n;
        private volatile boolean connected;
        private Integer currentHeading;
        private Integer targetHeading;
        private PresetButton activePresetButton;
        private boolean refreshHeadingAfterRelativeError;
        private volatile boolean scanning;
        private ExecutorService scanExecutor;
        private final CommunicationLogWindow communicationLogWindow;

        ArcoRotorFrame() {
            super();
            this.i18n = new I18n(Language.fromCode(windowPreferences.get("language", Language.EN.code())));
            this.communicationLogWindow = new CommunicationLogWindow();
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(1080, 680));
            setLocationByPlatform(true);
            setJMenuBar(createApplicationMenuBar());

            JPanel root = new JPanel(new BorderLayout(18, 18));
            root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            root.setBackground(APP_BACKGROUND);
            setContentPane(root);

            root.add(createTopPanel(), BorderLayout.NORTH);
            root.add(createCenterPanel(), BorderLayout.CENTER);
            root.add(createBottomPanel(), BorderLayout.SOUTH);

            hostCombo.setEditable(true);
            String savedHost = windowPreferences.get("host", "").trim();
            if (!savedHost.isBlank()) {
                hostCombo.addItem(savedHost);
            }
            hostCombo.setSelectedItem(savedHost);
            hostCombo.setFont(hostCombo.getFont().deriveFont(UI_FONT_SIZE));
            hostCombo.setPreferredSize(new Dimension(260, 44));
            portField.setHorizontalAlignment(SwingConstants.RIGHT);
            portField.setFont(portField.getFont().deriveFont(UI_FONT_SIZE));
            portField.setPreferredSize(new Dimension(110, 44));
            languageCombo.setFont(languageCombo.getFont().deriveFont(UI_FONT_SIZE));
            locatorField.setFont(locatorField.getFont().deriveFont(UI_FONT_SIZE));
            locatorField.setPreferredSize(new Dimension(120, 36));
            locatorField.setText(windowPreferences.get("locator", "").trim());
            graylineCheckBox.setFont(graylineCheckBox.getFont().deriveFont(UI_FONT_SIZE));
            graylineCheckBox.setOpaque(false);
            graylineCheckBox.setSelected(windowPreferences.getBoolean("showGrayline", true));
            compassPanel.setGraylineVisible(graylineCheckBox.isSelected());
            mapTypeCombo.setFont(mapTypeCombo.getFont().deriveFont(UI_FONT_SIZE));
            mapTypeCombo.setPreferredSize(new Dimension(118, 36));
            mapTypeCombo.setSelectedItem(MapStyle.fromCode(windowPreferences.get("mapStyle", MapStyle.PHYSICAL.code())));
            configureMapTypeRenderer();
            mapDistanceSpinner.setFont(mapDistanceSpinner.getFont().deriveFont(UI_FONT_SIZE));
            mapDistanceSpinner.setPreferredSize(new Dimension(96, 36));
            mapDistanceSpinner.setValue(savedMapDistanceKilometers());
            JSpinner.NumberEditor distanceEditor = new JSpinner.NumberEditor(mapDistanceSpinner, "0");
            distanceEditor.getTextField().setColumns(5);
            distanceEditor.getTextField().setFont(distanceEditor.getTextField().getFont().deriveFont(UI_FONT_SIZE));
            mapDistanceSpinner.setEditor(distanceEditor);

            connectButton.addActionListener(event -> toggleConnection());
            scanButton.addActionListener(event -> scanNetworkForArco());
            generateMapButton.addActionListener(event -> generateAzimuthMapFromLocator());
            clearMapButton.addActionListener(event -> clearAzimuthMap());
            locatorField.addActionListener(event -> generateAzimuthMapFromLocator());
            mapTypeCombo.addActionListener(event -> changeMapStyle());
            languageCombo.setSelectedItem(i18n.language());
            languageCombo.addActionListener(event -> changeLanguage((Language) languageCombo.getSelectedItem()));
            speedSlider.setValue(clampRotationSpeed(windowPreferences.getInt("rotationSpeed", 4)));
            speedSlider.addChangeListener(event -> changeRotationSpeed());
            updateTexts();
            restoreAzimuthMap();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    stopScanExecutor();
                    disconnect();
                    communicationLogWindow.dispose();
                }
            });

            pack();
        }

        private JMenuBar createApplicationMenuBar() {
            JMenuBar menuBar = new JMenuBar();
            connectionSettingsMenuItem.addActionListener(event -> showSettingsDialog());
            settingsMenu.add(connectionSettingsMenuItem);
            menuBar.add(settingsMenu);
            tcpCommunicationMenuItem.addActionListener(event -> showCommunicationLogWindow());
            windowsMenu.add(tcpCommunicationMenuItem);
            menuBar.add(windowsMenu);
            return menuBar;
        }

        private JPanel createTopPanel() {
            JPanel connectionPanel = createSectionPanel(connectionBorder, new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 0, 0, 10);
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.CENTER;

            constraints.gridx = 0;
            styleCommandButton(connectButton, ACTION_GREEN, Color.WHITE);
            connectionPanel.add(connectButton, constraints);

            constraints.gridx = 1;
            constraints.gridwidth = 1;
            constraints.weightx = 1;
            JPanel mapSpacer = new JPanel();
            mapSpacer.setOpaque(false);
            connectionPanel.add(mapSpacer, constraints);

            return connectionPanel;
        }

        private JPanel createSectionPanel(TitledBorder border, LayoutManager layout) {
            JPanel panel = new JPanel(layout);
            panel.setBackground(PANEL_BACKGROUND);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    border,
                    BorderFactory.createEmptyBorder(8, 10, 10, 10)
            ));
            border.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) UI_FONT_SIZE));
            return panel;
        }

        private JPanel createManualRotatePanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 2));
            panel.setOpaque(false);

            configureManualRotateButton(ccwButton, false);
            configureManualRotateButton(cwButton, true);
            styleCommandButton(stopButton, WARNING_RED, Color.WHITE);
            stopButton.setPreferredSize(new Dimension(140, 70));
            stopButton.setFont(stopButton.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE));
            stopButton.addActionListener(event -> stopRotation());

            JPanel buttonsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
            buttonsPanel.setOpaque(false);
            buttonsPanel.add(ccwButton);
            buttonsPanel.add(stopButton);
            buttonsPanel.add(cwButton);

            speedSlider.setMajorTickSpacing(1);
            speedSlider.setPaintTicks(true);
            speedSlider.setPaintLabels(true);
            speedSlider.setSnapToTicks(true);
            speedSlider.setOpaque(false);
            speedSlider.setFont(speedSlider.getFont().deriveFont(UI_FONT_SIZE));
            speedSlider.setPreferredSize(new Dimension(210, 42));

            JPanel speedPanel = new JPanel(new BorderLayout(8, 0));
            speedPanel.setOpaque(false);
            speedPanel.add(speedLabel, BorderLayout.WEST);
            speedPanel.add(speedSlider, BorderLayout.CENTER);

            panel.add(buttonsPanel, BorderLayout.NORTH);
            panel.add(speedPanel, BorderLayout.SOUTH);

            return panel;
        }

        private void configureManualRotateButton(JButton button, boolean clockwise) {
            button.setPreferredSize(new Dimension(140, 70));
            button.setFont(button.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBackground(TARGET_BLUE);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(TARGET_BLUE.darker()),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        startManualRotation(clockwise);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        stopManualRotation();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    if (button.getModel().isPressed()) {
                        stopManualRotation();
                    }
                }
            });
        }

        private static JLabel createFormLabel(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(TEXT_SECONDARY);
            label.setFont(label.getFont().deriveFont(Font.BOLD, UI_FONT_SIZE));
            return label;
        }

        private static void styleCommandButton(JButton button, Color background, Color foreground) {
            button.setOpaque(true);
            button.setBackground(background);
            button.setForeground(foreground);
            button.setFont(button.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE));
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(background.darker()),
                    BorderFactory.createEmptyBorder(7, 14, 7, 14)
            ));
        }

        private void changeLanguage(Language language) {
            if (language == null || language == i18n.language()) {
                return;
            }
            i18n = new I18n(language);
            windowPreferences.put("language", language.code());
            updateTexts();
        }

        private void updateTexts() {
            setTitle(t("app.title"));
            settingsMenu.setText(t("menu.settings"));
            connectionSettingsMenuItem.setText(t("menu.connectionSettings"));
            windowsMenu.setText(t("menu.windows"));
            tcpCommunicationMenuItem.setText(t("menu.tcpCommunication"));
            ipLabel.setText(t("label.ip"));
            tcpPortLabel.setText(t("label.tcpPort"));
            languageLabel.setText(t("label.language"));
            locatorLabel.setText(t("label.locator"));
            graylineCheckBox.setText(t("label.showGrayline"));
            mapTypeLabel.setText(t("label.mapType"));
            mapDistanceLabel.setText(t("label.mapDistance"));
            connectionBorder.setTitle(t("label.connection"));
            currentAzimuthBorder.setTitle(t("label.currentAzimuth"));
            targetAzimuthBorder.setTitle(t("label.targetAzimuth"));
            manualControlBorder.setTitle(t("label.manualControl"));
            presetsBorder.setTitle(t("label.presets"));
            communicationLogWindow.updateTexts();
            scanButton.setText(t("button.scan"));
            generateMapButton.setText(t("button.generateMap"));
            clearMapButton.setText(t("button.clearMap"));
            connectButton.setText(connected ? t("button.disconnect") : t("button.connect"));
            ccwButton.setText("<html><center>&#9664;&#9664;<br>" + t("button.ccw") + "</center></html>");
            cwButton.setText("<html><center>&#9654;&#9654;<br>" + t("button.cw") + "</center></html>");
            stopButton.setText(t("button.stop"));
            speedLabel.setText(t("label.speed"));
            captionLabel.setText(t("label.currentAzimuth"));
            headingLabel.setToolTipText(t("tooltip.heading"));
            hintLabel.setText(t("hint.main"));
            compassPanel.setCardinalLabels(t("compass.north"), t("compass.east"), t("compass.south"), t("compass.west"));
            updateConnectionStatus();
            updateTargetHeadingLabel();
            for (PresetButton presetButton : presetButtons) {
                if (presetButton != null) {
                    presetButton.refreshText();
                }
            }
            mapTypeCombo.repaint();
            updateStatusVisibility();
            repaint();
        }

        private void configureMapTypeRenderer() {
            mapTypeCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        javax.swing.JList<?> list,
                        Object value,
                        int index,
                        boolean isSelected,
                        boolean cellHasFocus
                ) {
                    Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof MapStyle mapStyle) {
                        setText(t(mapStyle.labelKey()));
                    }
                    return component;
                }
            });
        }

        private void changeMapStyle() {
            MapStyle mapStyle = selectedMapStyle();
            windowPreferences.put("mapStyle", mapStyle.code());
            String locator = locatorField.getText().trim();
            if (!locator.isBlank()) {
                generateAzimuthMapFromLocator();
            }
        }

        private void showSettingsDialog() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(PANEL_BACKGROUND);
            panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(6, 6, 6, 6);
            constraints.anchor = GridBagConstraints.WEST;

            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(ipLabel, constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(hostCombo, constraints);

            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            panel.add(tcpPortLabel, constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(portField, constraints);

            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            styleCommandButton(scanButton, TARGET_BLUE, Color.WHITE);
            panel.add(scanButton, constraints);

            constraints.gridx = 0;
            constraints.gridy = 3;
            panel.add(languageLabel, constraints);

            constraints.gridx = 1;
            languageCombo.setPreferredSize(new Dimension(110, 44));
            panel.add(languageCombo, constraints);

            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            panel.add(mapTypeLabel, constraints);

            constraints.gridx = 1;
            panel.add(mapTypeCombo, constraints);

            constraints.gridx = 0;
            constraints.gridy = 5;
            panel.add(mapDistanceLabel, constraints);

            constraints.gridx = 1;
            panel.add(mapDistanceSpinner, constraints);

            constraints.gridx = 0;
            constraints.gridy = 6;
            panel.add(locatorLabel, constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(locatorField, constraints);

            JPanel mapButtonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
            mapButtonPanel.setOpaque(false);
            styleCommandButton(generateMapButton, TARGET_BLUE, Color.WHITE);
            styleCommandButton(clearMapButton, BUTTON_BACKGROUND, TEXT_PRIMARY);
            mapButtonPanel.add(generateMapButton);
            mapButtonPanel.add(clearMapButton);

            constraints.gridx = 1;
            constraints.gridy = 7;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            panel.add(mapButtonPanel, constraints);

            constraints.gridx = 1;
            constraints.gridy = 8;
            panel.add(graylineCheckBox, constraints);

            setConnectionControls(!connected);
            JOptionPane.showMessageDialog(this, panel, t("dialog.settingsTitle"), JOptionPane.PLAIN_MESSAGE);
            applyMapSettings();
        }

        private void restoreAzimuthMap() {
            String locator = locatorField.getText().trim();
            if (locator.isBlank()) {
                return;
            }

            try {
                compassPanel.setAzimuthMap(AzimuthMapGenerator.generate(
                        locator,
                        selectedMapStyle(),
                        selectedMapDistanceKilometers(),
                        900
                ));
                locatorField.setText(locator.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                compassPanel.clearAzimuthMap();
                locatorField.setText("");
                windowPreferences.remove("locator");
            }
        }

        private void generateAzimuthMapFromLocator() {
            String locator = locatorField.getText().trim();
            if (locator.isBlank()) {
                showError(t("status.invalidLocator"));
                return;
            }

            try {
                int mapDistanceKilometers = selectedMapDistanceKilometers();
                MapStyle mapStyle = selectedMapStyle();
                compassPanel.setAzimuthMap(AzimuthMapGenerator.generate(locator, mapStyle, mapDistanceKilometers, 900));
                locatorField.setText(locator.toUpperCase(Locale.ROOT));
                windowPreferences.put("locator", locatorField.getText());
                windowPreferences.putInt("mapDistanceKm", mapDistanceKilometers);
                windowPreferences.put("mapStyle", mapStyle.code());
                windowPreferences.putBoolean("showGrayline", graylineCheckBox.isSelected());
                compassPanel.setGraylineVisible(graylineCheckBox.isSelected());
                clearStatus();
            } catch (IllegalArgumentException exception) {
                showError(t("status.invalidLocator"));
            }
        }

        private void clearAzimuthMap() {
            compassPanel.clearAzimuthMap();
            locatorField.setText("");
            windowPreferences.remove("locator");
            clearStatus();
        }

        private void applyMapSettings() {
            int mapDistanceKilometers = selectedMapDistanceKilometers();
            MapStyle mapStyle = selectedMapStyle();
            windowPreferences.putInt("mapDistanceKm", mapDistanceKilometers);
            windowPreferences.put("mapStyle", mapStyle.code());
            windowPreferences.putBoolean("showGrayline", graylineCheckBox.isSelected());
            compassPanel.setGraylineVisible(graylineCheckBox.isSelected());

            String locator = locatorField.getText().trim();
            if (!locator.isBlank()) {
                generateAzimuthMapFromLocator();
            }
        }

        private int selectedMapDistanceKilometers() {
            try {
                mapDistanceSpinner.commitEdit();
            } catch (java.text.ParseException exception) {
                mapDistanceSpinner.setValue(DEFAULT_AZIMUTH_MAP_DISTANCE_KM);
            }
            Object value = mapDistanceSpinner.getValue();
            if (value instanceof Number number) {
                int mapDistanceKilometers = clampMapDistanceKilometers(number.intValue());
                mapDistanceSpinner.setValue(mapDistanceKilometers);
                return mapDistanceKilometers;
            }
            mapDistanceSpinner.setValue(DEFAULT_AZIMUTH_MAP_DISTANCE_KM);
            return DEFAULT_AZIMUTH_MAP_DISTANCE_KM;
        }

        private MapStyle selectedMapStyle() {
            Object selectedItem = mapTypeCombo.getSelectedItem();
            if (selectedItem instanceof MapStyle mapStyle) {
                return mapStyle;
            }
            mapTypeCombo.setSelectedItem(MapStyle.PHYSICAL);
            return MapStyle.PHYSICAL;
        }

        private int savedMapDistanceKilometers() {
            int savedDistance = windowPreferences.getInt("mapDistanceKm", -1);
            if (savedDistance > 0) {
                return clampMapDistanceKilometers(savedDistance);
            }

            double oldZoom = windowPreferences.getDouble("mapZoom", -1.0);
            if (oldZoom > 0.0) {
                return clampMapDistanceKilometers((int) Math.round(EARTH_HALF_CIRCUMFERENCE_KM / oldZoom));
            }
            return DEFAULT_AZIMUTH_MAP_DISTANCE_KM;
        }

        private void updateConnectionStatus() {
        }

        private String t(String key) {
            return i18n.text(key);
        }

        private String tf(String key, Object... args) {
            return i18n.format(key, args);
        }

        private JPanel createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(6, 6, 6, 6);
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weighty = 1;

            constraints.gridx = 0;
            constraints.weightx = 0.26;
            panel.add(createLeftControlPanel(), constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.52;
            compassPanel.setPreferredSize(new Dimension(420, 420));
            panel.add(compassPanel, constraints);

            return panel;
        }

        private JPanel createLeftControlPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1;
            constraints.insets = new Insets(0, 0, 8, 0);

            constraints.gridy = 0;
            constraints.weighty = 0.48;
            panel.add(createCurrentAzimuthPanel(), constraints);

            constraints.gridy = 1;
            constraints.weighty = 0.18;
            panel.add(createTargetAzimuthPanel(), constraints);

            constraints.gridy = 2;
            constraints.weighty = 0.34;
            constraints.insets = new Insets(0, 0, 0, 0);
            panel.add(createManualControlPanel(), constraints);

            return panel;
        }

        private JPanel createCurrentAzimuthPanel() {
            JPanel panel = createSectionPanel(currentAzimuthBorder, new BorderLayout(0, 8));
            headingLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 118));
            headingLabel.setForeground(TEXT_PRIMARY);
            headingLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                        enterHeadingEditMode();
                    }
                }
            });
            headingEditField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
            headingEditField.setHorizontalAlignment(SwingConstants.CENTER);
            headingEditField.setForeground(TEXT_PRIMARY);
            headingEditField.setBackground(Color.WHITE);
            headingEditField.setBorder(BorderFactory.createLineBorder(TARGET_BLUE, 2));
            headingEditField.addActionListener(event -> submitHeadingEdit());
            headingEditField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        showHeadingReadout();
                    }
                }
            });
            headingCards.setOpaque(false);
            headingCards.setPreferredSize(new Dimension(260, 150));
            headingCards.setMinimumSize(new Dimension(260, 150));
            headingCards.add(headingLabel, "readout");
            headingCards.add(headingEditField, "edit");

            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            statusLabel.setVerticalAlignment(SwingConstants.CENTER);
            statusLabel.setOpaque(true);
            statusLabel.setPreferredSize(new Dimension(260, 50));
            statusLabel.setMinimumSize(new Dimension(260, 50));
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));

            panel.add(headingCards, BorderLayout.CENTER);
            panel.add(statusLabel, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel createTargetAzimuthPanel() {
            JPanel panel = createSectionPanel(targetAzimuthBorder, new BorderLayout(0, 0));

            targetAzimuthValueLabel.setFont(targetAzimuthValueLabel.getFont().deriveFont(Font.BOLD, 42f));
            targetAzimuthValueLabel.setForeground(TARGET_BLUE);
            targetAzimuthValueLabel.setPreferredSize(new Dimension(180, 42));
            panel.add(targetAzimuthValueLabel, BorderLayout.CENTER);

            return panel;
        }

        private JPanel createManualControlPanel() {
            JPanel panel = createSectionPanel(manualControlBorder, new BorderLayout(0, 0));
            panel.add(createManualRotatePanel(), BorderLayout.CENTER);
            return panel;
        }

        private JPanel createBottomPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 0));
            panel.setOpaque(false);

            hintLabel.setForeground(TEXT_SECONDARY);

            panel.add(createPresetPanel(), BorderLayout.CENTER);

            return panel;
        }

        private void showCommunicationLogWindow() {
            if (!communicationLogWindow.isVisible()) {
                communicationLogWindow.setLocationRelativeTo(this);
            }
            communicationLogWindow.setVisible(true);
            communicationLogWindow.toFront();
            communicationLogWindow.requestFocus();
        }

        private JPanel createPresetPanel() {
            JPanel panel = createSectionPanel(presetsBorder, new GridLayout(2, 5, 6, 6));

            for (int index = 0; index < presetButtons.length; index++) {
                Preset preset = presetStore.load(index);
                PresetButton presetButton = new PresetButton(index, preset);
                presetButtons[index] = presetButton;
                panel.add(presetButton.button());
            }

            return panel;
        }

        private void toggleConnection() {
            if (connected) {
                disconnect();
            } else {
                connect();
            }
        }

        private void connect() {
            try {
                String host = selectedHost();
                client = new TcpRotorClient(host, selectedPort(), this::recordCommunication);
                windowPreferences.put("host", host);
                executor = Executors.newScheduledThreadPool(2);
                connected = true;
                setConnectionControls(false);
                connectButton.setText(t("button.disconnect"));
                updateConnectionStatus();
                clearStatus();
                int speed = speedSlider.getValue();
                executor.execute(() -> applyRotationSpeed(speed));
                executor.execute(this::pollHeading);
                pollingTask = executor.scheduleAtFixedRate(this::pollHeading, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } catch (RuntimeException exception) {
                showError(exception.getMessage());
            }
        }

        private void disconnect() {
            connected = false;
            stopScanExecutor();
            scanning = false;
            if (pollingTask != null) {
                pollingTask.cancel(true);
                pollingTask = null;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            client = null;
            currentHeading = null;
            refreshHeadingAfterRelativeError = false;
            setActivePresetButton(null);
            setTargetHeading(null);
            setConnectionControls(true);
            connectButton.setText(t("button.connect"));
            updateConnectionStatus();
            clearStatus();
        }

        private void enterHeadingEditMode() {
            refreshHeadingAfterRelativeErrorIfNeeded();
            if (currentHeading != null) {
                headingEditField.setText(Integer.toString(currentHeading));
            } else if (targetHeading != null) {
                headingEditField.setText(Integer.toString(targetHeading));
            } else {
                headingEditField.setText("");
            }

            ((CardLayout) headingCards.getLayout()).show(headingCards, "edit");
            headingEditField.requestFocusInWindow();
            headingEditField.selectAll();
            clearStatus();
        }

        private void submitHeadingEdit() {
            int target;
            try {
                target = parseHeadingInput(headingEditField.getText());
            } catch (RelativeAzimuthOverflowException exception) {
                refreshHeadingAfterRelativeError = true;
                showError(t("status.relativeOverflow"));
                headingEditField.selectAll();
                return;
            } catch (NumberFormatException exception) {
                showError(t("status.invalidAzimuth"));
                headingEditField.selectAll();
                return;
            }

            showHeadingReadout();
            moveToAzimuth(target, t("status.connectFirst"), null);
        }

        private void showHeadingReadout() {
            ((CardLayout) headingCards.getLayout()).show(headingCards, "readout");
        }

        private void moveToClickedAzimuth(int target) {
            showHeadingReadout();
            refreshHeadingAfterRelativeErrorIfNeeded();
            moveToAzimuth(target, t("status.clickConnectFirst"), null);
        }

        private void moveToPreset(PresetButton presetButton) {
            showHeadingReadout();
            refreshHeadingAfterRelativeErrorIfNeeded();
            Preset preset = presetButton.preset();
            moveToAzimuth(preset.azimuth(), t("status.presetConnectFirst"), presetButton);
        }

        private void moveToAzimuth(int target, String disconnectedMessage, PresetButton presetButton) {
            if (!connected || executor == null || client == null) {
                showError(disconnectedMessage);
                return;
            }

            try {
                validateAzimuth(target);
            } catch (NumberFormatException exception) {
                showError(t("status.invalidAzimuth"));
                return;
            }

            setTargetHeading(target);
            setActivePresetButton(presetButton);
            clearStatus();
            executor.execute(() -> {
                try {
                    client.setRotationSpeed(speedSlider.getValue());
                    client.moveToAzimuth(target);
                    SwingUtilities.invokeLater(this::clearStatus);
                    executor.schedule(this::pollHeading, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                } catch (RuntimeException exception) {
                    SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
                }
            });
        }

        private void startManualRotation(boolean clockwise) {
            showHeadingReadout();
            refreshHeadingAfterRelativeErrorIfNeeded();
            if (!connected || executor == null || client == null) {
                showError(t("status.manualConnectFirst"));
                return;
            }

            setActivePresetButton(null);
            setTargetHeading(null);
            clearStatus();
            executor.execute(() -> {
                try {
                    client.setRotationSpeed(speedSlider.getValue());
                    if (clockwise) {
                        client.rotateClockwise();
                    } else {
                        client.rotateCounterClockwise();
                    }
                } catch (RuntimeException exception) {
                    SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
                }
            });
        }

        private void stopManualRotation() {
            showHeadingReadout();
            refreshHeadingAfterRelativeErrorIfNeeded();
            if (!connected || executor == null || client == null) {
                return;
            }

            sendStopCommand();
        }

        private void stopRotation() {
            showHeadingReadout();
            refreshHeadingAfterRelativeErrorIfNeeded();
            if (!connected || executor == null || client == null) {
                showError(t("status.stopConnectFirst"));
                return;
            }

            sendStopCommand();
        }

        private void sendStopCommand() {
            clearStatus();
            executor.execute(() -> {
                try {
                    client.stop();
                    SwingUtilities.invokeLater(this::clearStatus);
                    executor.schedule(this::pollHeading, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                } catch (RuntimeException exception) {
                    SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
                }
            });
        }

        private void changeRotationSpeed() {
            int speed = speedSlider.getValue();
            windowPreferences.putInt("rotationSpeed", speed);
            if (!connected || executor == null || client == null) {
                return;
            }

            executor.execute(() -> applyRotationSpeed(speed));
        }

        private void applyRotationSpeed(int speed) {
            TcpRotorClient currentClient = client;
            if (!connected || currentClient == null) {
                return;
            }

            try {
                currentClient.setRotationSpeed(speed);
                SwingUtilities.invokeLater(this::clearStatus);
            } catch (RuntimeException exception) {
                SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
            }
        }

        private static int clampRotationSpeed(int speed) {
            return Math.max(1, Math.min(4, speed));
        }

        private void setTargetHeading(Integer heading) {
            targetHeading = heading;
            compassPanel.setTargetHeading(heading);
            updateTargetHeadingLabel();
        }

        private void updateTargetHeadingLabel() {
            if (targetHeading == null) {
                targetAzimuthValueLabel.setText("---");
            } else {
                targetAzimuthValueLabel.setText(degreesHtml(targetHeading));
            }
        }

        private static String degreesHtml(int degrees) {
            return "<html>" + degrees + "<sup style='font-size:55%'>&deg;</sup></html>";
        }

        private static void validateAzimuth(int target) {
            if (target < 0 || target > MAX_TARGET_AZIMUTH) {
                throw new NumberFormatException("out of range");
            }
        }

        private int parseHeadingInput(String input) {
            String value = input == null ? "" : input.trim();
            if (value.startsWith("+") || value.startsWith("-")) {
                if (currentHeading == null) {
                    throw new NumberFormatException("current heading missing");
                }
                int offset = Integer.parseInt(value);
                int target = currentHeading + offset;
                if (target < 0 || target > MAX_TARGET_AZIMUTH) {
                    throw new RelativeAzimuthOverflowException();
                }
                return target;
            }

            int target = Integer.parseInt(value);
            validateAzimuth(target);
            return target;
        }

        private static final class RelativeAzimuthOverflowException extends NumberFormatException {
        }

        private void pollHeading() {
            TcpRotorClient currentClient = client;
            if (!connected || currentClient == null) {
                return;
            }

            try {
                int heading = currentClient.readAzimuth();
                SwingUtilities.invokeLater(() -> updateHeading(heading));
            } catch (RuntimeException exception) {
                SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
            }
        }

        private void updateHeading(int heading) {
            currentHeading = heading;
            headingLabel.setText(degreesHtml(heading));
            compassPanel.setHeading(heading);
        }

        private void refreshHeadingAfterRelativeErrorIfNeeded() {
            if (!refreshHeadingAfterRelativeError) {
                return;
            }
            refreshHeadingAfterRelativeError = false;
            if (connected && executor != null && client != null) {
                executor.execute(this::pollHeadingAfterRelativeError);
            }
        }

        private void pollHeadingAfterRelativeError() {
            TcpRotorClient currentClient = client;
            if (!connected || currentClient == null) {
                return;
            }

            try {
                int heading = currentClient.readAzimuth();
                SwingUtilities.invokeLater(() -> updateHeadingAfterRelativeError(heading));
            } catch (RuntimeException exception) {
                SwingUtilities.invokeLater(() -> showError(exception.getMessage()));
            }
        }

        private void updateHeadingAfterRelativeError(int heading) {
            updateHeading(heading);
            headingEditField.setText(Integer.toString(heading));
            showHeadingReadout();
        }

        private void setConnectionControls(boolean enabled) {
            hostCombo.setEnabled(enabled);
            portField.setEnabled(enabled);
            scanButton.setVisible(true);
            scanButton.setEnabled(enabled && !scanning);
        }

        private String selectedHost() {
            Object item = hostCombo.isEditable()
                    ? hostCombo.getEditor().getItem()
                    : hostCombo.getSelectedItem();
            return item == null ? "" : item.toString().trim();
        }

        private int selectedPort() {
            String rawPort = portField.getText() == null ? "" : portField.getText().trim();
            try {
                int port = Integer.parseInt(rawPort);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException("out of range");
                }
                return port;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(t("status.invalidTcpPort"));
            }
        }

        private void scanNetworkForArco() {
            if (connected || scanning) {
                return;
            }

            int port;
            try {
                port = selectedPort();
            } catch (IllegalArgumentException exception) {
                showError(exception.getMessage());
                return;
            }
            List<String> addressesToScan;
            try {
                addressesToScan = localIpv4Candidates();
            } catch (SocketException exception) {
                showError(tf("status.scanFailed", exception.getMessage()));
                return;
            }

            if (addressesToScan.isEmpty()) {
                showError(t("status.scanNoCandidates"));
                return;
            }

            scanning = true;
            scanButton.setEnabled(false);
            showError(tf("status.scanning", addressesToScan.size(), port));

            scanExecutor = Executors.newFixedThreadPool(SCAN_THREAD_COUNT);
            ExecutorService currentScanExecutor = scanExecutor;
            List<CompletableFuture<String>> scanTasks = addressesToScan.stream()
                    .map(address -> CompletableFuture.supplyAsync(() -> {
                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }
                        return TcpRotorClient.isArcoReachable(address, port, this::recordCommunication) ? address : null;
                    }, currentScanExecutor))
                    .toList();

            CompletableFuture.allOf(scanTasks.toArray(CompletableFuture[]::new))
                    .whenComplete((result, exception) -> {
                if (exception != null || scanExecutor != currentScanExecutor) {
                    return;
                }

                LinkedHashSet<String> foundHosts = new LinkedHashSet<>();
                for (CompletableFuture<String> scanTask : scanTasks) {
                    String address = scanTask.join();
                    if (address != null) {
                        foundHosts.add(address);
                    }
                }

                SwingUtilities.invokeLater(() -> finishScan(foundHosts));
            });
        }

        private void finishScan(LinkedHashSet<String> foundHosts) {
            scanning = false;
            stopScanExecutor();
            setConnectionControls(true);

            if (foundHosts.isEmpty()) {
                showError(t("status.scanNoArco"));
                return;
            }

            String selectedHost = selectedHost();
            LinkedHashSet<String> mergedHosts = new LinkedHashSet<>();
            if (!selectedHost.isBlank()) {
                mergedHosts.add(selectedHost);
            }
            for (int index = 0; index < hostCombo.getItemCount(); index++) {
                String existingHost = hostCombo.getItemAt(index);
                if (existingHost != null && !existingHost.isBlank()) {
                    mergedHosts.add(existingHost.trim());
                }
            }
            mergedHosts.addAll(foundHosts);

            hostCombo.removeAllItems();
            mergedHosts.stream()
                    .sorted(Comparator.naturalOrder())
                    .forEach(hostCombo::addItem);
            String foundHost = foundHosts.iterator().next();
            hostCombo.setSelectedItem(foundHost);
            windowPreferences.put("host", foundHost);
            showError(tf("status.scanFound", foundHosts.size()));
        }

        private void stopScanExecutor() {
            if (scanExecutor != null) {
                scanExecutor.shutdownNow();
                scanExecutor = null;
            }
        }

        private static List<String> localIpv4Candidates() throws SocketException {
            LinkedHashSet<String> addresses = new LinkedHashSet<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress rawAddress = interfaceAddress.getAddress();
                    if (!(rawAddress instanceof Inet4Address inet4Address)) {
                        continue;
                    }
                    short prefixLength = interfaceAddress.getNetworkPrefixLength();
                    if (prefixLength <= 0 || prefixLength > 30) {
                        continue;
                    }

                    int hostBits = 32 - prefixLength;
                    int hostCount = (1 << hostBits) - 2;
                    int limitedHostCount = Math.min(hostCount, MAX_SCAN_HOSTS);
                    int ownIp = ipv4ToInt(inet4Address);
                    int networkMask = (int) (0xFFFFFFFFL << hostBits);
                    int network = ownIp & networkMask;

                    for (int offset = 1; offset <= limitedHostCount; offset++) {
                        int candidateIp = network + offset;
                        if (candidateIp == ownIp) {
                            continue;
                        }
                        addresses.add(intToIpv4(candidateIp));
                    }
                }
            }
            return new ArrayList<>(addresses);
        }

        private static int ipv4ToInt(Inet4Address address) {
            byte[] bytes = address.getAddress();
            return ((bytes[0] & 0xFF) << 24)
                    | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8)
                    | (bytes[3] & 0xFF);
        }

        private static String intToIpv4(int value) {
            return ((value >>> 24) & 0xFF) + "."
                    + ((value >>> 16) & 0xFF) + "."
                    + ((value >>> 8) & 0xFF) + "."
                    + (value & 0xFF);
        }

        private void recordCommunication(TcpRotorClient.CommunicationEvent event) {
            if (SwingUtilities.isEventDispatchThread()) {
                appendCommunicationLogEntry(event);
            } else {
                SwingUtilities.invokeLater(() -> appendCommunicationLogEntry(event));
            }
        }

        private void appendCommunicationLogEntry(TcpRotorClient.CommunicationEvent event) {
            String endpoint = event.host() + ":" + event.port();
            String result;
            if (!event.successful()) {
                result = t("log.error") + ": " + event.errorMessage();
            } else if (event.responseExpected()) {
                result = "< " + printableTcpPayload(event.response());
            } else {
                result = t("log.noResponse");
            }

            String line = String.format(
                    Locale.ROOT,
                    "%s  %-22s > %-5s  %s",
                    LocalTime.now().format(LOG_TIME_FORMAT),
                    endpoint,
                    printableTcpPayload(event.command()),
                    result
            );
            communicationLogEntries.add(line);
            while (communicationLogEntries.size() > MAX_COMMUNICATION_LOG_ENTRIES) {
                communicationLogEntries.remove(0);
            }
            communicationLogArea.setText(String.join(System.lineSeparator(), communicationLogEntries));
            communicationLogArea.setCaretPosition(communicationLogArea.getDocument().getLength());
        }

        private final class CommunicationLogWindow extends JFrame {

            private final JLabel logLabel = createFormLabel("");

            private CommunicationLogWindow() {
                setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                setMinimumSize(new Dimension(760, 320));

                JPanel root = new JPanel(new BorderLayout(0, 8));
                root.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
                root.setBackground(APP_BACKGROUND);
                setContentPane(root);

                communicationLogArea.setEditable(false);
                communicationLogArea.setLineWrap(false);
                communicationLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                communicationLogArea.setForeground(TEXT_PRIMARY);
                communicationLogArea.setBackground(PANEL_BACKGROUND);
                communicationLogArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

                JScrollPane scrollPane = new JScrollPane(communicationLogArea);
                scrollPane.setPreferredSize(new Dimension(820, 260));
                scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                scrollPane.getViewport().setBackground(PANEL_BACKGROUND);

                root.add(logLabel, BorderLayout.NORTH);
                root.add(scrollPane, BorderLayout.CENTER);
                pack();
            }

            private void updateTexts() {
                setTitle(t("window.tcpCommunicationTitle"));
                logLabel.setText(t("label.communication"));
            }
        }

        private void showError(String message) {
            statusLabel.setText("<html><div style='width:300px;text-align:center;'>" + escapeHtml(message) + "</div></html>");
            statusLabel.setForeground(WARNING_RED);
            statusLabel.setBackground(new Color(255, 236, 232));
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(WARNING_RED),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));
            updateStatusVisibility();
        }

        private void clearStatus() {
            statusLabel.setText("");
            updateStatusVisibility();
        }

        private void updateStatusVisibility() {
            boolean hasMessage = !statusLabel.getText().isBlank();
            statusLabel.setForeground(hasMessage ? WARNING_RED : PANEL_BACKGROUND);
            statusLabel.setBackground(hasMessage ? new Color(255, 236, 232) : PANEL_BACKGROUND);
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(hasMessage ? WARNING_RED : PANEL_BACKGROUND),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));
        }

        private void setActivePresetButton(PresetButton presetButton) {
            if (activePresetButton != null) {
                activePresetButton.setActive(false);
            }
            activePresetButton = presetButton;
            if (activePresetButton != null) {
                activePresetButton.setActive(true);
            }
        }

        private void saveCurrentHeadingToPreset(PresetButton presetButton) {
            if (currentHeading == null) {
                showError(t("status.noHeading"));
                return;
            }

            Preset currentPreset = presetButton.preset();
            Preset updatedPreset = new Preset(currentPreset.label(), currentHeading);
            presetStore.save(presetButton.index(), updatedPreset);
            presetButton.setPreset(updatedPreset);
            clearStatus();
        }

        private void editPreset(PresetButton presetButton) {
            Preset currentPreset = presetButton.preset();
            JTextField labelField = new JTextField(currentPreset.label(), 12);
            JSpinner azimuthSpinner = new JSpinner(new SpinnerNumberModel(currentPreset.azimuth(), 0, MAX_TARGET_AZIMUTH, 1));
            JLabel labelCaption = createFormLabel(t("label.presetLabel"));
            JLabel azimuthCaption = createFormLabel(t("label.azimuth"));
            labelField.setFont(labelField.getFont().deriveFont(UI_FONT_SIZE));
            azimuthSpinner.setFont(azimuthSpinner.getFont().deriveFont(UI_FONT_SIZE));
            JComponent azimuthEditor = azimuthSpinner.getEditor();
            azimuthEditor.setFont(azimuthEditor.getFont().deriveFont(UI_FONT_SIZE));
            if (azimuthEditor instanceof JSpinner.DefaultEditor defaultEditor) {
                defaultEditor.getTextField().setFont(defaultEditor.getTextField().getFont().deriveFont(UI_FONT_SIZE));
            }

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;

            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(labelCaption, constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(labelField, constraints);

            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            panel.add(azimuthCaption, constraints);

            constraints.gridx = 1;
            panel.add(azimuthSpinner, constraints);

            while (true) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        tf("dialog.presetTitle", presetButton.index() + 1),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }

                String label = labelField.getText().trim();
                if (label.isBlank()) {
                    label = "P" + (presetButton.index() + 1);
                }
                if (label.length() > 10) {
                    JOptionPane.showMessageDialog(this, t("dialog.labelTooLong"), t("dialog.invalidLabelTitle"), JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                Preset updatedPreset = new Preset(label, (Integer) azimuthSpinner.getValue());
                presetStore.save(presetButton.index(), updatedPreset);
                presetButton.setPreset(updatedPreset);
                clearStatus();
                return;
            }
        }

        private final class PresetButton {

            private final int index;
            private final JButton button;
            private Preset preset;
            private Timer longPressTimer;
            private boolean longPressHandled;

            private PresetButton(int index, Preset preset) {
                this.index = index;
                this.preset = preset;
                this.button = new JButton();
                this.button.setPreferredSize(new Dimension(150, 58));
                this.button.setFocusPainted(false);
                this.button.setOpaque(true);
                this.button.setBorderPainted(true);
                this.button.setBackground(BUTTON_BACKGROUND);
                this.button.setForeground(TEXT_PRIMARY);
                this.button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                this.button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (SwingUtilities.isRightMouseButton(event)) {
                            editPreset(PresetButton.this);
                        } else if (SwingUtilities.isLeftMouseButton(event)) {
                            startLongPressTimer();
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent event) {
                        if (!SwingUtilities.isLeftMouseButton(event)) {
                            return;
                        }
                        stopLongPressTimer();
                        if (!longPressHandled && button.contains(event.getPoint())) {
                            moveToPreset(PresetButton.this);
                        }
                        longPressHandled = false;
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        stopLongPressTimer();
                    }
                });
                refreshText();
            }

            private int index() {
                return index;
            }

            private JButton button() {
                return button;
            }

            private Preset preset() {
                return preset;
            }

            private void setPreset(Preset preset) {
                this.preset = preset;
                refreshText();
            }

            private void setActive(boolean active) {
                if (active) {
                    button.setBackground(TARGET_BLUE);
                    button.setForeground(Color.WHITE);
                } else {
                    button.setBackground(BUTTON_BACKGROUND);
                    button.setForeground(TEXT_PRIMARY);
                }
            }

            private void refreshText() {
                button.setToolTipText(t("tooltip.preset"));
                button.setFont(button.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE));
                button.setText("<html><center>" + escapeHtml(preset.label()) + "<br><b>" + preset.azimuth() + "<sup style='font-size:55%'>&deg;</sup></b></center></html>");
            }

            private void startLongPressTimer() {
                stopLongPressTimer();
                longPressHandled = false;
                longPressTimer = new Timer(3000, event -> {
                    longPressHandled = true;
                    saveCurrentHeadingToPreset(PresetButton.this);
                });
                longPressTimer.setRepeats(false);
                longPressTimer.start();
            }

            private void stopLongPressTimer() {
                if (longPressTimer != null) {
                    longPressTimer.stop();
                    longPressTimer = null;
                }
            }
        }
    }

    private record Preset(String label, int azimuth) {
    }

    private static final class PresetStore {

        private static final int MAX_LABEL_LENGTH = 10;
        private final Preferences preferences = preferencesNode("presets");

        Preset load(int index) {
            String defaultLabel = "P" + (index + 1);
            int defaultAzimuth = Math.min(index * 40, MAX_TARGET_AZIMUTH);
            String label = preferences.get(labelKey(index), defaultLabel);
            int azimuth = preferences.getInt(azimuthKey(index), defaultAzimuth);
            return new Preset(trimLabel(label), clampAzimuth(azimuth));
        }

        void save(int index, Preset preset) {
            preferences.put(labelKey(index), trimLabel(preset.label()));
            preferences.putInt(azimuthKey(index), clampAzimuth(preset.azimuth()));
        }

        private static String labelKey(int index) {
            return "preset." + index + ".label";
        }

        private static String azimuthKey(int index) {
            return "preset." + index + ".azimuth";
        }

        private static String trimLabel(String label) {
            String normalizedLabel = label == null ? "" : label.trim();
            if (normalizedLabel.length() <= MAX_LABEL_LENGTH) {
                return normalizedLabel;
            }
            return normalizedLabel.substring(0, MAX_LABEL_LENGTH);
        }

        private static int clampAzimuth(int azimuth) {
            return Math.max(0, Math.min(MAX_TARGET_AZIMUTH, azimuth));
        }
    }

    private static int clampMapDistanceKilometers(int distanceKilometers) {
        int clampedDistance = Math.max(
                MIN_AZIMUTH_MAP_DISTANCE_KM,
                Math.min(MAX_AZIMUTH_MAP_DISTANCE_KM, distanceKilometers)
        );
        return Math.round(clampedDistance / (float) AZIMUTH_MAP_DISTANCE_STEP_KM) * AZIMUTH_MAP_DISTANCE_STEP_KM;
    }

    private static double mapAngularDistanceForDistance(int distanceKilometers) {
        return clampMapDistanceKilometers(distanceKilometers) / EARTH_HALF_CIRCUMFERENCE_KM * Math.PI;
    }

    private static double normalizeDegrees(double degrees) {
        double normalizedDegrees = degrees % 360.0;
        if (normalizedDegrees < 0.0) {
            normalizedDegrees += 360.0;
        }
        return normalizedDegrees;
    }

    private static double normalizeLongitude(double longitude) {
        double normalizedLongitude = longitude;
        while (normalizedLongitude < -180.0) {
            normalizedLongitude += 360.0;
        }
        while (normalizedLongitude > 180.0) {
            normalizedLongitude -= 360.0;
        }
        return normalizedLongitude;
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String printableTcpPayload(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static Preferences preferencesNode(String childNode) {
        Preferences preferences = Preferences.userNodeForPackage(ArcoRotorDesktopApplication.class).node(childNode);
        migratePreferences(childNode, preferences);
        return preferences;
    }

    private static void migratePreferences(String childNode, Preferences target) {
        try {
            if (target.keys().length > 0) {
                return;
            }

            Preferences source = Preferences.userRoot().node(OLD_PREFERENCES_NODE + "/" + childNode);
            for (String key : source.keys()) {
                target.put(key, source.get(key, null));
            }
        } catch (BackingStoreException exception) {
            // Preferences are optional persistence; a migration failure must not block startup.
        }
    }

    private enum Language {
        CS("CZ", "cs"),
        EN("EN", "en");

        private final String label;
        private final String code;

        Language(String label, String code) {
            this.label = label;
            this.code = code;
        }

        String code() {
            return code;
        }

        Locale locale() {
            return Locale.forLanguageTag(code);
        }

        static Language fromCode(String code) {
            for (Language language : values()) {
                if (language.code.equalsIgnoreCase(code)) {
                    return language;
                }
            }
            return CS;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum MapStyle {
        PHYSICAL("physical", "label.mapPhysical", "/maps/natural-earth-8192.jpg", false),
        POLITICAL("political", "label.mapPolitical", "/maps/natural-earth-countries-8192.png", true);

        private final String code;
        private final String labelKey;
        private final String resourcePath;
        private final boolean sharpSampling;

        MapStyle(String code, String labelKey, String resourcePath, boolean sharpSampling) {
            this.code = code;
            this.labelKey = labelKey;
            this.resourcePath = resourcePath;
            this.sharpSampling = sharpSampling;
        }

        String code() {
            return code;
        }

        String labelKey() {
            return labelKey;
        }

        String resourcePath() {
            return resourcePath;
        }

        boolean sharpSampling() {
            return sharpSampling;
        }

        static MapStyle fromCode(String code) {
            for (MapStyle mapStyle : values()) {
                if (mapStyle.code.equalsIgnoreCase(code)) {
                    return mapStyle;
                }
            }
            return PHYSICAL;
        }
    }

    private static final class I18n {

        private final Language language;
        private final ResourceBundle bundle;

        private I18n(Language language) {
            this.language = language;
            this.bundle = ResourceBundle.getBundle("cz.ok1xoe.arcorotor.desktop.messages", language.locale());
        }

        private Language language() {
            return language;
        }

        private String text(String key) {
            return bundle.getString(key);
        }

        private String format(String key, Object... args) {
            return MessageFormat.format(text(key), args);
        }
    }

    private static final class CompassPanel extends JPanel {

        private int heading = -1;
        private Integer targetHeading;
        private BufferedImage azimuthMapImage;
        private LocatorCoordinates azimuthMapOrigin;
        private int azimuthMapDistanceKilometers = DEFAULT_AZIMUTH_MAP_DISTANCE_KM;
        private boolean graylineVisible = true;
        private BufferedImage graylineOverlay;
        private int graylineOverlaySize = -1;
        private long graylineOverlayMinute = -1;
        private String northLabel = "N";
        private String eastLabel = "E";
        private String southLabel = "S";
        private String westLabel = "W";

        private final IntConsumer azimuthClickHandler;

        CompassPanel(IntConsumer azimuthClickHandler) {
            this.azimuthClickHandler = azimuthClickHandler;
            setOpaque(false);
            setPreferredSize(new Dimension(360, 360));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR));
            Timer graylineTimer = new Timer(60_000, event -> {
                graylineOverlayMinute = -1;
                repaint();
            });
            graylineTimer.setInitialDelay(60_000);
            graylineTimer.start();
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    azimuthFromClick(event.getX(), event.getY())
                            .ifPresent(azimuthClickHandler::accept);
                }
            });
        }

        void setHeading(int heading) {
            this.heading = heading;
            repaint();
        }

        void setTargetHeading(Integer targetHeading) {
            this.targetHeading = targetHeading;
            repaint();
        }

        void setGraylineVisible(boolean graylineVisible) {
            this.graylineVisible = graylineVisible;
            repaint();
        }

        void setAzimuthMap(GeneratedAzimuthMap azimuthMap) {
            this.azimuthMapImage = azimuthMap.image();
            this.azimuthMapOrigin = azimuthMap.origin();
            this.azimuthMapDistanceKilometers = azimuthMap.maximumDistanceKilometers();
            invalidateGraylineOverlay();
            repaint();
        }

        void clearAzimuthMap() {
            this.azimuthMapImage = null;
            this.azimuthMapOrigin = null;
            invalidateGraylineOverlay();
            repaint();
        }

        void setCardinalLabels(String northLabel, String eastLabel, String southLabel, String westLabel) {
            this.northLabel = northLabel;
            this.eastLabel = eastLabel;
            this.southLabel = southLabel;
            this.westLabel = westLabel;
            repaint();
        }

        private java.util.OptionalInt azimuthFromClick(int clickX, int clickY) {
            int size = Math.min(getWidth(), getHeight()) - 24;
            int centerX = (getWidth() - size) / 2 + size / 2;
            int centerY = (getHeight() - size) / 2 + size / 2;
            int radius = size / 2;
            int dx = clickX - centerX;
            int dy = clickY - centerY;
            double distance = Math.hypot(dx, dy);

            if (distance > radius || distance < 24) {
                return java.util.OptionalInt.empty();
            }

            int azimuth = (int) Math.round(Math.toDegrees(Math.atan2(dx, -dy)));
            if (azimuth < 0) {
                azimuth += 360;
            }
            if (azimuth == 360) {
                azimuth = 0;
            }
            return java.util.OptionalInt.of(azimuth);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 24;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                int centerX = x + size / 2;
                int centerY = y + size / 2;
                int radius = size / 2;

                if (azimuthMapImage == null) {
                    g.setPaint(new GradientPaint(x, y, Color.WHITE, x + size, y + size, new Color(226, 235, 238)));
                    g.fillOval(x, y, size, size);
                } else {
                    Shape oldClip = g.getClip();
                    g.setClip(new Ellipse2D.Double(x, y, size, size));
                    g.drawImage(azimuthMapImage, x, y, size, size, this);
                    g.setColor(new Color(255, 255, 255, 72));
                    g.fillOval(x, y, size, size);
                    drawGraylineOverlay(g, x, y, size);
                    g.setClip(oldClip);
                }
                g.setColor(BORDER_COLOR);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(x, y, size, size);

                drawTicks(g, centerX, centerY, radius);
                drawLabels(g, centerX, centerY, radius);
                drawDegreeLabels(g, centerX, centerY, radius);

                if (targetHeading != null) {
                    drawTargetLine(g, centerX, centerY, radius, targetHeading);
                }

                if (heading >= 0) {
                    drawNeedle(g, centerX, centerY, radius, heading);
                } else {
                    g.setColor(TEXT_PRIMARY);
                    g.fillOval(centerX - 7, centerY - 7, 14, 14);
                }
            } finally {
                g.dispose();
            }
        }

        private void drawGraylineOverlay(Graphics2D g, int x, int y, int size) {
            if (!graylineVisible || azimuthMapImage == null || azimuthMapOrigin == null) {
                return;
            }
            BufferedImage overlay = graylineOverlay(size);
            g.drawImage(overlay, x, y, size, size, this);
        }

        private BufferedImage graylineOverlay(int size) {
            long currentMinute = System.currentTimeMillis() / 60_000L;
            if (graylineOverlay != null && graylineOverlaySize == size && graylineOverlayMinute == currentMinute) {
                return graylineOverlay;
            }

            graylineOverlay = createGraylineOverlay(size, Instant.ofEpochMilli(currentMinute * 60_000L));
            graylineOverlaySize = size;
            graylineOverlayMinute = currentMinute;
            return graylineOverlay;
        }

        private BufferedImage createGraylineOverlay(int size, Instant instant) {
            BufferedImage overlay = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            SolarPosition solarPosition = SolarPosition.at(instant);
            double maximumAngularDistance = mapAngularDistanceForDistance(azimuthMapDistanceKilometers);
            double lat1 = Math.toRadians(azimuthMapOrigin.latitude());
            double lon1 = Math.toRadians(azimuthMapOrigin.longitude());
            double sinLat1 = Math.sin(lat1);
            double cosLat1 = Math.cos(lat1);
            int center = size / 2;
            double radius = size / 2.0;
            double radiusSquared = radius * radius;

            for (int y = 0; y < size; y++) {
                double dy = y - center;
                for (int x = 0; x < size; x++) {
                    double dx = x - center;
                    double distanceSquared = dx * dx + dy * dy;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    double angularDistance = Math.sqrt(distanceSquared) / radius * maximumAngularDistance;
                    double bearing = Math.atan2(dx, -dy);
                    double sinDistance = Math.sin(angularDistance);
                    double cosDistance = Math.cos(angularDistance);
                    double latitude = Math.asin(sinLat1 * cosDistance + cosLat1 * sinDistance * Math.cos(bearing));
                    double longitude = lon1 + Math.atan2(
                            Math.sin(bearing) * sinDistance * cosLat1,
                            cosDistance - sinLat1 * Math.sin(latitude)
                    );

                    double daylight = solarPosition.daylight(Math.toDegrees(latitude), normalizeLongitude(Math.toDegrees(longitude)));
                    int alpha = nightAlpha(daylight);
                    if (alpha > 0) {
                        overlay.setRGB(x, y, (alpha << 24) | 0x07121F);
                    }
                }
            }
            return overlay;
        }

        private static int nightAlpha(double daylight) {
            if (daylight >= 0.08) {
                return 0;
            }
            if (daylight <= -0.08) {
                return 90;
            }
            return (int) Math.round((0.08 - daylight) / 0.16 * 90.0);
        }

        private void invalidateGraylineOverlay() {
            graylineOverlay = null;
            graylineOverlaySize = -1;
            graylineOverlayMinute = -1;
        }

        private static void drawTicks(Graphics2D g, int centerX, int centerY, int radius) {
            for (int degrees = 0; degrees < 360; degrees += 10) {
                double radians = Math.toRadians(degrees);
                int outerX = centerX + (int) Math.round(Math.sin(radians) * (radius - 12));
                int outerY = centerY - (int) Math.round(Math.cos(radians) * (radius - 12));
                int tickLength = degrees % 30 == 0 ? 18 : 9;
                int innerX = centerX + (int) Math.round(Math.sin(radians) * (radius - 12 - tickLength));
                int innerY = centerY - (int) Math.round(Math.cos(radians) * (radius - 12 - tickLength));
                g.setStroke(new BasicStroke(degrees % 30 == 0 ? 2.4f : 1.3f));
                g.setColor(degrees % 30 == 0 ? TEXT_PRIMARY : new Color(142, 157, 166));
                g.drawLine(innerX, innerY, outerX, outerY);
            }
        }

        private void drawLabels(Graphics2D g, int centerX, int centerY, int radius) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.setColor(TEXT_PRIMARY);
            int cardinalInset = 76;
            drawCenteredString(g, northLabel, centerX, centerY - radius + cardinalInset);
            drawCenteredString(g, eastLabel, centerX + radius - cardinalInset, centerY);
            drawCenteredString(g, southLabel, centerX, centerY + radius - cardinalInset);
            drawCenteredString(g, westLabel, centerX - radius + cardinalInset, centerY);
        }

        private static void drawDegreeLabels(Graphics2D g, int centerX, int centerY, int radius) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.setColor(TEXT_SECONDARY);
            int degreeInset = 48;
            for (int degrees = 0; degrees < 360; degrees += 30) {
                double radians = Math.toRadians(degrees);
                int labelX = centerX + (int) Math.round(Math.sin(radians) * (radius - degreeInset));
                int labelY = centerY - (int) Math.round(Math.cos(radians) * (radius - degreeInset));
                drawCenteredString(g, Integer.toString(degrees), labelX, labelY);
            }
        }

        private static void drawNeedle(Graphics2D g, int centerX, int centerY, int radius, int heading) {
            double radians = Math.toRadians(heading % 360);
            int needleLength = radius - 36;
            int tipX = centerX + (int) Math.round(Math.sin(radians) * needleLength);
            int tipY = centerY - (int) Math.round(Math.cos(radians) * needleLength);
            int tailX = centerX - (int) Math.round(Math.sin(radians) * 34);
            int tailY = centerY + (int) Math.round(Math.cos(radians) * 34);

            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(HEADING_RED);
            g.drawLine(centerX, centerY, tipX, tipY);

            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(78, 103, 118));
            g.drawLine(centerX, centerY, tailX, tailY);

            g.setColor(TEXT_PRIMARY);
            g.fillOval(centerX - 11, centerY - 11, 22, 22);
        }

        private static void drawTargetLine(Graphics2D g, int centerX, int centerY, int radius, int targetHeading) {
            double radians = Math.toRadians(targetHeading % 360);
            int lineLength = radius - 36;
            int targetX = centerX + (int) Math.round(Math.sin(radians) * lineLength);
            int targetY = centerY - (int) Math.round(Math.cos(radians) * lineLength);

            g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(TARGET_BLUE);
            g.drawLine(centerX, centerY, targetX, targetY);
        }

        private static void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics metrics = g.getFontMetrics();
            int textX = x - metrics.stringWidth(text) / 2;
            int textY = y - metrics.getHeight() / 2 + metrics.getAscent();
            g.drawString(text, textX, textY);
        }
    }

    record LocatorCoordinates(double latitude, double longitude) {
    }

    record GeneratedAzimuthMap(BufferedImage image, LocatorCoordinates origin, int maximumDistanceKilometers) {
    }

    record SolarPosition(double declinationRadians, double subsolarLongitudeDegrees) {

        static SolarPosition at(Instant instant) {
            double julianDay = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5;
            double daysSinceJ2000 = julianDay - 2_451_545.0;
            double meanAnomaly = Math.toRadians(normalizeDegrees(357.529 + 0.98560028 * daysSinceJ2000));
            double meanLongitude = normalizeDegrees(280.459 + 0.98564736 * daysSinceJ2000);
            double eclipticLongitude = Math.toRadians(normalizeDegrees(
                    meanLongitude + 1.915 * Math.sin(meanAnomaly) + 0.020 * Math.sin(2.0 * meanAnomaly)
            ));
            double obliquity = Math.toRadians(23.439 - 0.00000036 * daysSinceJ2000);
            double declination = Math.asin(Math.sin(obliquity) * Math.sin(eclipticLongitude));
            double rightAscension = Math.atan2(Math.cos(obliquity) * Math.sin(eclipticLongitude), Math.cos(eclipticLongitude));
            double greenwichMeanSiderealTime = normalizeDegrees(280.46061837 + 360.98564736629 * daysSinceJ2000);
            double subsolarLongitude = normalizeLongitude(Math.toDegrees(rightAscension) - greenwichMeanSiderealTime);
            return new SolarPosition(declination, subsolarLongitude);
        }

        double daylight(double latitudeDegrees, double longitudeDegrees) {
            double latitude = Math.toRadians(latitudeDegrees);
            double hourAngle = Math.toRadians(normalizeLongitude(longitudeDegrees - subsolarLongitudeDegrees));
            return Math.sin(latitude) * Math.sin(declinationRadians)
                    + Math.cos(latitude) * Math.cos(declinationRadians) * Math.cos(hourAngle);
        }
    }

    static LocatorCoordinates parseMaidenheadLocator(String rawLocator) {
        String locator = rawLocator == null ? "" : rawLocator.trim().toUpperCase(Locale.ROOT);
        if (locator.length() < 4 || locator.length() % 2 != 0 || locator.length() > 8) {
            throw new IllegalArgumentException("Invalid Maidenhead locator");
        }

        double longitude = -180.0;
        double latitude = -90.0;
        double longitudeStep = 20.0;
        double latitudeStep = 10.0;

        for (int index = 0; index < locator.length(); index += 2) {
            char longitudeChar = locator.charAt(index);
            char latitudeChar = locator.charAt(index + 1);

            if (index == 0 || index == 4) {
                int maximum = index == 0 ? 18 : 24;
                int longitudeValue = letterValue(longitudeChar, maximum);
                int latitudeValue = letterValue(latitudeChar, maximum);
                longitude += longitudeValue * longitudeStep;
                latitude += latitudeValue * latitudeStep;
            } else {
                int longitudeValue = digitValue(longitudeChar);
                int latitudeValue = digitValue(latitudeChar);
                longitude += longitudeValue * longitudeStep;
                latitude += latitudeValue * latitudeStep;
            }

            if (index + 2 < locator.length()) {
                if (index == 0) {
                    longitudeStep = 2.0;
                    latitudeStep = 1.0;
                } else if (index == 2) {
                    longitudeStep = 5.0 / 60.0;
                    latitudeStep = 2.5 / 60.0;
                } else if (index == 4) {
                    longitudeStep = 0.5 / 60.0;
                    latitudeStep = 0.25 / 60.0;
                }
            }
        }

        longitude += longitudeStep / 2.0;
        latitude += latitudeStep / 2.0;
        return new LocatorCoordinates(latitude, longitude);
    }

    private static int letterValue(char character, int maximumExclusive) {
        int value = character - 'A';
        if (value < 0 || value >= maximumExclusive) {
            throw new IllegalArgumentException("Invalid Maidenhead locator letter");
        }
        return value;
    }

    private static int digitValue(char character) {
        int value = character - '0';
        if (value < 0 || value > 9) {
            throw new IllegalArgumentException("Invalid Maidenhead locator digit");
        }
        return value;
    }

    private static final class AzimuthMapGenerator {

        private static final Color WATER_COLOR = new Color(226, 238, 242);
        private static final Color LAND_COLOR = new Color(150, 202, 152);
        private static final Color LAND_OUTLINE_COLOR = new Color(92, 144, 112);
        private static final Color GRID_COLOR = new Color(129, 151, 160, 130);
        private static final Color CENTER_COLOR = new Color(214, 70, 52);
        private static final EnumMap<MapStyle, BufferedImage> WORLD_MAPS = new EnumMap<>(MapStyle.class);

        private static final double[][][] ROUGH_LANDMASSES = {
                {
                        {-168, 72}, {-142, 70}, {-126, 58}, {-124, 48}, {-116, 33}, {-103, 22},
                        {-87, 17}, {-78, 8}, {-62, 12}, {-52, 48}, {-62, 60}, {-85, 72}, {-120, 76}
                },
                {
                        {-82, 12}, {-72, 8}, {-64, -5}, {-55, -18}, {-48, -34}, {-61, -55},
                        {-70, -48}, {-76, -28}, {-81, -8}
                },
                {
                        {-18, 36}, {-8, 58}, {18, 70}, {48, 64}, {78, 54}, {104, 62},
                        {142, 54}, {154, 45}, {130, 22}, {96, 8}, {78, 22}, {52, 14},
                        {42, -8}, {30, -34}, {15, -35}, {5, -5}, {-12, 8}
                },
                {
                        {-18, 35}, {10, 37}, {34, 28}, {48, 12}, {42, -16}, {30, -34},
                        {18, -35}, {8, -20}, {-8, -4}, {-16, 12}
                },
                {
                        {112, -11}, {154, -12}, {152, -38}, {132, -44}, {114, -33}
                },
                {
                        {-180, -63}, {-120, -70}, {-60, -66}, {0, -72}, {60, -66}, {120, -70}, {180, -63}
                }
        };

        private AzimuthMapGenerator() {
        }

        static GeneratedAzimuthMap generate(String locator, MapStyle mapStyle, int maximumDistanceKilometers, int size) {
            LocatorCoordinates origin = parseMaidenheadLocator(locator);
            MapStyle selectedMapStyle = mapStyle == null ? MapStyle.PHYSICAL : mapStyle;
            BufferedImage worldMap = worldMapFor(selectedMapStyle);
            int clampedMaximumDistanceKilometers = clampMapDistanceKilometers(maximumDistanceKilometers);
            double maximumAngularDistance = mapAngularDistanceForDistance(clampedMaximumDistanceKilometers);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int padding = 18;
                int radius = size / 2 - padding;
                int center = size / 2;
                Ellipse2D circle = new Ellipse2D.Double(center - radius, center - radius, radius * 2.0, radius * 2.0);

                g.setColor(WATER_COLOR);
                g.fill(circle);
                Shape oldClip = g.getClip();
                g.setClip(circle);
                if (worldMap == null) {
                    drawGraticule(g, origin, center, radius, maximumAngularDistance);
                    drawLand(g, origin, center, radius, maximumAngularDistance);
                } else {
                    drawProjectedRaster(image, worldMap, selectedMapStyle.sharpSampling(), origin, center, radius, maximumAngularDistance);
                    drawGraticule(g, origin, center, radius, maximumAngularDistance);
                }
                g.setClip(oldClip);

                g.setColor(new Color(255, 255, 255, 46));
                g.fill(circle);
                g.setColor(new Color(155, 174, 181));
                g.setStroke(new BasicStroke(3f));
                g.draw(circle);
                drawCenterMarker(g, center);
            } finally {
                g.dispose();
            }
            return new GeneratedAzimuthMap(image, origin, clampedMaximumDistanceKilometers);
        }

        private static synchronized BufferedImage worldMapFor(MapStyle mapStyle) {
            BufferedImage cachedMap = WORLD_MAPS.get(mapStyle);
            if (cachedMap != null) {
                return cachedMap;
            }
            BufferedImage loadedMap = loadWorldMap(mapStyle.resourcePath());
            if (loadedMap != null) {
                WORLD_MAPS.put(mapStyle, loadedMap);
            }
            return loadedMap;
        }

        private static BufferedImage loadWorldMap(String resourcePath) {
            try (InputStream input = ArcoRotorDesktopApplication.class.getResourceAsStream(resourcePath)) {
                if (input == null) {
                    return null;
                }
                return ImageIO.read(input);
            } catch (IOException exception) {
                return null;
            }
        }

        private static void drawProjectedRaster(
                BufferedImage output,
                BufferedImage worldMap,
                boolean sharpSampling,
                LocatorCoordinates origin,
                int center,
                int radius,
                double maximumAngularDistance
        ) {
            double lat1 = Math.toRadians(origin.latitude());
            double lon1 = Math.toRadians(origin.longitude());
            int min = Math.max(0, center - radius);
            int max = Math.min(output.getWidth() - 1, center + radius);
            double radiusSquared = (double) radius * radius;

            for (int y = min; y <= max; y++) {
                double dy = y - center;
                for (int x = min; x <= max; x++) {
                    double dx = x - center;
                    double distanceSquared = dx * dx + dy * dy;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    double angularDistance = Math.sqrt(distanceSquared) / radius * maximumAngularDistance;
                    double bearing = Math.atan2(dx, -dy);
                    double sinLat1 = Math.sin(lat1);
                    double cosLat1 = Math.cos(lat1);
                    double sinDistance = Math.sin(angularDistance);
                    double cosDistance = Math.cos(angularDistance);
                    double latitude = Math.asin(sinLat1 * cosDistance + cosLat1 * sinDistance * Math.cos(bearing));
                    double longitude = lon1 + Math.atan2(
                            Math.sin(bearing) * sinDistance * cosLat1,
                            cosDistance - sinLat1 * Math.sin(latitude)
                    );

                    output.setRGB(
                            x,
                            y,
                            sampleWorldMap(worldMap, Math.toDegrees(latitude), normalizeLongitude(Math.toDegrees(longitude)), sharpSampling)
                    );
                }
            }
        }

        private static int sampleWorldMap(BufferedImage worldMap, double latitude, double longitude, boolean sharpSampling) {
            double wrappedLongitude = normalizeLongitude(longitude);
            double sourceX = (wrappedLongitude + 180.0) / 360.0 * (worldMap.getWidth() - 1);
            double sourceY = (90.0 - latitude) / 180.0 * (worldMap.getHeight() - 1);
            if (sharpSampling) {
                int x = Math.max(0, Math.min(worldMap.getWidth() - 1, (int) Math.round(sourceX)));
                int y = Math.max(0, Math.min(worldMap.getHeight() - 1, (int) Math.round(sourceY)));
                return worldMap.getRGB(x, y);
            }
            return bilinearSample(worldMap, sourceX, sourceY);
        }

        private static int bilinearSample(BufferedImage image, double sourceX, double sourceY) {
            int x0 = Math.max(0, Math.min(image.getWidth() - 1, (int) Math.floor(sourceX)));
            int y0 = Math.max(0, Math.min(image.getHeight() - 1, (int) Math.floor(sourceY)));
            int x1 = Math.max(0, Math.min(image.getWidth() - 1, x0 + 1));
            int y1 = Math.max(0, Math.min(image.getHeight() - 1, y0 + 1));
            double xWeight = sourceX - Math.floor(sourceX);
            double yWeight = sourceY - Math.floor(sourceY);

            int top = interpolateColor(image.getRGB(x0, y0), image.getRGB(x1, y0), xWeight);
            int bottom = interpolateColor(image.getRGB(x0, y1), image.getRGB(x1, y1), xWeight);
            return interpolateColor(top, bottom, yWeight);
        }

        private static int interpolateColor(int first, int second, double weight) {
            int alpha = interpolateChannel(first >>> 24, second >>> 24, weight);
            int red = interpolateChannel(first >>> 16, second >>> 16, weight);
            int green = interpolateChannel(first >>> 8, second >>> 8, weight);
            int blue = interpolateChannel(first, second, weight);
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        private static int interpolateChannel(int first, int second, double weight) {
            return (int) Math.round((first & 0xFF) * (1.0 - weight) + (second & 0xFF) * weight);
        }

        private static double normalizeLongitude(double longitude) {
            double normalizedLongitude = longitude;
            while (normalizedLongitude < -180.0) {
                normalizedLongitude += 360.0;
            }
            while (normalizedLongitude > 180.0) {
                normalizedLongitude -= 360.0;
            }
            return normalizedLongitude;
        }

        private static void drawGraticule(Graphics2D g, LocatorCoordinates origin, int center, int radius, double maximumAngularDistance) {
            g.setColor(GRID_COLOR);
            g.setStroke(new BasicStroke(1.2f));
            for (int latitude = -60; latitude <= 60; latitude += 30) {
                Path2D path = null;
                for (int longitude = -180; longitude <= 180; longitude += 3) {
                    ProjectedPoint point = project(origin, latitude, longitude, center, radius, maximumAngularDistance);
                    path = appendPoint(path, point);
                }
                if (path != null) {
                    g.draw(path);
                }
            }

            for (int longitude = -180; longitude < 180; longitude += 30) {
                Path2D path = null;
                for (int latitude = -85; latitude <= 85; latitude += 3) {
                    ProjectedPoint point = project(origin, latitude, longitude, center, radius, maximumAngularDistance);
                    path = appendPoint(path, point);
                }
                if (path != null) {
                    g.draw(path);
                }
            }
        }

        private static void drawLand(Graphics2D g, LocatorCoordinates origin, int center, int radius, double maximumAngularDistance) {
            g.setStroke(new BasicStroke(1.4f));
            for (double[][] landmass : ROUGH_LANDMASSES) {
                Path2D path = new Path2D.Double();
                boolean started = false;
                for (int index = 0; index < landmass.length; index++) {
                    double[] from = landmass[index];
                    double[] to = landmass[(index + 1) % landmass.length];
                    int samples = Math.max(2, (int) Math.ceil(Math.abs(to[0] - from[0]) / 4.0));
                    for (int sample = 0; sample < samples; sample++) {
                        double fraction = sample / (double) samples;
                        double longitude = from[0] + (to[0] - from[0]) * fraction;
                        double latitude = from[1] + (to[1] - from[1]) * fraction;
                        ProjectedPoint point = project(origin, latitude, longitude, center, radius, maximumAngularDistance);
                        if (!started) {
                            path.moveTo(point.x(), point.y());
                            started = true;
                        } else {
                            path.lineTo(point.x(), point.y());
                        }
                    }
                }
                path.closePath();
                g.setColor(LAND_COLOR);
                g.fill(path);
                g.setColor(LAND_OUTLINE_COLOR);
                g.draw(path);
            }
        }

        private static Path2D appendPoint(Path2D path, ProjectedPoint point) {
            if (path == null) {
                path = new Path2D.Double();
                path.moveTo(point.x(), point.y());
            } else {
                path.lineTo(point.x(), point.y());
            }
            return path;
        }

        private static ProjectedPoint project(
                LocatorCoordinates origin,
                double latitude,
                double longitude,
                int center,
                int radius,
                double maximumAngularDistance
        ) {
            double lat1 = Math.toRadians(origin.latitude());
            double lon1 = Math.toRadians(origin.longitude());
            double lat2 = Math.toRadians(latitude);
            double lon2 = Math.toRadians(longitude);
            double deltaLongitude = lon2 - lon1;

            double cosDistance = Math.sin(lat1) * Math.sin(lat2)
                    + Math.cos(lat1) * Math.cos(lat2) * Math.cos(deltaLongitude);
            double angularDistance = Math.acos(Math.max(-1.0, Math.min(1.0, cosDistance)));
            double azimuth = Math.atan2(
                    Math.sin(deltaLongitude) * Math.cos(lat2),
                    Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLongitude)
            );
            double projectedRadius = angularDistance / maximumAngularDistance * radius;
            double x = center + Math.sin(azimuth) * projectedRadius;
            double y = center - Math.cos(azimuth) * projectedRadius;
            return new ProjectedPoint(x, y);
        }

        private static void drawCenterMarker(Graphics2D g, int center) {
            g.setColor(CENTER_COLOR);
            g.fillOval(center - 5, center - 5, 10, 10);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(center - 14, center, center + 14, center);
            g.drawLine(center, center - 14, center, center + 14);
        }

        private record ProjectedPoint(double x, double y) {
        }
    }
}
