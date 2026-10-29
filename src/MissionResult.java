import java.util.List;

public record MissionResult(Mission mission, String hero, List<TurnResult> turns,
                            List<String> events, int finalInstability) {
    public MissionResult {
        turns = List.copyOf(turns);
        events = List.copyOf(events);
    }

    public boolean success() { return finalInstability <= 20; }

    public String getRank() {
        if (!success()) { return "Миссия провалена"; }
        return finalInstability <= 10 ? "Мастер времени" : "Ветка стабилизирована";
    }

    public String describe() {
        StringBuilder text = new StringBuilder(getRank());
        text.append("\n").append(mission.getLocation()).append(" / ").append(mission.getDifficulty());
        text.append("\n").append(hero);
        text.append("\nНестабильность: ").append(mission.getDifficulty().getStartingInstability())
                .append(" → ").append(finalInstability).append("\n");
        for (TurnResult turn : turns) {
            text.append("\n").append(turn.describe()).append("\n");
        }
        text.append("\nЖурнал TVA:\n");
        for (String event : events) { text.append(event).append("\n"); }
        return text.toString();
    }

    public String toString() {
        return mission.getLocation() + " / " + mission.getDifficulty() + " / "
                + (success() ? "Успех" : "Провал") + " / " + finalInstability;
    }
}
