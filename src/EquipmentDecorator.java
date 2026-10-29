import java.util.Objects;

public abstract class EquipmentDecorator implements LokiVariant {
    protected final LokiVariant wrapped;

    protected EquipmentDecorator(LokiVariant wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    public String getName() { return wrapped.getName(); }
    public String getDescription() { return wrapped.getDescription(); }
    public int getMagicPower() { return wrapped.getMagicPower(); }
    public int getCombatPower() { return wrapped.getCombatPower(); }
    public int getDiplomacy() { return wrapped.getDiplomacy(); }
    public int getFailurePenalty() { return wrapped.getFailurePenalty(); }
}
