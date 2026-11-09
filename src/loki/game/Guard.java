package loki.game;

import loki.builder.Level;
import loki.factory.Loki;
import loki.strategy.GuardStrategy;
import loki.strategy.HuntContext;
import loki.strategy.ScatterStrategy;
import java.util.Objects;

/** A TVA hunter: it picks a direction at cell centres and follows its current strategy. */
public final class Guard {
    /** Ties between equal-squared-distance passages break in this order, as the brief asks. */
    private static final Player.Direction[] TURN_ORDER = {
            Player.Direction.UP, Player.Direction.LEFT, Player.Direction.DOWN, Player.Direction.RIGHT};

    private final Player body;
    private final GuardType type;
    private final Level.Cell scatterCorner;
    private GuardStrategy strategy = new ScatterStrategy();
    private boolean reverseAllowed;

    public Guard(Level level, Level.Cell spawn, GuardType type) {
        body = new Player(new Loki("TVA Guard", 0), spawn);
        this.type = Objects.requireNonNull(type);
        scatterCorner = corner(level, spawn, type);
    }

    /** Every hunter owns one corner to run to while the mode is SCATTER. */
    private static Level.Cell corner(Level level, Level.Cell spawn, GuardType type) {
        int left = 1, right = level.width() - 2, bottom = 1, top = level.height() - 2;
        Level.Cell cell = switch (type) {
            case B15 -> new Level.Cell(right, top);
            case D90 -> new Level.Cell(left, top);
            case C20 -> new Level.Cell(right, bottom);
            case X5 -> new Level.Cell(left, bottom);
        };
        return level.wall(cell.x(), cell.y()) ? spawn : cell;
    }

    public float x() { return body.x(); }
    public float y() { return body.y(); }
    public GuardType type() { return type; }
    public Level.Cell scatterCorner() { return scatterCorner; }
    public Level.Cell currentCell() { return body.currentCell(); }
    public Player.Direction currentDirection() { return body.currentDirection(); }

    /** GameSession swaps this when the global CHASE/SCATTER mode changes. */
    public void setStrategy(GuardStrategy strategy) { this.strategy = Objects.requireNonNull(strategy); }

    /** A mode change may send the hunter back the way it came, even mid-corridor. */
    public void allowReverse() { reverseAllowed = true; }

    public void update(Level level, HuntContext context, float dt) {
        strategy.update(this, level, context);
        body.update(level, Player.Direction.NONE, dt);
    }

    /** Pac-Man style: choose only at a cell centre, by the squared distance from the neighbour to the target. */
    public void steerTo(Level level, Level.Cell target, float speed) {
        body.setSpeed(speed);
        if (body.isMoving()) return; // between centres the direction is kept

        Level.Cell from = body.currentCell();
        Player.Direction back = opposite(body.currentDirection());
        boolean mayTurnBack = reverseAllowed || body.currentDirection() == Player.Direction.NONE;
        Player.Direction best = Player.Direction.NONE;
        long bestDistance = Long.MAX_VALUE;
        for (Player.Direction dir : TURN_ORDER) {
            if (!body.canMove(level, from, dir)) continue;
            if (dir == back && !mayTurnBack) continue;
            Level.Cell next = step(from, dir);
            long dx = next.x() - target.x();
            long dy = next.y() - target.y();
            long distance = dx * dx + dy * dy;
            if (distance < bestDistance) { bestDistance = distance; best = dir; }
        }
        if (best == Player.Direction.NONE) { // dead end: the way back is the only way out
            for (Player.Direction dir : TURN_ORDER) {
                if (body.canMove(level, from, dir)) { best = dir; break; }
            }
        }
        reverseAllowed = false;
        body.queueDirection(best);
    }

    private static Level.Cell step(Level.Cell cell, Player.Direction dir) {
        return switch (dir) {
            case UP -> new Level.Cell(cell.x(), cell.y() + 1);
            case DOWN -> new Level.Cell(cell.x(), cell.y() - 1);
            case LEFT -> new Level.Cell(cell.x() - 1, cell.y());
            case RIGHT -> new Level.Cell(cell.x() + 1, cell.y());
            default -> cell;
        };
    }

    private static Player.Direction opposite(Player.Direction dir) {
        return switch (dir) {
            case UP -> Player.Direction.DOWN;
            case DOWN -> Player.Direction.UP;
            case LEFT -> Player.Direction.RIGHT;
            case RIGHT -> Player.Direction.LEFT;
            default -> Player.Direction.NONE;
        };
    }
}
