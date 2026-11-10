package loki.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import loki.builder.Level;
import loki.decorator.MagicAmplifier;
import loki.decorator.TimeShield;
import loki.facade.GameSession;
import loki.factory.LokiVariant;
import loki.observer.GameListener;

public final class GameScreen extends ScreenAdapter {
    private final OrthographicCamera camera = new OrthographicCamera();
    // The world is one green strip taller than the level on both sides, so the HUD never covers the maze.
    private final FitViewport viewport = new FitViewport(25 * Level.TILE, 19 * Level.TILE + 2 * STRIP, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont(Gdx.files.internal("ui/interface.fnt"));
    private final Texture lokiTexture;
    private final TextureRegion[][] lokiSprites;
    private CharacterAnimation playerAnimation;
    private List<CharacterAnimation> guardAnimations;
    private CharacterAnimation illusionAnimation;
    private final EnumMap<GuardType, Texture> guardTextures = new EnumMap<>(GuardType.class);
    private final EnumMap<GuardType, TextureRegion[][]> guardSprites = new EnumMap<>(GuardType.class);
    private final Texture interiorTexture = new Texture(Gdx.files.internal("interior.png"));
    private final TextureRegion[] tiles = TextureRegion.split(interiorTexture, 48, 48)[0];
    private final Texture temPadTexture = new Texture(Gdx.files.internal("tempad.png"));
    private final TextureRegion temPadSprite = new TextureRegion(temPadTexture);
    private final BitmapFont hudFont = new BitmapFont(Gdx.files.internal("ui/interface.fnt"));
    private final Texture heartFullTexture = new Texture(Gdx.files.internal("ui/icons/heart-full.png"));
    private final Texture heartHalfTexture = new Texture(Gdx.files.internal("ui/icons/heart-half.png"));
    private final Texture heartEmptyTexture = new Texture(Gdx.files.internal("ui/icons/heart-empty.png"));
    private final Texture dashIconTexture = new Texture(Gdx.files.internal("ui/icons/ability-1.png"));
    private final Texture illusionIconTexture = new Texture(Gdx.files.internal("ui/icons/ability-2.png"));
    private final Texture timeStopIconTexture = new Texture(Gdx.files.internal("ui/icons/ability-3.png"));
    private final Texture timerTexture = new Texture(Gdx.files.internal("ui/miss-minutes-timer-empty.png"));
    private final TextureRegion heartFull = new TextureRegion(heartFullTexture);
    private final TextureRegion heartHalf = new TextureRegion(heartHalfTexture);
    private final TextureRegion heartEmpty = new TextureRegion(heartEmptyTexture);
    private final TextureRegion timerSprite = new TextureRegion(timerTexture);
    private final GlyphLayout centeredLayout = new GlyphLayout();
    private static final Color DIMMED = new Color(1, 1, 1, .35f);
    private static final Color STRIP_GREEN = new Color(.06f, .26f, .16f, 1);
    private static final int HEARTS = 5;
    /** Height of the green status strips, and the square size of every HUD icon. */
    private static final float STRIP = 76, ICON = 44, PAD = 12;
    /** Ability labels sit in the strip below their icon: icon gap, text box height, bottom padding. */
    private static final float ICON_GAP = 4, LABEL_HEIGHT = 16, TEXT_PAD = 6;
    /** Centre of the clock reading inside the timer sprite, as a fraction of the sprite size. */
    private static final float TIMER_TEXT_X = .62f, TIMER_TEXT_Y = .39f;
    private static final String[] ABILITY_KEYS = { "SPACE", "E", "Q" };
    private float playTime;
    private final Music music = Gdx.audio.newMusic(Gdx.files.internal("sounds/timeline-music.wav"));
    private final Sound pickupSound = Gdx.audio.newSound(Gdx.files.internal("sounds/pickup.wav"));
    private final Sound dashSound = Gdx.audio.newSound(Gdx.files.internal("sounds/dash.wav"));
    private final Sound illusionSound = Gdx.audio.newSound(Gdx.files.internal("sounds/illusion.wav"));
    private final Sound stopSound = Gdx.audio.newSound(Gdx.files.internal("sounds/stop.wav"));
    private final Sound hurtSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hurt.wav"));
    private final Sound winSound = Gdx.audio.newSound(Gdx.files.internal("sounds/win.wav"));
    private final Sound loseSound = Gdx.audio.newSound(Gdx.files.internal("sounds/lose.wav"));
    private final GameListener stateListener = this::onStateChanged;
    private GameSession session;
    private GameSession.Snapshot snapshot;
    private final LokiGame game;
    private final PlayerSkin skin;
    private final GameUi ui = new GameUi();
    private final Stage stage = new Stage(new FitViewport(1200, 984));
    private final Label overlayTitle = ui.label("", 1, Color.WHITE);
    private final Label overlayText = ui.label("", .6f, GameUi.MUTED);
    private final Table overlay = new Table();
    private TextButton overlayButton;
    private boolean paused;
    private float elapsed;

    public GameScreen(LokiGame game) {
        this.game = game;
        music.setLooping(true);
        music.setVolume(.22f);
        font.getData().setScale(.25f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        skin = game.selectedSkin();
        lokiTexture = new Texture(Gdx.files.internal(skin.textureFile()));
        lokiSprites = createSprites(lokiTexture);
        for (GuardType type : GuardType.values()) {
            Texture texture = new Texture(Gdx.files.internal(type.textureFile()));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            guardTextures.put(type, texture);
            guardSprites.put(type, createSprites(texture));
        }
        lokiTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        interiorTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        temPadTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        hudFont.getData().setScale(.4f);
        hudFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (Texture texture : hudTextures()) {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        buildInterface();
        restart();
    }

    private void restart() {
        music.stop();
        if (session != null) {
            session.removeListener(stateListener);
        }
        LokiVariant hero = new TimeShield(new MagicAmplifier(skin.createVariant()));
        illusionAnimation = null;
        session = new GameSession(Level.tva(), hero);
        playerAnimation = new CharacterAnimation(session.player().x(), session.player().y());
        guardAnimations = new ArrayList<>();
        for (Guard guard : session.guards()) {
            guardAnimations.add(new CharacterAnimation(guard.x(), guard.y()));
        }
        snapshot = null;
        session.addListener(stateListener);
        paused = false;
        elapsed = 0;
        playTime = 0;
        overlay.setVisible(false);
    }

    @Override
    public void render(float deltaTime) {
        updateGame(deltaTime);
        if (canPlay()) {
            if (!music.isPlaying()) {
                music.play();
            }
        } else {
            music.pause();
        }
        ScreenUtils.clear(.035f, .055f, .065f, 1);
        Level level = session.level();
        viewport.apply();
        // Centre on the maze so the strips above and below it stay outside the playfield.
        camera.position.set(level.width() * Level.TILE / 2f, level.height() * Level.TILE / 2f, 0);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        drawLevel(level);
        drawCharacters(level);
        drawHud(level);
        updateOverlay();
        stage.getViewport().apply();
        stage.act(Math.min(deltaTime, .1f));
        stage.draw();
    }

    private void updateGame(float deltaTime) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
        }
        if (snapshot.state() == GameSession.State.PLAYING && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }
        if (canPlay() && deltaTime > 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (session.dash()) {
                    dashSound.play(.4f);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                useIllusion();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                if (session.stopTime()) {
                    stopSound.play(.4f);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                if (session.activateTerminal()) {
                    pickupSound.play(.4f);
                }
            }
            Player.Direction inputDirection = getInputDirection();
            if (inputDirection != Player.Direction.NONE) {
                session.player().queueDirection(inputDirection);
            }
            int itemsBefore = session.dots().size() + session.powers().size() + session.charges().size();
            elapsed += Math.min(deltaTime, .25f);
            while (elapsed >= 1f / 120) {
                session.update(1f / 120, inputDirection);
                elapsed -= 1f / 120;
                playTime += 1f / 120;
            }
            int itemsAfter = session.dots().size() + session.powers().size() + session.charges().size();
            if (itemsAfter < itemsBefore) {
                pickupSound.play(.2f);
            }
            playerAnimation.updatePosition(session.player().x(), session.player().y());
            playerAnimation.updateDirection(session.player().currentDirection());
            Illusion clone = session.getIllusion();
            if (clone != null) {
                if (illusionAnimation == null) {
                    illusionAnimation = new CharacterAnimation(clone.x(), clone.y());
                }
                illusionAnimation.updatePosition(clone.x(), clone.y());
                illusionAnimation.updateDirection(clone.direction());
            }
            for (int i = 0; i < session.guards().size(); i++) {
                Guard guard = session.guards().get(i);
                guardAnimations.get(i).updatePosition(guard.x(), guard.y());
                guardAnimations.get(i).updateDirection(guard.currentDirection());
            }
        }
    }

    private void drawLevel(Level level) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                int tile = level.wall(x, y) ? 2 + ((x + y) % 7 == 0 ? 1 : 0) : ((x * 7 + y * 3) % 11 == 0 ? 1 : 0);
                if (level.house(new Level.Cell(x, y))) {
                    batch.setColor(.48f, .58f, .64f, 1);

                }
                batch.draw(tiles[tile], x * Level.TILE, y * Level.TILE, Level.TILE, Level.TILE);
                batch.setColor(Color.WHITE);
            }
        }
        for (Level.Cell charge : session.charges()) {
            batch.draw(temPadSprite, charge.worldX() - 12, charge.worldY() - 12, 24, 24);
        }
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float terminalX = level.terminal().worldX(), terminalY = level.terminal().worldY();
        shapes.setColor(.03f, .12f, .18f, 1);
        shapes.rect(terminalX - 17, terminalY - 17, 34, 34);
        shapes.setColor(snapshot.terminalOn() ? Color.LIME : Color.CYAN);
        shapes.rect(terminalX - 12, terminalY - 3, 24, 14);
        shapes.rect(terminalX - 3, terminalY - 14, 6, 8);
        shapes.rect(terminalX - 13, terminalY - 18, 26, 4);
        float exitX = level.exit().worldX(), exitY = level.exit().worldY();
        shapes.setColor(session.isExitOpen() ? Color.LIME : Color.ORANGE);
        shapes.ellipse(exitX - 21, exitY - 24, 42, 48);
        shapes.setColor(.06f, .12f, .10f, 1);
        shapes.ellipse(exitX - 15, exitY - 19, 30, 38);
        shapes.setColor(1, .65f, .18f, 1);
        shapes.rect(level.door().worldX() - 24, level.door().worldY() - 4, 48, 8);
        for (int edge : new int[] { 0, level.width() - 1 }) {
            float x = edge * Level.TILE, y = level.tunnelRow() * Level.TILE;
            shapes.rect(x, y + 2, 48, 4);
            shapes.rect(x, y + 42, 48, 4);
        }
        shapes.setColor(.8f, .85f, .65f, 1);
        for (Level.Cell dot : session.dots()) {
            shapes.circle(dot.worldX(), dot.worldY(), 2.5f);
        }
        shapes.setColor(Color.CYAN);
        for (Level.Cell power : session.powers()) {
            shapes.circle(power.worldX(), power.worldY(), 7);
        }
        shapes.setColor(Color.SCARLET);
        for (Guard guard : session.guards()) {
            shapes.setColor(session.mode() == GameSession.Mode.CHASE ? Color.SCARLET : Color.ORANGE);
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
    }

    private void drawCharacters(Level level) {
        Illusion illusion = session.getIllusion();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        if (illusion != null) {
            batch.setColor(.35f, 1, 1, .65f);
            drawCharacter(lokiSprites[illusionAnimation.direction().ordinal()][illusionAnimation.frame()], illusion.x(),
                    illusion.y());
            batch.setColor(Color.WHITE);
        }
        font.setColor(Color.CYAN);
        font.draw(batch, "TERMINAL", level.terminal().worldX() - 31, level.terminal().worldY() + 35);
        font.setColor(Color.ORANGE);
        font.draw(batch, "EXIT", level.exit().worldX() - 13, level.exit().worldY() + 35);
        font.setColor(Color.GOLD);
        font.draw(batch, "TVA", level.door().worldX() - 14, level.door().worldY() + 19);
        font.setColor(Color.WHITE);
        // Actors higher up the map are drawn behind the player.
        for (int i = 0; i < session.guards().size(); i++) {
            Guard guard = session.guards().get(i);
            if (guard.y() >= session.player().y()) {
                drawGuard(guard, guardAnimations.get(i));
            }
        }
        drawCharacter(lokiSprites[playerAnimation.direction().ordinal()][playerAnimation.frame()], session.player().x(),
                session.player().y());
        for (int i = 0; i < session.guards().size(); i++) {
            Guard guard = session.guards().get(i);
            if (guard.y() < session.player().y()) {
                drawGuard(guard, guardAnimations.get(i));
            }
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    /** Green status strips: lives, orbs and the timer above the maze, ability slots below it. */
    private void drawHud(Level level) {
        float width = level.width() * Level.TILE, height = level.height() * Level.TILE;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(STRIP_GREEN);
        shapes.rect(0, -STRIP, width, STRIP);
        shapes.rect(0, height, width, STRIP);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        float topY = height + (STRIP - ICON) / 2;
        int health = snapshot.health();
        float perHeart = 100f / HEARTS;
        for (int i = 0; i < HEARTS; i++) {
            float rest = health - i * perHeart;
            TextureRegion heart = rest <= 0 ? heartEmpty : rest < perHeart ? heartHalf : heartFull;
            batch.draw(heart, PAD + i * (ICON + 4), topY, ICON, ICON);
        }
        int total = level.dots().size() + level.powers().size() + level.charges().size();
        int orbsLeft = session.dots().size() + session.powers().size() + session.charges().size();
        float orbsX = PAD + HEARTS * (ICON + 4) + 16;
        batch.draw(temPadSprite, orbsX, topY, ICON, ICON);
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, (total - orbsLeft) + "/" + total, orbsX + ICON + 10, topY + ICON / 2 + 6);

        float timerHeight = 56;
        float timerWidth = timerHeight * timerTexture.getWidth() / timerTexture.getHeight();
        float timerX = width - PAD - timerWidth, timerY = height + (STRIP - timerHeight) / 2;
        batch.draw(timerSprite, timerX, timerY, timerWidth, timerHeight);
        hudFont.setColor(Color.LIME);
        centeredLayout.setText(hudFont, clock());
        hudFont.draw(batch, clock(), timerX + timerWidth * TIMER_TEXT_X - centeredLayout.width / 2,
                timerY + timerHeight * TIMER_TEXT_Y + centeredLayout.height / 2);

        float abilitiesX = width - PAD - (3 * ICON + 2 * 16);
        float abilityY = -STRIP + TEXT_PAD + LABEL_HEIGHT + ICON_GAP;
        Texture[] icons = { dashIconTexture, illusionIconTexture, timeStopIconTexture };
        float[] cooldowns = { session.dashCooldown(), session.illusionCooldown(), session.timeStopCooldown() };
        for (int i = 0; i < icons.length; i++) {
            drawAbility(abilitiesX + i * (ICON + 16), abilityY, icons[i], cooldowns[i], ABILITY_KEYS[i]);
        }
        hudFont.setColor(Color.WHITE);
        batch.end();
    }

    /** One ability slot: dimmed with the seconds left, or bright with its key once ready. */
    private void drawAbility(float x, float y, Texture icon, float remaining, String label) {
        boolean ready = remaining <= 0;
        batch.setColor(ready ? Color.WHITE : DIMMED);
        batch.draw(icon, x, y, ICON, ICON);
        batch.setColor(Color.WHITE);
        hudFont.setColor(ready ? Color.WHITE : Color.SCARLET);
        drawCentered(ready ? label : Integer.toString((int) Math.ceil(remaining)), x + ICON / 2, y - ICON_GAP);
    }

    /** Draws text centred on x, with the top of the line on y. */
    private void drawCentered(String text, float x, float y) {
        centeredLayout.setText(hudFont, text);
        hudFont.draw(batch, text, x - centeredLayout.width / 2, y);
    }

    private String clock() {
        int seconds = (int) playTime, minutes = seconds / 60, rest = seconds % 60;
        return minutes + ":" + (rest < 10 ? "0" : "") + rest;
    }

    private Texture[] hudTextures() {
        return new Texture[] { heartFullTexture, heartHalfTexture, heartEmptyTexture,
                dashIconTexture, illusionIconTexture, timeStopIconTexture, timerTexture };
    }

    private void buildInterface() {
        overlay.setFillParent(true);
        overlay.setBackground(ui.panel("020B08E8"));
        Table card = new Table();
        card.setBackground(ui.panel("102C23"));
        card.pad(34);
        card.add(overlayTitle).padBottom(16).row();
        card.add(overlayText).padBottom(28).row();
        overlayButton = ui.button("Resume", true, () -> {
            if (snapshot.state() == GameSession.State.PLAYING) {
                paused = false;
            } else {
                restart();
            }
        });
        card.add(overlayButton).width(340).height(54).padBottom(12).row();
        card.add(ui.button("Main Menu", false, () -> {
            Gdx.app.postRunnable(game::showMenu);
        })).width(340).height(46);
        overlay.add(card);
        overlay.setVisible(false);
        stage.addActor(overlay);
    }

    private boolean canPlay() {
        return !paused && snapshot.state() == GameSession.State.PLAYING;
    }

    private void useIllusion() {
        if (canPlay() && session.illusion()) {
            illusionSound.play(.4f);
            Illusion clone = session.getIllusion();
            illusionAnimation = new CharacterAnimation(clone.x(), clone.y());
            illusionAnimation.updateDirection(session.player().currentDirection());
        }
    }

    private void onStateChanged(GameSession.Snapshot current) {
        if (snapshot != null) {
            if (current.state() != snapshot.state()) {
                if (current.state() == GameSession.State.WON) {
                    winSound.play(.5f);
                } else if (current.state() == GameSession.State.LOST) {
                    loseSound.play(.5f);
                }
            } else if (current.health() < snapshot.health()) {
                hurtSound.play(.5f);
            }
        }
        snapshot = current;
    }

    private void updateOverlay() {
        overlay.setVisible(paused || snapshot.state() != GameSession.State.PLAYING);
        if (overlay.isVisible()) {
            boolean won = snapshot.state() == GameSession.State.WON, lost = snapshot.state() == GameSession.State.LOST;
            overlayTitle.setText(won ? "Timeline Saved" : lost ? "You Were Caught" : "Paused");
            overlayText.setText(won || lost ? "Score: " + session.score() : "Time can wait. So can the guards.");
            overlayButton.setText(won || lost ? "Try Again" : "Resume");
        }
    }

    private Player.Direction getInputDirection() {
        int[] keys = { Input.Keys.W, Input.Keys.UP, Input.Keys.S, Input.Keys.DOWN,
                Input.Keys.A, Input.Keys.LEFT, Input.Keys.D, Input.Keys.RIGHT };
        Player.Direction[] directions = { Player.Direction.UP, Player.Direction.UP,
                Player.Direction.DOWN, Player.Direction.DOWN, Player.Direction.LEFT,
                Player.Direction.LEFT, Player.Direction.RIGHT, Player.Direction.RIGHT };
        Player.Direction direction = Player.Direction.NONE;
        for (int i = 0; i < keys.length; i++) {
            if (Gdx.input.isKeyJustPressed(keys[i])) {
                direction = directions[i];
            }
        }
        return direction;
    }

    private static TextureRegion[][] createSprites(Texture texture) {
        int frames = texture.getWidth() / 64;
        TextureRegion[][] sprites = new TextureRegion[4][frames];
        for (int direction = 0; direction < 4; direction++) {
            for (int frame = 0; frame < frames; frame++) {
                TextureRegion sprite = new TextureRegion(texture, frame * 64, Math.min(direction, 2) * 96, 64, 96);
                if (direction == 3) {
                    sprite.flip(true, false);
                }
                sprites[direction][frame] = sprite;
            }
        }
        return sprites;
    }

    private void drawGuard(Guard guard, CharacterAnimation guardAnimation) {
        TextureRegion[][] sprites = guardSprites.get(guard.type());
        if (guard.isReturning()) {
            batch.setColor(1, 1, 1, .35f);
        } else if (session.timeStop() > 0) {
            batch.setColor(.5f, .8f, 1, 1);
        } else if (guard.isFrightened()) {
            batch.setColor(session.frightenedTime() < 2 && (int) (session.frightenedTime() * 6) % 2 == 0 ? Color.WHITE
                    : Color.CYAN);
        }
        drawCharacter(sprites[guardAnimation.direction().ordinal()][guardAnimation.frame()], guard.x(), guard.y());
        batch.setColor(Color.WHITE);
    }

    private void drawCharacter(TextureRegion sprite, float x, float y) {
        float height = 48;
        float width = height * sprite.getRegionWidth() / sprite.getRegionHeight();
        batch.draw(sprite, x - width / 2, y - Player.RADIUS, width, height);
        float mapWidth = session.level().width() * Level.TILE;
        if (x < width / 2) {
            batch.draw(sprite, x + mapWidth - width / 2, y - Player.RADIUS, width, height);
        }
        if (x > mapWidth - width / 2) {
            batch.draw(sprite, x - mapWidth - width / 2, y - Player.RADIUS, width, height);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        music.pause();
        paused = true;
    }

    @Override
    public void hide() {
        music.pause();
    }

    @Override
    public void dispose() {
        music.dispose();
        session.removeListener(stateListener);
        shapes.dispose();
        batch.dispose();
        font.dispose();
        hudFont.dispose();
        stage.dispose();
        ui.dispose();
        for (Texture texture : guardTextures.values()) {
            texture.dispose();
        }
        for (Texture texture : hudTextures()) {
            texture.dispose();
        }
        interiorTexture.dispose();
        temPadTexture.dispose();
        lokiTexture.dispose();
        pickupSound.dispose();
        dashSound.dispose();
        illusionSound.dispose();
        stopSound.dispose();
        hurtSound.dispose();
        winSound.dispose();
        loseSound.dispose();
    }
}
