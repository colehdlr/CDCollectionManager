package main.gui;

import javax.swing.*;
import java.awt.*;

public class ScaledImageLabel extends JLabel {
    private final ImageIcon originalIcon;

    public ScaledImageLabel(ImageIcon icon) {
        super(icon);
        this.originalIcon = icon;
    }

    @Override
    public Dimension getPreferredSize() {
        int size = Math.min(getWidth(), getHeight());
        return new Dimension(size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets insets = getBorder() != null ? getBorder().getBorderInsets(this) : new Insets(0,0,0,0);

        int containerWidth = getWidth() - (insets.left + insets.right);
        int containerHeight = getHeight() - (insets.top + insets.bottom);

        if (originalIcon != null) {
            Image img = originalIcon.getImage();
            int size = Math.min(containerWidth, containerHeight);

            g2d.drawImage(img,
                    insets.left + (containerWidth - size) / 2,
                    insets.top + (containerHeight - size) / 2,
                    size, size, null);
        }

        g2d.dispose();
    }
}
