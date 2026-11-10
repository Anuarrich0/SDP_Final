package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;
import loki.game.Player;

public final class ChaseStrategy implements GuardStrategy {
    private static final int X5_FAR_CELLS = 8;

    @Override
    public void update(Guard guard, Level level, HuntContext context) {
        Level.Cell target = switch (guard.type()) {
        case B15 -> context.prey();
        case D90 -> ahead(context.prey(), context.preyDirection(), 4);
        case C20 -> ambush(context);
        case X5 -> far(guard, context) ? context.prey() : guard.scatterCorner();
        };
        guard.steerTo(level, target);
    }

    private static Level.Cell ambush(HuntContext context) {
        Level.Cell pivot = ahead(context.prey(), context.preyDirection(), 2);
        Level.Cell leader = context.leader();
        return new Level.Cell(2 * pivot.x() - leader.x(), 2 * pivot.y() - leader.y());
    }

    private static boolean far(Guard guard, HuntContext context) {
        int dx = context.prey().x() - guard.currentCell().x();
        int dy = context.prey().y() - guard.currentCell().y();
        return dx * dx + dy * dy >= X5_FAR_CELLS * X5_FAR_CELLS;
    }

    private static Level.Cell ahead(Level.Cell cell, Player.Direction direction, int cells) {
        return switch (direction) {
        // Keep the original Pac-Man upward targeting offset.
        case UP -> new Level.Cell(cell.x() - cells, cell.y() + cells);
        case DOWN -> new Level.Cell(cell.x(), cell.y() - cells);
        case LEFT -> new Level.Cell(cell.x() - cells, cell.y());
        case RIGHT -> new Level.Cell(cell.x() + cells, cell.y());
        default -> cell;
        };
    }
}
