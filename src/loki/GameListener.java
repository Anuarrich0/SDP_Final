package loki;
@FunctionalInterface
public interface GameListener {
    void onChanged(GameSession.Snapshot snapshot);
}
