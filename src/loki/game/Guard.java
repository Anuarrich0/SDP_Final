package loki.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import loki.builder.Level;
import loki.factory.Loki;
import loki.strategy.GuardStrategy;
import loki.strategy.HuntContext;
import loki.strategy.ScatterStrategy;

public final class Guard {

    // Equal distances use this turn priority.
    private static final Player.Direction[] TURN_ORDER = { Player.Direction.UP, Player.Direction.LEFT,
            Player.Direction.DOWN, Player.Direction.RIGHT };

    private final Player body;
    private final GuardType type;
    private final Level.Cell scatterCorner;
    private GuardStrategy strategy = new ScatterStrategy();
    private final Level.Cell spawn, home;
    private final Random random;

    public enum State {
        WAITING, LEAVING, ACTIVE, RETURNING
    }

    private State state;
    private boolean frightened, forceTurn;

    public Guard(Level level, Level.Cell spawn, GuardType type) {
        body = new Player(new Loki("TVA Guard", 0), spawn);
        this.type = Objects.requireNonNull(type);
        this.spawn = spawn;
        home = type == GuardType.B15 ? new Level.Cell(level.door().x(), level.door().y() - 1) : spawn;
        random = new Random(type.ordinal());
        reset();
        scatterCorner = corner(level, type);
    }

    private static Level.Cell corner(Level level, GuardType type) {
        return switch (type) {
        case B15 -> new Level.Cell(level.width() + 1, level.height() + 1);
        case D90 -> new Level.Cell(-2, level.height() + 1);
        case C20 -> new Level.Cell(level.width() + 1, -2);
        case X5 -> new Level.Cell(-2, -2);
        };
    }

    public float x() {
        return body.x();
    }

    public float y() {
        return body.y();
    }

    public GuardType type() {
        return type;
    }

    public Level.Cell scatterCorner() {
        return scatterCorner;
    }

    public Level.Cell currentCell() {
        return body.currentCell();
    }

    public Player.Direction currentDirection() {
        return body.currentDirection();
    }

    public void setStrategy(GuardStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy);
    }

    public void allowReverse() {
        if (state == State.ACTIVE) {
            forceTurn = !body.isMoving();
            body.reverse();
        }
    }

    public void distract(Level level, Level.Cell target) {
        if (state != State.ACTIVE || frightened || currentDirection() == Player.Direction.NONE) {
            return;
        }
        Level.Cell ahead = Player.neighbor(level, currentCell(), currentDirection());
        Player.Direction back = Player.opposite(currentDirection());
        Level.Cell behind = Player.neighbor(level, currentCell(), back);
        if (body.canMove(level, currentCell(), back)
                && level.distance(behind.worldX(), behind.worldY(), target.worldX(), target.worldY()) < level
                        .distance(ahead.worldX(), ahead.worldY(), target.worldX(), target.worldY())) {
            allowReverse();
        }
    }

    public boolean isFrightened() {
        return frightened && state == State.ACTIVE;
    }

    public boolean isReturning() {
        return state == State.RETURNING;
    }

    public State state() {
        return state;
    }

    public boolean dangerous() {
        return state == State.ACTIVE;
    }

    public void release() {
        if (state == State.WAITING) {
            state = State.LEAVING;
            body.setHouseAccess(true);
        }
    }

    public void setFrightened(boolean value) {
        if (!isReturning()) {
            frightened = value;
        }
    }

    public void caught() {
        state = State.RETURNING;
        frightened = false;
        forceTurn = false;
        body.setHouseAccess(true);
        body.reverse();
    }

    public void reset() {
        body.setPosition(spawn);
        frightened = false;
        forceTurn = false;
        state = type == GuardType.B15 ? State.ACTIVE : State.WAITING;
        body.setHouseAccess(state != State.ACTIVE);
        random.setSeed(type.ordinal());
    }

    public void update(Level level, HuntContext context, float deltaTime) {
        if (!Float.isFinite(deltaTime) || deltaTime <= 0 || deltaTime > .05f) {
            throw new IllegalArgumentException("Invalid time step");
        }
        if (state == State.WAITING) {
            return;
        }
        float remainingTime = deltaTime;
        while (remainingTime > .000001f) {
            if (!body.isMoving() && !chooseDirection(level, context)) {
                return;
            }
            body.setSpeed(movementSpeed(level));
            float stepTime = Math.min(remainingTime, body.timeToCenter());
            body.update(level, Player.Direction.NONE, stepTime);
            remainingTime -= stepTime;
        }
    }

    private boolean chooseDirection(Level level, HuntContext context) {
        if (state == State.RETURNING && currentCell().equals(home)) {
            state = State.LEAVING;
        }
        if (state == State.LEAVING && currentCell().equals(level.houseExit())) {
            state = State.ACTIVE;
            body.setHouseAccess(false);
            body.queueDirection(Player.Direction.LEFT);
            forceTurn = false;
        } else if (state == State.RETURNING || state == State.LEAVING) {
            Level.Cell goal = state == State.RETURNING ? home : level.houseExit();
            List<Level.Cell> path = level.findPath(currentCell(), goal, true);
            if (path.isEmpty()) {
                return false;
            }
            for (Player.Direction direction : TURN_ORDER) {
                if (Player.neighbor(level, currentCell(), direction).equals(path.get(0))) {
                    body.queueDirection(direction);
                }
            }
        } else if (forceTurn && body.canMove(level, currentCell(), currentDirection())) {
            body.queueDirection(currentDirection());
            forceTurn = false;
        } else if (frightened) {
            ArrayList<Player.Direction> choices = new ArrayList<>();
            for (Player.Direction direction : TURN_ORDER) {
                if (direction != Player.opposite(currentDirection()) && body.canMove(level, currentCell(), direction)) {
                    choices.add(direction);
                }
            }
            body.queueDirection(choices.isEmpty() ? Player.opposite(currentDirection())
                    : choices.get(random.nextInt(choices.size())));
        } else if (context.decoy()) {
            steerTo(level, context.prey());
        } else {
            strategy.update(this, level, context);
        }
        forceTurn = false;
        return true;
    }

    private float movementSpeed(Level level) {
        if (state == State.RETURNING) {
            return 260;
        }
        if (state == State.LEAVING) {
            return 120;
        }
        if (frightened || level.tunnel(currentCell())) {
            return 85;
        }
        return 160;
    }

    public void steerTo(Level level, Level.Cell target) {
        if (body.isMoving()) {
            return;
        }

        Level.Cell from = body.currentCell();
        Player.Direction back = Player.opposite(body.currentDirection());
        boolean mayTurnBack = body.currentDirection() == Player.Direction.NONE;
        Player.Direction best = Player.Direction.NONE;
        long bestDistance = Long.MAX_VALUE;
        for (Player.Direction direction : TURN_ORDER) {
            if (!body.canMove(level, from, direction)) {
                continue;
            }
            if (direction == back && !mayTurnBack) {
                continue;
            }
            Level.Cell next = Player.neighbor(level, from, direction);
            long dx = next.x() - target.x();
            long dy = next.y() - target.y();
            long distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = direction;
            }
        }
        if (best == Player.Direction.NONE) { // dead end: the way back is the only way out
            for (Player.Direction direction : TURN_ORDER) {
                if (body.canMove(level, from, direction)) {
                    best = direction;
                    break;
                }
            }
        }
        body.queueDirection(best);
    }

}
