import java.util.ArrayList;
import java.util.List;

public class EventLog implements TimelineListener {
    private final List<String> entries = new ArrayList<>();

    @Override
    public void onTimelineChanged(Timeline.Event event) {
        entries.add(event.reason() + ": " + event.before() + " → " + event.after());
    }

    public List<String> getEntries() { return List.copyOf(entries); }
}
