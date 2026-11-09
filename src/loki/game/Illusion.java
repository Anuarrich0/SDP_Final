package loki.game;
public final class Illusion {
    private final float x, y;
    private float timeLeft;

    public Illusion(float x, float y, float duration) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(duration) || duration <= 0) {
            throw new IllegalArgumentException("Invalid illusion");
        }
        this.x = x;
        this.y = y;
        timeLeft = duration;
    }
    public float x() { return x; }
    public float y() { return y; }
    public float getTimeLeft() { return timeLeft; }
    public void update(float dt) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Invalid time step");
        timeLeft = Math.max(0, timeLeft - dt);
    }
}
