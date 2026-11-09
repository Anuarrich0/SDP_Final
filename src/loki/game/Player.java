package loki.game;

import loki.builder.Level;
import loki.factory.LokiVariant;
import java.util.Objects;

/** Grid-based movement: moves smoothly between cell centers (48px), 4 directions only. */
public final class Player {
    public static final float RADIUS = 13;
    public enum Direction { UP, DOWN, LEFT, RIGHT, NONE }

    private final LokiVariant variant;
    private float x, y;
    private Level.Cell currentCell;
    private Level.Cell targetCell;
    private Direction currentDirection = Direction.NONE;
    private Direction nextDirection = Direction.NONE;
    private float moveProgress; // 0..1 between currentCell and targetCell
    private float moveSpeed;
    private boolean isDashing;
    private float dashDistanceRemaining;

    public Player(LokiVariant variant, Level.Cell spawn) {
        this.variant = Objects.requireNonNull(variant);
        this.currentCell = spawn;
        this.targetCell = spawn;
        this.x = spawn.worldX();
        this.y = spawn.worldY();
        this.moveProgress = 1f;
        this.moveSpeed = 150 + variant.getCombatPower() * .4f;
    }

    public LokiVariant variant() { return variant; }
    public float x() { return x; }
    public float y() { return y; }
    public float speed() { return moveSpeed; }

    /** Guards move at the pace their strategy asks for; the hero keeps the variant pace. */
    public void setSpeed(float speed) { moveSpeed = speed; }
    public Direction currentDirection() { return currentDirection; }
    public Level.Cell currentCell() { return currentCell; }
    public boolean isMoving() { return moveProgress < 1f; }

    /** Package-private for Guard to check if a move is valid. */
    boolean canMove(Level level, Level.Cell from, Direction dir) {
        Level.Cell next = cellInDirection(from, dir);
        return !level.wall(next.x(), next.y());
    }

    /** Call each frame with the direction the player holds (or NONE). */
    public void update(Level level, Direction inputDir, float dt) {
        if (!Float.isFinite(dt) || dt <= 0 || dt > .05f) return;

        // Handle dash
        if (isDashing) {
            updateDash(level, dt);
            return;
        }

        // A queued turn (guards) wins over the held key. The hero never queues.
        Direction desired = nextDirection != Direction.NONE ? nextDirection : inputDir;

        // Turn around instantly, without waiting for the next cell center.
        if (moveProgress < 1f && desired != Direction.NONE && desired == opposite(currentDirection)) {
            reverse(desired);
            nextDirection = Direction.NONE;
        }

        // At a cell center we are allowed to pick the next movement.
        if (moveProgress >= 1f) {
            if (desired != Direction.NONE && canMove(level, currentCell, desired)) {
                startMove(level, desired);
            } else if (inputDir != Direction.NONE && currentDirection != Direction.NONE
                    && canMove(level, currentCell, currentDirection)) {
                // Key held but the turn is not possible yet: keep the current movement.
                startMove(level, currentDirection);
            } else {
                // Nothing held (or boxed in): stop exactly at this center.
                currentDirection = Direction.NONE;
                targetCell = currentCell;
                moveProgress = 1f;
            }
            nextDirection = Direction.NONE;
        }

        // Move towards the target cell, driven by game time instead of frames.
        if (moveProgress < 1f) {
            moveProgress = Math.min(1f, moveProgress + moveSpeed / Level.TILE * dt);
            float cx = currentCell.worldX();
            float cy = currentCell.worldY();
            x = cx + (targetCell.worldX() - cx) * moveProgress;
            y = cy + (targetCell.worldY() - cy) * moveProgress;
            if (moveProgress >= 1f) {
                currentCell = targetCell;
                x = currentCell.worldX();
                y = currentCell.worldY();
            }
        }
    }

    private static Direction opposite(Direction dir) {
        return switch (dir) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            default -> Direction.NONE;
        };
    }

    /** Flip the move in progress: the previous target becomes the new start. */
    private void reverse(Direction dir) {
        Level.Cell from = currentCell;
        currentCell = targetCell;
        targetCell = from;
        moveProgress = 1f - moveProgress;
        currentDirection = dir;
        x = currentCell.worldX() + (targetCell.worldX() - currentCell.worldX()) * moveProgress;
        y = currentCell.worldY() + (targetCell.worldY() - currentCell.worldY()) * moveProgress;
    }

    private void startMove(Level level, Direction dir) {
        Level.Cell next = cellInDirection(currentCell, dir);
        if (level.wall(next.x(), next.y())) return;
        currentDirection = dir;
        targetCell = next;
        moveProgress = 0f;
    }

    private static Level.Cell cellInDirection(Level.Cell cell, Direction dir) {
        return switch (dir) {
            case UP -> new Level.Cell(cell.x(), cell.y() + 1);
            case DOWN -> new Level.Cell(cell.x(), cell.y() - 1);
            case LEFT -> new Level.Cell(cell.x() - 1, cell.y());
            case RIGHT -> new Level.Cell(cell.x() + 1, cell.y());
            default -> cell;
        };
    }

    /** Direction to apply at the next cell center. NONE means "stop there". */
    public void queueDirection(Direction dir) {
        nextDirection = dir;
    }

    /** Start a dash in the current movement direction (or last input). */
    public boolean startDash(Direction lastInputDir) {
        if (isDashing) return false;
        Direction dashDir = currentDirection != Direction.NONE ? currentDirection : lastInputDir;
        if (dashDir == Direction.NONE) return false;
        isDashing = true;
        dashDistanceRemaining = Level.TILE * 2; // dash 2 cells
        return true;
    }

    private void updateDash(Level level, float dt) {
        float dashSpeed = speed() * 3f; // 3x speed
        float dashStep = dashSpeed * dt;
        float dx = 0, dy = 0;
        switch (currentDirection) {
            case UP -> dy = dashStep;
            case DOWN -> dy = -dashStep;
            case LEFT -> dx = -dashStep;
            case RIGHT -> dx = dashStep;
            default -> { isDashing = false; return; }
        }

        // Check wall collision during dash
        float newX = x + dx;
        float newY = y + dy;
        if (!level.canStand(newX, newY, RADIUS)) {
            // Hit wall - stop at wall
            isDashing = false;
            // Snap to nearest cell center
            snapToNearestCell(level);
            return;
        }

        x = newX;
        y = newY;
        dashDistanceRemaining -= dashStep;

        if (dashDistanceRemaining <= 0) {
            isDashing = false;
            snapToNearestCell(level);
        }
    }

    private void snapToNearestCell(Level level) {
        // Cell centers sit at (index + .5) * TILE, so floor gives the containing cell.
        int cellX = (int) Math.floor(x / Level.TILE);
        int cellY = (int) Math.floor(y / Level.TILE);
        // Clamp to valid cell
        if (level.wall(cellX, cellY)) {
            // Find nearest valid cell (current cell)
            cellX = currentCell.x();
            cellY = currentCell.y();
        }
        currentCell = new Level.Cell(cellX, cellY);
        targetCell = currentCell;
        x = currentCell.worldX();
        y = currentCell.worldY();
        moveProgress = 1f;
    }

    /** Teleport to a cell (for illusion spawn, etc.) */
    public void setPosition(Level.Cell cell) {
        currentCell = cell;
        targetCell = cell;
        x = cell.worldX();
        y = cell.worldY();
        moveProgress = 1f;
        currentDirection = Direction.NONE;
        nextDirection = Direction.NONE;
    }
}