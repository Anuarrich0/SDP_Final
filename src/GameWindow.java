import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import static java.awt.BorderLayout.*;

public class GameWindow {
    private final JFrame frame = new JFrame("LOKI: TVA Missions");
    private final MissionControlFacade control;
    private final CampaignStore store;
    private boolean savingEnabled = true;
    private final JLabel saveStatus = TvaTheme.label("Прогресс сохраняется автоматически", 12, TvaTheme.MUTED);
    private final CardLayout layout = new CardLayout();
    private final JPanel screens = plain(new BorderLayout());
    private final JPanel home = plain(new BorderLayout(0, 20));
    private final JPanel results = plain(new BorderLayout(20, 20));
    private final JLabel pageName = TvaTheme.label("ОПЕРАТИВНЫЙ ШТАБ", 12, TvaTheme.MUTED);
    private final JLabel progress = TvaTheme.label("0 / 3", 28, TvaTheme.GOLD);
    private final JButton homeNav = TvaTheme.button("01    Миссии", false, this::goHome);
    private final JButton archiveNav = TvaTheme.button("02    Архив", false, this::goArchive);
    private MissionLocation selectedLocation = MissionLocation.NEW_YORK;
    private Mission preparedMission;
    private final JLabel setupTitle = TvaTheme.heading("", 30);
    private final JTextArea setupBriefing = TvaTheme.text("", 16);
    private final JTextField missionTitle = new JTextField("Стабилизировать временную ветку");
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JTextArea difficultyInfo = TvaTheme.text("", 16);
    private final TvaTheme.ArtPanel setupArt = new TvaTheme.ArtPanel("new_york", 360, true);
    private final LokiCreator[] creators = {new LokiCreator.OrdinaryCreator(), new LokiCreator.ClassicCreator(),
            new LokiCreator.AlligatorCreator(), new LokiCreator.SylvieCreator()};
    private final String[] portraits = {"loki", "classic", "alligator", "sylvie"};
    private final String[] roles = {"Мастер переговоров", "Сильная магия", "Решительный боец", "Чары и дипломатия"};
    private int selectedHero;
    private final JPanel[] heroCards = new JPanel[creators.length];
    private final JCheckBox amplifier = new JCheckBox("Усилитель магии");
    private final JCheckBox amulet = new JCheckBox("Амулет переговоров");
    private final JCheckBox shield = new JCheckBox("Временной щит");
    private final JTextArea heroPreview = TvaTheme.text("", 15);
    private final JTextArea heroStats = TvaTheme.text("", 15);
    private final JLabel sceneTitle = TvaTheme.heading("", 29);
    private final JLabel sceneNumber = TvaTheme.label("", 12, TvaTheme.GOLD);
    private final JLabel sceneLocation = TvaTheme.heading("", 28);
    private final JTextArea sceneText = TvaTheme.text("", 16);
    private final JTextArea sceneHint = TvaTheme.text("", 14);
    private final JTextArea turnFeedback = TvaTheme.text("", 15);
    private final JTextArea eventText = TvaTheme.text("", 14);
    private final JProgressBar instability = new JProgressBar(0, 100);
    private final JLabel timelineStatus = TvaTheme.label("", 14, TvaTheme.GOLD);
    private final TimelineMonitor monitor = new TimelineMonitor(instability, timelineStatus);
    private final TvaTheme.ArtPanel sceneArt = new TvaTheme.ArtPanel("new_york", 185, true);
    private final TvaTheme.ArtPanel heroArt = new TvaTheme.ArtPanel("loki", 160, false);
    private final ActionStrategy[] strategies = {new NegotiateStrategy(), new IllusionStrategy(), new CombatStrategy()};
    private final JToggleButton[] actionButtons = new JToggleButton[3];
    private final ButtonGroup actionGroup = new ButtonGroup();
    private ActionStrategy selectedStrategy;
    private final JButton confirm = TvaTheme.button("Подтвердить действие", true, this::performAction);
    private final JButton next = TvaTheme.button("Далее", false, this::nextScene);
    private final DefaultListModel<MissionResult> archiveModel = new DefaultListModel<>();
    private final JList<MissionResult> archiveList = new JList<>(archiveModel);
    private final JTextArea archiveDetails = TvaTheme.text("Пока нет завершённых миссий.", 15);
    private final JButton retryArchive = TvaTheme.button("Повторить миссию", true, this::retrySelected);

    public GameWindow() {
        this(Path.of(System.getProperty("user.home"), ".loki-tva", "campaign.xml"));
    }

    GameWindow(Path saveFile) {
        store = new CampaignStore(saveFile);
        MissionControlFacade restored;
        try {
            restored = new MissionControlFacade(store.load());
        } catch (IOException | IllegalArgumentException error) {
            restored = new MissionControlFacade();
            savingEnabled = false;
            saveStatus.setText("Сохранение не прочитано. Исходный файл оставлен без изменений.");
            saveStatus.setForeground(TvaTheme.AMBER);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Не удалось прочитать сохранение:\n" + saveFile + "\n\n" + error.getMessage()
                    + "\n\nИгра доступна в текущем сеансе. Чтобы снова включить сохранение,"
                    + "\nпереместите этот файл в другую папку и перезапустите игру.",
                    "Сохранение кампании", JOptionPane.WARNING_MESSAGE));
        }
        control = restored;
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (!leaveMission()) { return; }
                if (saveCampaign() || JOptionPane.showConfirmDialog(frame,
                        "Выйти без сохранения последних результатов?", "Выход из игры",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { frame.dispose(); }
            }
        });
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(TvaTheme.BACKGROUND);
        root.add(buildHeader(), NORTH);
        root.add(buildSidebar(), WEST);
        screens.setLayout(layout);
        screens.add(home, "home");
        screens.add(buildSetup(), "setup");
        screens.add(buildLoadout(), "loadout");
        screens.add(buildMission(), "mission");
        screens.add(results, "result");
        screens.add(buildArchive(), "archive");
        JPanel workspace = plain(new BorderLayout(0, 16));
        workspace.setBorder(new EmptyBorder(24, 26, 18, 26));
        workspace.add(pageName, NORTH);
        workspace.add(screens, CENTER);
        saveStatus.setToolTipText(saveFile.toAbsolutePath().toString());
        workspace.add(saveStatus, SOUTH);
        root.add(workspace, CENTER);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(1160, 800));
        Dimension display = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(Math.min(1440, display.width - 60), Math.min(980, display.height - 80));
        frame.setLocationRelativeTo(null);
        refreshHome();
        showScreen("home", "ОПЕРАТИВНЫЙ ШТАБ");
    }

    public void show() { frame.setVisible(true); }
    static void installTheme() { TvaTheme.install(); }

    private JPanel buildHeader() {
        JPanel header = plain(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TvaTheme.BORDER),
                new EmptyBorder(15, 26, 15, 26)));
        JLabel brand = TvaTheme.heading("LOKI", 32);
        brand.setForeground(TvaTheme.GOLD);
        header.add(TvaTheme.row(brand, TvaTheme.label("/  TVA MISSIONS", 14, TvaTheme.MUTED)), WEST);
        header.add(TvaTheme.label("TVA   /   ОТДЕЛ ВРЕМЕННЫХ АНОМАЛИЙ", 12, TvaTheme.GOLD), EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = plain(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, TvaTheme.BORDER),
                new EmptyBorder(30, 18, 24, 18)));
        homeNav.setHorizontalAlignment(SwingConstants.LEFT);
        archiveNav.setHorizontalAlignment(SwingConstants.LEFT);
        homeNav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        archiveNav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        sidebar.add(TvaTheme.column(12, TvaTheme.label("НАВИГАЦИЯ", 11, TvaTheme.MUTED), homeNav, archiveNav), NORTH);
        sidebar.add(TvaTheme.column(10, TvaTheme.label("КАМПАНИЯ", 11, TvaTheme.MUTED), progress,
                TvaTheme.text("Стабилизируйте три\nвременные ветки.", 13)), SOUTH);
        return sidebar;
    }

    private void showScreen(String screen, String title) {
        pageName.setText(title);
        homeNav.setSelected(!screen.equals("archive"));
        archiveNav.setSelected(screen.equals("archive"));
        layout.show(screens, screen);
    }

    private void refreshHome() {
        home.removeAll();
        EnumSet<MissionLocation> completed = EnumSet.noneOf(MissionLocation.class);
        for (MissionResult result : control.getArchive()) {
            if (result.success()) { completed.add(result.mission().getLocation()); }
        }
        progress.setText(completed.size() + " / 3");
        TvaTheme.ArtPanel banner = new TvaTheme.ArtPanel("new_york", 205, true);
        JPanel caption = TvaTheme.column(9, TvaTheme.label("ОПЕРАЦИЯ: СВЯЩЕННАЯ ЛИНИЯ ВРЕМЕНИ", 11, TvaTheme.GOLD),
                TvaTheme.heading(completed.size() == 3 ? "Время снова под контролем." : "Каждая ветка — новый выбор.", 30),
                TvaTheme.label("Выберите Локи. Измените тактику. Стабилизируйте реальность.", 15, TvaTheme.TEXT));
        caption.setBorder(new EmptyBorder(28, 26, 26, 26));
        banner.add(caption, SOUTH);
        home.add(banner, NORTH);
        JPanel cards = plain(new GridLayout(1, 3, 16, 0));
        for (MissionLocation location : MissionLocation.values()) {
            boolean unlocked = control.isUnlocked(location);
            TvaTheme.Surface card = TvaTheme.surface(12);
            card.setLayout(new BorderLayout(0, 15));
            card.add(new TvaTheme.ArtPanel(locationArt(location), 150, false), NORTH);
            card.setToolTipText(location.getBriefing());
            JPanel info = TvaTheme.column(10, TvaTheme.label("МИССИЯ 0" + (location.ordinal() + 1), 10, TvaTheme.GOLD),
                    TvaTheme.heading(location.toString(), 21));
            card.add(info, CENTER);
            String status = completed.contains(location) ? "Ветка стабилизирована" : unlocked ? "3 сцены / 3 тактики" : "После предыдущей миссии";
            JButton prepare = TvaTheme.button(unlocked ? "Подготовить  →" : "Недоступно", unlocked, () -> prepare(location));
            prepare.setEnabled(unlocked);
            prepare.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            card.add(TvaTheme.column(12, TvaTheme.label(status, 12, TvaTheme.MUTED), prepare), SOUTH);
            cards.add(card);
        }
        home.add(cards, CENTER);
        home.add(TvaTheme.label("Цель: нестабильность ≤ 20 после трёх сцен. При 100 ветка разрушается.", 13, TvaTheme.MUTED), SOUTH);
        home.revalidate();
        home.repaint();
    }

    private JPanel buildSetup() {
        JPanel page = plain(new BorderLayout(24, 18));
        page.add(titleBlock("01 / ПОДГОТОВКА", "Соберите миссию", "Выберите сложность перед отправкой в другую реальность."), NORTH);
        JPanel fields = TvaTheme.column(16, setupTitle, setupBriefing,
                TvaTheme.label("НАЗВАНИЕ МИССИИ", 11, TvaTheme.GOLD), missionTitle,
                TvaTheme.label("СЛОЖНОСТЬ", 11, TvaTheme.GOLD), difficulty, difficultyInfo);
        missionTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        missionTitle.setBorder(new EmptyBorder(10, 12, 10, 12));
        difficulty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        difficulty.setSelectedItem(Difficulty.STANDARD);
        difficulty.addActionListener(event -> updateDifficulty());
        setupBriefing.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        setupBriefing.setRows(2);
        difficultyInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        difficultyInfo.setRows(2);
        TvaTheme.Surface form = TvaTheme.surface(24);
        form.setLayout(new BorderLayout());
        form.add(fields, NORTH);
        JPanel split = plain(new GridLayout(1, 2, 22, 0));
        split.add(form);
        JPanel artCaption = TvaTheme.column(10, TvaTheme.heading("Одна миссия. Три решения.", 23),
                TvaTheme.text("Успех снижает нестабильность на 25 / 25 / 20.\nНеудача добавляет 15; со щитом — 10.", 15));
        artCaption.setBorder(new EmptyBorder(22, 22, 22, 22));
        setupArt.add(artCaption, SOUTH);
        split.add(setupArt);
        page.add(split, CENTER);
        page.add(TvaTheme.row(TvaTheme.button("Назад", false, this::goHome),
                TvaTheme.button("Выбрать вариант Локи  →", true, () -> {
                    try {
                        preparedMission = new Mission.Builder().title(missionTitle.getText().trim())
                                .location(selectedLocation).difficulty((Difficulty) difficulty.getSelectedItem()).build();
                        updateHeroPreview();
                        showScreen("loadout", "ПОДГОТОВКА / ВАРИАНТ И СНАРЯЖЕНИЕ");
                    } catch (IllegalArgumentException error) { showError(error); }
                })), SOUTH);
        return page;
    }

    private void prepare(MissionLocation location) {
        selectedLocation = location;
        setupTitle.setText(location.toString());
        setupBriefing.setText(location.getBriefing());
        setupArt.setAsset(locationArt(location));
        updateDifficulty();
        showScreen("setup", "ПОДГОТОВКА / ПАРАМЕТРЫ МИССИИ");
    }

    private void updateDifficulty() {
        Difficulty value = (Difficulty) difficulty.getSelectedItem();
        difficultyInfo.setText("Нестабильность на старте: " + value.getStartingInstability() + " / 100\n"
                + "Порог действия: " + value.getThreshold() + "   /   Сцен: 3");
    }

    private JPanel buildLoadout() {
        JPanel page = plain(new BorderLayout(22, 20));
        page.add(titleBlock("02 / ВАРИАНТ И СНАРЯЖЕНИЕ", "У каждого Локи — свой подход", "Подберите героя и улучшения под выбранную миссию."), NORTH);
        JPanel cards = plain(new GridLayout(2, 2, 14, 14));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < creators.length; i++) {
            final int index = i;
            LokiVariant hero = creators[i].createVariant();
            TvaTheme.Surface card = TvaTheme.surface(12);
            card.setLayout(new BorderLayout(14, 0));
            TvaTheme.ArtPanel portrait = new TvaTheme.ArtPanel(portraits[i], 180, false);
            portrait.setPreferredSize(new Dimension(112, 180));
            card.add(portrait, WEST);
            JToggleButton choose = TvaTheme.toggle("Выбрать");
            choose.setSelected(i == selectedHero);
            choose.addActionListener(event -> { selectedHero = index; updateHeroPreview(); });
            group.add(choose);
            JPanel details = plain(new BorderLayout(0, 10));
            JTextArea name = TvaTheme.text(hero.getName(), 14);
            name.setForeground(TvaTheme.TEXT);
            name.setPreferredSize(new Dimension(90, 36));
            JTextArea role = TvaTheme.text(roles[i], 11);
            role.setForeground(TvaTheme.GOLD);
            role.setPreferredSize(new Dimension(90, 28));
            details.add(TvaTheme.column(5, name, role), NORTH);
            details.add(TvaTheme.text("Магия: " + hero.getMagicPower() + "\nБой: " + hero.getCombatPower()
                    + "\nПереговоры: " + hero.getDiplomacy(), 12), CENTER);
            details.add(choose, SOUTH);
            card.add(details, CENTER);
            heroCards[i] = card;
            cards.add(card);
        }
        JPanel center = plain(new BorderLayout(20, 0));
        center.add(cards, CENTER);
        TvaTheme.Surface equipment = TvaTheme.surface(22);
        equipment.setPreferredSize(new Dimension(285, 0));
        equipment.setLayout(new BorderLayout(0, 16));
        equipment.add(TvaTheme.column(14, TvaTheme.heading("Снаряжение", 23),
                equipmentItem(amplifier, "+15 к магии"), equipmentItem(amulet, "+15 к переговорам"),
                equipmentItem(shield, "Штраф за неудачу: +10 вместо +15")), NORTH);
        equipment.add(TvaTheme.column(12, TvaTheme.label("ВАШ ВАРИАНТ", 11, TvaTheme.GOLD),
                TvaTheme.scroll(heroPreview)), CENTER);
        center.add(equipment, EAST);
        page.add(center, CENTER);
        page.add(TvaTheme.row(TvaTheme.button("Назад", false, () -> showScreen("setup", "ПОДГОТОВКА / ПАРАМЕТРЫ МИССИИ")),
                TvaTheme.button("Войти в портал  →", true, this::startMission)), SOUTH);
        updateHeroPreview();
        return page;
    }

    private JPanel equipmentItem(JCheckBox item, String description) {
        item.setOpaque(false);
        item.setFont(new Font("Segoe UI", Font.BOLD, 14));
        item.addActionListener(event -> updateHeroPreview());
        JTextArea detail = TvaTheme.text(description, 12);
        detail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return TvaTheme.column(3, item, detail);
    }

    private LokiVariant createEquippedHero() {
        LokiVariant hero = creators[selectedHero].createVariant();
        if (amplifier.isSelected()) { hero = new MagicAmplifier(hero); }
        if (amulet.isSelected()) { hero = new DiplomacyAmulet(hero); }
        if (shield.isSelected()) { hero = new TimeShield(hero); }
        return hero;
    }

    private void updateHeroPreview() {
        LokiVariant hero = createEquippedHero();
        heroPreview.setText(hero.getDescription() + "\n\n" + stats(hero)
                + "\n\nШтраф при неудаче: +" + hero.getFailurePenalty());
        heroPreview.setCaretPosition(0);
        for (int i = 0; i < heroCards.length; i++) {
            if (heroCards[i] != null) {
                heroCards[i].putClientProperty("selected", i == selectedHero);
                heroCards[i].repaint();
            }
        }
    }

    private void startMission() {
        try {
            control.startMission(preparedMission, createEquippedHero());
            control.getTimeline().addListener(monitor);
            monitor.showValue(control.getTimeline().getInstability());
            sceneArt.setAsset(locationArt(control.getMission().getLocation()));
            heroArt.setAsset(portraits[selectedHero]);
            loadScene();
            showScreen("mission", "МИССИЯ / " + control.getMission().getLocation());
        } catch (IllegalArgumentException | IllegalStateException error) { showError(error); }
    }

    private JPanel buildMission() {
        JPanel page = plain(new BorderLayout(20, 18));
        JPanel story = plain(new BorderLayout(0, 15));
        JPanel artCaption = TvaTheme.column(7, sceneNumber, sceneTitle);
        artCaption.setBorder(new EmptyBorder(20, 24, 22, 24));
        sceneArt.add(artCaption, SOUTH);
        JPanel top = plain(new BorderLayout(0, 14));
        top.add(sceneLocation, NORTH);
        top.add(sceneArt, CENTER);
        story.add(top, NORTH);
        TvaTheme.Surface hint = TvaTheme.surface(13);
        hint.setLayout(new BorderLayout(0, 5));
        hint.add(TvaTheme.label("ПОДСКАЗКА TVA", 10, TvaTheme.GOLD), NORTH);
        hint.add(sceneHint, CENTER);
        hint.setPreferredSize(new Dimension(200, 66));
        JPanel choices = plain(new GridLayout(1, 3, 10, 0));
        for (int i = 0; i < strategies.length; i++) {
            ActionStrategy strategy = strategies[i];
            JToggleButton choice = TvaTheme.toggle(strategy.toString());
            choice.addActionListener(event -> { selectedStrategy = strategy; confirm.setEnabled(true); });
            actionButtons[i] = choice;
            actionGroup.add(choice);
            choices.add(choice);
        }
        JPanel decision = plain(new BorderLayout(0, 12));
        JPanel description = plain(new BorderLayout(0, 12));
        description.add(sceneText, CENTER);
        description.add(hint, SOUTH);
        decision.add(description, CENTER);
        JPanel feedback = plain(new BorderLayout(0, 10));
        feedback.add(choices, NORTH);
        feedback.add(turnFeedback, CENTER);
        feedback.setPreferredSize(new Dimension(300, 130));
        decision.add(feedback, SOUTH);
        story.add(decision, CENTER);
        story.add(TvaTheme.row(confirm, next), SOUTH);
        page.add(story, CENTER);
        TvaTheme.Surface sidebar = TvaTheme.surface(18);
        sidebar.setLayout(new BorderLayout(0, 16));
        sidebar.setPreferredSize(new Dimension(255, 0));
        instability.setUI(new BasicProgressBarUI());
        instability.setStringPainted(true);
        instability.setFont(new Font("Segoe UI", Font.BOLD, 11));
        instability.setBorderPainted(false);
        instability.setPreferredSize(new Dimension(200, 25));
        JPanel hero = plain(new BorderLayout(0, 12));
        hero.add(heroArt, NORTH);
        hero.add(heroStats, CENTER);
        hero.setPreferredSize(new Dimension(210, 325));
        sidebar.add(TvaTheme.column(8, TvaTheme.label("СОСТОЯНИЕ ВЕТКИ", 10, TvaTheme.GOLD),
                instability, timelineStatus, hero), NORTH);
        JPanel journal = plain(new BorderLayout(0, 8));
        journal.add(TvaTheme.label("ЖУРНАЛ TVA", 11, TvaTheme.GOLD), NORTH);
        journal.add(TvaTheme.scroll(eventText), CENTER);
        sidebar.add(journal, CENTER);
        page.add(sidebar, EAST);
        return page;
    }

    private void loadScene() {
        Scene scene = control.getCurrentScene();
        sceneLocation.setText(control.getMission().getLocation().toString());
        sceneNumber.setText("СЦЕНА 0" + control.getSceneNumber() + " / 03");
        sceneTitle.setText(scene.title());
        sceneText.setText(scene.story());
        sceneHint.setText(scene.hint());
        heroStats.setText(control.getHero().getName() + "\n" + stats(control.getHero())
                + "\nПорог действия: " + control.getMission().getDifficulty().getThreshold());
        turnFeedback.setForeground(TvaTheme.MUTED);
        turnFeedback.setText("Выберите тактику. Решение будет применено после подтверждения.");
        actionGroup.clearSelection();
        selectedStrategy = null;
        for (JToggleButton choice : actionButtons) { choice.setEnabled(true); }
        confirm.setEnabled(false);
        next.setEnabled(false);
        next.setText("Следующая сцена  →");
        updateEventText();
    }

    private void updateEventText() {
        eventText.setText(control.getEventEntries().isEmpty() ? "Портал открыт.\nОжидаем первое решение."
                : String.join("\n\n", control.getEventEntries()));
        eventText.setCaretPosition(0);
    }

    private void performAction() {
        if (selectedStrategy == null) { return; }
        try {
            TurnResult turn = control.performAction(selectedStrategy);
            turnFeedback.setForeground(turn.action().success() ? new Color(139, 205, 159) : TvaTheme.AMBER);
            turnFeedback.setText((turn.action().success() ? "УСПЕХ" : "НЕУДАЧА") + "  /  "
                    + turn.action().action() + "\nСила " + turn.action().score() + " / порог " + turn.action().threshold()
                    + "    •    Нестабильность: " + turn.before() + " → " + turn.after());
            updateEventText();
            confirm.setEnabled(false);
            for (JToggleButton choice : actionButtons) { choice.setEnabled(false); }
            next.setText(control.isActive() ? "Следующая сцена  →" : "Показать результат  →");
            next.setEnabled(true);
            if (!control.isActive()) { saveCampaign(); }
        } catch (IllegalStateException error) { showError(error); }
    }

    private void nextScene() {
        if (control.isActive()) { control.nextScene(); loadScene(); }
        else { refreshHome(); showResult(); }
    }

    private void showResult() {
        MissionResult result = control.getResult();
        results.removeAll();
        results.add(titleBlock("ОТЧЁТ TVA / " + result.mission().getLocation(), result.getRank(),
                result.success() ? "Ветка под контролем. Операция завершена." : "Попробуйте другого героя или измените тактику."), NORTH);
        TvaTheme.ArtPanel art = new TvaTheme.ArtPanel(locationArt(result.mission().getLocation()), 400, true);
        art.setPreferredSize(new Dimension(310, 400));
        JLabel score = TvaTheme.label(result.mission().getDifficulty().getStartingInstability()
                + " → " + result.finalInstability(), 48, TvaTheme.GOLD);
        score.setForeground(result.success() ? TvaTheme.GOLD : TvaTheme.AMBER);
        JPanel scorePanel = TvaTheme.column(10, TvaTheme.label("НЕСТАБИЛЬНОСТЬ ВЕТКИ", 11, TvaTheme.GOLD), score,
                TvaTheme.label(result.success() ? "СТАБИЛИЗИРОВАНА" : "НЕСТАБИЛЬНА", 13, TvaTheme.TEXT));
        scorePanel.setBorder(new EmptyBorder(22, 22, 28, 22));
        art.add(scorePanel, SOUTH);
        JPanel content = plain(new BorderLayout(20, 0));
        content.add(art, WEST);
        JPanel turns = new JPanel();
        turns.setOpaque(false);
        turns.setLayout(new BoxLayout(turns, BoxLayout.Y_AXIS));
        JTextArea missionName = TvaTheme.text(result.mission().getTitle() + "\n" + result.hero(), 14);
        missionName.setPreferredSize(new Dimension(300, 70));
        missionName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        missionName.setAlignmentX(Component.LEFT_ALIGNMENT);
        turns.add(missionName);
        for (TurnResult turn : result.turns()) {
            TvaTheme.Surface item = TvaTheme.surface(18);
            item.setLayout(new BorderLayout(0, 8));
            item.add(TvaTheme.label("0" + turn.sceneNumber() + " / " + turn.sceneTitle(), 16, TvaTheme.TEXT), NORTH);
            JTextArea detail = TvaTheme.text(turn.action().action() + "  •  Сила " + turn.action().score()
                    + " / порог " + turn.action().threshold() + "\nНестабильность: " + turn.before() + " → " + turn.after()
                    + "  /  " + (turn.action().success() ? "успех" : "неудача"), 14);
            item.add(detail, CENTER);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            item.setPreferredSize(new Dimension(300, 110));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            turns.add(item);
            turns.add(Box.createVerticalStrut(12));
        }
        content.add(TvaTheme.scroll(turns), CENTER);
        results.add(content, CENTER);
        results.add(TvaTheme.row(TvaTheme.button("Повторить", false, () -> prepare(result.mission().getLocation())),
                TvaTheme.button("В штаб  →", true, this::goHome), TvaTheme.button("Открыть архив", false, this::goArchive)), SOUTH);
        results.revalidate();
        results.repaint();
        showScreen("result", "ОТЧЁТ / ЗАВЕРШЕНИЕ МИССИИ");
    }

    private JPanel buildArchive() {
        JPanel page = plain(new BorderLayout(0, 20));
        page.add(titleBlock("АРХИВ TVA", "История ваших решений", "Завершённые попытки и результаты каждого действия."), NORTH);
        archiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        archiveList.setBackground(TvaTheme.PANEL);
        archiveList.setForeground(TvaTheme.TEXT);
        archiveList.setFixedCellHeight(86);
        archiveList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                MissionResult result = (MissionResult) value;
                label.setText("<html><b>" + result.mission().getLocation() + "</b><br>" + result.mission().getDifficulty()
                        + " / " + (result.success() ? "Успех" : "Провал") + " / " + result.finalInstability() + " из 100</html>");
                label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                label.setBorder(new EmptyBorder(12, 16, 12, 16));
                return label;
            }
        });
        archiveList.addListSelectionListener(event -> {
            MissionResult selected = archiveList.getSelectedValue();
            retryArchive.setEnabled(selected != null);
            if (selected != null) { archiveDetails.setText(selected.describe()); archiveDetails.setCaretPosition(0); }
        });
        TvaTheme.Surface entries = TvaTheme.surface(12);
        entries.setLayout(new BorderLayout());
        entries.add(TvaTheme.scroll(archiveList), CENTER);
        TvaTheme.Surface detail = TvaTheme.surface(24);
        detail.setLayout(new BorderLayout(0, 16));
        detail.add(TvaTheme.label("ДОСЬЕ ОПЕРАЦИИ", 11, TvaTheme.GOLD), NORTH);
        detail.add(TvaTheme.scroll(archiveDetails), CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, entries, detail);
        split.setDividerLocation(300);
        split.setResizeWeight(0.32);
        split.setDividerSize(14);
        split.setOpaque(false);
        split.setBorder(null);
        page.add(split, CENTER);
        page.add(TvaTheme.row(retryArchive, TvaTheme.button("В штаб", false, this::goHome)), SOUTH);
        return page;
    }

    private void retrySelected() {
        MissionResult selected = archiveList.getSelectedValue();
        if (selected != null) { prepare(selected.mission().getLocation()); }
    }

    private void goHome() {
        if (leaveMission()) { refreshHome(); showScreen("home", "ОПЕРАТИВНЫЙ ШТАБ"); }
    }

    private void goArchive() {
        if (!leaveMission()) { return; }
        archiveModel.clear();
        for (MissionResult result : control.getArchive()) { archiveModel.addElement(result); }
        archiveDetails.setText("Архив пока пуст.\n\nЗавершите первую миссию, чтобы увидеть разбор своих решений.");
        retryArchive.setEnabled(false);
        if (!archiveModel.isEmpty()) { archiveList.setSelectedIndex(archiveModel.size() - 1); }
        showScreen("archive", "АРХИВ / ЗАВЕРШЁННЫЕ ПОПЫТКИ");
    }

    private boolean leaveMission() {
        if (!control.isActive()) { return true; }
        int answer = JOptionPane.showConfirmDialog(frame, "Прервать миссию? Незавершённая попытка не попадёт в архив.",
                "Выход из миссии", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) { return false; }
        control.abortMission();
        return true;
    }

    private void showError(RuntimeException error) {
        JOptionPane.showMessageDialog(frame, error.getMessage(), "Не удалось продолжить", JOptionPane.WARNING_MESSAGE);
    }

    private boolean saveCampaign() {
        if (!savingEnabled) { return true; }
        try {
            store.save(control.getArchive());
            saveStatus.setText("Прогресс сохранён  /  Завершённых миссий: " + control.getArchive().size());
            saveStatus.setForeground(TvaTheme.MUTED);
            return true;
        } catch (IOException error) {
            saveStatus.setText("Не удалось сохранить прогресс. Результаты доступны в текущем сеансе.");
            saveStatus.setForeground(TvaTheme.AMBER);
            JOptionPane.showMessageDialog(frame, "Не удалось сохранить прогресс:\n" + store.getPath()
                    + "\n\n" + error.getMessage() + "\n\nПроверьте доступ к папке и попробуйте закрыть игру снова.",
                    "Сохранение кампании", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private static String stats(LokiVariant hero) {
        return "Магия                     " + hero.getMagicPower() + "\nБой                          " + hero.getCombatPower()
                + "\nПереговоры          " + hero.getDiplomacy();
    }

    private static String locationArt(MissionLocation location) {
        return switch (location) { case NEW_YORK -> "new_york"; case ASGARD -> "asgard"; case VOID -> "void"; };
    }

    private static JPanel titleBlock(String eyebrow, String title, String description) {
        return TvaTheme.column(6, TvaTheme.label(eyebrow, 10, TvaTheme.GOLD), TvaTheme.heading(title, 29),
                TvaTheme.label(description, 14, TvaTheme.MUTED));
    }

    private static JPanel plain(LayoutManager manager) {
        JPanel panel = new JPanel(manager);
        panel.setOpaque(false);
        return panel;
    }
}
