package loki.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Disposable;

/** Shared colours, font and buttons for the menu and game. */
public final class GameUi implements Disposable {
    public static final Color GREEN = Color.valueOf("8FE6B2"), GOLD = Color.valueOf("E8B777");
    public static final Color MUTED = Color.valueOf("9FB5AF");
    public final Skin skin = new Skin();

    public GameUi() {
        BitmapFont font = new BitmapFont(Gdx.files.internal("ui/interface.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("default-font", font);
        Pixmap pixels = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        pixels.fillRectangle(8, 0, 8, 24);
        pixels.fillRectangle(0, 8, 24, 8);
        pixels.fillCircle(8, 8, 8);
        pixels.fillCircle(15, 8, 8);
        pixels.fillCircle(8, 15, 8);
        pixels.fillCircle(15, 15, 8);
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("rounded-texture", texture);
        skin.add("rounded", new NinePatchDrawable(new NinePatch(texture, 8, 8, 8, 8)), Drawable.class);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));
        TextButton.TextButtonStyle normal = new TextButton.TextButtonStyle();
        normal.font = font;
        normal.fontColor = Color.WHITE;
        normal.up = panel("19342D");
        normal.over = panel("285043");
        normal.down = panel("346A53");
        normal.disabled = panel("182521");
        normal.disabledFontColor = Color.valueOf("657972");
        skin.add("default", normal);
        TextButton.TextButtonStyle primary = new TextButton.TextButtonStyle(normal);
        primary.up = panel("BCE6C4");
        primary.over = panel("D7F7D7");
        primary.down = panel("8FCDA4");
        primary.fontColor = Color.valueOf("10281E");
        skin.add("primary", primary);
    }

    public Drawable panel(String hex) {
        return skin.newDrawable("rounded", Color.valueOf(hex));
    }

    public Label label(String text, float scale, Color color) {
        Label label = new Label(text, skin);
        label.setFontScale(scale);
        label.setColor(color);
        return label;
    }

    public TextButton button(String text, boolean primary, Runnable action) {
        TextButton button = new TextButton(text, skin, primary ? "primary" : "default");
        button.getLabel().setFontScale(.58f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    @Override
    public void dispose() {
        skin.dispose();
    }
}
