package loki.game;

public enum GuardType {
    B15("characters/b15.png"), D90("characters/d90.png"), C20("characters/c20.png"), X5("characters/x5.png");

    private final String textureFile;

    GuardType(String textureFile) {
        this.textureFile = textureFile;
    }

    public String textureFile() {
        return textureFile;
    }
}
