package loki;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import loki.game.LokiGame;

public final class GameLauncher {
    private GameLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Loki TVA Escape");
        config.setWindowedMode(1000, 820);
        config.setWindowSizeLimits(700, 580, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new LokiGame(), config);
    }
}
