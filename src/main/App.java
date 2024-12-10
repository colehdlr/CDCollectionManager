package main;

import main.gui.TabButton;
import main.gui.CDPanel;
import main.manager.CDCollection;
import main.manager.CDManager;

import javax.swing.*;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Objects;

public class App {
    private final JFrame frame;
    private final CDManager cdManager;

    private final JPanel appPanel;
    private final JPanel topPanel;
    private final JPanel sidePanel;
    private final JPanel mainPanel;
    private final JPanel activePanel;

    private final ArrayList<TabButton> sideTabs = new ArrayList<>();
    private final TabButton newCollectionButton;
    private int currentTab = 0;

    private final TabButton addCDButton;

    private JMenuBar menuBar;

    public App(JFrame frame) {
        this.frame = frame;

        activePanel = new JPanel();
        activePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        activePanel.setPreferredSize(new Dimension(300, 300));
        activePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // Load CDs
        System.out.println("Creating CD Manager...");
        cdManager = new CDManager(activePanel);
        cdManager.loadCDs();

        updateMenuBar();

        // Init GUI
        topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.LIGHT_GRAY);
        topPanel.setPreferredSize(new Dimension(0, 40));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(3, 4, 3, 3)  // Adjust these values as needed
        ));

        JLabel cdIconLabel = new JLabel(new ImageIcon(
                new ImageIcon("./src/main/data/images/cd_icon.png")
                        .getImage()
                        .getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        topPanel.add(cdIconLabel, BorderLayout.WEST);
        topPanel.add(new JLabel(" CD Manager"), BorderLayout.CENTER);

        addCDButton = new TabButton("+ Add CD");
        addCDButton.setPreferredSize(new Dimension(85, 0));
        addCDButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                cdManager.addCD();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                addCDButton.onHover();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                addCDButton.offHover();
            }
        });
        topPanel.add(addCDButton, BorderLayout.EAST);

        sidePanel = new JPanel();
        sidePanel.setMinimumSize(new Dimension(150, 0));
        sidePanel.setPreferredSize(new Dimension(150, 0));
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(-2, 2, 0, 3)
        ));
        sidePanel.setLayout( new FlowLayout(FlowLayout.CENTER, 0, 0));

        // Create library
        createNewCollection("Library");
        // Load collections
        cdManager.loadCollections(this);

        changeActivePanel(0);

        newCollectionButton = new TabButton("+ New Collection");
        newCollectionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                createNewCollection();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                newCollectionButton.onHover();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                newCollectionButton.offHover();
            }
        });
        sidePanel.add(newCollectionButton);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setMinimumSize(new Dimension(0, 100));
        mainPanel.add(activePanel, BorderLayout.CENTER);
        mainPanel.add(sidePanel, BorderLayout.WEST);

        appPanel = new JPanel(new BorderLayout());
        appPanel.add(mainPanel, BorderLayout.CENTER);
        appPanel.add(topPanel, BorderLayout.NORTH);
    }

    public void createNewCollection(String name) {
        TabButton newCollectionTab = new TabButton(name);
        newCollectionTab.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    changeActivePanel(sideTabs.indexOf(newCollectionTab));
                } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3 && !Objects.equals(name, "Library") && !Objects.equals(name, "♥ Favorites")) {
                    JPopupMenu popupMenu = new JPopupMenu();

                    JMenuItem renameItem = new JMenuItem("Rename");
                    renameItem.addActionListener(e1 -> {
                        String newName = JOptionPane.showInputDialog(null, "Enter new name:", "Rename", JOptionPane.PLAIN_MESSAGE);
                        if (newName != null && !newName.trim().isEmpty()) {
                            // TODO change name
                        }
                    });
                    popupMenu.add(renameItem);

                    JMenuItem removeItem = new JMenuItem("<html><font color='#8B0000'>Delete</font></html>");
                    removeItem.addActionListener(e1 -> {
                        final String message = "Are you sure you want to delete " + name + "?";
                        final String title = "Confirm Deletion";
                        int response = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                        if (response == JOptionPane.YES_OPTION) {
                            // TODO remove collection
                        }
                    });

                    // Add all items to popup menu
                    popupMenu.addSeparator();  // Adds a separation line
                    popupMenu.add(removeItem);

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                newCollectionTab.onHover();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                newCollectionTab.offHover();
            }
        });
        sidePanel.add(newCollectionTab);
        sideTabs.add(newCollectionTab);

        updateMenuBar();

        // Switch to new tab
        changeActivePanel(sideTabs.indexOf(newCollectionTab));
    }

    public void createNewCollection() {
        String newCollectionName = (String)JOptionPane.showInputDialog(
                appPanel,
                "Enter new collection name:",
                "Create new collection",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                ""
        );
        // Customer has entered empty string
        if (Objects.equals(newCollectionName, "")) {
            JOptionPane.showMessageDialog(new JFrame(), "Please use valid collection name.", "Invalid Collection Name",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Customer has closed the dialog
        else if (Objects.equals(newCollectionName, null)) {
            return;
        }

        // Add new collection tab then add back new collection button and repaint
        sidePanel.remove(newCollectionButton);
        cdManager.createNewCollection(newCollectionName);
        createNewCollection(newCollectionName);
        sidePanel.add(newCollectionButton);
        sidePanel.revalidate();
        sidePanel.repaint();
    }

    public void updateMenuBar() {
        menuBar = new JMenuBar();

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenu addSubMenu = new JMenu("Add");
        JMenuItem newCDItem = new JMenuItem("New CD");
        JMenuItem newCollectionItem = new JMenuItem("New Collection");
        addSubMenu.add(newCDItem);
        addSubMenu.add(newCollectionItem);
        editMenu.add(addSubMenu);
        menuBar.add(editMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem libraryItem = new JMenuItem("Library");
        JMenuItem favoritesItem = new JMenuItem("Favorites");
        viewMenu.add(libraryItem);
        viewMenu.add(favoritesItem);
        // Add separator before dynamic collections
        viewMenu.addSeparator();
        for (int i = 2; i < cdManager.getCollectionList().size(); i++) {
            CDCollection collection = cdManager.getCollectionList().get(i);
            JMenuItem collectionItem = new JMenuItem(collection.getName());
            int finalI = i;
            collectionItem.addActionListener(e ->
                    changeActivePanel(finalI)
            );

            viewMenu.add(collectionItem);
        }
        menuBar.add(viewMenu);

        this.frame.setJMenuBar(menuBar);

        // Action Listeners
        newCDItem.addActionListener(e -> cdManager.addCD());
        newCollectionItem.addActionListener(e -> createNewCollection());
        libraryItem.addActionListener(e ->
            changeActivePanel(0)
        );
        favoritesItem.addActionListener(e ->
            changeActivePanel(1)
        );
    }

    public void changeActivePanel(int collectionTabIndex) {
        if (currentTab != collectionTabIndex) {
            // Set all other tabs to inactive
            for (TabButton tab : sideTabs) {
                tab.setInactive();
            }
            sideTabs.get(collectionTabIndex).setActive();
            currentTab = collectionTabIndex;

            // Set page to current tab
            activePanel.removeAll();
            for (CDPanel cdPanel : cdManager.getCDCollection(currentTab)) {
                activePanel.add(cdPanel);
            }

            // Update popup menu so that current tab is up to date
            cdManager.getCDCollection(currentTab).forEach(cdPanel -> cdPanel.updateCDPanelPopupMenu(cdManager.getCollectionList(), currentTab));

            activePanel.revalidate();
            activePanel.repaint();
        }
    }

    public static void main(String[] args) {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CDManager");
            frame.setContentPane(new App(frame).appPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setResizable(true);
            frame.setMinimumSize(new Dimension(200, 100));
        });
    }
}

