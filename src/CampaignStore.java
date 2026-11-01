import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// XML Properties keeps the save readable without adding a JSON library.
public final class CampaignStore {
    private final Path path;

    public CampaignStore(Path path) { this.path = path.toAbsolutePath(); }

    public Path getPath() { return path; }

    public List<MissionResult> load() throws IOException {
        if (Files.notExists(path)) { return List.of(); }
        Properties data = new Properties();
        try (InputStream input = Files.newInputStream(path)) { data.loadFromXML(input); }
        try {
            if (!required(data, "version").equals("1")) {
                throw new IllegalArgumentException("Неизвестная версия сохранения.");
            }
            int count = number(data, "missions", 0, data.size());
            List<MissionResult> archive = new ArrayList<>();
            for (int i = 0; i < count; i++) { archive.add(readResult(data, i + ".")); }
            // Restore through the facade so locked locations cannot be introduced by a bad save.
            return new MissionControlFacade(archive).getArchive();
        } catch (IllegalArgumentException error) {
            throw new IOException("Повреждены данные кампании: " + error.getMessage(), error);
        }
    }

    public void save(List<MissionResult> archive) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", "1");
        data.setProperty("missions", Integer.toString(archive.size()));
        for (int i = 0; i < archive.size(); i++) {
            MissionResult result = archive.get(i);
            String key = i + ".";
            data.setProperty(key + "title", result.mission().getTitle());
            data.setProperty(key + "location", result.mission().getLocation().name());
            data.setProperty(key + "difficulty", result.mission().getDifficulty().name());
            data.setProperty(key + "hero", result.hero());
            data.setProperty(key + "final", Integer.toString(result.finalInstability()));
            data.setProperty(key + "turns", Integer.toString(result.turns().size()));
            for (int j = 0; j < result.turns().size(); j++) {
                TurnResult turn = result.turns().get(j);
                String turnKey = key + "turn." + j + ".";
                data.setProperty(turnKey + "action", turn.action().action());
                data.setProperty(turnKey + "score", Integer.toString(turn.action().score()));
                data.setProperty(turnKey + "threshold", Integer.toString(turn.action().threshold()));
                data.setProperty(turnKey + "before", Integer.toString(turn.before()));
                data.setProperty(turnKey + "after", Integer.toString(turn.after()));
            }
            data.setProperty(key + "events", Integer.toString(result.events().size()));
            for (int j = 0; j < result.events().size(); j++) {
                data.setProperty(key + "event." + j, result.events().get(j));
            }
        }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "campaign-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                data.storeToXML(output, "LOKI TVA campaign - version 1", StandardCharsets.UTF_8);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static MissionResult readResult(Properties data, String key) {
        Mission mission = new Mission.Builder().title(required(data, key + "title"))
                .location(MissionLocation.valueOf(required(data, key + "location")))
                .difficulty(Difficulty.valueOf(required(data, key + "difficulty"))).build();
        int count = number(data, key + "turns", 1, 3);
        int finalValue = number(data, key + "final", 0, 100);
        int previous = mission.getDifficulty().getStartingInstability();
        List<TurnResult> turns = new ArrayList<>();
        for (int j = 0; j < count; j++) {
            String turnKey = key + "turn." + j + ".";
            ActionResult action = new ActionResult(required(data, turnKey + "action"),
                    number(data, turnKey + "score", -100, 200),
                    number(data, turnKey + "threshold", 0, 100));
            int before = number(data, turnKey + "before", 0, 99);
            int after = number(data, turnKey + "after", 0, 100);
            int expected = action.success() ? Math.max(0, before - (j == 2 ? 20 : 25)) : Math.min(100, before + 15);
            boolean shieldPenalty = !action.success() && after == Math.min(100, before + 10);
            if (before != previous || action.threshold() != mission.getDifficulty().getThreshold()
                    || (after != expected && !shieldPenalty)) {
                throw new IllegalArgumentException("Несогласованные результаты действий.");
            }
            turns.add(new TurnResult(j + 1, mission.getScenes().get(j).title(), action, before, after));
            previous = after;
        }
        if (previous != finalValue || (count < 3 && finalValue != 100)) {
            throw new IllegalArgumentException("В архиве есть незавершённая миссия.");
        }
        int eventCount = number(data, key + "events", 0, count);
        List<String> events = new ArrayList<>();
        for (int j = 0; j < eventCount; j++) { events.add(required(data, key + "event." + j)); }
        return new MissionResult(mission, required(data, key + "hero"), turns, events, finalValue);
    }

    private static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Нет поля " + key); }
        return value;
    }

    private static int number(Properties data, String key, int min, int max) {
        int value = Integer.parseInt(required(data, key));
        if (value < min || value > max) { throw new IllegalArgumentException("Неверное значение " + key); }
        return value;
    }
}
