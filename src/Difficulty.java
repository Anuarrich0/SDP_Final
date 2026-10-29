public enum Difficulty {
    EASY("Лёгкая", 60, 55), STANDARD("Обычная", 70, 65), HARD("Сложная", 80, 75);

    private final String name;
    private final int startingInstability;
    private final int threshold;

    Difficulty(String name, int startingInstability, int threshold) {
        this.name = name;
        this.startingInstability = startingInstability;
        this.threshold = threshold;
    }

    public int getStartingInstability() { return startingInstability; }
    public int getThreshold() { return threshold; }
    public String toString() { return name; }
}
