import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

// Exercises our Swing components on the EDT without taking over the user's window.
public class UiChecks {
    public static void main(String[] args) throws Exception {
        if (!UiChecks.class.desiredAssertionStatus()) { throw new IllegalStateException("Use java -ea"); }
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = null;
            try {
                GameWindow.installTheme();
                Path save = Files.createTempDirectory(Path.of("out"), "ui-check-").resolve("campaign.xml");
                GameWindow window = new GameWindow(save);
                frame = field(window, "frame", JFrame.class);
                frame.addNotify();
                capture(frame, "01-home");
                call(window, "prepare", new Class<?>[]{MissionLocation.class}, MissionLocation.NEW_YORK);
                capture(frame, "02-setup");
                click(frame.getContentPane(), "Выбрать вариант Локи  →");
                JPanel[] cards = field(window, "heroCards", JPanel[].class);
                click(cards[3], "Выбрать");
                field(window, "amplifier", JCheckBox.class).doClick();
                field(window, "amulet", JCheckBox.class).doClick();
                capture(frame, "03-loadout");
                click(frame.getContentPane(), "Войти в портал  →");
                MissionControlFacade control = field(window, "control", MissionControlFacade.class);
                assert control.getHero().getName().equals("Сильви");
                assert control.getHero().getMagicPower() == 80 && control.getHero().getDiplomacy() == 70;
                capture(frame, "04-mission");
                JToggleButton[] actions = field(window, "actionButtons", JToggleButton[].class);
                for (int action : new int[]{1, 1, 0}) {
                    actions[action].doClick();
                    field(window, "confirm", JButton.class).doClick();
                    assert !field(window, "confirm", JButton.class).isEnabled();
                    field(window, "next", JButton.class).doClick();
                }
                assert control.getResult().success() && control.getResult().finalInstability() == 0;
                assert new CampaignStore(save).load().size() == 1;
                capture(frame, "05-result");
                call(window, "goArchive", new Class<?>[0]);
                assert field(window, "archiveModel", DefaultListModel.class).getSize() == 1;
                capture(frame, "06-archive");
                GameWindow reopened = new GameWindow(save);
                try {
                    MissionControlFacade reloaded = field(reopened, "control", MissionControlFacade.class);
                    assert reloaded.isUnlocked(MissionLocation.ASGARD) && reloaded.getArchive().size() == 1;
                } finally { field(reopened, "frame", JFrame.class).dispose(); }
                Files.delete(save);
                Files.delete(save.getParent());
                System.out.println("PASS: six Swing screens at two sizes, Sylvie/amulet mission, saved campaign restart");
            } catch (Exception error) { throw new AssertionError(error); }
            finally { if (frame != null) { frame.dispose(); } }
        });
    }

    private static <T> T field(GameWindow window, String name, Class<T> type) throws Exception {
        var field = GameWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(window));
    }

    private static void call(GameWindow window, String name, Class<?>[] types, Object... arguments) throws Exception {
        var method = GameWindow.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(window, arguments);
    }

    private static void click(Container parent, String text) {
        AbstractButton button = find(parent, text);
        if (button == null) { throw new AssertionError("No button: " + text); }
        button.doClick();
    }

    private static AbstractButton find(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (!component.isVisible()) { continue; }
            if (component instanceof AbstractButton button && button.getText().equals(text)) { return button; }
            if (component instanceof Container child) {
                AbstractButton found = find(child, text);
                if (found != null) { return found; }
            }
        }
        return null;
    }

    private static void capture(JFrame frame, String name) throws Exception {
        for (Dimension size : new Dimension[]{new Dimension(1160, 800), new Dimension(1440, 980)}) {
            frame.setSize(size);
            frame.validate();
            Container root = frame.getContentPane();
            BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            root.printAll(graphics);
            graphics.dispose();
            Path directory = Path.of("out", "ui");
            Files.createDirectories(directory);
            ImageIO.write(image, "png", directory.resolve(name + "-" + size.width + ".png").toFile());
        }
    }
}
