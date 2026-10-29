public class MagicAmplifier extends EquipmentDecorator {
    public MagicAmplifier(LokiVariant wrapped) { super(wrapped); }

    @Override
    public int getMagicPower() { return wrapped.getMagicPower() + 15; }

    @Override
    public String getDescription() { return wrapped.getDescription() + " + усилитель магии"; }
}
