import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

// Общие цвета и реальные повторяющиеся элементы шести экранов.
public final class TvaTheme {
    public static final Color BACKGROUND = new Color(13, 23, 21);
    public static final Color PANEL = new Color(23, 39, 33);
    public static final Color BORDER = new Color(52, 69, 57);
    public static final Color TEXT = new Color(239, 235, 217);
    public static final Color MUTED = new Color(151, 168, 155);
    public static final Color GOLD = new Color(214, 177, 99);
    public static final Color GREEN = new Color(51, 123, 86);
    public static final Color AMBER = new Color(222, 139, 77);
    private static final Map<String, BufferedImage> images = new HashMap<>();

    private TvaTheme() { }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel heading(String text, int size) {
        JLabel label = label(text, size, TEXT);
        label.setFont(new Font("Georgia", Font.BOLD, size));
        return label;
    }

    public static JTextArea text(String value, int size) {
        JTextArea text = new JTextArea(value);
        text.setFont(new Font("Segoe UI", Font.PLAIN, size));
        text.setForeground(MUTED);
        text.setOpaque(false);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(null);
        return text;
    }

    public static JPanel column(int gap, JComponent... items) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JComponent item : items) {
            if (panel.getComponentCount() > 0) { panel.add(Box.createVerticalStrut(gap)); }
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(item);
        }
        return panel;
    }

    public static JPanel row(JComponent... items) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        for (JComponent item : items) { panel.add(item); }
        return panel;
    }

    public static Surface surface(int padding) {
        Surface panel = new Surface();
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    public static JButton button(String text, boolean primary, Runnable action) {
        JButton button = new JButton(text);
        styleButton(button, primary);
        button.addActionListener(event -> action.run());
        return button;
    }

    public static JToggleButton toggle(String text) {
        JToggleButton button = new JToggleButton(text);
        styleButton(button, false);
        return button;
    }

    private static void styleButton(AbstractButton button, boolean primary) {
        button.setUI(new BasicButtonUI() {
            @Override
            protected void paintText(Graphics graphics, AbstractButton component, Rectangle bounds, String text) {
                if (component.isEnabled()) { super.paintText(graphics, component, bounds, text); }
                else {
                    graphics.setColor(MUTED);
                    graphics.drawString(text, bounds.x, bounds.y + graphics.getFontMetrics().getAscent());
                }
            }

            @Override
            public void paint(Graphics graphics, JComponent component) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = button.getModel().isSelected();
                Color fill = primary ? GOLD : selected ? GREEN : PANEL;
                if (button.getModel().isRollover() && button.isEnabled()) { fill = fill.brighter(); }
                if (button.getModel().isPressed()) { fill = fill.darker(); }
                if (!button.isEnabled()) { fill = new Color(37, 47, 41); }
                g.setColor(fill);
                g.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 12, 12);
                g.setColor(button.hasFocus() || selected ? GOLD : BORDER);
                g.drawRoundRect(1, 1, component.getWidth() - 3, component.getHeight() - 3, 12, 12);
                g.dispose();
                super.paint(graphics, component);
            }
        });
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }

    public static BufferedImage image(String name) {
        return images.computeIfAbsent(name, key -> {
            URL resource = TvaTheme.class.getResource("/assets/" + key + ".png");
            if (resource == null) { throw new IllegalStateException("Нет изображения " + key + ". Запустите run.cmd."); }
            try {
                BufferedImage image = ImageIO.read(resource);
                if (image == null) { throw new IOException("Unsupported image"); }
                return image;
            } catch (IOException error) { throw new IllegalStateException("Не удалось открыть " + key, error); }
        });
    }

    public static void install() {
        Font font = new Font("Segoe UI", Font.PLAIN, 15);
        for (String name : new String[]{"Label", "Button", "ToggleButton", "ComboBox", "CheckBox", "TextField", "OptionPane"}) {
            UIManager.put(name + ".font", font);
            UIManager.put(name + ".background", PANEL);
            UIManager.put(name + ".foreground", TEXT);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("ToggleButton.disabledText", MUTED);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.selectionBackground", GREEN);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("List.selectionBackground", GREEN);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("ProgressBar.background", BACKGROUND);
        UIManager.put("ProgressBar.foreground", GREEN);
        UIManager.put("ProgressBar.selectionForeground", TEXT);
        UIManager.put("ProgressBar.selectionBackground", TEXT);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.track", BACKGROUND);
    }

    public static final class Surface extends JPanel {
        private static final long serialVersionUID = 1L;

        public Surface() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(PANEL);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(Boolean.TRUE.equals(getClientProperty("selected")) ? GOLD : BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static final class ArtPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient BufferedImage image;
        private final boolean shade;

        public ArtPanel(String asset, int height, boolean shade) {
            this.shade = shade;
            setAsset(asset);
            setOpaque(false);
            setPreferredSize(new Dimension(150, height));
            setLayout(new BorderLayout());
        }

        public void setAsset(String asset) {
            // Два портрета берём из исходного концепт-листа как из sprite atlas.
            if (asset.equals("loki") || asset.equals("classic")) {
                BufferedImage sheet = TvaTheme.image("variants-sheet");
                int x = asset.equals("loki") ? 175 : 550;
                int y = asset.equals("loki") ? 125 : 100;
                image = sheet.getSubimage(x, y, 300, 495 - y);
            } else { image = TvaTheme.image(asset); }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
            double scale = Math.max((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int width = (int) Math.ceil(image.getWidth() * scale);
            int height = (int) Math.ceil(image.getHeight() * scale);
            int top = image.getHeight() > image.getWidth() ? 0 : (getHeight() - height) / 2;
            g.drawImage(image, (getWidth() - width) / 2, top, width, height, null);
            if (shade) {
                g.setPaint(new GradientPaint(0, 0, new Color(8, 18, 15, 10), 0, getHeight(), new Color(8, 18, 15, 240)));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }
}
