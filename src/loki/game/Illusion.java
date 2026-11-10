package loki.game;

import loki.builder.Level;
import java.util.ArrayList;
import java.util.Random;

public final class Illusion {
    private final Player body;
    private final Random random = new Random(1130);
    private final Player.Direction startDirection;
    private float timeLeft;

    public Illusion(Player player, Player.Direction lastDirection, float duration) {
        if (!Float.isFinite(duration) || duration <= 0) {
            throw new IllegalArgumentException("Invalid illusion duration");
        }
        body = new Player(player);
        startDirection = lastDirection == Player.Direction.NONE ? Player.Direction.RIGHT : lastDirection;
        timeLeft = duration;
    }

    public float x() {
        return body.x();
    }

    public float y() {
        return body.y();
    }

    public Player.Direction direction() {
        return body.currentDirection();
    }

    public float getTimeLeft() {
        return timeLeft;
    }

    public void update(Level level, float deltaTime) {
        if (!Float.isFinite(deltaTime) || deltaTime < 0 || deltaTime > .05f) {
            throw new IllegalArgumentException("Invalid time step");
        }
        float remainingTime = Math.min(deltaTime, timeLeft);
        timeLeft = Math.max(0, timeLeft - deltaTime);
        while (remainingTime > .000001f) {
            if (!body.isMoving()) {
                ArrayList<Player.Direction> choices = new ArrayList<>();
                for (Player.Direction direction : Player.Direction.values()) {
                    if (direction == Player.Direction.NONE || !body.canMove(level, body.currentCell(), direction)) {
                        continue;
                    }
                    Level.Cell next = Player.neighbor(level, body.currentCell(), direction);
                    if (!level.suppressed(next)) {
                        choices.add(direction);
                    }
                }
                if (choices.size() > 1) {
                    choices.remove(Player.opposite(body.currentDirection()));
                }
                if (choices.isEmpty()) {
                    timeLeft = 0;
                    return;
                }
                Player.Direction turn = body.currentDirection() == Player.Direction.NONE
                        && choices.contains(startDirection) ? startDirection
                                : choices.get(random.nextInt(choices.size()));
                body.queueDirection(turn);
            }
            float stepTime = Math.min(remainingTime, body.timeToCenter());
            body.update(level, Player.Direction.NONE, stepTime);
            remainingTime -= stepTime;
            if (level.suppressed(body.currentCell())) {
                timeLeft = 0;
                return;
            }
        }
    }
}
