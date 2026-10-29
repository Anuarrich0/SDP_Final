public interface LokiVariant {
    String getName();
    int getMagicPower();
    int getCombatPower();
    int getDiplomacy();

    default int getFailurePenalty() { return 15; }
    default String getDescription() { return getName(); }
}
