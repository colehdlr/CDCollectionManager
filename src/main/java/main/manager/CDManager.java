package main.manager;

import java.io.*;

import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Objects;

import main.App;
import main.gui.CDPanel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import javax.swing.*;

public class CDManager {
    private final ArrayList<CDFolder> folderList;
    private final JPanel activePanel;
    private final JTextField searchTextField;

    private static File DATA_DIR;
    private static File FOLDERS_FILE;
    private static File CD_INFO_FILE;
    private static File IMAGES_DIR;

    public CDManager(JPanel activePanel, JTextField searchTextField) {
        folderList = new ArrayList<>();
        folderList.add(new CDFolder("Library"));
        System.out.println("CD Manager successfully created.\n");

        this.activePanel = activePanel;
        this.searchTextField = searchTextField;
        initDataPaths();
    }

    private void initDataPaths() {
        File jarFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        DATA_DIR = new File(jarFile.getParentFile(), "data");
        FOLDERS_FILE = new File(DATA_DIR, "folders.json");
        CD_INFO_FILE = new File(DATA_DIR, "cd_info.json");
        IMAGES_DIR = new File(DATA_DIR, "images");
    }

    public void loadCDs() {
        try {
            JSONArray cdArray = getJSONArray(CD_INFO_FILE.toPath(), "cds");
            for (Object cd : cdArray) {
                JSONObject cdObject = (JSONObject) cd;

                // Extract CD information
                String title = (String) cdObject.get("title");
                String artist = (String) cdObject.get("artist");
                String genre = (String) cdObject.get("genre");
                String date = (String) cdObject.get("date");

                // Load cover image from resources
                ImageIcon cover;
                String coverFileName = (String) cdObject.get("cover");
                File coverFile = new File(IMAGES_DIR, coverFileName);
                if (coverFile.exists()) {
                    cover = new ImageIcon(coverFile.getAbsolutePath());
                    System.out.println("Loaded cover from file: " + coverFile.getAbsolutePath());
                } else {
                    // Fallback to resources if the file doesn't exist in the external directory
                    try (InputStream imageStream = getClass().getResourceAsStream("/images/" + coverFileName)) {
                        if (imageStream != null) {
                            cover = new ImageIcon(ImageIO.read(imageStream));

                            // Copy to local data for future use
                            copyResourceToFile("/images/" + coverFileName, coverFile);
                            System.out.println("Loaded cover from resources: /images/" + coverFileName);
                        } else {
                            System.err.println("Could not find cover image: " + coverFileName);
                            cover = new ImageIcon(); // Empty icon as fallback
                        }
                    }
                }

                ArrayList<Track> tracks = new ArrayList<>();
                for (Object track : (JSONArray) cdObject.get("tracks")) {
                    JSONObject trackObject = (JSONObject) track;
                    tracks.add(new Track((String) trackObject.get("title"), (String) trackObject.get("duration")));
                }

                // Create new CD object and add to list
                System.out.println(title + " " + artist + " " + genre + " " + date + " " + cover + " " + tracks);
                CDPanel newCDPanel = new CDPanel(title, artist, genre, date, cover, tracks, this, 0);

                folderList.get(0).add(newCDPanel);
            }
            System.out.println("CDs successfully loaded.\n");
        } catch (Exception e) {
            System.err.println("Error loading CDs from database: " + e.getMessage());
        }
    }


    public void loadFolders(App app) {
        System.out.println("Loading folders from database...");
        try {
            JSONArray folderArray = getJSONArray(FOLDERS_FILE.toPath(), "folders");
            System.out.println(folderArray);

            for (Object folderObject : folderArray) {
                JSONObject folderJSONObject = (JSONObject) folderObject;

                // Extract CD information
                String name = (String) folderJSONObject.get("name");
                @SuppressWarnings("unchecked")
                int[] cdIds = ((JSONArray) folderJSONObject.get("cdIds")).stream()
                        .mapToInt(obj -> Integer.parseInt(obj.toString()))
                        .toArray();
                System.out.println("\n" + name + " " + Arrays.toString(cdIds));

                createNewFolder(name);
                app.createNewFolder(name);

                for (int i : cdIds) {
                    if (i < folderList.get(0).size()) {
                        System.out.println("Adding " + folderList.get(0).get(i).getTitle() + " to " + name + ".");
                        getCDFolder(name).add(folderList.get(0).get(i));
                    } else {
                        System.err.println("Invalid CD ID: " + i + " for folder: " + name);
                    }
                }
            }
            System.out.println("Folders successfully loaded.\n");
        } catch (Exception e) {
            System.err.println("Error loading folders from database: " + e.getMessage());
        }
    }

    public void saveToFoldersFile(int cdIndex, int folderIndex) {
        try {
            JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE.toPath());
            JSONObject folderObject = (JSONObject) ((JSONArray) folderArrayObject.get("folders")).get(folderIndex - 1);
            JSONArray cdIds = (JSONArray) folderObject.get("cdIds");

            @SuppressWarnings("unchecked")
            boolean added = cdIds.add(cdIndex);

            // Write the updated JSON back to the file
            try (FileWriter file = new FileWriter(FOLDERS_FILE)) {
                file.write(folderArrayObject.toJSONString());
                file.flush();
            }

            if (added) {
                System.out.println("CD saved to folder database successfully!");
            } else {
                System.out.println("Failed to save CD to folder database.");
            }
        } catch (IOException e) {
            System.err.println("Error saving CD to folder database: " + e.getMessage());
        }
    }

    public void addCD(String title, String artist, String genre, String date, String coverPath, ArrayList<Track> tracks, int currentTab) {
        System.out.println("Adding CD: " + title + " by " + artist + " (" + date + "), " + genre + ", Cover: " + coverPath + ", Tracks: " + tracks.size());

        // Save image to data/images
        String imageName = title.replaceAll("\\s+", "").toLowerCase() + ".jpg";
        File destImageFile = new File(IMAGES_DIR, imageName);
        try {
            Files.copy(Paths.get(coverPath), destImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error copying image file: " + e.getMessage());
        }

        // Save CD info
        try {
            JSONObject cdInfoObject = getJSONObject(CD_INFO_FILE.toPath());
            JSONArray cdsArray = (JSONArray) cdInfoObject.get("cds");

            JSONObject newCD = new JSONObject();
            newCD.put("title", title);
            newCD.put("artist", artist);
            newCD.put("genre", genre);
            newCD.put("date", date);
            newCD.put("cover", imageName);

            JSONArray tracksArray = new JSONArray();
            for (Track track : tracks) {
                JSONObject trackObject = new JSONObject();
                trackObject.put("title", track.getTitle());
                trackObject.put("duration", track.getDuration());
                tracksArray.add(trackObject);
            }
            newCD.put("tracks", tracksArray);

            cdsArray.add(newCD);

            try (FileWriter file = new FileWriter(CD_INFO_FILE)) {
                file.write(cdInfoObject.toJSONString());
                file.flush();
            }
        } catch (IOException e) {
            System.err.println("Error saving CD to cd_info.json: " + e.getMessage());
        }

        // Add cdPanel to Library
        ImageIcon cover = new ImageIcon(destImageFile.getAbsolutePath());
        CDPanel newCDPanel = new CDPanel(title, artist, genre, date, cover, tracks, this, currentTab);
        folderList.get(0).add(newCDPanel);

        // Refresh the active panel to show new CD
        refreshActivePanel(currentTab);
        newCDPanel.updateCDPanelPopupMenu(folderList, currentTab);

        System.out.println("CD added successfully: " + title);
    }

    public void createNewFolder(String name) {
        folderList.add(new CDFolder(name));
        System.out.println("Folder " + name + " successfully created.");

        for (CDPanel cdPanel : folderList.get(0)) {
            cdPanel.updateCDPanelPopupMenu(folderList, folderList.size());
        }
    }

    @SuppressWarnings("unchecked")
    public void saveNewFolder(String name) {
        try {
            JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE.toPath());
            JSONArray foldersArray = (JSONArray) folderArrayObject.get("folders");

            // Create a new folder
            JSONObject newFolder = new JSONObject();
            newFolder.put("name", name);
            newFolder.put("cdIds", new JSONArray());

            foldersArray.add(newFolder);

            // Write the updated JSON back to file
            try (FileWriter file = new FileWriter(FOLDERS_FILE)) {
                file.write(folderArrayObject.toJSONString());
                file.flush();
            }

            System.out.println("Folder saved to database successfully!");
        } catch (IOException e) {
            System.err.println("Error saving folder to database: " + e.getMessage());
        }
    }

    public CDFolder getCDFolder(int index) {
        return folderList.get(index);
    }

    public CDFolder getCDFolder(String name) {
        for (CDFolder folder : folderList) {
            if (folder.getName().equals(name)) {
                return folder;
            }
        }
        return null;
    }

    public void addToFolder(String folderName, CDPanel cdPanel) {
        for (CDFolder folder : folderList) {
            // Looks for name in folderList
            if (folder.getName().equals(folderName)) {
                if (folder.stream().noneMatch(cd -> cd.getTitle().equals(cdPanel.getTitle()))) {
                    // Adds passed cdPanel to selected folder
                    folder.add(cdPanel);
                    saveToFoldersFile(getCDFolder(0).indexOf(cdPanel), folderList.indexOf(folder));
                    break;
                } else {
                    JOptionPane.showMessageDialog(null,
                            cdPanel.getTitle() + " is already in " + folderName,
                            "Cancelled",
                            JOptionPane.WARNING_MESSAGE);
                    System.err.println("CD already exists in folder.");
                }
            }
        }
    }

    public void addToFolder(int folderIndex, CDPanel cdPanel) {
        folderList.get(folderIndex).add(cdPanel);
        saveToFoldersFile(getCDFolder(0).indexOf(cdPanel), folderIndex);
    }

    public ArrayList<CDFolder> getFolderList() {
        return folderList;
    }

    // Remove CD from entire library
    public void removeCD(CDPanel cdPanel) {
        for (int i = 1; i < folderList.size(); i++) {
            removeFromFolderFile(folderList.get(i), cdPanel);
        }

        // Remove from cd_info file
        try {
            JSONObject cdInfoObject = getJSONObject(CD_INFO_FILE.toPath());
            JSONArray cdsArray = (JSONArray) cdInfoObject.get("cds");

            for (int i = 0; i < cdsArray.size(); i++) {
                JSONObject cd = (JSONObject) cdsArray.get(i);
                if (cd.get("title").equals(cdPanel.getTitle()) && cd.get("artist").equals(cdPanel.getArtist())) {
                    cdsArray.remove(i);
                    break;
                }
            }

            try (FileWriter file = new FileWriter(CD_INFO_FILE)) {
                file.write(cdInfoObject.toJSONString());
                file.flush();
            }
        } catch (IOException e) {
            System.err.println("Error removing CD from cd_info.json: " + e.getMessage());
        }

        // Remove image from library
        String imageName = cdPanel.getTitle().replaceAll("\\s+", "").toLowerCase() + ".jpg";
        File imageFile = new File(IMAGES_DIR, imageName);
        if (imageFile.exists()) {
            if (!imageFile.delete()) {
                System.err.println("Failed to delete image file: " + imageFile.getAbsolutePath());
            }
        }

        // Remove from the Library
        folderList.get(0).remove(cdPanel);
        for (CDPanel item : folderList.get(0)) {
            System.out.println(item.getTitle());
        }
        refreshActivePanel(0);
        System.out.println("CD removed successfully: " + cdPanel.getTitle());
    }

    // Remove CD from selected folder
    public void removeCD(CDPanel cdPanel, int currentTab) {
        // Remove from all folder in list
        folderList.get(currentTab).remove(cdPanel);

        // Remove from folder in file
        removeFromFolderFile(folderList.get(currentTab), cdPanel);
    }

    public void removeFolder(CDFolder folder) {
        folderList.remove(folder);
        removeFromFolderFile(folder);
    }

    // Removes cd from a folder
    public void removeFromFolderFile(CDFolder folder, CDPanel cdPanel) {
        if (folderList.indexOf(folder) > 0) {
            try {
                // Read file
                JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE.toPath());
                JSONObject folderObject = (JSONObject) ((JSONArray) folderArrayObject.get("folders")).get(folderList.indexOf(folder) - 1);
                JSONArray cdIds = (JSONArray) folderObject.get("cdIds");

                // Remove cd
                cdIds.remove(Long.valueOf(folderList.get(0).indexOf(cdPanel)));

                // Write back to file
                try (FileWriter file = new FileWriter(FOLDERS_FILE)) {
                    file.write(folderArrayObject.toJSONString());
                    file.flush();
                }

                System.out.println("Removed " + cdPanel.getTitle() + " from " + folder.getName());

            } catch (IOException e) {
                System.err.println("Error saving folder change to database: " + e.getMessage());
            }
        }
    }

    // With no CD specified, this override removes the Folder
    public void removeFromFolderFile(CDFolder folder) {
        if (folderList.indexOf(folder) > 0) {
            try {
                JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE.toPath());
                JSONArray folders = (JSONArray) folderArrayObject.get("folders");

                folders.remove(folderList.indexOf(folder) - 1);

                try (FileWriter file = new FileWriter(FOLDERS_FILE)) {
                    file.write(folderArrayObject.toJSONString());
                    file.flush();
                }

                System.out.println("Removed folder from database: " + folder.getName());

            } catch (IOException e) {
                System.err.println("Error removing folder from database: " + e.getMessage());
            }
        }
    }

    // Reloads all CDs on the active panel
    public void refreshActivePanel(int currentTab) {
        activePanel.removeAll();
        for (CDPanel cdPanel : getCDFolder(currentTab)) {
            if (!Objects.equals(searchTextField.getText(), null) && (cdPanel.getTitle().toLowerCase().contains(searchTextField.getText().toLowerCase()) || cdPanel.getArtist().toLowerCase().contains(searchTextField.getText().toLowerCase()))) {
                activePanel.add(cdPanel);
            }
        }

        activePanel.revalidate();
        activePanel.repaint();
    }

    public void loadDataFiles() {
        System.out.println("Data directory path: " + DATA_DIR.getAbsolutePath());

        if (DATA_DIR.exists() && DATA_DIR.isDirectory()) {
            System.out.println("Data folder found - loading from external data directory.");
            loadFromExternalDirectory();
        } else {
            System.out.println("No data folder found - creating data folder and loading from resources.");
            initFromResources();
        }
    }

    private void loadFromExternalDirectory() {
        try {
            System.out.println("LOADING FROM LOCAL DATA...");
            if (!FOLDERS_FILE.exists() || !CD_INFO_FILE.exists()) {
                throw new IOException("Required files are missing in the data directory.");
            }
            // Files exist, so we can proceed with loading them
            System.out.println("Successfully loaded data from external directory.");
        } catch (IOException e) {
            System.err.println("Error loading from external data directory: " + e.getMessage());
            initFromResources();
        }
    }

    private void initFromResources() {
        try {
            System.out.println("CREATING DATA FOLDER AND LOADING FROM RESOURCES...");
            DATA_DIR.mkdirs();
            IMAGES_DIR.mkdirs();

            copyResourceToFile("/folders.json", FOLDERS_FILE);
            copyResourceToFile("/cd_info.json", CD_INFO_FILE);

            System.out.println("Successfully created data folder and loaded resources.");
        } catch (IOException e) {
            System.err.println("Error creating data folder and loading from resources: " + e.getMessage());
        }
    }

    private void copyResourceToFile(String resourcePath, File destFile) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Could not find " + resourcePath + " in resources");
            }
        }
    }

    public JSONArray getJSONArray(Path filepath, String name) {
        try (FileReader reader = new FileReader(filepath.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject jsonArrayObject = (JSONObject) parser.parse(reader);
            // Get the JSON array
            return (JSONArray) jsonArrayObject.get(name);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getJSONObject(Path filepath) {
        try (FileReader reader = new FileReader(filepath.toFile())) {
            JSONParser parser = new JSONParser();
            // Get the JSON array
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
