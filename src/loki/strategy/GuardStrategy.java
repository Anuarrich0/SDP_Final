package loki.strategy;

import loki.builder.Level;
import loki.game.Guard;
public interface GuardStrategy {
    void update(Guard guard, Level level, float targetX, float targetY, float dt);
}
