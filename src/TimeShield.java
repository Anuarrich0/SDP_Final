public class TimeShield extends EquipmentDecorator {
    public TimeShield(LokiVariant wrapped) { super(wrapped); }

    @Override
    public int getFailurePenalty() { return 10; }

    @Override
    public String getDescription() { return wrapped.getDescription() + " + временной щит"; }
}
