package loki.factory;

public abstract class LokiCreator {
    private static final int BASE_MAGIC_POWER = 75;
    private static final int BASE_AGILITY = 30;
    public abstract LokiVariant createVariant();

    public static class OrdinaryCreator extends LokiCreator {
        @Override
        public LokiVariant createVariant() {
            return new Loki("Ordinary Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    public static class ClassicCreator extends LokiCreator {
        @Override
        public LokiVariant createVariant() {
            return new Loki("Classic Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    public static class AlligatorCreator extends LokiCreator {
        @Override
        public LokiVariant createVariant() {
            return new Loki("Alligator Loki", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }

    public static class SylvieCreator extends LokiCreator {
        @Override
        public LokiVariant createVariant() {
            return new Loki("Sylvie", BASE_MAGIC_POWER, BASE_AGILITY);
        }
    }
}
