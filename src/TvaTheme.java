import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

// Общие цвета и реальные повторяющиеся элементы шести экранов.
public final class TvaTheme {
    public static final Color BACKGROUND = new Color(8, 20, 18);
    public static final Color PANEL = new Color(15, 33, 29);
    public static final Color BORDER = new Color(133, 105, 54);
    public static final Color TEXT = new Color(246, 237, 211);
    public static final Color MUTED = new Color(181, 189, 174);
    public static final Color GOLD = new Color(229, 184, 99);
    public static final Color GREEN = new Color(45, 114, 77);
    public static final Color AMBER = new Color(239, 159, 61);
    public static final Color MINT = new Color(133, 225, 163);

    // Типографика собрана в одном месте: два семейства и несколько ступеней размера.
    // Заголовки: 24-30 px, основной текст: 14-16 px, подписи: 12-13 px.
    // DISPLAY - крупные декоративные числа (логотип, итоговая нестабильность).
    public static final String HEADING_FAMILY = "Georgia";
    public static final String BODY_FAMILY = "Segoe UI";
    public static final int DISPLAY_LARGE = 42;
    public static final int DISPLAY = 34;
    public static final int HEADING_LARGE = 30;
    public static final int HEADING = 28;
    public static final int HEADING_SMALL = 24;
    public static final int BODY = 16;
    public static final int BODY_SMALL = 15;
    public static final int LABEL = 13;
    public static final int LABEL_SMALL = 12;
    // Тёмная палитра ниже проверена на контраст: текст не сливается с фоном.

    private static final Map<String, BufferedImage> images = new HashMap<>();

    private TvaTheme() { }

    public static Font headingFont(int size) { return new Font(HEADING_FAMILY, Font.BOLD, size); }
    public static Font bodyFont(int size) { return new Font(BODY_FAMILY, Font.PLAIN, size); }
    public static Font labelFont(int size) { return new Font(BODY_FAMILY, Font.BOLD, size); }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(bodyFont(size));
        label.setForeground(color);
        return label;
    }

    public static JLabel heading(String text, int size) {
        JLabel label = label(text, size, TEXT);
        label.setFont(headingFont(size));
        return label;
    }

    public static JTextArea text(String value, int size) {
        JTextArea text = new JTextArea(value);
        text.setFont(bodyFont(size));
        text.setForeground(MUTED);
        text.setOpaque(false);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(null);
        text.setSelectionColor(GREEN);
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

    public static void styleCheckBox(JCheckBox item) {
        item.setOpaque(false);
        item.setFont(labelFont(BODY_SMALL - 1));
        item.setIcon(new CheckIcon(false));
        item.setSelectedIcon(new CheckIcon(true));
        item.setIconTextGap(9);
        item.setFocusPainted(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Поля ввода и списки выглядывают из тёмной темы по умолчанию, поэтому красим их вручную.
    public static void styleTextField(JTextField field) {
        field.setFont(bodyFont(BODY));
        field.setForeground(TEXT);
        field.setBackground(PANEL);
        field.setCaretColor(GOLD);
        field.setSelectionColor(GREEN);
        field.setSelectedTextColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(9, 12, 9, 12)));
    }

    public static void styleComboBox(JComboBox<?> box) {
        box.setFont(bodyFont(BODY));
        box.setForeground(TEXT);
        box.setBackground(PANEL);
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(2, 4, 2, 4)));
        box.setRenderer(new ComboRenderer());
    }

    private static final class ComboRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                     boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            label.setFont(bodyFont(BODY));
            label.setOpaque(true);
            label.setForeground(TEXT);
            label.setBackground(selected ? GREEN : PANEL);
            label.setBorder(new EmptyBorder(7, 10, 7, 10));
            return label;
        }
    }

    private record CheckIcon(boolean selected) implements Icon {
        public int getIconWidth() { return 23; }
        public int getIconHeight() { return 23; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? GREEN : BACKGROUND); g.fillRect(x + 1, y + 1, 20, 20);
            g.setColor(selected ? MINT : BORDER); g.setStroke(new BasicStroke(2f));
            g.drawRect(x + 1, y + 1, 20, 20);
            if (selected) { g.drawLine(x + 5, y + 11, x + 10, y + 16); g.drawLine(x + 10, y + 16, x + 18, y + 6); }
            g.dispose();
        }
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
                Color top = primary && button.isEnabled() ? new Color(247, 203, 129) : fill.brighter();
                Color bottom = primary && button.isEnabled() ? new Color(183, 128, 49) : fill.darker();
                g.setPaint(new GradientPaint(0, 0, top, 0, component.getHeight(), bottom));
                Shape face = frameShape(component.getWidth(), component.getHeight(), 1);
                g.fill(face);
                g.setStroke(new BasicStroke(button.hasFocus() || selected ? 2f : 1f));
                g.setColor(selected ? MINT : primary || button.hasFocus() ? GOLD : BORDER);
                g.draw(face);
                if (primary && button.isEnabled()) {
                    g.setColor(new Color(255, 226, 161, 100));
                    g.draw(frameShape(component.getWidth(), component.getHeight(), 4));
                }
                g.dispose();
                super.paint(graphics, component);
            }
        });
        button.setFont(labelFont(BODY_SMALL));
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBorder(new EmptyBorder(14, 18, 14, 18));
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
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() { thumbColor = BORDER; trackColor = BACKGROUND; }
            @Override
            protected JButton createDecreaseButton(int orientation) { return hiddenButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return hiddenButton(); }
            private JButton hiddenButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    public static JPanel stat(String name, int value, Color color) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        panel.add(label(name, BODY_SMALL - 1, TEXT), BorderLayout.WEST);
        panel.add(label(Integer.toString(value), BODY, GOLD), BorderLayout.EAST);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setUI(new BasicProgressBarUI());
        bar.setValue(value);
        bar.setForeground(color);
        bar.setBackground(BACKGROUND);
        bar.setBorder(BorderFactory.createLineBorder(BORDER));
        bar.setPreferredSize(new Dimension(50, 6));
        panel.add(bar, BorderLayout.SOUTH);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return panel;
    }

    private static Shape frameShape(int width, int height, int inset) {
        int right = width - inset - 1, bottom = height - inset - 1, corner = 8;
        Path2D shape = new Path2D.Double();
        shape.moveTo(inset + corner, inset); shape.lineTo(right, inset);
        shape.lineTo(right, bottom - corner); shape.lineTo(right - corner, bottom);
        shape.lineTo(inset, bottom); shape.lineTo(inset, inset + corner); shape.closePath();
        return shape;
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
        Font font = bodyFont(BODY_SMALL);
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
        // Стрелка списка и его всплывающее окно тоже должны быть зелёно-золотыми, а не серыми.
        UIManager.put("ComboBox.buttonBackground", PANEL);
        UIManager.put("ComboBox.buttonHighlight", GOLD);
        UIManager.put("ComboBox.buttonShadow", BORDER);
        UIManager.put("ComboBox.buttonDarkShadow", BORDER);
        UIManager.put("ComboBox.disabledBackground", PANEL);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("PopupMenu.background", PANEL);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("List.background", PANEL);
        UIManager.put("List.foreground", TEXT);
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
            boolean selected = Boolean.TRUE.equals(getClientProperty("selected"));
            g.setPaint(new GradientPaint(0, 0, selected ? new Color(27, 63, 43) : PANEL,
                    getWidth(), getHeight(), BACKGROUND));
            g.fill(frameShape(getWidth(), getHeight(), 2));
            if (selected) {
                g.setColor(new Color(229, 184, 99, 45));
                g.setStroke(new BasicStroke(7f));
                g.draw(frameShape(getWidth(), getHeight(), 4));
            }
            g.setStroke(new BasicStroke(selected ? 2f : 1f));
            g.setColor(selected ? GOLD : BORDER);
            g.draw(frameShape(getWidth(), getHeight(), 2));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static final class Backdrop extends JPanel {
        private static final long serialVersionUID = 1L;
        public Backdrop() { super(new BorderLayout()); }
        @Override
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            super.paint(g);
            g.dispose();
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.drawImage(TvaTheme.image("new_york"), 0, 0, getWidth(), getHeight(), null);
            g.setPaint(new GradientPaint(0, 0, new Color(5, 18, 16, 242), getWidth(), getHeight(), new Color(5, 18, 16, 213)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(229, 184, 99, 30));
            for (int y = 100; y < getHeight(); y += 110) {
                g.drawLine(0, y, 30, y); g.drawLine(30, y, 52, y + 22);
                g.drawLine(52, y + 22, 100, y + 22);
            }
            g.dispose();
        }
    }

    public static final class TimelinePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final transient java.util.List<Integer> values;
        private final int completed;
        public TimelinePanel(int completed, java.util.List<Integer> values) {
            this.completed = completed;
            this.values = java.util.List.copyOf(values);
            setOpaque(false);
            setPreferredSize(new Dimension(220, 130));
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(bodyFont(LABEL));
            int width = getWidth(), height = getHeight();
            g.setColor(new Color(229, 184, 99, 24));
            for (int y = 20; y < height; y += 30) { g.drawLine(12, y, width - 12, y); }
            if (values.isEmpty()) {
                for (int branch = 0; branch < 3; branch++) {
                    Path2D path = new Path2D.Double();
                    path.moveTo(14, height / 2.0);
                    path.curveTo(width * .36, height / 2.0, width * .5, 22 + branch * (height - 44) / 2.0,
                            width - 20, 22 + branch * (height - 44) / 2.0);
                    g.setColor(branch < completed ? MINT : GOLD);
                    g.setStroke(new BasicStroke(branch < completed ? 3f : 1.5f)); g.draw(path);
                    int y = 22 + branch * (height - 44) / 2;
                    g.fillOval(width - 25, y - 5, 10, 10);
                }
                g.setColor(GOLD); g.fillOval(9, height / 2 - 5, 10, 10);
            } else {
                int previousX = 0, previousY = 0;
                for (int i = 0; i < values.size(); i++) {
                    int x = 24 + i * (width - 48) / Math.max(1, values.size() - 1);
                    int y = 24 + (100 - values.get(i)) * Math.max(1, height - 64) / 100;
                    g.setColor(values.get(i) <= 20 ? MINT : GOLD);
                    g.setStroke(new BasicStroke(2.5f));
                    if (i > 0) { g.drawLine(previousX, previousY, x, y); }
                    g.fillOval(x - 4, y - 4, 8, 8);
                    g.drawString(Integer.toString(values.get(i)), x - 9, y - 10);
                    g.setColor(MUTED); g.drawString(i == 0 ? "Старт" : "0" + i, x - 14, height - 8);
                    previousX = x; previousY = y;
                }
            }
            g.dispose();
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
            if (asset.equals("encounter")) {
                image = TvaTheme.image("encounter-sheet").getSubimage(350, 148, 1290, 398);
            } else if (asset.equals("loki") || asset.equals("classic")) {
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
            g.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 5, 5));
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
