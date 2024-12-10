package main.manager;

import main.gui.CDPanel;

import java.util.ArrayList;

public class CDCollection extends ArrayList<CDPanel> {
    private final String name;

    public CDCollection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
