package main.manager;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

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

    private static final String USER_HOME = System.getProperty("user.home");
    private static final Path DATA_DIR = Paths.get(USER_HOME, ".collectionmanager");
    private static final Path FOLDERS_FILE = DATA_DIR.resolve("folders.json");
    private static final Path CD_INFO_FILE = DATA_DIR.resolve("cd_info.json");

    public CDManager(JPanel activePanel) {
        folderList = new ArrayList<>();
        folderList.add(new CDFolder("Library"));
        System.out.println("CD Manager successfully created.\n");

        this.activePanel = activePanel;
    }
    public void loadCDs() {
        try {
            JSONArray cdArray = getJSONArray(CD_INFO_FILE, "cds");
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
                try (InputStream imageStream = getClass().getResourceAsStream("/images/" + coverFileName)) {
                    if (imageStream != null) {
                        cover = new ImageIcon(ImageIO.read(imageStream));
                    } else {
                        System.err.println("Could not find cover image: " + coverFileName);
                        cover = new ImageIcon(); // Empty icon as fallback
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

                addToFolder(0, newCDPanel);
            }
            System.out.println("CDs successfully loaded.\n");
        } catch (Exception e) {
            System.err.println("Error loading CDs from database: " + e.getMessage());
        }
    }


    public void loadFolders(App app) {
        System.out.println("Loading folders from database...");
        try {
            JSONArray folderArray = getJSONArray(FOLDERS_FILE, "folders");
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
            JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE);
            JSONObject folderObject = (JSONObject)((JSONArray) folderArrayObject.get("folders")).get(folderIndex-1);
            JSONArray cdIds = (JSONArray) folderObject.get("cdIds");

            @SuppressWarnings("unchecked")
            boolean added = cdIds.add(cdIndex);

            // Write the updated JSON back to the file
            try (FileWriter file = new FileWriter(FOLDERS_FILE.toFile())) {
                file.write(folderArrayObject.toJSONString());
                file.flush();
            }

            if (added) {
                System.out.println("CD saved to folder database successfully!");
            }
            else {
                System.out.println("Failed to save CD to folder database.");
            }
        } catch (IOException e) {
            System.err.println("Error saving CD to folder database: " + e.getMessage());
        }
    }

    public void addCD() {
        // TODO : Add a new CD to the list

        // Return CD Panel
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
            JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE);
            JSONArray foldersArray = (JSONArray) folderArrayObject.get("folders");

            // Create a new folder object
            JSONObject newFolder = new JSONObject();
            newFolder.put("name", name);
            newFolder.put("cdIds", new JSONArray());

            // Add the new folder to the array
            foldersArray.add(newFolder);

            // Write the updated JSON back to the file
            try (FileWriter file = new FileWriter(FOLDERS_FILE.toFile())) {
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
            if (folder.getName().equals(folderName) && folder.stream().noneMatch(cd -> cd.getTitle().equals(cdPanel.getTitle()))) {
                folder.add(cdPanel);
                saveToFoldersFile(getCDFolder(0).indexOf(cdPanel), folderList.indexOf(folder));
                break;
            }
        }
    }

    public void addToFolder(int folderIndex, CDPanel cdPanel) {
        // TODO : ADD VALIDATION
        folderList.get(folderIndex).add(cdPanel);
    }

    public ArrayList<CDFolder> getFolderList() {
        return folderList;
    }

    // Remove CD from entire library
    public void removeCD(CDPanel cdPanel) {
        for (CDFolder folder : folderList) {
            // Remove from all folders in list
            folder.remove(cdPanel);

            // Remove from all folders in file
            //removeFromFolderFile(folder, cdPanel);

            refreshActivePanel(folderList.indexOf(folder));
        }

        // Remove from cd_info file
    }
    // Remove CD from selected folder
    public void removeCD(CDPanel cdPanel, int currentTab) {
        // Remove from all folder in list
        folderList.get(currentTab).remove(cdPanel);

        // Remove from folder in file
        removeFromFolderFile(folderList.get(currentTab), cdPanel);


        refreshActivePanel(currentTab);
    }

    public void removeFromFolderFile(CDFolder folder, CDPanel cdPanel) {
        try {
            JSONObject folderArrayObject = getJSONObject(FOLDERS_FILE);
            JSONObject folderObject = (JSONObject)((JSONArray) folderArrayObject.get("folders")).get(folderList.indexOf(folder)-1);
            JSONArray cdIds = (JSONArray) folderObject.get("cdIds");
            cdIds.remove(Long.valueOf(folderList.get(0).indexOf(cdPanel)));

            try (FileWriter file = new FileWriter(FOLDERS_FILE.toFile())) {
                file.write(folderArrayObject.toJSONString());
                file.flush();
            }

            System.out.println("Removed " + cdPanel.getTitle() + " from " + folder.getName());

        } catch (IOException e) {
            System.err.println("Error saving folder change to database: " + e.getMessage());
        }
    }
    public void refreshActivePanel(int currentTab) {
        activePanel.removeAll();
        for (CDPanel cdPanel : getCDFolder(currentTab)) {
            activePanel.add(cdPanel);
        }

        activePanel.revalidate();
        activePanel.repaint();
    }

    public void loadDataFiles() {
        try {
            Files.createDirectories(DATA_DIR);

            if (!Files.exists(FOLDERS_FILE)) {
                try (InputStream in = getClass().getResourceAsStream("/folders.json")) {
                    if (in != null) {
                        Files.copy(in, FOLDERS_FILE);
                    } else {
                        System.err.println("Could not find folders.json in resources");
                    }
                }
            }

            if (!Files.exists(CD_INFO_FILE)) {
                try (InputStream in = getClass().getResourceAsStream("/cd_info.json")) {
                    if (in != null) {
                        Files.copy(in, CD_INFO_FILE);
                    } else {
                        System.err.println("Could not find cd_info.json in resources");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error initializing data files: " + e.getMessage());
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
