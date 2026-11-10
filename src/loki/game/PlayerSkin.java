package loki.game;

import loki.factory.LokiCreator;
import loki.factory.LokiVariant;

public enum PlayerSkin {
    CLASSIC("Classic", "loki-classic.png", new LokiCreator.ClassicCreator()),
    ORDINARY("Ordinary Loki", "loki-ordinary.png", new LokiCreator.OrdinaryCreator()),
    SYLVIE("Sylvie", "sylvie.png", new LokiCreator.SylvieCreator()),
    ALLIGATOR("Alligator", "loki-alligator.png", new LokiCreator.AlligatorCreator());

    private final String displayName;
    private final String textureFile;
    private final LokiCreator creator;

    PlayerSkin(String displayName, String textureFile, LokiCreator creator) {
        this.displayName = displayName;
        this.textureFile = textureFile;
        this.creator = creator;
    }

    public LokiVariant createVariant() {
        return creator.createVariant();
    }

    public String displayName() {
        return displayName;
    }

    public String textureFile() {
        return textureFile;
    }
}
