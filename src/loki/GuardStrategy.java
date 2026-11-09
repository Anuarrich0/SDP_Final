package loki;

public interface GuardStrategy {
    void update(Guard guard, Level level, float targetX, float targetY, float dt);
}
