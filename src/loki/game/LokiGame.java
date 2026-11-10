package loki.game;
import com.badlogic.gdx.Game;

public final class LokiGame extends Game {
    @Override public void create() { showMenu(); }
    public void showMenu() {
        var old=getScreen();setScreen(new MenuScreen(this));if(old!=null) old.dispose();
    }
    public void play() {
        var old=getScreen();setScreen(new GameScreen(this));if(old!=null) old.dispose();
    }
    @Override public void dispose() { if(getScreen()!=null) getScreen().dispose(); super.dispose(); }
}
