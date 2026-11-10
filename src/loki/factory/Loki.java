package loki.factory;

public class Loki implements LokiVariant {
    private final String name;
    private final int magicPower;
    private final int agility;

    public Loki(String name, int magicPower) {
        this(name, magicPower, 50);
    }

    public Loki(String name, int magicPower, int agility) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Character must have a name");
        }
        if (magicPower < 0 || agility < 0) {
            throw new IllegalArgumentException("Stats cannot be negative");
        }
        this.name = name;
        this.magicPower = magicPower;
        this.agility = agility;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMagicPower() {
        return magicPower;
    }

    @Override
    public int getAgility() {
        return agility;
    }
}
