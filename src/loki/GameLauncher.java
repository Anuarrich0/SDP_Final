package loki;
import loki.game.LokiGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class GameLauncher {
    private GameLauncher() { }
    public static void main(String[] args) {
        var config=new Lwjgl3ApplicationConfiguration();
        config.setTitle("Loki TVA Escape - learning prototype");
        config.setWindowedMode(1100,752);
        config.setWindowSizeLimits(720,492,-1,-1);
        config.useVsync(true); config.setForegroundFPS(60);
        config.disableAudio(true);
        new Lwjgl3Application(new LokiGame(),config);
    }
}
