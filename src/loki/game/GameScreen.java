package loki.game;

import loki.builder.Level;
import loki.decorator.MagicAmplifier;
import loki.decorator.TimeShield;
import loki.facade.GameSession;
import loki.factory.LokiCreator;
import loki.factory.LokiVariant;
import loki.observer.GameListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.List;

/** Input and drawing; game rules stay in GameSession. */
public final class GameScreen extends ScreenAdapter {
    private final OrthographicCamera camera=new OrthographicCamera();
    private final FitViewport viewport=new FitViewport(960,576,camera);
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final SpriteBatch batch=new SpriteBatch();
    private final BitmapFont font=new BitmapFont();
    private final Texture lokiTexture = new Texture(Gdx.files.internal("loki.png"));
    private final TextureRegion[][] lokiSprites = createSprites(lokiTexture);
    private CharacterAnimation animation;
    private List<CharacterAnimation> guardAnimations;
    private TextureRegion illusionSprite;
    private final Texture guardTexture = new Texture(Gdx.files.internal("guard.png"));
    private final TextureRegion[][] guardSprites = createSprites(guardTexture);
    private final Texture interiorTexture = new Texture(Gdx.files.internal("interior.png"));
    private final TextureRegion[] tiles = TextureRegion.split(interiorTexture, 48, 48)[0];
    private final Texture objectsTexture = new Texture(Gdx.files.internal("objects.png"));
    private final TextureRegion[] objects = TextureRegion.split(objectsTexture, 48, 48)[0];
    private final GameListener hudListener=s -> hud=s;
    private GameSession session;
    private GameSession.Snapshot hud;
    private boolean paused;
    public GameScreen() {
        guardTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        lokiTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        interiorTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        objectsTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        restart();
    }
    private void restart() {
        if(session!=null) session.removeListener(hudListener);
        LokiVariant hero=new TimeShield(new MagicAmplifier(new LokiCreator.ClassicCreator().createVariant()));
        illusionSprite = lokiSprites[0][0];
        session=new GameSession(Level.training(),hero);
        animation = new CharacterAnimation(session.player().x(), session.player().y());
        guardAnimations = session.guards().stream()
                .map(guard -> new CharacterAnimation(guard.x(), guard.y())).toList();
        session.addListener(hudListener); paused=false;
    }
    @Override public void render(float delta) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)) restart();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) paused=!paused;
        if(!paused && delta>0 && session.snapshot().state()==GameSession.State.PLAYING) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                session.dash();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                if (session.illusion()) illusionSprite = lokiSprites[animation.direction().ordinal()][0];
            }
            float dx=key(Input.Keys.D,Input.Keys.RIGHT)-key(Input.Keys.A,Input.Keys.LEFT);
            float dy=key(Input.Keys.W,Input.Keys.UP)-key(Input.Keys.S,Input.Keys.DOWN);
            session.update(Math.min(delta,.05f),dx,dy);
            animation.updatePosition(session.player().x(), session.player().y());
            for (int i = 0; i < session.guards().size(); i++) {
                Guard guard = session.guards().get(i);
                guardAnimations.get(i).updatePosition(guard.x(), guard.y());
            }
        }
        ScreenUtils.clear(.035f,.055f,.065f,1);
        viewport.apply(); camera.update(); shapes.setProjectionMatrix(camera.combined);
        Level level=session.level();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                int tile = level.wall(x, y) ? 2 + ((x + y) % 7 == 0 ? 1 : 0)
                        : ((x * 7 + y * 3) % 11 == 0 ? 1 : 0);
                batch.draw(tiles[tile], x * Level.TILE, y * Level.TILE, Level.TILE, Level.TILE);
            }
        }
        TextureRegion portal = objects[session.charges().isEmpty() ? 0 : 1];
        batch.draw(portal, level.exit().worldX() - 24, level.exit().worldY() - 24, 48, 48);
        for (Level.Cell charge : session.charges()) {
            batch.draw(objects[2], charge.worldX() - 12, charge.worldY() - 12, 24, 24);
        }
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.SCARLET);
        for (Guard guard : session.guards()) {
            shapes.setColor(guard.isSearching() ? Color.SCARLET : Color.ORANGE);
            shapes.ellipse(guard.x() - 12, guard.y() - 11, 24, 7);
        }
        Illusion illusion = session.getIllusion();
        if (illusion != null) {
            shapes.setColor(Color.TEAL);
            shapes.ellipse(illusion.x() - 13, illusion.y() - 11, 26, 9);
        }
        shapes.setColor(.06f, .10f, .08f, 1);
        shapes.ellipse(session.player().x() - 12, session.player().y() - 11, 24, 9);
        shapes.end();
        batch.setProjectionMatrix(camera.combined); batch.begin();
        batch.setColor(Color.WHITE);
        if (illusion != null) {
            batch.setColor(.35f, 1, 1, .65f);
            drawCharacter(illusionSprite, illusion.x(), illusion.y());
            batch.setColor(Color.WHITE);
        }
        // Actors higher up the map are drawn behind the player.
        for (int i = 0; i < session.guards().size(); i++) {
            Guard guard = session.guards().get(i);
            if (guard.y() >= session.player().y()) drawGuard(guard, guardAnimations.get(i));
        }
        drawCharacter(lokiSprites[animation.direction().ordinal()][animation.frame()], session.player().x(), session.player().y());
        for (int i = 0; i < session.guards().size(); i++) {
            Guard guard = session.guards().get(i);
            if (guard.y() < session.player().y()) drawGuard(guard, guardAnimations.get(i));
        }
        font.setColor(Color.WHITE);
        if(paused || hud.state()!=GameSession.State.PLAYING) {
            String message=paused?"PAUSED - ESC to resume":hud.state()==GameSession.State.WON?"ESCAPED!  R to restart":"CAUGHT!  R to retry";
            font.draw(batch,message,0,298,viewport.getWorldWidth(),Align.center,false);
        }
        batch.end();
    }
    private static TextureRegion[][] createSprites(Texture texture) {
        int frames = texture.getWidth() / 64;
        TextureRegion[][] sprites = new TextureRegion[4][frames];
        for (int direction = 0; direction < 4; direction++) {
            for (int frame = 0; frame < frames; frame++) {
                TextureRegion sprite = new TextureRegion(texture, frame * 64, Math.min(direction, 2) * 96, 64, 96);
                if (direction == 3) sprite.flip(true, false);
                sprites[direction][frame] = sprite;
            }
        }
        return sprites;
    }

    private void drawGuard(Guard guard, CharacterAnimation guardAnimation) {
        drawCharacter(guardSprites[guardAnimation.direction().ordinal()][guardAnimation.frame()], guard.x(), guard.y());
    }

    private void drawCharacter(TextureRegion sprite, float x, float y) {
        float height = 48;
        float width = height * sprite.getRegionWidth() / sprite.getRegionHeight();
        batch.draw(sprite, x - width / 2, y - Player.RADIUS, width, height);
    }

    private int key(int a,int b) { return Gdx.input.isKeyPressed(a)||Gdx.input.isKeyPressed(b)?1:0; }
    @Override public void resize(int width,int height) { viewport.update(width,height,true); }
    @Override public void pause() { paused=true; }
    @Override public void dispose() {
        session.removeListener(hudListener); shapes.dispose(); batch.dispose(); font.dispose();
        guardTexture.dispose();
        interiorTexture.dispose();
        objectsTexture.dispose();
        lokiTexture.dispose();
    }
}
