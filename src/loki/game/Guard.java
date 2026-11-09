package loki.game;

import loki.builder.Level;
import loki.factory.Loki;
import loki.strategy.GuardStrategy;
import loki.strategy.PatrolStrategy;
import java.util.List;
import java.util.Objects;

public final class Guard {
    private final Player body;
    private final Level.Cell home;
    private final Level.Cell patrolPoint;
    private boolean goingHome;
    private GuardStrategy strategy = new PatrolStrategy();
    private List<Level.Cell> path = List.of();
    private int pathIndex;
    private Level.Cell pathTarget;
    private float lastSeenX, lastSeenY;
    private float searchTime;

    public Guard(Level level, Level.Cell spawn) {
        body = new Player(new Loki("TVA Guard", 0), spawn);
        home = spawn;
        patrolPoint = findPatrolPoint(level, spawn);
    }

    private Level.Cell findPatrolPoint(Level level, Level.Cell spawn) {
        Level.Cell best = spawn;
        int bestDistance = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direction : directions) {
            for (int distance = 1; distance <= 3; distance++) {
                int x = spawn.x() + direction[0] * distance;
                int y = spawn.y() + direction[1] * distance;
                if (level.wall(x, y)) break;
                if (distance > bestDistance) {
                    best = new Level.Cell(x, y);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    public float x() { return body.x(); }
    public float y() { return body.y(); }
    public float getLastSeenX() { return lastSeenX; }
    public float getLastSeenY() { return lastSeenY; }
    public boolean isSearching() { return searchTime > 0; }
    public Level.Cell getPatrolTarget() { return goingHome ? home : patrolPoint; }
    public void nextPatrolPoint() { goingHome = !goingHome; }
    public void setStrategy(GuardStrategy strategy) { this.strategy = Objects.requireNonNull(strategy); }
    public void update(Level level, float targetX, float targetY, float dt) {
        strategy.update(this, level, targetX, targetY, dt);
    }
    public void remember(float targetX, float targetY) {
        lastSeenX = targetX;
        lastSeenY = targetY;
        searchTime = 2;
    }
    public void forget(float dt) { searchTime = Math.max(0, searchTime - dt); }
    public boolean canSee(Level level, float targetX, float targetY) {
        return Math.hypot(x() - targetX, y() - targetY) <= 240
                && level.canSee(x(), y(), targetX, targetY);
    }

    public void walkTo(Level level, float targetX, float targetY, float speed, float dt) {
        Level.Cell from = new Level.Cell((int) (x() / Level.TILE), (int) (y() / Level.TILE));
        Level.Cell target = new Level.Cell((int) (targetX / Level.TILE), (int) (targetY / Level.TILE));
        boolean atCenter = Math.hypot(x() - from.worldX(), y() - from.worldY()) < .01;

        // Finish the current cell before turning, otherwise the body clips a corner.
        if (atCenter && (!target.equals(pathTarget) || pathIndex >= path.size())) {
            path = level.findPath(from, target);
            pathIndex = 0;
            pathTarget = target;
        }
        if (pathIndex >= path.size()) {
            if (from.equals(target)) {
                moveTowards(level, targetX, targetY, speed, dt);
            } else if (!atCenter) {
                moveTowards(level, from.worldX(), from.worldY(), speed, dt);
            }
            return;
        }
        Level.Cell next = path.get(pathIndex);
        if (moveTowards(level, next.worldX(), next.worldY(), speed, dt)) pathIndex++;
    }

    private boolean moveTowards(Level level, float targetX, float targetY, float speed, float dt) {
        float dx = targetX - x();
        float dy = targetY - y();
        float distance = (float) Math.hypot(dx, dy);
        if (distance < .01f) return true;
        float step = Math.min(distance, speed * dt);
        body.move(level, dx / distance * step, dy / distance * step);
        return Math.hypot(targetX - x(), targetY - y()) < .01;
    }
}
