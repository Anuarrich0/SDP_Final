package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;
public final class PatrolStrategy implements GuardStrategy {
    @Override
    public void update(Guard guard, Level level, float targetX, float targetY, float dt) {
        Level.Cell point = guard.getPatrolTarget();
        if (Math.hypot(guard.x() - point.worldX(), guard.y() - point.worldY()) < .1) {
            guard.nextPatrolPoint();
            point = guard.getPatrolTarget();
        }
        guard.walkTo(level, point.worldX(), point.worldY(), 65, dt);
    }
}
