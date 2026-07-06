package cz.ok1xoe.macrotor.desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
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
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.ResourceBundle;

public class MacRotorDesktopApplication {

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
    private static final long POLL_INTERVAL_MS = 150;
    private static final int MAX_TARGET_AZIMUTH = 360;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MacRotorFrame frame = new MacRotorFrame();
            frame.setVisible(true);
        });
    }

    private static final class MacRotorFrame extends JFrame {

        private final JTextField hostField = new JTextField("192.168.15.131", 15);
        private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(4001, 1, 65535, 1));
        private final JComboBox<Language> languageCombo = new JComboBox<>(Language.values());
        private final JButton connectButton = new JButton("Pripojit");
        private final JLabel statusLabel = new JLabel("");
        private final JLabel ipLabel = createFormLabel("");
        private final JLabel tcpPortLabel = createFormLabel("");
        private final JLabel languageLabel = createFormLabel("");
        private final JLabel captionLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel hintLabel = new JLabel("");
        private final JLabel headingLabel = new JLabel("---", SwingConstants.CENTER);
        private final JTextField headingEditField = new JTextField(4);
        private final JPanel headingCards = new JPanel(new CardLayout());
        private final CompassPanel compassPanel = new CompassPanel(this::moveToClickedAzimuth);
        private final JButton ccwButton = new JButton("CCW");
        private final JButton cwButton = new JButton("CW");
        private final PresetStore presetStore = new PresetStore();
        private final PresetButton[] presetButtons = new PresetButton[10];

        private ScheduledExecutorService executor;
        private ScheduledFuture<?> pollingTask;
        private TcpRotorClient client;
        private final Preferences windowPreferences = Preferences.userNodeForPackage(MacRotorDesktopApplication.class).node("window");
        private I18n i18n;
        private volatile boolean connected;
        private Integer currentHeading;
        private Integer targetHeading;
        private PresetButton activePresetButton;
        private boolean refreshHeadingAfterRelativeError;

        MacRotorFrame() {
            super();
            this.i18n = new I18n(Language.fromCode(windowPreferences.get("language", Language.EN.code())));
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(900, 660));
            setLocationByPlatform(true);

            JPanel root = new JPanel(new BorderLayout(18, 18));
            root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            root.setBackground(APP_BACKGROUND);
            setContentPane(root);

            root.add(createTopPanel(), BorderLayout.NORTH);
            root.add(createCenterPanel(), BorderLayout.CENTER);
            root.add(createBottomPanel(), BorderLayout.SOUTH);

            connectButton.addActionListener(event -> toggleConnection());
            languageCombo.setSelectedItem(i18n.language());
            languageCombo.addActionListener(event -> changeLanguage((Language) languageCombo.getSelectedItem()));
            updateTexts();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    disconnect();
                }
            });

            pack();
        }

        private JPanel createTopPanel() {
            JPanel connectionPanel = new JPanel(new GridBagLayout());
            connectionPanel.setOpaque(false);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 0, 0, 10);
            constraints.gridy = 0;

            constraints.gridx = 0;
            connectionPanel.add(ipLabel, constraints);
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            connectionPanel.add(hostField, constraints);

            constraints.gridx = 2;
            constraints.fill = GridBagConstraints.NONE;
            connectionPanel.add(tcpPortLabel, constraints);
            constraints.gridx = 3;
            portSpinner.setPreferredSize(new Dimension(90, 30));
            connectionPanel.add(portSpinner, constraints);

            constraints.gridx = 4;
            styleCommandButton(connectButton, ACTION_GREEN, Color.WHITE);
            connectionPanel.add(connectButton, constraints);

            constraints.gridx = 5;
            connectionPanel.add(languageLabel, constraints);

            constraints.gridx = 6;
            languageCombo.setPreferredSize(new Dimension(74, 30));
            connectionPanel.add(languageCombo, constraints);

            return connectionPanel;
        }

        private JPanel createManualRotatePanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 0, 0, 10);
            constraints.gridy = 0;

            configureManualRotateButton(ccwButton, false);
            configureManualRotateButton(cwButton, true);

            constraints.gridx = 0;
            panel.add(ccwButton, constraints);

            constraints.gridx = 1;
            panel.add(cwButton, constraints);

            return panel;
        }

        private void configureManualRotateButton(JButton button, boolean clockwise) {
            button.setPreferredSize(new Dimension(120, 58));
            button.setFont(button.getFont().deriveFont(Font.BOLD, 18f));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBackground(BUTTON_BACKGROUND);
            button.setForeground(TEXT_PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
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
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
            return label;
        }

        private static void styleCommandButton(JButton button, Color background, Color foreground) {
            button.setOpaque(true);
            button.setBackground(background);
            button.setForeground(foreground);
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
            ipLabel.setText(t("label.ip"));
            tcpPortLabel.setText(t("label.tcpPort"));
            languageLabel.setText(t("label.language"));
            connectButton.setText(connected ? t("button.disconnect") : t("button.connect"));
            ccwButton.setText(t("button.ccw"));
            cwButton.setText(t("button.cw"));
            captionLabel.setText(t("label.currentAzimuth"));
            headingLabel.setToolTipText(t("tooltip.heading"));
            hintLabel.setText(t("hint.main"));
            compassPanel.setDegreeSuffix(t("degree"));
            for (PresetButton presetButton : presetButtons) {
                if (presetButton != null) {
                    presetButton.refreshText();
                }
            }
            updateStatusVisibility();
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

            JPanel readoutPanel = new JPanel(new BorderLayout(0, 0));
            readoutPanel.setBackground(PANEL_BACKGROUND);
            readoutPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(18, 18, 18, 18)
            ));
            captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD, 30f));
            captionLabel.setForeground(TEXT_SECONDARY);
            captionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
            statusLabel.setPreferredSize(new Dimension(320, 66));
            statusLabel.setMinimumSize(new Dimension(320, 66));
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(7, 10, 7, 10)
            ));
            readoutPanel.add(captionLabel, BorderLayout.NORTH);
            readoutPanel.add(headingCards, BorderLayout.CENTER);

            JPanel southPanel = new JPanel(new BorderLayout(0, 10));
            southPanel.setOpaque(false);
            southPanel.add(statusLabel, BorderLayout.NORTH);
            southPanel.add(createManualRotatePanel(), BorderLayout.SOUTH);
            readoutPanel.add(southPanel, BorderLayout.SOUTH);

            constraints.gridx = 0;
            constraints.weightx = 0.48;
            panel.add(readoutPanel, constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.52;
            panel.add(compassPanel, constraints);

            return panel;
        }

        private JPanel createBottomPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.setOpaque(false);

            hintLabel.setForeground(TEXT_SECONDARY);

            panel.add(hintLabel, BorderLayout.NORTH);
            panel.add(createPresetPanel(), BorderLayout.CENTER);

            return panel;
        }

        private JPanel createPresetPanel() {
            JPanel panel = new JPanel(new GridLayout(2, 5, 8, 8));
            panel.setOpaque(false);

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
                client = new TcpRotorClient(hostField.getText(), (Integer) portSpinner.getValue());
                executor = Executors.newSingleThreadScheduledExecutor();
                connected = true;
                setConnectionControls(false);
                connectButton.setText(t("button.disconnect"));
                clearStatus();
                executor.execute(this::pollHeading);
                pollingTask = executor.scheduleAtFixedRate(this::pollHeading, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } catch (RuntimeException exception) {
                showError(exception.getMessage());
            }
        }

        private void disconnect() {
            connected = false;
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
            targetHeading = null;
            refreshHeadingAfterRelativeError = false;
            setActivePresetButton(null);
            compassPanel.setTargetHeading(null);
            setConnectionControls(true);
            connectButton.setText(t("button.connect"));
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

            targetHeading = target;
            compassPanel.setTargetHeading(target);
            setActivePresetButton(presetButton);
            clearStatus();
            executor.execute(() -> {
                try {
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
            targetHeading = null;
            compassPanel.setTargetHeading(null);
            clearStatus();
            executor.execute(() -> {
                try {
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
            headingLabel.setText(Integer.toString(heading));
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
            hostField.setEnabled(enabled);
            portSpinner.setEnabled(enabled);
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

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;

            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(new JLabel(t("label.presetLabel")), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(labelField, constraints);

            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            panel.add(new JLabel(t("label.azimuth")), constraints);

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
                this.button.setPreferredSize(new Dimension(120, 58));
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
                button.setText("<html><center>" + escapeHtml(preset.label()) + "<br><b>" + preset.azimuth() + " " + t("degree") + "</b></center></html>");
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
        private final Preferences preferences = Preferences.userNodeForPackage(MacRotorDesktopApplication.class).node("presets");

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

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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

    private static final class I18n {

        private final Language language;
        private final ResourceBundle bundle;

        private I18n(Language language) {
            this.language = language;
            this.bundle = ResourceBundle.getBundle("cz.ok1xoe.macrotor.desktop.messages", language.locale());
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
        private String degreeSuffix = "deg";

        private final IntConsumer azimuthClickHandler;

        CompassPanel(IntConsumer azimuthClickHandler) {
            this.azimuthClickHandler = azimuthClickHandler;
            setOpaque(false);
            setPreferredSize(new Dimension(360, 360));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR));
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

        void setDegreeSuffix(String degreeSuffix) {
            this.degreeSuffix = degreeSuffix;
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

                g.setPaint(new GradientPaint(x, y, Color.WHITE, x + size, y + size, new Color(226, 235, 238)));
                g.fillOval(x, y, size, size);
                g.setColor(BORDER_COLOR);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(x, y, size, size);

                drawTicks(g, centerX, centerY, radius);
                drawLabels(g, centerX, centerY, radius);

                if (targetHeading != null) {
                    drawTargetLine(g, centerX, centerY, radius, targetHeading);
                }

                if (heading >= 0) {
                    drawNeedle(g, centerX, centerY, radius, heading);
                    drawHeadingText(g, centerX, centerY, heading);
                } else {
                    drawHeadingText(g, centerX, centerY, 0);
                }
            } finally {
                g.dispose();
            }
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

        private static void drawLabels(Graphics2D g, int centerX, int centerY, int radius) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.setColor(TEXT_PRIMARY);
            drawCenteredString(g, "N", centerX, centerY - radius + 48);
            drawCenteredString(g, "E", centerX + radius - 48, centerY);
            drawCenteredString(g, "S", centerX, centerY + radius - 42);
            drawCenteredString(g, "W", centerX - radius + 48, centerY);
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

        private void drawHeadingText(Graphics2D g, int centerX, int centerY, int heading) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            g.setColor(TEXT_PRIMARY);
            drawCenteredString(g, heading + " " + degreeSuffix, centerX, centerY + 60);
        }

        private static void drawCenteredString(Graphics2D g, String text, int x, int y) {
            FontMetrics metrics = g.getFontMetrics();
            int textX = x - metrics.stringWidth(text) / 2;
            int textY = y - metrics.getHeight() / 2 + metrics.getAscent();
            g.drawString(text, textX, textY);
        }
    }
}
