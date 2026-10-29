// Factory Method: каждый Creator переопределяет создание своего варианта.
public abstract class LokiCreator {
    protected abstract LokiVariant createLoki();

    public final LokiVariant createVariant() { return createLoki(); }

    public static class OrdinaryCreator extends LokiCreator {
        protected LokiVariant createLoki() { return new OrdinaryLoki(); }
        public String toString() { return "Обычный Локи"; }
    }

    public static class ClassicCreator extends LokiCreator {
        protected LokiVariant createLoki() { return new ClassicLoki(); }
        public String toString() { return "Классический Локи"; }
    }

    public static class AlligatorCreator extends LokiCreator {
        protected LokiVariant createLoki() { return new AlligatorLoki(); }
        public String toString() { return "Локи-аллигатор"; }
    }

    public static class SylvieCreator extends LokiCreator {
        protected LokiVariant createLoki() { return new Sylvie(); }
        public String toString() { return "Сильви"; }
    }

    private static class OrdinaryLoki extends Loki {
        OrdinaryLoki() { super("Обычный Локи", 45, 50, 65); }
    }

    private static class ClassicLoki extends Loki {
        ClassicLoki() { super("Классический Локи", 75, 30, 45); }
    }

    private static class AlligatorLoki extends Loki {
        AlligatorLoki() { super("Локи-аллигатор", 20, 80, 10); }
    }

    private static class Sylvie extends Loki {
        Sylvie() { super("Сильви", 65, 45, 55); }
    }
}
