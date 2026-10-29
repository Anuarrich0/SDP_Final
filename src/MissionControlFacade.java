import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class MissionControlFacade {
    private final List<MissionResult> archive = new ArrayList<>();
    private final EnumSet<MissionLocation> unlocked = EnumSet.of(MissionLocation.NEW_YORK);
    private Mission mission;
    private LokiVariant hero;
    private Timeline timeline;
    private EventLog log;
    private final List<TurnResult> turns = new ArrayList<>();
    private int sceneIndex;
    private boolean awaitingNext;
    private MissionResult result;

    public void startMission(Mission mission, LokiVariant hero) {
        Objects.requireNonNull(mission);
        Objects.requireNonNull(hero);
        if (isActive()) {
            throw new IllegalStateException("Сначала завершите текущую миссию.");
        }
        if (!isUnlocked(mission.getLocation())) {
            throw new IllegalArgumentException("Сначала пройдите предыдущую миссию.");
        }
        if (timeline != null) { timeline.clearListeners(); }
        this.mission = mission;
        this.hero = hero;
        timeline = new Timeline(mission.getDifficulty().getStartingInstability());
        log = new EventLog();
        timeline.addListener(log);
        sceneIndex = 0;
        awaitingNext = false;
        result = null;
        turns.clear();
    }

    public TurnResult performAction(ActionStrategy strategy) {
        Objects.requireNonNull(strategy);
        if (!isActive() || awaitingNext) {
            throw new IllegalStateException("Сейчас нельзя выполнить действие.");
        }
        Scene scene = getCurrentScene();
        ActionResult action = strategy.execute(hero, scene, mission.getDifficulty());
        int before = timeline.getInstability();
        int reduction = sceneIndex == 2 ? 20 : 25;
        int change = action.success() ? -reduction : hero.getFailurePenalty();
        awaitingNext = true;
        timeline.changeBy(change, "Сцена " + (sceneIndex + 1) + ", " + action.action());
        TurnResult turn = new TurnResult(sceneIndex + 1, scene.title(), action,
                before, timeline.getInstability());
        turns.add(turn);
        if (timeline.getInstability() == 100 || sceneIndex == mission.getScenes().size() - 1) {
            result = new MissionResult(mission, hero.getDescription(), turns,
                    log.getEntries(), timeline.getInstability());
            archive.add(result);
            if (result.success()) {
                int next = mission.getLocation().ordinal() + 1;
                if (next < MissionLocation.values().length) { unlocked.add(MissionLocation.values()[next]); }
            }
            timeline.clearListeners();
        }
        return turn;
    }

    public void nextScene() {
        if (!isActive() || !awaitingNext) {
            throw new IllegalStateException("Сначала подтвердите действие в текущей сцене.");
        }
        sceneIndex++;
        awaitingNext = false;
    }

    public void abortMission() {
        if (timeline != null) { timeline.clearListeners(); }
        mission = null;
        result = null;
        turns.clear();
    }

    public boolean isActive() { return mission != null && result == null; }
    public boolean isUnlocked(MissionLocation location) { return unlocked.contains(location); }

    public Scene getCurrentScene() {
        if (!isActive()) { throw new IllegalStateException("Нет активной миссии."); }
        return mission.getScenes().get(sceneIndex);
    }

    public int getSceneNumber() { return sceneIndex + 1; }
    public Mission getMission() { return mission; }
    public LokiVariant getHero() { return hero; }

    public Timeline getTimeline() {
        if (timeline == null) { throw new IllegalStateException("Миссия ещё не запущена."); }
        return timeline;
    }

    public List<String> getEventEntries() { return log == null ? List.of() : log.getEntries(); }

    public MissionResult getResult() {
        if (result == null) { throw new IllegalStateException("Миссия ещё не завершена."); }
        return result;
    }

    public List<MissionResult> getArchive() { return List.copyOf(archive); }
}
