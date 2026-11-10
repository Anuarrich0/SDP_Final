package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;

public interface GuardStrategy {
    void update(Guard guard, Level level, HuntContext context);
}
