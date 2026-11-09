package loki;

public final class ChaseStrategy implements GuardStrategy {
    @Override
    public void update(Guard guard, Level level, float targetX, float targetY, float dt) {
        guard.walkTo(level, targetX, targetY, 100, dt);
    }
}
