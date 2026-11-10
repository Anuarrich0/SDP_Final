package loki.game;

import loki.builder.Level;
import loki.factory.LokiVariant;
import java.util.Objects;

public final class Player {
    public static final float RADIUS = 13;
    public enum Direction { UP, DOWN, LEFT, RIGHT, NONE }
    private final LokiVariant variant;
    private Level.Cell cell, target;
    private float x, y, progress = 1, moveSpeed, dashTime;
    private boolean houseAccess;
    private Direction direction = Direction.NONE, nextDirection = Direction.NONE;

    public Player(LokiVariant variant, Level.Cell spawn) {
        this.variant = Objects.requireNonNull(variant);
        moveSpeed = 150 + variant.getCombatPower() * .4f;
        setPosition(spawn);
    }
    Player(Player source) {
        variant=source.variant;cell=source.cell;target=source.target;
        x=source.x;y=source.y;progress=source.progress;
        moveSpeed=source.moveSpeed;direction=source.direction;
    }
    public LokiVariant variant() { return variant; }
    public float x() { return x; }
    public float y() { return y; }
    public float speed() { return moveSpeed; }
    public void setSpeed(float speed) {
        if (!Float.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Invalid speed");
        moveSpeed = speed;
    }
    public Direction currentDirection() { return direction; }
    public Level.Cell currentCell() {
        return new Level.Cell((int)Math.floor(x / Level.TILE), (int)Math.floor(y / Level.TILE));
    }
    public boolean isMoving() { return progress < 1; }
    public float timeToCenter() { return isMoving() ? (1-progress)*Level.TILE/moveSpeed : Level.TILE/moveSpeed; }
    boolean canMove(Level level, Level.Cell from, Direction dir) {
        Level.Cell next = neighbor(level,from,dir);
        return dir != Direction.NONE && level.canEnter(next,houseAccess);
    }
    void setHouseAccess(boolean value) { houseAccess=value; }
    public void cancelDash() { dashTime=0; }
    public static Level.Cell neighbor(Level level, Level.Cell from, Direction dir) {
        Level.Cell next=step(from,dir);
        return level.neighbor(from,next.x()-from.x(),next.y()-from.y());
    }
    public void queueDirection(Direction dir) { nextDirection = Objects.requireNonNull(dir); }

    public void update(Level level, Direction input, float dt) {
        Objects.requireNonNull(input);
        if (!Float.isFinite(dt) || dt <= 0 || dt > .05f) throw new IllegalArgumentException("Invalid time step");
        if (input != Direction.NONE) nextDirection = input;
        if (isMoving() && nextDirection == opposite(direction)) reverse();
        float time = dt;
        while (time > .000001f) {
            if (!isMoving()) {
                Direction turn = canMove(level, cell, nextDirection) ? nextDirection : direction;
                if (!canMove(level, cell, turn)) { dashTime = 0; break; }
                direction = turn;
                target = neighbor(level,cell,direction);
                progress = 0;
                if (turn == nextDirection) nextDirection = Direction.NONE;
            }
            if (level.suppressed(cell) || level.suppressed(target)) dashTime=0;
            float speed = moveSpeed * (dashTime > 0 ? 3 : 1);
            float used = Math.min(time, (1 - progress) * Level.TILE / speed);
            if (dashTime > 0) used = Math.min(used, dashTime);
            progress = Math.min(1, progress + used * speed / Level.TILE);
            time -= used;
            dashTime = Math.max(0, dashTime - used);
            if (progress > .99999f) { progress = 1; cell = target; }
            float dx=target.worldX()-cell.worldX();
            float width=level.width()*Level.TILE;
            if (dx>Level.TILE) dx-=width;
            else if (dx < -Level.TILE) dx+=width;
            x=(cell.worldX()+dx*progress+width)%width;
            y = cell.worldY() + (target.worldY() - cell.worldY()) * progress;
        }
    }
    public boolean startDash(Direction lastInput) {
        if (dashTime > 0 || (direction == Direction.NONE && lastInput == Direction.NONE)) return false;
        if (direction == Direction.NONE) nextDirection = lastInput;
        dashTime = Level.TILE * 2 / (moveSpeed * 3);
        return true;
    }
    public void reverse() {
        if (direction == Direction.NONE) return;
        if (isMoving()) {
            Level.Cell old = cell; cell = target; target = old;
            progress = 1 - progress;
        }
        direction = opposite(direction);
        nextDirection = Direction.NONE;
    }
    public void setPosition(Level.Cell spawn) {
        cell = Objects.requireNonNull(spawn); target = cell;
        x = cell.worldX(); y = cell.worldY(); progress = 1; dashTime = 0;
        direction = Direction.NONE; nextDirection = Direction.NONE;
    }
    public static Direction opposite(Direction dir) {
        return switch (dir) {
            case UP -> Direction.DOWN; case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT; case RIGHT -> Direction.LEFT;
            default -> Direction.NONE;
        };
    }
    public static Level.Cell step(Level.Cell cell, Direction dir) {
        return switch (dir) {
            case UP -> new Level.Cell(cell.x(), cell.y()+1);
            case DOWN -> new Level.Cell(cell.x(), cell.y()-1);
            case LEFT -> new Level.Cell(cell.x()-1, cell.y());
            case RIGHT -> new Level.Cell(cell.x()+1, cell.y());
            default -> cell;
        };
    }
}
