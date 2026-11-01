import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GameChecks {
    public static void main(String[] args) throws Exception {
        if (!GameChecks.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Запустите проверку с java -ea.");
        }
        checkCreation();
        checkEquipmentAndStrategies();
        checkSylvieAndAmulet();
        SwingUtilities.invokeAndWait(GameChecks::checkObservers);
        SwingUtilities.invokeAndWait(GameChecks::checkArtwork);
        checkCampaign();
        checkFailureAndRetry();
        checkBounds();
        checkPersistence();
        System.out.println("PASS: Builder, Factory Method, Decorator, Strategy, Observer, Facade");
        System.out.println("PASS: campaign, failed mission, retry, collapse, guards, immutable results");
    }

    private static void checkArtwork() {
        for (String name : new String[]{"new_york", "asgard", "void", "alligator", "sylvie", "variants-sheet"}) {
            java.awt.image.BufferedImage image = TvaTheme.image(name);
            assert image.getWidth() >= 200 && image.getHeight() >= 200 : name;
        }
        for (String name : new String[]{"loki", "classic", "alligator", "sylvie", "new_york"}) {
            TvaTheme.ArtPanel panel = new TvaTheme.ArtPanel(name, 160, true);
            panel.setSize(280, 160);
            java.awt.image.BufferedImage rendered = new java.awt.image.BufferedImage(280, 160,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics = rendered.createGraphics();
            panel.paint(graphics);
            graphics.dispose();
            assert (rendered.getRGB(140, 80) >>> 24) == 255 : name;
        }
        System.out.println("PASS: bundled artwork loads and paints at card size");
    }

    private static Mission mission(MissionLocation location) {
        return new Mission.Builder().location(location).build();
    }

    private static LokiVariant classic() {
        return new MagicAmplifier(new LokiCreator.ClassicCreator().createVariant());
    }

    private static void checkCreation() {
        fails(IllegalArgumentException.class, () -> new Mission.Builder().build());
        fails(IllegalArgumentException.class, () -> new Mission.Builder().location(MissionLocation.NEW_YORK)
                .title(" ").build());
        fails(IllegalArgumentException.class, () -> new Mission.Builder().location(MissionLocation.NEW_YORK)
                .difficulty(null).build());
        Mission.Builder builder = new Mission.Builder().location(MissionLocation.NEW_YORK).difficulty(Difficulty.EASY);
        Mission easy = builder.build();
        Mission hard = builder.difficulty(Difficulty.HARD).build();
        assert easy.getDifficulty().getStartingInstability() == 60;
        assert hard.getDifficulty().getThreshold() == 75;
        assert easy.getScenes().size() == 3;
        fails(UnsupportedOperationException.class, () -> easy.getScenes().clear());
        LokiCreator[] creators = {new LokiCreator.OrdinaryCreator(), new LokiCreator.ClassicCreator(),
                new LokiCreator.AlligatorCreator()};
        int[][] expected = {{45, 50, 65}, {75, 30, 45}, {20, 80, 10}};
        for (int i = 0; i < creators.length; i++) {
            LokiVariant hero = creators[i].createVariant();
            assert hero.getMagicPower() == expected[i][0];
            assert hero.getCombatPower() == expected[i][1];
            assert hero.getDiplomacy() == expected[i][2];
            assert hero != creators[i].createVariant();
        }
        assert creators[0].createVariant().getClass() != creators[1].createVariant().getClass();
        fails(IllegalArgumentException.class, () -> new Loki("", 50));
        fails(IllegalArgumentException.class, () -> new Loki("Локи", -1));
    }

    private static void checkEquipmentAndStrategies() {
        LokiVariant base = new LokiCreator.ClassicCreator().createVariant();
        LokiVariant equipped = new TimeShield(new MagicAmplifier(base));
        LokiVariant reversed = new MagicAmplifier(new TimeShield(base));
        assert base.getMagicPower() == 75;
        assert base.getFailurePenalty() == 15;
        assert equipped.getMagicPower() == 90 && equipped.getFailurePenalty() == 10;
        assert equipped.getCombatPower() == 30 && equipped.getDiplomacy() == 45;
        assert reversed.getMagicPower() == equipped.getMagicPower();
        assert reversed.getFailurePenalty() == equipped.getFailurePenalty();
        Scene last = MissionLocation.NEW_YORK.getScenes().get(2);
        assert new NegotiateStrategy().execute(equipped, last, Difficulty.STANDARD).success();
        assert new NegotiateStrategy().execute(equipped, last, Difficulty.STANDARD).score() == 65;
        assert !new IllusionStrategy().execute(equipped, last, Difficulty.STANDARD).success();
        assert !new CombatStrategy().execute(equipped, last, Difficulty.STANDARD).success();
        assert new CombatStrategy().execute(new LokiCreator.AlligatorCreator().createVariant(),
                last, Difficulty.STANDARD).success();
    }

    private static void checkSylvieAndAmulet() {
        LokiVariant sylvie = new LokiCreator.SylvieCreator().createVariant();
        assert sylvie.getName().equals("Сильви");
        assert sylvie.getMagicPower() == 65 && sylvie.getCombatPower() == 45 && sylvie.getDiplomacy() == 55;
        assert sylvie != new LokiCreator.SylvieCreator().createVariant();
        LokiVariant amulet = new DiplomacyAmulet(sylvie);
        assert amulet.getDiplomacy() == 70;
        assert amulet.getMagicPower() == 65 && amulet.getCombatPower() == 45;
        assert amulet.getFailurePenalty() == 15;
        LokiVariant full = new TimeShield(new MagicAmplifier(new DiplomacyAmulet(sylvie)));
        assert full.getDiplomacy() == 70 && full.getMagicPower() == 80 && full.getCombatPower() == 45;
        assert full.getFailurePenalty() == 10;
        assert full.getDescription().contains("амулет переговоров");
        Scene plain = MissionLocation.NEW_YORK.getScenes().get(0);
        assert plain.diplomacyModifier() == 0;
        assert !new NegotiateStrategy().execute(sylvie, plain, Difficulty.STANDARD).success();
        assert new NegotiateStrategy().execute(amulet, plain, Difficulty.STANDARD).success();
    }

    private static void checkObservers() {
        Timeline timeline = new Timeline(70);
        EventLog log = new EventLog();
        JProgressBar bar = new JProgressBar(0, 100);
        JLabel status = new JLabel();
        TimelineMonitor monitor = new TimelineMonitor(bar, status);
        timeline.addListener(log);
        timeline.addListener(log);
        timeline.addListener(monitor);
        timeline.changeBy(-25, "Иллюзия");
        assert log.getEntries().size() == 1;
        assert log.getEntries().get(0).contains("70 → 45");
        assert bar.getValue() == 45 && status.getText().contains("Нестабильна");
        timeline.removeListener(log);
        timeline.changeBy(-25, "Второй ход");
        assert log.getEntries().size() == 1;
        assert bar.getValue() == 20 && status.getText().contains("Стабильна");
        timeline.clearListeners();
        timeline.changeBy(20, "Не наблюдается");
        assert bar.getValue() == 20;
        EventLog unchanged = new EventLog();
        timeline.addListener(unchanged);
        timeline.changeBy(0, "Без изменений");
        assert unchanged.getEntries().isEmpty();
        fails(UnsupportedOperationException.class, () -> log.getEntries().clear());
    }

    private static void play(MissionControlFacade facade, ActionStrategy[] choices, int[] expected) {
        for (int i = 0; i < choices.length; i++) {
            TurnResult turn = facade.performAction(choices[i]);
            assert turn.sceneNumber() == i + 1;
            assert turn.after() == expected[i] : turn.describe();
            fails(IllegalStateException.class, () -> facade.performAction(choices[0]));
            if (facade.isActive()) { facade.nextScene(); }
        }
    }

    private static void checkCampaign() {
        MissionControlFacade facade = new MissionControlFacade();
        fails(IllegalStateException.class, () -> facade.performAction(new IllusionStrategy()));
        fails(IllegalStateException.class, facade::getResult);
        fails(IllegalArgumentException.class, () -> facade.startMission(mission(MissionLocation.ASGARD), classic()));
        facade.startMission(mission(MissionLocation.NEW_YORK), classic());
        fails(IllegalStateException.class, facade::nextScene);
        fails(IllegalStateException.class, () -> facade.startMission(mission(MissionLocation.NEW_YORK), classic()));
        play(facade, new ActionStrategy[]{new IllusionStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                new int[]{45, 20, 0});
        MissionResult first = facade.getResult();
        assert first.success() && first.events().size() == 3;
        assert first.getRank().equals("Мастер времени");
        assert facade.isUnlocked(MissionLocation.ASGARD) && !facade.isUnlocked(MissionLocation.VOID);
        fails(IllegalStateException.class, facade::nextScene);
        fails(UnsupportedOperationException.class, () -> facade.getArchive().clear());
        fails(UnsupportedOperationException.class, () -> first.turns().clear());
        LokiVariant ordinary = new MagicAmplifier(new LokiCreator.OrdinaryCreator().createVariant());
        facade.startMission(mission(MissionLocation.ASGARD), ordinary);
        play(facade, new ActionStrategy[]{new NegotiateStrategy(), new IllusionStrategy(), new CombatStrategy()},
                new int[]{45, 20, 0});
        assert facade.isUnlocked(MissionLocation.VOID);
        facade.startMission(mission(MissionLocation.VOID), ordinary);
        play(facade, new ActionStrategy[]{new CombatStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                new int[]{45, 20, 0});
        assert facade.getArchive().size() == 3;
        assert first.turns().size() == 3 && first.finalInstability() == 0;
        assert !facade.isActive();
    }

    private static void checkFailureAndRetry() {
        MissionControlFacade facade = new MissionControlFacade();
        facade.startMission(mission(MissionLocation.NEW_YORK), classic());
        play(facade, new ActionStrategy[]{new CombatStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                new int[]{85, 60, 40});
        assert !facade.getResult().success();
        assert !facade.isUnlocked(MissionLocation.ASGARD);
        facade.startMission(mission(MissionLocation.NEW_YORK), new TimeShield(classic()));
        assert facade.getTimeline().getInstability() == 70;
        assert facade.performAction(new CombatStrategy()).after() == 80;
        Timeline oldTimeline = facade.getTimeline();
        int[] callbacks = {0};
        oldTimeline.addListener(event -> callbacks[0]++);
        facade.abortMission();
        oldTimeline.changeBy(1, "Закрытая сессия");
        assert callbacks[0] == 0;
        assert facade.getArchive().size() == 1;
        fails(IllegalStateException.class, () -> facade.performAction(new IllusionStrategy()));
        Mission hard = new Mission.Builder().location(MissionLocation.NEW_YORK).difficulty(Difficulty.HARD).build();
        facade.startMission(hard, new LokiCreator.ClassicCreator().createVariant());
        assert facade.performAction(new CombatStrategy()).after() == 95;
        facade.nextScene();
        assert facade.performAction(new CombatStrategy()).after() == 100;
        assert !facade.isActive() && !facade.getResult().success();
        assert facade.getResult().turns().size() == 2;
        assert facade.getArchive().size() == 2;
    }

    private static void checkBounds() {
        Timeline timeline = new Timeline(70);
        timeline.changeBy(Integer.MAX_VALUE, "Верхняя граница");
        assert timeline.getInstability() == 100;
        timeline.changeBy(Integer.MIN_VALUE, "Нижняя граница");
        assert timeline.getInstability() == 0;
        fails(IllegalArgumentException.class, () -> new Timeline(-1));
        fails(IllegalArgumentException.class, () -> new Timeline(101));
        assert Timeline.status(80).equals("Критично");
        assert Timeline.status(100).equals("Коллапс");
        MissionControlFacade facade = new MissionControlFacade();
        facade.startMission(new Mission.Builder().location(MissionLocation.NEW_YORK)
                .difficulty(Difficulty.EASY).build(), classic());
        play(facade, new ActionStrategy[]{new IllusionStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                new int[]{35, 10, 0});
        assert facade.getResult().success();
        List<TurnResult> turns = facade.getResult().turns();
        assert turns.get(2).before() == 10 && turns.get(2).after() == 0;
    }

    private static void checkPersistence() throws IOException {
        Path directory = Files.createTempDirectory(Path.of("out"), "campaign-check-");
        Path file = directory.resolve("campaign.xml");
        CampaignStore store = new CampaignStore(file);
        try {
            assert store.load().isEmpty();
            MissionControlFacade game = new MissionControlFacade();
            Mission named = new Mission.Builder().location(MissionLocation.NEW_YORK)
                    .title("Локи & Сильви: <разлом> \"2012\"").build();
            game.startMission(named, new MagicAmplifier(new DiplomacyAmulet(new LokiCreator.SylvieCreator().createVariant())));
            play(game, new ActionStrategy[]{new IllusionStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                    new int[]{45, 20, 0});
            store.save(game.getArchive());
            MissionControlFacade restored = new MissionControlFacade(store.load());
            assert !restored.isActive() && restored.isUnlocked(MissionLocation.ASGARD);
            assert !restored.isUnlocked(MissionLocation.VOID);
            assert restored.getArchive().get(0).mission().getTitle().equals(named.getTitle());
            assert restored.getArchive().get(0).describe().equals(game.getResult().describe());
            restored.startMission(mission(MissionLocation.ASGARD), classic());
            restored.abortMission();
            store.save(restored.getArchive());
            assert store.load().size() == 1 : "Aborted missions must not enter the save";
            Mission hard = new Mission.Builder().location(MissionLocation.NEW_YORK).difficulty(Difficulty.HARD).build();
            restored.startMission(hard, new LokiCreator.ClassicCreator().createVariant());
            play(restored, new ActionStrategy[]{new CombatStrategy(), new CombatStrategy()}, new int[]{95, 100});
            store.save(restored.getArchive());
            assert store.load().size() == 2 && store.load().get(1).turns().size() == 2;
            assert !store.load().get(1).success();
            LokiVariant ordinary = new MagicAmplifier(new LokiCreator.OrdinaryCreator().createVariant());
            restored.startMission(mission(MissionLocation.ASGARD), ordinary);
            play(restored, new ActionStrategy[]{new NegotiateStrategy(), new IllusionStrategy(), new CombatStrategy()},
                    new int[]{45, 20, 0});
            restored.startMission(mission(MissionLocation.VOID), ordinary);
            play(restored, new ActionStrategy[]{new CombatStrategy(), new IllusionStrategy(), new NegotiateStrategy()},
                    new int[]{45, 20, 0});
            store.save(restored.getArchive());
            MissionControlFacade completed = new MissionControlFacade(store.load());
            assert completed.getArchive().size() == 4 && completed.isUnlocked(MissionLocation.VOID);
            for (int i = 0; i < 4; i++) {
                assert completed.getArchive().get(i).describe().equals(restored.getArchive().get(i).describe());
            }

            Properties corrupt = new Properties();
            try (var input = Files.newInputStream(file)) { corrupt.loadFromXML(input); }
            corrupt.setProperty("0.turn.0.after", "101");
            try (var output = Files.newOutputStream(file)) { corrupt.storeToXML(output, "Invalid test save"); }
            byte[] original = Files.readAllBytes(file);
            try { store.load(); throw new AssertionError("Corrupt save was accepted"); }
            catch (IOException expected) { assert expected.getMessage().contains("данные кампании"); }
            assert java.util.Arrays.equals(original, Files.readAllBytes(file));

            Files.writeString(file, "broken xml");
            try { store.load(); throw new AssertionError("Malformed XML was accepted"); }
            catch (IOException expected) { assert Files.readString(file).equals("broken xml"); }
            store.save(List.of());
            assert store.load().isEmpty();
            byte[] emptySave = Files.readAllBytes(file);
            try {
                new CampaignStore(file.resolve("blocked.xml")).save(List.of());
                throw new AssertionError("Write into a file as directory was accepted");
            } catch (IOException expected) { assert java.util.Arrays.equals(emptySave, Files.readAllBytes(file)); }
            try (var files = Files.list(directory)) { assert files.count() == 1 : "Temporary save leaked"; }
            System.out.println("PASS: campaign save/reload, unlocks, UTF-8, collapse, abort and corrupt saves");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void fails(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) { return; }
            throw error;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
