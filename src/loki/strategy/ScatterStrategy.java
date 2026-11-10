package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;

/** SCATTER: every hunter runs off to its own corner until the mode flips back to CHASE. */
public final class ScatterStrategy implements GuardStrategy {

    @Override
    public void update(Guard guard, Level level, HuntContext context) {
        guard.steerTo(level, guard.scatterCorner());
    }
}
