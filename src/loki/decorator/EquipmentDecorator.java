package loki.decorator;

import java.util.Objects;
import loki.factory.LokiVariant;

public abstract class EquipmentDecorator implements LokiVariant {
    protected final LokiVariant wrapped;

    protected EquipmentDecorator(LokiVariant wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public int getMagicPower() {
        return wrapped.getMagicPower();
    }

    @Override
    public int getAgility() {
        return wrapped.getAgility();
    }

    @Override
    public int getHitDamage() {
        return wrapped.getHitDamage();
    }
}
