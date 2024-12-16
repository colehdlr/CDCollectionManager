package main.gui;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class TabButton extends JPanel {
    private final String name;

    private static final Border ACTIVE_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED),
            BorderFactory.createEmptyBorder(0, 5, 0, 5));
    private static final Border INACTIVE_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createEmptyBorder(0, 5, 0, 5));
    private static final Border HOVER_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            BorderFactory.createEmptyBorder(0, 5, 0, 5));

    public TabButton(String name) {
        this.name = name;
        this.setLayout(new BorderLayout());
        this.add(new JLabel(name), BorderLayout.WEST);
        this.setPreferredSize(new Dimension(150, 50));
        this.setBorder(INACTIVE_BORDER);
    }

    public void setActive() {
        this.setBorder(ACTIVE_BORDER);
    }
    public void setInactive() {
        this.setBorder(INACTIVE_BORDER);
    }
    public void onHover() {
        if (this.getBorder() == INACTIVE_BORDER) {
            this.setBorder(HOVER_BORDER);
        }

        // Option to remove appears
    }
    public void offHover() {
        if (this.getBorder() == HOVER_BORDER) {
            this.setBorder(INACTIVE_BORDER);
        }
    }
}
