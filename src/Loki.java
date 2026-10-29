public class Loki implements LokiVariant {
    private final String name;
    private final int magicPower;
    private final int combatPower;
    private final int diplomacy;

    public Loki(String name, int magicPower) {
        this(name, magicPower, 50, 50);
    }

    public Loki(String name, int magicPower, int combatPower, int diplomacy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("У Локи должно быть имя.");
        }
        if (magicPower < 0 || combatPower < 0 || diplomacy < 0) {
            throw new IllegalArgumentException("Характеристики не могут быть отрицательными.");
        }
        this.name = name;
        this.magicPower = magicPower;
        this.combatPower = combatPower;
        this.diplomacy = diplomacy;
    }

    public String getName() {
        return name;
    }

    public int getMagicPower() {
        return magicPower;
    }

    public int getCombatPower() { return combatPower; }
    public int getDiplomacy() { return diplomacy; }
}
