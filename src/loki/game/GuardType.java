package loki.game;

/** Which TVA hunter this guard is. The type picks the sprite and, later, the chase rule. */
public enum GuardType {
    B15("b15.png"), D90("d90.png"), C20("c20.png"), X5("x5.png");

    private final String textureFile;

    GuardType(String textureFile) {
        this.textureFile = textureFile;
    }

    public String textureFile() {
        return textureFile;
    }
}
