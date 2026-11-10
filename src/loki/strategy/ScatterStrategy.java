package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;

public final class ScatterStrategy implements GuardStrategy {

    @Override
    public void update(Guard guard, Level level, HuntContext context) {
        guard.steerTo(level, guard.scatterCorner());
    }
}
