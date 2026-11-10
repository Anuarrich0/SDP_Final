package loki.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.EnumMap;

public final class MenuScreen extends ScreenAdapter {
    private final LokiGame game;
    private final GameUi ui = new GameUi();
    private final Stage stage = new Stage(new FitViewport(1200, 800));
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture background = new Texture(Gdx.files.internal("menu-background.png"));
    private final Texture shade;
    private final EnumMap<PlayerSkin, Texture> skinTextures = new EnumMap<>(PlayerSkin.class);
    private final EnumMap<PlayerSkin, Button> skinButtons = new EnumMap<>(PlayerSkin.class);

    public MenuScreen(LokiGame game) {
        this.game = game;
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Pixmap pixels = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                float distance = (float) Math.hypot((x - 63.5f) / 90, (y - 63.5f) / 90);
                pixels.setColor(.015f, .035f, .025f, .12f + .48f * distance * distance);
                pixels.drawPixel(x, y);
            }
        }
        shade = new Texture(pixels);
        pixels.dispose();
        shade.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Table root = new Table();
        root.setFillParent(true);
        root.add(ui.label("CHARACTER SKIN", .48f, GameUi.GOLD)).padBottom(16).row();
        root.add(createSkinPicker()).padBottom(28).row();
        TextButton play = ui.button("PLAY", true, this::play);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(play.getStyle());
        style.up = ui.panel("10291FF2");
        style.over = ui.panel("2C513CF7");
        style.down = ui.panel("081B13FA");
        style.fontColor = Color.valueOf("F3D6A6");
        style.overFontColor = Color.WHITE;
        play.setStyle(style);
        play.getLabel().setFontScale(.8f);
        Table frame = new Table();
        frame.setBackground(ui.panel("BF9560"));
        frame.pad(1.5f);
        frame.add(play).width(300).height(68);
        root.add(frame);
        stage.addActor(root);
    }

    private Table createSkinPicker() {
        Table picker = new Table();
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = ui.panel("304238");
        style.over = ui.panel("7D8459");
        style.down = ui.panel("526544");
        style.checked = ui.panel("D5B479");
        for (PlayerSkin skin : PlayerSkin.values()) {
            Texture texture = new Texture(Gdx.files.internal(skin.textureFile()));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            skinTextures.put(skin, texture);
            Image preview = new Image(new TextureRegionDrawable(new TextureRegion(texture, 0, 0, 64, 96)));
            Table content = new Table();
            content.setBackground(ui.panel("10251FF5"));
            content.add(preview).size(80, 120).padTop(8).row();
            content.add(ui.label(skin.displayName(), .46f, Color.WHITE)).pad(8, 4, 12, 4);
            Button button = new Button(style);
            button.setName("skin-" + skin.name());
            button.add(content).grow().pad(2);
            button.setChecked(skin == game.selectedSkin());
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.selectSkin(skin);
                    for (PlayerSkin choice : PlayerSkin.values()) {
                        skinButtons.get(choice).setChecked(choice == skin);
                    }
                }
            });
            skinButtons.put(skin, button);
            picker.add(button).width(174).height(174).pad(0, 6, 0, 6);
        }
        return picker;
    }

    private void play() {
        Gdx.app.postRunnable(game::play);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && stage.getActors().size == 1) {
            play();
        }
        ScreenUtils.clear(.02f, .05f, .04f, 1);
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        float scale = Math.max((float) width / background.getWidth(), (float) height / background.getHeight());
        float drawWidth = background.getWidth() * scale, drawHeight = background.getHeight() * scale;
        batch.begin();
        batch.draw(background, width - drawWidth, (height - drawHeight) / 2, drawWidth, drawHeight);
        batch.draw(shade, 0, 0, width, height);
        batch.end();
        stage.getViewport().apply();
        stage.act(Math.min(delta, .1f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        ui.dispose();
        batch.dispose();
        background.dispose();
        shade.dispose();
        for (Texture texture : skinTextures.values()) {
            texture.dispose();
        }
    }
}
