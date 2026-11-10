package loki.game;

import loki.builder.Level;
import loki.factory.LokiVariant;
import java.util.Objects;

public final class Player {
    public static final float RADIUS = 13;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT, NONE
    }

    private final LokiVariant variant;
    private Level.Cell startCell, targetCell;
    private float x, y, moveProgress = 1, moveSpeed, dashTime;
    private boolean houseAccess;
    private Direction direction = Direction.NONE, nextDirection = Direction.NONE;

    public Player(LokiVariant variant, Level.Cell spawn) {
        this.variant = Objects.requireNonNull(variant);
        moveSpeed = 150 + variant.getAgility() * .4f;
        setPosition(spawn);
    }

    // The clone keeps its position between cells without snapping to the center.
    Player(Player source) {
        variant = source.variant;
        startCell = source.startCell;
        targetCell = source.targetCell;
        x = source.x;
        y = source.y;
        moveProgress = source.moveProgress;
        moveSpeed = source.moveSpeed;
        direction = source.direction;
    }

    public LokiVariant variant() {
        return variant;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }


    public void setSpeed(float speed) {
        if (!Float.isFinite(speed) || speed <= 0) {
            throw new IllegalArgumentException("Invalid speed");
        }
        moveSpeed = speed;
    }

    public Direction currentDirection() {
        return direction;
    }

    public Level.Cell currentCell() {
        return new Level.Cell((int) Math.floor(x / Level.TILE), (int) Math.floor(y / Level.TILE));
    }

    public boolean isMoving() {
        return moveProgress < 1;
    }

    public float timeToCenter() {
        return isMoving() ? (1 - moveProgress) * Level.TILE / moveSpeed : Level.TILE / moveSpeed;
    }

    boolean canMove(Level level, Level.Cell from, Direction direction) {
        Level.Cell next = neighbor(level, from, direction);
        return direction != Direction.NONE && level.canEnter(next, houseAccess);
    }

    void setHouseAccess(boolean value) {
        houseAccess = value;
    }

    public void cancelDash() {
        dashTime = 0;
    }

    public static Level.Cell neighbor(Level level, Level.Cell from, Direction direction) {
        Level.Cell next = step(from, direction);
        return level.neighbor(from, next.x() - from.x(), next.y() - from.y());
    }

    public void queueDirection(Direction direction) {
        nextDirection = Objects.requireNonNull(direction);
    }

    public void update(Level level, Direction input, float deltaTime) {
        Objects.requireNonNull(input);
        if (!Float.isFinite(deltaTime) || deltaTime <= 0 || deltaTime > .05f) {
            throw new IllegalArgumentException("Invalid time step");
        }
        if (input != Direction.NONE) {
            nextDirection = input;
        }
        if (isMoving() && nextDirection == opposite(direction)) {
            reverse();
        }
        float remainingTime = deltaTime;
        // Preserve the remaining time when moving to the next cell.
        while (remainingTime > .000001f) {
            if (!isMoving()) {
                Direction turn = canMove(level, startCell, nextDirection) ? nextDirection : direction;
                if (!canMove(level, startCell, turn)) {
                    dashTime = 0;
                    break;
                }
                direction = turn;
                targetCell = neighbor(level, startCell, direction);
                moveProgress = 0;
                if (turn == nextDirection) {
                    nextDirection = Direction.NONE;
                }
            }
            if (level.suppressed(startCell) || level.suppressed(targetCell)) {
                dashTime = 0;
            }
            float speed = moveSpeed * (dashTime > 0 ? 3 : 1);
            float stepTime = Math.min(remainingTime, (1 - moveProgress) * Level.TILE / speed);
            if (dashTime > 0) {
                stepTime = Math.min(stepTime, dashTime);
            }
            moveProgress = Math.min(1, moveProgress + stepTime * speed / Level.TILE);
            remainingTime -= stepTime;
            dashTime = Math.max(0, dashTime - stepTime);
            if (moveProgress > .99999f) {
                moveProgress = 1;
                startCell = targetCell;
            }
            float dx = targetCell.worldX() - startCell.worldX();
            float width = level.width() * Level.TILE;
            // Take the shortest tunnel route across the screen edge.
            if (dx > Level.TILE) {
                dx -= width;
            } else if (dx < -Level.TILE) {
                dx += width;
            }
            x = (startCell.worldX() + dx * moveProgress + width) % width;
            y = startCell.worldY() + (targetCell.worldY() - startCell.worldY()) * moveProgress;
        }
    }

    public boolean startDash(Direction lastInput) {
        if (dashTime > 0 || (direction == Direction.NONE && lastInput == Direction.NONE)) {
            return false;
        }
        if (direction == Direction.NONE) {
            nextDirection = lastInput;
        }
        dashTime = Level.TILE * 2 / (moveSpeed * 3);
        return true;
    }

    public void reverse() {
        if (direction == Direction.NONE) {
            return;
        }
        if (isMoving()) {
            Level.Cell old = startCell;
            startCell = targetCell;
            targetCell = old;
            moveProgress = 1 - moveProgress;
        }
        direction = opposite(direction);
        nextDirection = Direction.NONE;
    }

    public void setPosition(Level.Cell spawn) {
        startCell = Objects.requireNonNull(spawn);
        targetCell = startCell;
        x = startCell.worldX();
        y = startCell.worldY();
        moveProgress = 1;
        dashTime = 0;
        direction = Direction.NONE;
        nextDirection = Direction.NONE;
    }

    public static Direction opposite(Direction direction) {
        return switch (direction) {
        case UP -> Direction.DOWN;
        case DOWN -> Direction.UP;
        case LEFT -> Direction.RIGHT;
        case RIGHT -> Direction.LEFT;
        default -> Direction.NONE;
        };
    }

    public static Level.Cell step(Level.Cell startCell, Direction direction) {
        return switch (direction) {
        case UP -> new Level.Cell(startCell.x(), startCell.y() + 1);
        case DOWN -> new Level.Cell(startCell.x(), startCell.y() - 1);
        case LEFT -> new Level.Cell(startCell.x() - 1, startCell.y());
        case RIGHT -> new Level.Cell(startCell.x() + 1, startCell.y());
        default -> startCell;
        };
    }
}
