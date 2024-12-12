package main.manager;

import main.gui.CDPanel;

import java.util.ArrayList;

public class CDFolder extends ArrayList<CDPanel> {
    private final String name;

    public CDFolder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
