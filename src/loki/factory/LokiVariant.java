package loki.factory;

public interface LokiVariant {
    String getName();
    int getMagicPower();
    int getAgility();

    default int getHitDamage() {
        return 30;
    }
}
