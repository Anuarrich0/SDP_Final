package loki.game;
import com.badlogic.gdx.Game;

public final class LokiGame extends Game {
    @Override public void create() { setScreen(new GameScreen()); }
    @Override public void dispose() { if(getScreen()!=null) getScreen().dispose(); super.dispose(); }
}
