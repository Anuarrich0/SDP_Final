package loki.game;

public final class CharacterAnimation {
    public enum Direction {
        DOWN, UP, RIGHT, LEFT
    }

    private Direction direction = Direction.DOWN;
    private float distance;
    private int frame;
    private float lastX, lastY;

    public CharacterAnimation(float x, float y) {
        lastX = x;
        lastY = y;
    }

    public void updatePosition(float x, float y) {
        update(x - lastX, y - lastY);
        lastX = x;
        lastY = y;
    }

    /** Set direction explicitly (for grid-based movement). */
    public void updateDirection(Player.Direction dir) {
        if (dir == null || dir == Player.Direction.NONE) {
            return;
        }
        direction = switch (dir) {
        case UP -> Direction.UP;
        case DOWN -> Direction.DOWN;
        case LEFT -> Direction.LEFT;
        case RIGHT -> Direction.RIGHT;
        default -> direction;
        };
    }

    public Direction direction() {
        return direction;
    }

    public int frame() {
        return frame;
    }

    public void update(float dx, float dy) {
        float moved = (float) Math.hypot(dx, dy);
        if (moved < .001f) {
            distance = 0;
            frame = 0;
            return;
        }
        if (Math.abs(dy) > Math.abs(dx)) {
            direction = dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        distance = (distance + moved) % 48;
        frame = switch ((int) (distance / 12)) {
        case 0 -> 1;
        case 2 -> 2;
        default -> 0;
        };
    }
}