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
    private final JButton variantNav = TvaTheme.button("Герои", false, this::goVariants);
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
    private final JPanel heroMeters = plain(new GridLayout(3, 1, 0, 10));
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
    private final JPanel archiveChart = plain(new BorderLayout());
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
        JPanel root = new TvaTheme.Backdrop();
        root.add(buildHeader(), NORTH);
        root.add(buildSidebar(), WEST);
        screens.setLayout(layout);
        screens.add(home, "home");
        screens.add(buildSetup(), "setup");
        screens.add(buildLoadout(), "loadout");
        screens.add(buildMission(), "mission");
        screens.add(results, "result");
        screens.add(buildArchive(), "archive");
        JPanel workspace = plain(new BorderLayout());
        workspace.setBorder(new EmptyBorder(18, 18, 18, 22));
        workspace.add(screens, CENTER);
        saveStatus.setToolTipText(saveFile.toAbsolutePath().toString());
        root.add(workspace, CENTER);
        JPanel footer = plain(new BorderLayout(20, 0));
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TvaTheme.BORDER),
                new EmptyBorder(10, 20, 10, 24)));
        progress.setFont(new Font("Segoe UI", Font.BOLD, 16));
        footer.add(TvaTheme.row(TvaTheme.label("КАМПАНИЯ", 12, TvaTheme.GOLD), progress), WEST);
        footer.add(saveStatus, EAST);
        root.add(footer, SOUTH);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(1160, 800));
        Dimension display = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(Math.min(1600, display.width - 40), Math.min(1000, display.height - 60));
        frame.setLocationRelativeTo(null);
        refreshHome();
        showScreen("home", "ОПЕРАТИВНЫЙ ШТАБ");
    }

    public void show() { frame.setVisible(true); }
    static void installTheme() { TvaTheme.install(); }

    private JPanel buildHeader() {
        JPanel header = plain(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TvaTheme.BORDER),
                new EmptyBorder(14, 25, 14, 25)));
        JLabel brand = TvaTheme.heading("LOKI", 42);
        brand.setForeground(TvaTheme.GOLD);
        header.add(TvaTheme.column(0, brand, TvaTheme.label("TVA  M I S S I O N S", 11, TvaTheme.GOLD)), WEST);
        pageName.setFont(new Font("Georgia", Font.BOLD, 25));
        pageName.setForeground(TvaTheme.TEXT);
        pageName.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(pageName, CENTER);
        JLabel tva = TvaTheme.heading("TVA", 34);
        tva.setForeground(TvaTheme.AMBER);
        header.add(tva, EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = plain(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(112, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, TvaTheme.BORDER),
                new EmptyBorder(24, 8, 20, 8)));
        homeNav.setText("Миссии");
        archiveNav.setText("Архив");
        for (JButton nav : new JButton[]{homeNav, variantNav, archiveNav}) {
            nav.setBorder(new EmptyBorder(22, 5, 22, 5));
            nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        }
        sidebar.add(TvaTheme.column(12, TvaTheme.label("TVA / 07", 14, TvaTheme.GOLD), homeNav, variantNav, archiveNav), NORTH);
        sidebar.add(TvaTheme.column(10, TvaTheme.label("SECTOR 07", 11, TvaTheme.GOLD),
                TvaTheme.text("Все ветки.\nВсе варианты.\nОдна миссия.", 12)), SOUTH);
        return sidebar;
    }

    private void showScreen(String screen, String title) {
        pageName.setText(switch (screen) {
            case "home" -> "ОПЕРАТИВНЫЙ ШТАБ";
            case "setup" -> "ПОДГОТОВКА МИССИИ";
            case "loadout" -> "ВАРИАНТ И СНАРЯЖЕНИЕ";
            case "mission" -> "ОПЕРАЦИЯ TVA";
            case "result" -> "ОТЧЁТ ОБ ОПЕРАЦИИ";
            default -> "АРХИВ МИССИЙ";
        });
        pageName.setToolTipText(title);
        homeNav.setSelected(screen.equals("home") || screen.equals("setup"));
        variantNav.setSelected(screen.equals("loadout") || screen.equals("mission"));
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
        home.add(titleBlock("СВЯЩЕННАЯ ЛИНИЯ ВРЕМЕНИ / СЕКТОР 07", "Выберите временную ветку",
                "Три локации. Четыре варианта Локи. Ваши решения меняют ход операции."), NORTH);
        JPanel cards = plain(new GridLayout(1, 3, 12, 0));
        for (MissionLocation location : MissionLocation.values()) {
            boolean unlocked = control.isUnlocked(location);
            TvaTheme.Surface card = TvaTheme.surface(10);
            card.putClientProperty("selected", selectedLocation == location);
            card.setLayout(new BorderLayout(0, 12));
            card.add(TvaTheme.column(6, TvaTheme.label("ВЕТКА 0" + (location.ordinal() + 1), 11, TvaTheme.GOLD),
                    TvaTheme.heading(location == MissionLocation.NEW_YORK ? "Нью-Йорк" : location.toString(), 22)), NORTH);
            TvaTheme.ArtPanel art = new TvaTheme.ArtPanel(locationArt(location), 260, true);
            JPanel caption = TvaTheme.column(4, TvaTheme.label(location == MissionLocation.NEW_YORK ? "2012 / ЗЕМЛЯ" : "ВНЕ ОСНОВНОЙ ВЕТКИ", 11, TvaTheme.GOLD),
                    TvaTheme.label(completed.contains(location) ? "СТАБИЛИЗИРОВАНА" : unlocked ? "ДОСТУПНА" : "ЗАБЛОКИРОВАНА", 12, TvaTheme.TEXT));
            caption.setBorder(new EmptyBorder(12, 12, 14, 12));
            art.add(caption, SOUTH);
            card.add(art, CENTER);
            JButton select = TvaTheme.button(selectedLocation == location ? "ВЫБРАНО" : "Выбрать ветку", false,
                    () -> { selectedLocation = location; refreshHome(); });
            select.setSelected(selectedLocation == location);
            select.setFont(new Font("Segoe UI", Font.BOLD, 13));
            card.add(select, SOUTH);
            cards.add(card);
        }
        JPanel center = plain(new BorderLayout(18, 0));
        center.add(cards, CENTER);
        TvaTheme.Surface dossier = TvaTheme.surface(20);
        dossier.setPreferredSize(new Dimension(290, 0));
        dossier.setLayout(new BorderLayout(0, 16));
        JPanel branch = TvaTheme.column(6, TvaTheme.label("ВРЕМЕННЫЕ ВЕТКИ", 12, TvaTheme.GOLD),
                new TvaTheme.TimelinePanel(completed.size(), java.util.List.of()));
        dossier.add(branch, NORTH);
        JTextArea briefing = TvaTheme.text(selectedLocation.getBriefing()
                + "\n\nЦЕЛЬ ОПЕРАЦИИ\nПройдите 3 сцены и снизьте нестабильность до 20 или ниже."
                + "\n\nПРИ 100 — КОЛЛАПС\nПодбирайте тактику под способности выбранного Локи.", 15);
        briefing.setRows(9);
        JPanel info = plain(new BorderLayout(0, 12));
        info.add(TvaTheme.heading(selectedLocation.toString(), 23), NORTH);
        info.add(TvaTheme.scroll(briefing), CENTER);
        dossier.add(info, CENTER);
        JButton prepare = TvaTheme.button("Подготовить миссию  →", true, () -> prepare(selectedLocation));
        prepare.setEnabled(control.isUnlocked(selectedLocation));
        dossier.add(prepare, SOUTH);
        center.add(dossier, EAST);
        home.add(center, CENTER);
        JPanel bottom = plain(new GridLayout(1, 3, 16, 0));
        bottom.add(TvaTheme.label("01   СОБЕРИТЕ МИССИЮ", 12, TvaTheme.GOLD));
        bottom.add(TvaTheme.label("02   ВЫБЕРИТЕ ВАРИАНТ", 12, TvaTheme.GOLD));
        bottom.add(TvaTheme.label("03   ИЗМЕНИТЕ ВРЕМЯ", 12, TvaTheme.GOLD));
        home.add(bottom, SOUTH);
        home.revalidate();
        home.repaint();
    }

    private void goVariants() {
        if (!leaveMission()) { return; }
        if (!control.isUnlocked(selectedLocation)) { selectedLocation = MissionLocation.NEW_YORK; }
        prepareLoadout();
    }

    private void prepareLoadout() {
        try {
            preparedMission = new Mission.Builder().title(missionTitle.getText().trim())
                    .location(selectedLocation).difficulty((Difficulty) difficulty.getSelectedItem()).build();
            updateHeroPreview();
            showScreen("loadout", "ВАРИАНТ И СНАРЯЖЕНИЕ");
        } catch (IllegalArgumentException error) { showError(error); }
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
                TvaTheme.button("Выбрать вариант Локи  →", true, this::prepareLoadout)), SOUTH);
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
        JPanel cards = plain(new GridLayout(1, 4, 12, 0));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < creators.length; i++) {
            final int index = i;
            LokiVariant hero = creators[i].createVariant();
            TvaTheme.Surface card = TvaTheme.surface(10);
            card.setLayout(new BorderLayout(0, 12));
            TvaTheme.ArtPanel portrait = new TvaTheme.ArtPanel(portraits[i], 270, false);
            card.add(portrait, CENTER);
            JToggleButton choose = TvaTheme.toggle("Выбрать");
            choose.setSelected(i == selectedHero);
            choose.addActionListener(event -> { selectedHero = index; updateHeroPreview(); });
            group.add(choose);
            JPanel details = plain(new BorderLayout(0, 9));
            JTextArea name = TvaTheme.text(hero.getName(), 16);
            name.setForeground(TvaTheme.TEXT);
            name.setPreferredSize(new Dimension(100, 44));
            details.add(name, NORTH);
            details.add(TvaTheme.column(7, TvaTheme.stat("Магия", hero.getMagicPower(), TvaTheme.MINT),
                    TvaTheme.stat("Бой", hero.getCombatPower(), TvaTheme.AMBER),
                    TvaTheme.stat("Переговоры", hero.getDiplomacy(), TvaTheme.GOLD)), CENTER);
            details.add(choose, SOUTH);
            details.setPreferredSize(new Dimension(130, 224));
            card.add(details, SOUTH);
            heroCards[i] = card;
            cards.add(card);
        }
        JPanel center = plain(new BorderLayout(20, 0));
        center.add(cards, CENTER);
        TvaTheme.Surface equipment = TvaTheme.surface(16);
        equipment.setPreferredSize(new Dimension(260, 0));
        equipment.setLayout(new BorderLayout(0, 16));
        equipment.add(TvaTheme.column(14, TvaTheme.heading("Снаряжение", 24),
                equipmentItem(amplifier, "+15 к магии"), equipmentItem(amulet, "+15 к переговорам"),
                equipmentItem(shield, "Штраф за неудачу: +10 вместо +15")), NORTH);
        equipment.add(TvaTheme.text("Улучшения суммируются.\n\nВыбирайте снаряжение под слабые стороны героя.", 14), CENTER);
        center.add(equipment, EAST);
        page.add(center, CENTER);
        TvaTheme.Surface footer = TvaTheme.surface(14);
        footer.setLayout(new BorderLayout(18, 0));
        footer.add(TvaTheme.button("Назад", false, () -> showScreen("setup", "ПОДГОТОВКА / ПАРАМЕТРЫ МИССИИ")), WEST);
        heroPreview.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        footer.add(heroPreview, CENTER);
        footer.add(TvaTheme.button("Войти в портал  →", true, this::startMission), EAST);
        page.add(footer, SOUTH);
        updateHeroPreview();
        return page;
    }

    private JPanel equipmentItem(JCheckBox item, String description) {
        TvaTheme.styleCheckBox(item);
        item.addActionListener(event -> updateHeroPreview());
        JTextArea detail = TvaTheme.text(description, 13);
        detail.setRows(2);
        TvaTheme.Surface panel = TvaTheme.surface(10);
        panel.setLayout(new BorderLayout(0, 8));
        panel.add(item, NORTH);
        panel.add(detail, CENTER);
        return panel;
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
        heroPreview.setText(hero.getName() + "  /  " + roles[selectedHero] + "\nМагия " + hero.getMagicPower()
                + "   ·   Бой " + hero.getCombatPower() + "   ·   Переговоры " + hero.getDiplomacy()
                + "   ·   Штраф +" + hero.getFailurePenalty());
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
        JPanel page = plain(new BorderLayout(18, 0));
        JPanel story = plain(new BorderLayout(0, 12));
        JPanel artCaption = TvaTheme.column(7, sceneNumber, sceneTitle);
        artCaption.setBorder(new EmptyBorder(20, 24, 22, 24));
        sceneArt.add(artCaption, SOUTH);
        story.add(sceneLocation, NORTH);
        story.add(sceneArt, CENTER);
        JPanel choices = plain(new GridLayout(1, 3, 10, 0));
        String[] captions = {"ПЕРЕГОВОРЫ", "ИЛЛЮЗИЯ", "БОЙ"};
        String[] descriptions = {"Убедить и договориться", "Обмануть с помощью магии", "Проложить путь силой"};
        for (int i = 0; i < strategies.length; i++) {
            ActionStrategy strategy = strategies[i];
            JToggleButton choice = TvaTheme.toggle("<html><center>" + captions[i]
                    + "<br><br><font size='3'>" + descriptions[i] + "</font></center></html>");
            choice.setFont(new Font("Georgia", Font.BOLD, 18));
            choice.setBorder(new EmptyBorder(12, 6, 12, 6));
            choice.addActionListener(event -> { selectedStrategy = strategy; confirm.setEnabled(true); });
            actionButtons[i] = choice;
            actionGroup.add(choice);
            choices.add(choice);
        }
        choices.setPreferredSize(new Dimension(300, 90));
        sceneText.setRows(2);
        sceneText.setForeground(TvaTheme.TEXT);
        sceneText.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        JPanel decision = plain(new BorderLayout(0, 10));
        decision.add(sceneText, NORTH);
        decision.add(choices, CENTER);
        TvaTheme.Surface command = TvaTheme.surface(12);
        command.setLayout(new BorderLayout(0, 8));
        sceneHint.setRows(2);
        turnFeedback.setRows(2);
        JPanel message = plain(new GridLayout(1, 2, 16, 0));
        sceneHint.setForeground(TvaTheme.GOLD);
        message.add(sceneHint);
        message.add(turnFeedback);
        command.add(message, CENTER);
        command.add(TvaTheme.row(confirm, next), SOUTH);
        command.setPreferredSize(new Dimension(300, 132));
        decision.add(command, SOUTH);
        decision.setPreferredSize(new Dimension(300, 286));
        story.add(decision, SOUTH);
        page.add(story, CENTER);
        TvaTheme.Surface sidebar = TvaTheme.surface(14);
        sidebar.setLayout(new BorderLayout(0, 12));
        sidebar.setPreferredSize(new Dimension(215, 0));
        instability.setUI(new BasicProgressBarUI());
        instability.setStringPainted(true);
        instability.setFont(new Font("Segoe UI", Font.BOLD, 12));
        instability.setBorderPainted(false);
        instability.setPreferredSize(new Dimension(200, 25));
        heroArt.setPreferredSize(new Dimension(180, 230));
        heroStats.setRows(2);
        heroStats.setForeground(TvaTheme.TEXT);
        sidebar.add(TvaTheme.column(10, heroStats, heroArt, heroMeters,
                TvaTheme.label("НЕСТАБИЛЬНОСТЬ", 11, TvaTheme.GOLD), instability, timelineStatus), NORTH);
        JPanel journal = plain(new BorderLayout(0, 8));
        journal.add(TvaTheme.label("ЖУРНАЛ TVA", 11, TvaTheme.GOLD), NORTH);
        journal.add(TvaTheme.scroll(eventText), CENTER);
        sidebar.add(journal, CENTER);
        page.add(sidebar, WEST);
        return page;
    }

    private void loadScene() {
        Scene scene = control.getCurrentScene();
        sceneLocation.setText(control.getMission().getLocation().toString());
        sceneNumber.setText("СЦЕНА 0" + control.getSceneNumber() + " / 03");
        sceneTitle.setText(scene.title());
        sceneText.setText(scene.story());
        sceneHint.setText("ПОДСКАЗКА TVA\n" + scene.hint());
        LokiVariant hero = control.getHero();
        heroStats.setText(hero.getName() + "\nПорог действия: " + control.getMission().getDifficulty().getThreshold());
        heroMeters.removeAll();
        heroMeters.add(TvaTheme.stat("Магия", hero.getMagicPower(), TvaTheme.MINT));
        heroMeters.add(TvaTheme.stat("Бой", hero.getCombatPower(), TvaTheme.AMBER));
        heroMeters.add(TvaTheme.stat("Переговоры", hero.getDiplomacy(), TvaTheme.GOLD));
        heroMeters.revalidate();
        sceneArt.setAsset(selectedHero == 1 && control.getMission().getLocation() == MissionLocation.NEW_YORK
                && control.getSceneNumber() == 2 ? "encounter" : locationArt(control.getMission().getLocation()));
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
        results.add(titleBlock("ОТЧЁТ TVA / " + result.mission().getLocation(),
                result.success() ? "ВЕТКА СТАБИЛИЗИРОВАНА" : "ВЕТКА ОСТАЛАСЬ НЕСТАБИЛЬНОЙ",
                result.success() ? "Ветка под контролем. Операция завершена." : "Попробуйте другого героя или измените тактику."), NORTH);
        TvaTheme.ArtPanel art = new TvaTheme.ArtPanel(portraits[selectedHero], 400, true);
        art.setPreferredSize(new Dimension(230, 400));
        JPanel caption = TvaTheme.column(6, TvaTheme.label("ВАРИАНТ TVA", 11, TvaTheme.GOLD),
                TvaTheme.text(control.getHero().getName(), 22),
                TvaTheme.label(result.getRank(), 14, result.success() ? TvaTheme.MINT : TvaTheme.AMBER));
        caption.setBorder(new EmptyBorder(18, 18, 24, 18));
        art.add(caption, SOUTH);
        JLabel score = TvaTheme.label(result.mission().getDifficulty().getStartingInstability()
                + " → " + result.finalInstability(), 50, TvaTheme.GOLD);
        score.setForeground(result.success() ? TvaTheme.MINT : TvaTheme.AMBER);
        TvaTheme.Surface scorePanel = TvaTheme.surface(18);
        scorePanel.setLayout(new BorderLayout(8, 4));
        scorePanel.add(TvaTheme.label("НЕСТАБИЛЬНОСТЬ ВЕТКИ", 12, TvaTheme.GOLD), NORTH);
        scorePanel.add(score, CENTER);
        scorePanel.setPreferredSize(new Dimension(300, 110));
        JPanel content = plain(new BorderLayout(16, 0));
        content.add(art, WEST);
        JPanel breakdown = plain(new BorderLayout(0, 14));
        breakdown.add(scorePanel, NORTH);
        JPanel turns = plain(new GridLayout(result.turns().size(), 1, 0, 10));
        for (TurnResult turn : result.turns()) {
            TvaTheme.Surface item = TvaTheme.surface(10);
            item.setLayout(new BorderLayout(8, 4));
            item.add(TvaTheme.label("СЦЕНА 0" + turn.sceneNumber() + " / " + turn.action().action(), 16, TvaTheme.TEXT), NORTH);
            item.add(TvaTheme.text("Сила " + turn.action().score() + " / порог " + turn.action().threshold()
                    + "\nНестабильность " + turn.before() + " → " + turn.after(), 14), CENTER);
            int delta = turn.after() - turn.before();
            item.add(TvaTheme.label((delta > 0 ? "+" : "") + delta, 28,
                    turn.action().success() ? TvaTheme.MINT : TvaTheme.AMBER), EAST);
            turns.add(item);
        }
        breakdown.add(turns, CENTER);
        JPanel report = plain(new BorderLayout(16, 0));
        report.add(breakdown, CENTER);
        TvaTheme.Surface timeline = TvaTheme.surface(18);
        timeline.setPreferredSize(new Dimension(260, 0));
        timeline.setLayout(new BorderLayout(0, 16));
        timeline.add(TvaTheme.label("ХОД ОПЕРАЦИИ", 13, TvaTheme.GOLD), NORTH);
        JPanel charts = plain(new GridLayout(2, 1, 0, 20));
        charts.add(new TvaTheme.TimelinePanel(0, historyValues(result)));
        charts.add(new TvaTheme.TimelinePanel(result.success() ? 3 : 0, java.util.List.of()));
        timeline.add(charts, CENTER);
        JTextArea missionName = TvaTheme.text(result.mission().getTitle() + "\n\n" + result.hero(), 14);
        missionName.setRows(4);
        timeline.add(missionName, SOUTH);
        report.add(timeline, EAST);
        content.add(report, CENTER);
        results.add(content, CENTER);
        int nextLocation = result.mission().getLocation().ordinal() + 1;
        boolean hasNext = result.success() && nextLocation < MissionLocation.values().length;
        results.add(TvaTheme.row(TvaTheme.button(hasNext ? "Следующая миссия  →" : "В штаб  →", true,
                        () -> { if (hasNext) { prepare(MissionLocation.values()[nextLocation]); } else { goHome(); } }),
                TvaTheme.button("Повторить", false, () -> prepare(result.mission().getLocation())),
                TvaTheme.button("Открыть архив", false, this::goArchive)), SOUTH);
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
            if (selected != null) {
                archiveDetails.setText(selected.describe()); archiveDetails.setCaretPosition(0);
                archiveChart.removeAll();
                archiveChart.add(new TvaTheme.TimelinePanel(0, historyValues(selected)), CENTER);
                archiveChart.revalidate(); archiveChart.repaint();
            }
        });
        TvaTheme.Surface entries = TvaTheme.surface(12);
        entries.setLayout(new BorderLayout());
        entries.add(TvaTheme.scroll(archiveList), CENTER);
        TvaTheme.Surface detail = TvaTheme.surface(24);
        detail.setLayout(new BorderLayout(0, 16));
        detail.add(TvaTheme.label("ДОСЬЕ ОПЕРАЦИИ", 11, TvaTheme.GOLD), NORTH);
        detail.add(TvaTheme.scroll(archiveDetails), CENTER);
        archiveChart.setPreferredSize(new Dimension(200, 175));
        detail.add(archiveChart, SOUTH);
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
        archiveChart.removeAll();
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

    private static java.util.List<Integer> historyValues(MissionResult result) {
        java.util.List<Integer> values = new java.util.ArrayList<>();
        values.add(result.mission().getDifficulty().getStartingInstability());
        for (TurnResult turn : result.turns()) { values.add(turn.after()); }
        return values;
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
