public class DiplomacyAmulet extends EquipmentDecorator {
    public DiplomacyAmulet(LokiVariant wrapped) { super(wrapped); }

    @Override
    public int getDiplomacy() { return wrapped.getDiplomacy() + 15; }

    @Override
    public String getDescription() { return wrapped.getDescription() + " + амулет переговоров"; }
}
