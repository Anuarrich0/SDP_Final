package loki.observer;

import loki.facade.GameSession;

@FunctionalInterface
public interface GameListener {
    void onChanged(GameSession.Snapshot snapshot);
}
