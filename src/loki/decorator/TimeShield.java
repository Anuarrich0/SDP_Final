package loki.decorator;

import loki.factory.LokiVariant;

public class TimeShield extends EquipmentDecorator {
    public TimeShield(LokiVariant wrapped) {
        super(wrapped);
    }

    @Override
    public int getHitDamage() {
        return 20;
    }
}
