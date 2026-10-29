import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Color;

// Второй Observer: меняет виджеты, не рассчитывая результат действия.
public class TimelineMonitor implements TimelineListener {
    private final JProgressBar bar;
    private final JLabel status;

    public TimelineMonitor(JProgressBar bar, JLabel status) {
        this.bar = bar;
        this.status = status;
    }

    public void showValue(int value) {
        bar.setValue(value);
        bar.setString("Нестабильность: " + value + " / 100");
        bar.setForeground(value >= 80 ? new Color(190, 100, 60) : new Color(61, 139, 103));
        status.setText("TVA: " + Timeline.status(value));
    }

    @Override
    public void onTimelineChanged(Timeline.Event event) { showValue(event.after()); }
}
