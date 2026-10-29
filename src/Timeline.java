import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Timeline {
    public record Event(int before, int after, String reason) { }

    private int instability;
    private final List<TimelineListener> listeners = new ArrayList<>();

    public Timeline(int instability) {
        if (instability < 0 || instability > 100) {
            throw new IllegalArgumentException("Нестабильность должна быть от 0 до 100.");
        }
        this.instability = instability;
    }

    public int getInstability() { return instability; }

    public void addListener(TimelineListener listener) {
        Objects.requireNonNull(listener);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TimelineListener listener) { listeners.remove(listener); }
    public void clearListeners() { listeners.clear(); }

    public void changeBy(int change, String reason) {
        Objects.requireNonNull(reason);
        int before = instability;
        instability = (int) Math.max(0L, Math.min(100L, (long) before + change));
        if (before != instability) {
            Event event = new Event(before, instability, reason);
            for (TimelineListener listener : List.copyOf(listeners)) {
                listener.onTimelineChanged(event);
            }
        }
    }

    public static String status(int value) {
        if (value == 100) { return "Коллапс"; }
        if (value >= 80) { return "Критично"; }
        if (value > 20) { return "Нестабильна"; }
        return "Стабильна";
    }
}
