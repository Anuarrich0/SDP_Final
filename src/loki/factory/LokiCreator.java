package loki.factory;

// Factory Method: each creator overrides the creation of its variant.
public abstract class LokiCreator {
    private static final int BASE_MAGIC_POWER = 75;
    private static final int BASE_AGILITY = 30;
    protected abstract LokiVariant createLoki();

    public final LokiVariant createVariant() {
        return createLoki();
    }

    public static class OrdinaryCreator extends LokiCreator {
        @Override
        protected LokiVariant createLoki() {
            return new OrdinaryLoki();
        }

    }

    public static class ClassicCreator extends LokiCreator {
        @Override
        protected LokiVariant createLoki() {
            return new ClassicLoki();
        }

    }

    public static class AlligatorCreator extends LokiCreator {
        @Override
        protected LokiVariant createLoki() {
            return new AlligatorLoki();
        }

    }

    public static class SylvieCreator extends LokiCreator {
        @Override
        protected LokiVariant createLoki() {
            return new Sylvie();
        }

    }

    private static class OrdinaryLoki extends Loki {
        OrdinaryLoki() {
            super("Ordinary Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    private static class ClassicLoki extends Loki {
        ClassicLoki() {
            super("Classic Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    private static class AlligatorLoki extends Loki {
        AlligatorLoki() {
            super("Alligator Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    private static class Sylvie extends Loki {
        Sylvie() {
            super("Sylvie", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }
}
