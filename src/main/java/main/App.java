package main;

import main.gui.TabButton;
import main.gui.CDPanel;
import main.manager.CDFolder;
import main.manager.CDManager;
import main.manager.Track;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;

public class App {
    private final JFrame frame;
    private final CDManager cdManager;

    private final JPanel appPanel;
    private final JToolBar topPanel;
    private final JPanel sidePanel;
    private final JPanel mainPanel;
    private final JPanel activePanel;

    private final ArrayList<TabButton> sideTabs = new ArrayList<>();
    private final TabButton newFolderButton;
    private int currentTab;

    private final TabButton addCDButton;

    private JMenuBar menuBar;
    private final String DISCOGS_ACCESS_TOKEN = "NycfrNIvKJAghiKJOOuJwhuvjHxWOzrmGUnxYUjb";
    private String mostRecentDiscogsResponse = "";

    public App(JFrame frame) {
        this.frame = frame;

        activePanel = new JPanel();
        activePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        activePanel.setBackground(Color.WHITE);
        activePanel.setPreferredSize(new Dimension(125, -1));
        activePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        JScrollPane activeScrollPane = new JScrollPane(activePanel);
        activeScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        activeScrollPane.setPreferredSize(new Dimension(125, 0));

        JTextField searchField = new JTextField();

        // Load CDs
        System.out.println("Creating CD Manager...");
        cdManager = new CDManager(activePanel, searchField);
        cdManager.loadDataFiles();
        cdManager.loadCDs();

        updateMenuBar();

        // Init GUI
        // Top panel
        topPanel = new JToolBar();
        topPanel.setFloatable(false);
        topPanel.setBackground(Color.LIGHT_GRAY);
        topPanel.setPreferredSize(new Dimension(0, 40));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(3, 4, 3, 3)
        ));

        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftTopPanel.setOpaque(false);

        JLabel cdIconLabel = new JLabel(new ImageIcon(
                new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/cd_icon.png")))
                        .getImage()
                        .getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        leftTopPanel.add(cdIconLabel);
        leftTopPanel.add(new JLabel("CD Manager"));

        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightTopPanel.setOpaque(false);

        addCDButton = new TabButton("+ Add CD");
        addCDButton.setPreferredSize(new Dimension(85, 30));
        addCDButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                createNewCDPopup();
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
        rightTopPanel.add(addCDButton);

        JLabel infoIconLabel = new JLabel(new ImageIcon(
                new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/info_icon.png")))
                        .getImage()
                        .getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        infoIconLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        infoIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Collection info popup window
                int numberOfCDs = cdManager.getCDFolder(0).size();
                Map<String, Long> genreCounts = new HashMap<>();
                Map<String, Long> artistCounts = new HashMap<>();
                int numberOfTracks = 0;

                // Get number of Artists and Genres
                for (CDPanel cdPanel : cdManager.getCDFolder(0)) {
                    String artist = cdPanel.getArtist();
                    if (artist != null && !artist.trim().isEmpty()) {
                        artist = artist.trim();
                        artistCounts.put(artist, artistCounts.getOrDefault(artist, 0L) + 1);
                    }
                    String genre = cdPanel.getGenre();
                    if (genre != null && !genre.trim().isEmpty()) {
                        genre = genre.trim();
                        genreCounts.put(genre, genreCounts.getOrDefault(genre, 0L) + 1);
                    }
                    numberOfTracks += cdPanel.getNumberOfTracks();
                }

                // Add to table
                Function<Map<String, Long>, String> createTopTable = (Map<String, Long> counts) -> {
                    StringBuilder table = new StringBuilder();
                    table.append("<table border='1' style='border-collapse: collapse;'>");
                    table.append("<tr><th>Name</th><th>Count</th></tr>");
                    counts.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .forEach(entry -> table.append(String.format("<tr><td>%s</td><td>%d</td></tr>",
                                    entry.getKey(), entry.getValue())));
                    table.append("</table>");
                    return table.toString();
                };

                String genreTable = createTopTable.apply(genreCounts);
                String artistTable = createTopTable.apply(artistCounts);

                String message = String.format(
                        "<html><body>" +
                                "<h2>CD Collection Info:</h2>" +
                                "CDs: %d<br>" +
                                "Tracks: %d<br>" +
                                "Genres: %d<br>" +
                                "Artists: %d<br><br>" +
                                "<h3>Top Genres:</h3>%s<br>" +
                                "<h3>Top Artists:</h3>%s" +
                                "</body></html>",
                        numberOfCDs, numberOfTracks, genreCounts.size(), artistCounts.size(),
                        genreTable, artistTable
                );

                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "CD Collection Information",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        rightTopPanel.add(infoIconLabel);

        JPanel searchBar = new JPanel();
        searchBar.setPreferredSize(new Dimension(300, 10));
        searchBar.setOpaque(false);

        searchBar.add(new JLabel("Search: "));

        searchField.setPreferredSize(new Dimension(200, 20));
        searchField.getCaret().setBlinkRate(0);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                // This will be called every time a key is typed
                SwingUtilities.invokeLater(() ->
                    cdManager.refreshActivePanel(currentTab)
                );
            }
        });
        searchBar.add(searchField);

        topPanel.add(leftTopPanel);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(searchBar);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(rightTopPanel);

        // Side panel
        sidePanel = new JPanel();
        sidePanel.setMinimumSize(new Dimension(150, 0));
        sidePanel.setPreferredSize(new Dimension(150, 0));
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(-2, 2, 0, 3)
        ));
        sidePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        // Create library
        createNewFolder("Library");
        // Load folders
        cdManager.loadFolders(this);

        for (CDPanel cdPanel : cdManager.getCDFolder(0)) {
            // Update the popup menu
            cdPanel.updateCDPanelPopupMenu(cdManager.getFolderList(), 0);
        }

        changeActivePanel(0);

        newFolderButton = new TabButton("+ New Folder");
        newFolderButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                createNewFolder();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                newFolderButton.onHover();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                newFolderButton.offHover();
            }
        });
        sidePanel.add(newFolderButton);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(0, 1, 0, 0));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setMinimumSize(new Dimension(0, 100));
        mainPanel.add(activeScrollPane, BorderLayout.CENTER);
        mainPanel.add(sidePanel, BorderLayout.WEST);

        appPanel = new JPanel(new BorderLayout());
        appPanel.add(mainPanel, BorderLayout.CENTER);
        appPanel.add(topPanel, BorderLayout.NORTH);
    }

    public void createNewFolder(String name) {
        TabButton newFolderTab = new TabButton(name);

        // Add heart for favorites
        if (Objects.equals(name, "Favorites")) {
            newFolderTab.add(new JLabel("♥ "), BorderLayout.EAST);
            System.out.println("Adding custom appearance for Favorites");
        }

        // Create new folder
        newFolderTab.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click -> open tab
                    changeActivePanel(sideTabs.indexOf(newFolderTab));
                } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3 && !Objects.equals(name, "Library") && !Objects.equals(name, "Favorites")) {
                    // Right click -> open options menu
                    JPopupMenu popupMenu = new JPopupMenu();

                    JMenuItem renameItem = new JMenuItem("Rename");
                    renameItem.addActionListener(e1 -> {
                        String newName = JOptionPane.showInputDialog(null, "Enter new name:", "Rename", JOptionPane.PLAIN_MESSAGE);
                        if (newName != null && !newName.trim().isEmpty()) {
                            // This feature will be implemented in v2.0
                        }
                    });
                    popupMenu.add(renameItem);

                    // Remove folder from system
                    JMenuItem removeItem = new JMenuItem("<html><font color='#8B0000'>Delete</font></html>");
                    removeItem.addActionListener(e1 -> {
                        final String message = "Are you sure you want to delete " + name + "?";
                        final String title = "Confirm Deletion";
                        int response = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                        if (response == JOptionPane.YES_OPTION) {
                            cdManager.removeFolder(cdManager.getCDFolder(name));
                            sidePanel.remove(newFolderTab);
                            sideTabs.remove(newFolderTab);

                            sidePanel.revalidate();
                            sidePanel.repaint();
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
                newFolderTab.onHover();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                newFolderTab.offHover();
            }
        });
        sidePanel.add(newFolderTab);
        sideTabs.add(newFolderTab);

        updateMenuBar();

        // Switch to new tab
        changeActivePanel(sideTabs.indexOf(newFolderTab));
    }

    public void createNewFolder() {
        String newFolderName = (String) JOptionPane.showInputDialog(
                null,
                "Enter new folder name:",
                "Create new folder",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                ""
        );
        // Customer has entered empty string
        if (Objects.equals(newFolderName, "")) {
            JOptionPane.showMessageDialog(new JFrame(), "Please use valid folder name.", "Invalid Folder Name",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (Objects.equals(newFolderName, null)) {
            return;
        }

        // Add new folder tab then add back new folder button and repaint
        sidePanel.remove(newFolderButton);

        // Done separately as not all cases require creation of logical folder
        cdManager.createNewFolder(newFolderName); // New logical folder
        createNewFolder(newFolderName); // New visual folder

        // Save folder to file
        cdManager.saveNewFolder(newFolderName);

        sidePanel.add(newFolderButton);
        sidePanel.revalidate();
        sidePanel.repaint();
    }

    public void updateMenuBar() {
        menuBar = new JMenuBar();

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenu addSubMenu = new JMenu("Add");
        JMenuItem newCDItem = new JMenuItem("New CD");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        addSubMenu.add(newCDItem);
        addSubMenu.add(newFolderItem);
        editMenu.add(addSubMenu);
        menuBar.add(editMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem libraryItem = new JMenuItem("Library");
        JMenuItem favoritesItem = new JMenuItem("Favorites");
        viewMenu.add(libraryItem);
        viewMenu.add(favoritesItem);
        // Add separator before dynamic folders
        viewMenu.addSeparator();
        for (int i = 2; i < cdManager.getFolderList().size(); i++) {
            CDFolder folder = cdManager.getFolderList().get(i);
            JMenuItem folderItem = new JMenuItem(folder.getName());
            int finalI = i;
            folderItem.addActionListener(e ->
                    changeActivePanel(finalI)
            );

            viewMenu.add(folderItem);
        }
        menuBar.add(viewMenu);

        this.frame.setJMenuBar(menuBar);

        // Action Listeners
        newCDItem.addActionListener(e -> createNewCDPopup());
        newFolderItem.addActionListener(e -> createNewFolder());
        libraryItem.addActionListener(e ->
                changeActivePanel(0)
        );
        favoritesItem.addActionListener(e ->
                changeActivePanel(1)
        );
    }

    public void changeActivePanel(int folderTabIndex) {
        if (currentTab != folderTabIndex) {
            // Set all other tabs to inactive
            for (TabButton tab : sideTabs) {
                tab.setInactive();
            }
            sideTabs.get(folderTabIndex).setActive();
            currentTab = folderTabIndex;

            // Set page to current tab
            activePanel.removeAll();
            for (CDPanel cdPanel : cdManager.getCDFolder(currentTab)) {
                activePanel.add(cdPanel);
            }

            // Update popup menu so that current tab is up to date
            cdManager.getCDFolder(currentTab).forEach(cdPanel -> cdPanel.updateCDPanelPopupMenu(cdManager.getFolderList(), currentTab));

            activePanel.revalidate();
            activePanel.repaint();
        }
    }

    public void createNewCDPopup() {
        JDialog dialog = new JDialog(frame, "Add new CD");
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout(10, 10));

        JTabbedPane addCDPanel = new JTabbedPane();

        // Manual entry panel
        JPanel addManually = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        String[] labels = {"Title:", "Artist:", "Genre:", "Date:"};
        JTextField[] textFields = new JTextField[labels.length - 1];

        // Add labels to table
        for (int i = 0; i < labels.length - 1; i++) {
            addManually.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            textFields[i] = new JTextField(20);
            addManually.add(textFields[i], gbc);
            gbc.gridx = 0;
            gbc.gridy++;
        }

        // Create form
        addManually.add(new JLabel(labels[labels.length - 1]), gbc);
        gbc.gridx = 1;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField dayField = new JTextField(2);
        JTextField monthField = new JTextField(2);
        JTextField yearField = new JTextField(4);
        datePanel.add(new JLabel("Y: "));
        datePanel.add(yearField);
        datePanel.add(new JLabel("M: "));
        datePanel.add(monthField);
        datePanel.add(new JLabel("D: "));
        datePanel.add(dayField);
        addManually.add(datePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        // Track list panel
        JPanel trackListPanel = new JPanel(new BorderLayout());
        DefaultListModel<String> trackListModel = new DefaultListModel<>();
        JList<String> trackList = new JList<>(trackListModel);
        JScrollPane trackScrollPane = new JScrollPane(trackList);
        trackListPanel.add(trackScrollPane, BorderLayout.CENTER);

        // Track input controls
        JPanel trackInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField trackNameField = new JTextField(15);
        JTextField trackDurationField = new JTextField(5);
        JButton addTrackButton = new JButton("Add Track");
        JButton removeTrackButton = new JButton("<html><font color='#8B0000'>Remove Selected</font></html>");

        trackInputPanel.add(new JLabel("Track Name:"));
        trackInputPanel.add(trackNameField);
        trackInputPanel.add(new JLabel("Duration:"));
        trackInputPanel.add(trackDurationField);
        trackInputPanel.add(addTrackButton);
        trackInputPanel.add(removeTrackButton);

        trackListPanel.add(trackInputPanel, BorderLayout.NORTH);

        addTrackButton.addActionListener(e1 -> {
            String trackName = trackNameField.getText().trim();
            String trackDuration = trackDurationField.getText().trim();
            if (!trackName.isEmpty() && !trackDuration.isEmpty()) {
                trackListModel.addElement(trackName + " - " + trackDuration);
                trackNameField.setText("");
                trackDurationField.setText("");
            }
        });

        removeTrackButton.addActionListener(e1 -> {
            int selectedIndex = trackList.getSelectedIndex();
            if (selectedIndex != -1) {
                trackListModel.remove(selectedIndex);
            }
        });

        gbc.gridwidth = 2;
        addManually.add(trackListPanel, gbc);
        gbc.gridy++;

        // Image upload components
        JLabel imageLabel = new JLabel("Cover Image:");
        addManually.add(imageLabel, gbc);
        gbc.gridx = 1;
        JButton chooseImageButton = new JButton("Upload Image");
        chooseImageButton.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,40,0,0), chooseImageButton.getBorder()));
        chooseImageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addManually.add(chooseImageButton, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel selectedImageLabel = new JLabel("No image selected");
        selectedImageLabel.setHorizontalAlignment(JLabel.CENTER);
        addManually.add(selectedImageLabel, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        JButton addManuallyButton = new JButton("Add CD");
        gbc.anchor = GridBagConstraints.EAST;
        addManually.add(addManuallyButton, gbc);

        // File chooser for image selection
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));

        // Image file path
        final String[] selectedImagePath = {null};

        chooseImageButton.addActionListener(e1 -> {
            int result = fileChooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                selectedImagePath[0] = selectedFile.getAbsolutePath();

                // Display thumbnail
                ImageIcon imageIcon = new ImageIcon(new ImageIcon(selectedImagePath[0]).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                selectedImageLabel.setIcon(imageIcon);
                selectedImageLabel.setText("");
            }
        });

        addManuallyButton.addActionListener(e1 -> {
            // Validate input
            for (JTextField field : textFields) {
                if (field.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Validate date
            String day = dayField.getText().trim();
            String month = monthField.getText().trim();
            String year = yearField.getText().trim();
            if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a complete date.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Simple date validation
            try {
                int d = Integer.parseInt(day);
                int m = Integer.parseInt(month);
                int y = Integer.parseInt(year);
                if (d < 1 || d > 31 || m < 1 || m > 12 || y < 1900) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid date.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedImagePath[0] == null) {
                JOptionPane.showMessageDialog(dialog, "Please select an image.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (trackListModel.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please add at least one track.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String title = textFields[0].getText().trim();

            try {
                // Create track list
                ArrayList<Track> tracks = new ArrayList<>();
                for (int i = 0; i < trackListModel.size(); i++) {
                    String[] trackInfo = trackListModel.getElementAt(i).split(" - ");
                    tracks.add(new Track(trackInfo[0], trackInfo[1]));
                }

                // Create CD object and add to collection
                cdManager.addCD(
                        title,                  // Title
                        textFields[1].getText(),  // Artist
                        textFields[2].getText(),  // Genre
                        year+"-"+month+"-"+day,    // Date
                        selectedImagePath[0],     // Image path
                        tracks,                    // Track list
                        currentTab
                );

                JOptionPane.showMessageDialog(dialog, "CD added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error adding CD: " + ex.getMessage(), "Add CD Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Search panel
        JPanel addBySearch = new JPanel(new BorderLayout(10, 10));
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel();
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        addBySearch.add(searchPanel, BorderLayout.NORTH);
        JList<String> searchResults = new JList<>();
        JScrollPane scrollPane = new JScrollPane(searchResults);
        addBySearch.add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e1 -> {
            if (Objects.equals(searchField.getText(), "")) {
                JOptionPane.showMessageDialog(dialog, "No text in search field.", "Add CD Error", JOptionPane.WARNING_MESSAGE);
            } else {
                // Access Discogs API
                String urlString = "https://api.discogs.com/database/search?q=" + searchField.getText() + "&type=release";
                try {
                    // Connect to API
                    // Search API with search paramater
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    connection.setRequestProperty("Authorization", "Discogs token=" + DISCOGS_ACCESS_TOKEN);
                    connection.setRequestProperty("UserAgent", "CDManager/1.0");

                    int responseCode = connection.getResponseCode();
                    System.out.println("Response Code: " + responseCode);

                    // Read output of API
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    mostRecentDiscogsResponse = response.toString();

                    // Parse results
                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    JSONArray results = (JSONArray) jsonResponse.get("results");

                    DefaultListModel<String> listModel = new DefaultListModel<>();

                    // Add results to table for user to select
                    for (Object resultObj : results) {
                        JSONObject result = (JSONObject) resultObj;
                        String title = (String) result.get("title");
                        String year = result.containsKey("year") ? result.get("year").toString() : "N/A";
                        String country = result.containsKey("country") ? (String) result.get("country") : "N/A";
                        String format = "N/A";
                        if (result.containsKey("format")) {
                            JSONArray formatArray = (JSONArray) result.get("format");
                            StringBuilder formatBuilder = new StringBuilder();
                            for (int i = 0; i < formatArray.size(); i++) {
                                if (i > 0) {
                                    formatBuilder.append(", ");
                                }
                                formatBuilder.append(formatArray.get(i));
                            }
                            format = formatBuilder.toString();
                        }

                        String formattedResult = String.format("%s (%s, %s) - %s", title, year, country, format);
                        listModel.addElement(formattedResult);
                    }

                    // Update the search results with the new data
                    searchResults.setModel(listModel);
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        JButton addSearchButton = new JButton("Add Selected CD");
        addSearchButton.addActionListener(e1 -> {
            // Check user has selected a CD
            int selectedIndex = searchResults.getSelectedIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a CD from the search results.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Get the selected result
                JSONObject selectedResult = (JSONObject) ((JSONArray) ((JSONObject) new JSONParser().parse(mostRecentDiscogsResponse)).get("results")).get(selectedIndex);

                // Get the release ID
                String releaseId = selectedResult.get("id").toString();

                // New API call to get more release information
                String detailUrlString = "https://api.discogs.com/releases/" + releaseId;
                URL detailUrl = new URL(detailUrlString);
                HttpURLConnection detailConnection = (HttpURLConnection) detailUrl.openConnection();
                detailConnection.setRequestMethod("GET");
                detailConnection.setRequestProperty("Authorization", "Discogs token=" + DISCOGS_ACCESS_TOKEN);
                detailConnection.setRequestProperty("User-Agent", "YourAppName/1.0");

                BufferedReader detailIn = new BufferedReader(new InputStreamReader(detailConnection.getInputStream()));
                StringBuilder detailResponse = new StringBuilder();
                String detailInputLine;
                while ((detailInputLine = detailIn.readLine()) != null) {
                    detailResponse.append(detailInputLine);
                }
                detailIn.close();

                // Parse the JSON response
                JSONObject detailJson = (JSONObject) new JSONParser().parse(detailResponse.toString());

                // Extract required information
                String title = (String) detailJson.get("title");
                String artist = (String) ((JSONObject) ((JSONArray) detailJson.get("artists")).get(0)).get("name");
                String genre = (String) ((JSONArray) detailJson.get("genres")).get(0);
                String releaseDate = (String) detailJson.get("released");
                String imageUrl = (String) detailJson.get("thumb");

                // Save cover image
                URL imageURL = new URL(imageUrl);
                Path coversDir = Paths.get("temp");
                if (!Files.exists(coversDir)) {
                    Files.createDirectories(coversDir);
                }
                String imagePath = coversDir.resolve(releaseId + ".jpg").toString();
                try (InputStream in = imageURL.openStream()) {
                    Files.copy(in, Paths.get(imagePath), StandardCopyOption.REPLACE_EXISTING);
                }

                // Extract tracks
                JSONArray tracklist = (JSONArray) detailJson.get("tracklist");
                ArrayList<Track> tracks = new ArrayList<>();
                for (Object trackObj : tracklist) {
                    JSONObject track = (JSONObject) trackObj;
                    String trackTitle = (String) track.get("title");
                    String duration = (String) track.get("duration");
                    tracks.add(new Track(trackTitle, duration));
                }

                // Add the CD to the collection
                cdManager.addCD(
                        title,
                        artist,
                        genre,
                        releaseDate,
                        imagePath,
                        tracks,
                        currentTab
                );

                JOptionPane.showMessageDialog(dialog, "CD added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error adding CD: " + ex.getMessage(), "Add CD Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        addBySearch.add(addSearchButton, BorderLayout.SOUTH);

        // Add panels to tabbed pane
        addCDPanel.addTab("Search", addBySearch);
        addCDPanel.addTab("Manual Entry", addManually);

        dialog.add(addCDPanel, BorderLayout.CENTER);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        dialog.setSize(800, 600);
    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CDManager");
            Image iconImage = new ImageIcon(Objects.requireNonNull(App.class.getResource("/images/cd_icon.png"))).getImage();
            frame.setIconImage(iconImage);
            try {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar.getTaskbar().setIconImage(iconImage);
                }
            } catch (final UnsupportedOperationException | SecurityException e) {
                System.err.println("Taskbar icon is not supported by OS: " + e.getMessage());
            }

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
