package loki.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public final class LokiGame extends Game {
    @Override
    public void create() {
        showMenu();
    }

    public void showMenu() {
        Screen previousScreen = getScreen();
        setScreen(new MenuScreen(this));
        if (previousScreen != null) {
            previousScreen.dispose();
        }
    }

    public void play() {
        Screen previousScreen = getScreen();
        setScreen(new GameScreen(this));
        if (previousScreen != null) {
            previousScreen.dispose();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        super.dispose();
    }
}
