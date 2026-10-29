import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;

public class GameWindow {
    private static final Color BACKGROUND = new Color(19, 33, 30);
    private static final Color PANEL = new Color(29, 49, 42);
    private static final Color CREAM = new Color(239, 232, 207);
    private static final Color GOLD = new Color(202, 166, 85);
    private static final Color GREEN = new Color(45, 105, 76);
    private final JFrame frame = new JFrame("LOKI: TVA Missions");
    private final MissionControlFacade control = new MissionControlFacade();
    private final CardLayout layout = new CardLayout();
    private final JPanel screens = new JPanel(layout);
    private final JPanel home = vertical();
    private final JLabel progress = label("", 15, GOLD);
    private final JLabel setupLocation = label("", 26, GOLD);
    private final JTextArea setupBriefing = text("");
    private final JTextField missionTitle = new JTextField("Стабилизировать временную ветку");
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JLabel difficultyInfo = label("", 17, CREAM);
    private MissionLocation selectedLocation = MissionLocation.NEW_YORK;
    private Mission preparedMission;
    private final LokiCreator[] creators = {new LokiCreator.OrdinaryCreator(),
            new LokiCreator.ClassicCreator(), new LokiCreator.AlligatorCreator(),
            new LokiCreator.SylvieCreator()};
    private LokiCreator selectedCreator = creators[0];
    private final JCheckBox amplifier = new JCheckBox("Усилитель магии: +15 к магии");
    private final JCheckBox amulet = new JCheckBox("Амулет переговоров: +15 к переговорам");
    private final JCheckBox shield = new JCheckBox("Временной щит: штраф за неудачу +10 вместо +15");
    private final JTextArea heroPreview = text("");
    private final JLabel sceneTitle = label("", 25, GOLD);
    private final JTextArea sceneText = text("");
    private final JTextArea sceneHint = text("");
    private final JTextArea heroStats = text("");
    private final JTextArea turnFeedback = text("");
    private final JTextArea eventText = text("");
    private final JProgressBar instability = new JProgressBar(0, 100);
    private final JLabel timelineStatus = label("", 17, CREAM);
    private final TimelineMonitor monitor = new TimelineMonitor(instability, timelineStatus);
    private final ButtonGroup actionGroup = new ButtonGroup();
    private final JRadioButton[] actionButtons = new JRadioButton[3];
    private final ActionStrategy[] strategies = {new NegotiateStrategy(), new IllusionStrategy(), new CombatStrategy()};
    private ActionStrategy selectedStrategy;
    private final JButton confirm = button("Подтвердить действие", this::performAction);
    private final JButton next = button("Далее", this::nextScene);
    private final JTextArea resultText = text("");
    private final DefaultListModel<MissionResult> archiveModel = new DefaultListModel<>();
    private final JList<MissionResult> archiveList = new JList<>(archiveModel);
    private final JTextArea archiveDetails = text("Пока нет завершённых миссий.");
    private final JButton retryArchive = button("Повторить выбранную", this::retrySelected);

    public GameWindow() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (leaveMission()) { frame.dispose(); }
            }
        });
        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(24, 28, 18, 28));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label("LOKI  /  TVA MISSIONS", 30, GOLD), BorderLayout.WEST);
        header.add(row(button("Штаб", this::goHome), button("Архив", this::goArchive)), BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        screens.setOpaque(false);
        screens.add(home, "home");
        screens.add(buildSetup(), "setup");
        screens.add(buildLoadout(), "loadout");
        screens.add(buildMission(), "mission");
        screens.add(buildResult(), "result");
        screens.add(buildArchive(), "archive");
        root.add(screens, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(progress, BorderLayout.WEST);
        footer.add(label("Прогресс и архив хранятся до закрытия приложения", 13, CREAM), BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        alignColumns(root);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(1060, 760));
        frame.setSize(1200, 830);
        frame.setLocationRelativeTo(null);
        refreshHome();
        layout.show(screens, "home");
    }

    public void show() { frame.setVisible(true); }

    private void refreshHome() {
        home.removeAll();
        home.add(label("Временные аномалии", 26, CREAM));
        home.add(Box.createVerticalStrut(10));
        home.add(text("Выберите миссию. Подготовьте Локи и пройдите три сцены.\n"
                + "Цель: после третьей сцены снизить нестабильность до 20 или меньше."));
        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 0));
        cards.setOpaque(false);
        EnumSet<MissionLocation> completed = EnumSet.noneOf(MissionLocation.class);
        for (MissionResult result : control.getArchive()) {
            if (result.success()) { completed.add(result.mission().getLocation()); }
        }
        for (MissionLocation location : MissionLocation.values()) {
            JPanel card = vertical();
            card.setBackground(PANEL);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD),
                    new EmptyBorder(24, 20, 24, 20)));
            card.add(label(location.toString(), 23, GOLD));
            card.add(Box.createVerticalStrut(20));
            card.add(text(location.getBriefing()));
            card.add(Box.createVerticalStrut(20));
            card.add(label(completed.contains(location) ? "Ветка стабилизирована" : "3 сцены / 3 тактики", 16, CREAM));
            card.add(Box.createVerticalGlue());
            JButton prepare = button("Подготовить", () -> prepare(location));
            prepare.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            prepare.setEnabled(control.isUnlocked(location));
            if (!prepare.isEnabled()) { prepare.setText("Пока недоступна"); }
            card.add(prepare);
            cards.add(card);
        }
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        home.add(Box.createVerticalStrut(22));
        home.add(cards);
        home.add(Box.createVerticalStrut(24));
        home.add(text(completed.size() == 3 ? "Кампания завершена! Можно переиграть любую миссию."
                : "Одинаковые решения дают одинаковый результат. Читайте подсказки и меняйте тактику."));
        progress.setText("Кампания: " + completed.size() + " / 3");
        alignColumns(home);
        home.revalidate();
        home.repaint();
    }

    private JPanel buildSetup() {
        JPanel panel = vertical();
        panel.add(setupLocation);
        panel.add(Box.createVerticalStrut(16));
        panel.add(setupBriefing);
        panel.add(Box.createVerticalStrut(20));
        panel.add(label("Название миссии", 17, GOLD));
        missionTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.add(missionTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(label("Сложность", 17, GOLD));
        difficulty.setMaximumSize(new Dimension(380, 42));
        difficulty.setSelectedItem(Difficulty.STANDARD);
        difficulty.addActionListener(event -> updateDifficulty());
        panel.add(difficulty);
        panel.add(Box.createVerticalStrut(16));
        panel.add(difficultyInfo);
        panel.add(Box.createVerticalStrut(25));
        panel.add(text("Успех в первых двух сценах: −25 к нестабильности. В третьей: −20.\n"
                + "Неудача: +15; со щитом: +10. При значении 100 миссия заканчивается сразу."));
        panel.add(Box.createVerticalGlue());
        panel.add(row(button("Назад в штаб", this::goHome), button("Выбрать Локи", () -> {
            try {
                preparedMission = new Mission.Builder().title(missionTitle.getText().trim())
                        .location(selectedLocation).difficulty((Difficulty) difficulty.getSelectedItem()).build();
                updateHeroPreview();
                layout.show(screens, "loadout");
            } catch (IllegalArgumentException error) { showError(error); }
        })));
        return panel;
    }

    private void prepare(MissionLocation location) {
        selectedLocation = location;
        setupLocation.setText("Подготовка / " + location);
        setupBriefing.setText(location.getBriefing());
        updateDifficulty();
        layout.show(screens, "setup");
    }

    private void updateDifficulty() {
        Difficulty value = (Difficulty) difficulty.getSelectedItem();
        difficultyInfo.setText("Старт: " + value.getStartingInstability()
                + " / 100     Порог действия: " + value.getThreshold() + "     Сцен: 3");
    }

    private JPanel buildLoadout() {
        JPanel panel = vertical();
        panel.add(label("Выберите вариант Локи", 26, GOLD));
        panel.add(Box.createVerticalStrut(20));
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        ButtonGroup variants = new ButtonGroup();
        for (LokiCreator creator : creators) {
            JPanel card = vertical();
            card.setBackground(PANEL);
            card.setBorder(new EmptyBorder(20, 18, 20, 18));
            JRadioButton choice = new JRadioButton(creator.toString());
            choice.setSelected(creator == selectedCreator);
            choice.addActionListener(event -> { selectedCreator = creator; updateHeroPreview(); });
            variants.add(choice);
            card.add(choice);
            card.add(Box.createVerticalStrut(12));
            card.add(text(stats(creator.createVariant())));
            cards.add(card);
        }
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        panel.add(cards);
        panel.add(Box.createVerticalStrut(20));
        panel.add(amplifier);
        panel.add(amulet);
        panel.add(shield);
        amplifier.addActionListener(event -> updateHeroPreview());
        amulet.addActionListener(event -> updateHeroPreview());
        shield.addActionListener(event -> updateHeroPreview());
        panel.add(Box.createVerticalStrut(24));
        panel.add(label("Итоговая подготовка", 20, GOLD));
        panel.add(heroPreview);
        panel.add(Box.createVerticalGlue());
        panel.add(row(button("Назад", () -> layout.show(screens, "setup")),
                button("Войти в портал", this::startMission)));
        return panel;
    }

    private LokiVariant createEquippedHero() {
        LokiVariant hero = selectedCreator.createVariant();
        if (amplifier.isSelected()) { hero = new MagicAmplifier(hero); }
        if (amulet.isSelected()) { hero = new DiplomacyAmulet(hero); }
        if (shield.isSelected()) { hero = new TimeShield(hero); }
        return hero;
    }

    private void updateHeroPreview() {
        LokiVariant hero = createEquippedHero();
        heroPreview.setText(hero.getDescription() + "\n" + stats(hero)
                + "\nШтраф при неудаче: +" + hero.getFailurePenalty());
    }

    private void startMission() {
        try {
            control.startMission(preparedMission, createEquippedHero());
            control.getTimeline().addListener(monitor);
            monitor.showValue(control.getTimeline().getInstability());
            loadScene();
            layout.show(screens, "mission");
        } catch (IllegalArgumentException | IllegalStateException error) { showError(error); }
    }

    private JPanel buildMission() {
        JPanel panel = new JPanel(new BorderLayout(20, 16));
        panel.setOpaque(false);
        JPanel body = vertical();
        instability.setStringPainted(true);
        instability.setPreferredSize(new Dimension(600, 30));
        body.add(instability);
        body.add(timelineStatus);
        body.add(Box.createVerticalStrut(24));
        body.add(sceneTitle);
        body.add(Box.createVerticalStrut(20));
        body.add(sceneText);
        body.add(Box.createVerticalStrut(15));
        body.add(sceneHint);
        body.add(Box.createVerticalStrut(22));
        JPanel choices = new JPanel(new GridLayout(1, 3, 10, 0));
        choices.setOpaque(false);
        for (int i = 0; i < strategies.length; i++) {
            ActionStrategy strategy = strategies[i];
            JRadioButton choice = new JRadioButton(strategy.toString());
            choice.addActionListener(event -> { selectedStrategy = strategy; confirm.setEnabled(true); });
            actionButtons[i] = choice;
            actionGroup.add(choice);
            choices.add(choice);
        }
        choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        body.add(choices);
        body.add(Box.createVerticalStrut(15));
        body.add(turnFeedback);
        body.add(Box.createVerticalGlue());
        body.add(row(confirm, next));
        panel.add(body, BorderLayout.CENTER);
        JPanel sidebar = vertical();
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBackground(PANEL);
        sidebar.setBorder(new EmptyBorder(18, 18, 18, 18));
        sidebar.add(label("Вариант", 20, GOLD));
        heroStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        sidebar.add(heroStats);
        sidebar.add(Box.createVerticalStrut(25));
        sidebar.add(label("Журнал TVA", 18, GOLD));
        sidebar.add(new JScrollPane(eventText));
        panel.add(sidebar, BorderLayout.EAST);
        return panel;
    }

    private void loadScene() {
        Scene scene = control.getCurrentScene();
        sceneTitle.setText(control.getMission().getLocation() + " / сцена " + control.getSceneNumber() + " из 3");
        sceneText.setText(scene.title() + "\n\n" + scene.story());
        sceneHint.setText("Подсказка: " + scene.hint());
        heroStats.setText(control.getHero().getDescription() + "\n\n" + stats(control.getHero())
                + "\n\nПорог: " + control.getMission().getDifficulty().getThreshold());
        turnFeedback.setText("Выберите тактику и подтвердите действие.");
        actionGroup.clearSelection();
        selectedStrategy = null;
        for (JRadioButton choice : actionButtons) { choice.setEnabled(true); }
        confirm.setEnabled(false);
        next.setEnabled(false);
        next.setText("Следующая сцена");
        eventText.setText(String.join("\n\n", control.getEventEntries()));
    }

    private void performAction() {
        if (selectedStrategy == null) { return; }
        try {
            TurnResult turn = control.performAction(selectedStrategy);
            turnFeedback.setText(turn.describe());
            eventText.setText(String.join("\n\n", control.getEventEntries()));
            confirm.setEnabled(false);
            for (JRadioButton choice : actionButtons) { choice.setEnabled(false); }
            next.setText(control.isActive() ? "Следующая сцена" : "Показать результат");
            next.setEnabled(true);
        } catch (IllegalStateException error) { showError(error); }
    }

    private void nextScene() {
        if (control.isActive()) {
            control.nextScene();
            loadScene();
        } else {
            resultText.setText(control.getResult().describe());
            resultText.setCaretPosition(0);
            refreshHome();
            layout.show(screens, "result");
        }
    }

    private JPanel buildResult() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);
        panel.add(label("Отчёт о миссии", 26, GOLD), BorderLayout.NORTH);
        panel.add(new JScrollPane(resultText), BorderLayout.CENTER);
        panel.add(row(button("Повторить миссию", () -> prepare(control.getResult().mission().getLocation())),
                button("В штаб", this::goHome), button("Открыть архив", this::goArchive)), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildArchive() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);
        panel.add(label("Архив завершённых попыток", 26, GOLD), BorderLayout.NORTH);
        archiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        archiveList.setBackground(PANEL);
        archiveList.setForeground(CREAM);
        archiveList.setFont(new Font("Dialog", Font.PLAIN, 15));
        archiveList.setFixedCellHeight(48);
        archiveList.addListSelectionListener(event -> {
            MissionResult selected = archiveList.getSelectedValue();
            retryArchive.setEnabled(selected != null);
            if (selected != null) { archiveDetails.setText(selected.describe()); archiveDetails.setCaretPosition(0); }
        });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(archiveList), new JScrollPane(archiveDetails));
        split.setDividerLocation(390);
        split.setResizeWeight(0.38);
        panel.add(split, BorderLayout.CENTER);
        panel.add(row(retryArchive, button("В штаб", this::goHome)), BorderLayout.SOUTH);
        return panel;
    }

    private void retrySelected() {
        MissionResult selected = archiveList.getSelectedValue();
        if (selected != null) { prepare(selected.mission().getLocation()); }
    }

    private void goHome() {
        if (leaveMission()) { refreshHome(); layout.show(screens, "home"); }
    }

    private void goArchive() {
        if (!leaveMission()) { return; }
        archiveModel.clear();
        for (MissionResult result : control.getArchive()) { archiveModel.addElement(result); }
        archiveDetails.setText("Пока нет завершённых миссий.");
        retryArchive.setEnabled(false);
        if (!archiveModel.isEmpty()) { archiveList.setSelectedIndex(archiveModel.size() - 1); }
        layout.show(screens, "archive");
    }

    private boolean leaveMission() {
        if (!control.isActive()) { return true; }
        int answer = JOptionPane.showConfirmDialog(frame,
                "Прервать миссию? Незавершённая попытка не попадёт в архив.", "Выход из миссии",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) { return false; }
        control.abortMission();
        return true;
    }

    private void showError(RuntimeException error) {
        JOptionPane.showMessageDialog(frame, error.getMessage(), "Не удалось продолжить", JOptionPane.WARNING_MESSAGE);
    }

    private static String stats(LokiVariant hero) {
        return "Магия: " + hero.getMagicPower() + "\nБой: " + hero.getCombatPower()
                + "\nПереговоры: " + hero.getDiplomacy();
    }

    private static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        return panel;
    }

    private static void alignColumns(Container container) {
        for (Component child : container.getComponents()) {
            if (container.getLayout() instanceof BoxLayout && child instanceof JComponent component) {
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            if (child instanceof Container nested) { alignColumns(nested); }
        }
    }

    private static JPanel row(JComponent... items) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        for (JComponent item : items) { panel.add(item); }
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return panel;
    }

    private static JLabel label(String value, int size, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Dialog", Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    private static JTextArea text(String value) {
        JTextArea area = new JTextArea(value);
        area.setFont(new Font("Dialog", Font.PLAIN, 17));
        area.setForeground(CREAM);
        area.setBackground(BACKGROUND);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        return area;
    }

    private static JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.setFont(new Font("Dialog", Font.BOLD, 16));
        button.setBackground(GREEN);
        button.setForeground(CREAM);
        button.setMargin(new Insets(10, 14, 10, 14));
        button.addActionListener(event -> action.run());
        return button;
    }

    static void installTheme() {
        Font font = new Font("Dialog", Font.PLAIN, 17);
        String[] controls = {"Label", "Button", "ComboBox", "CheckBox", "RadioButton", "TextField", "OptionPane"};
        for (String name : controls) {
            UIManager.put(name + ".font", font);
            UIManager.put(name + ".background", PANEL);
            UIManager.put(name + ".foreground", CREAM);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("TextField.caretForeground", CREAM);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", GREEN);
        UIManager.put("ComboBox.selectionForeground", CREAM);
    }
}
