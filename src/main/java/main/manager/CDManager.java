package main.manager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.io.FileReader;
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
    private final ArrayList<CDCollection> collectionList;
    private final JPanel activePanel;

    private static final String USER_HOME = System.getProperty("user.home");
    private static final Path DATA_DIR = Paths.get(USER_HOME, ".collectionmanager");
    private static final Path COLLECTIONS_FILE = DATA_DIR.resolve("collections.json");
    private static final Path CD_INFO_FILE = DATA_DIR.resolve("cd_info.json");

    public CDManager(JPanel activePanel) {
        collectionList = new ArrayList<>();
        collectionList.add(new CDCollection("Library"));
        System.out.println("CD Manager successfully created.\n");

        this.activePanel = activePanel;
    }
    public void loadCDs() {
        try (FileReader reader = new FileReader(CD_INFO_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject cdArrayObject = (JSONObject) parser.parse(reader);
            JSONArray cdArray = (JSONArray) cdArrayObject.get("cds");
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

                addToCollection(0, newCDPanel);
            }
            System.out.println("CDs successfully loaded.\n");
        } catch (Exception e) {
            System.err.println("Error loading CDs from database: " + e.getMessage());
        }
    }


    public void loadCollections(App app) {
        System.out.println("Loading collections from database...");

        try (FileReader reader = new FileReader(COLLECTIONS_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject collectionArrayObject = (JSONObject) parser.parse(reader);
            JSONArray collectionArray = (JSONArray) collectionArrayObject.get("collections");
            for (Object collectionObject : collectionArray) {
                JSONObject collectionJSONObject = (JSONObject) collectionObject;

                // Extract CD information
                String name = (String) collectionJSONObject.get("name");
                @SuppressWarnings("unchecked")
                int[] cdIds = ((JSONArray) collectionJSONObject.get("cdIds")).stream()
                        .mapToInt(obj -> Integer.parseInt(obj.toString()))
                        .toArray();
                System.out.println("\n" + name + " " + Arrays.toString(cdIds));

                createNewCollection(name);
                app.createNewCollection(name);

                for (int i : cdIds) {
                    if (i < collectionList.get(0).size()) {
                        System.out.println("Adding " + collectionList.get(0).get(i).getTitle() + " to " + name + ".");
                        getCDCollection(name).add(collectionList.get(0).get(i));
                    } else {
                        System.err.println("Invalid CD ID: " + i + " for collection: " + name);
                    }
                }
            }
            System.out.println("Collections successfully loaded.\n");
        } catch (Exception e) {
            System.err.println("Error loading collections from database: " + e.getMessage());
        }
    }

    public void saveToCollectionFile(int cdIndex, int collectionIndex) {
        try (FileReader reader = new FileReader(COLLECTIONS_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject collectionArrayObject = (JSONObject) parser.parse(reader);
            // Get the collections array
            JSONArray collectionsArray = (JSONArray) collectionArrayObject.get("collections");
            JSONObject collectionObject = (JSONObject) collectionsArray.get(collectionIndex-1);
            JSONArray cdIds = (JSONArray) collectionObject.get("cdIds");

            @SuppressWarnings("unchecked")
            boolean added = cdIds.add(cdIndex);

            // Write the updated JSON back to the file
            try (FileWriter file = new FileWriter(COLLECTIONS_FILE.toFile())) {
                file.write(collectionArrayObject.toJSONString());
                file.flush();
            }

            if (added) {
                System.out.println("CD saved to collection database successfully!");
            }
            else {
                System.out.println("Failed to save CD to collection database.");
            }
        } catch (IOException e) {
            System.err.println("Error saving CD to collection database: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("Error parsing collection database: " + e.getMessage());
        }
    }

    public void addCD() {
        // TODO : Add a new CD to the list

        // Return CD Panel
    }

    public void createNewCollection(String name) {
        collectionList.add(new CDCollection(name));
        System.out.println("Collection " + name + " successfully created.\n");

        for (CDPanel cdPanel : collectionList.get(0)) {
            cdPanel.updateCDPanelPopupMenu(collectionList, collectionList.size());
        }
    }

    public CDCollection getCDCollection(int index) {
        return collectionList.get(index);
    }

    public CDCollection getCDCollection(String name) {
        for (CDCollection collection : collectionList) {
            if (collection.getName().equals(name)) {
                return collection;
            }
        }
        return null;
    }

    public void addToCollection(String collectionName, CDPanel cdPanel) {
        for (CDCollection collection : collectionList) {
            if (collection.getName().equals(collectionName) && collection.stream().noneMatch(cd -> cd.getTitle().equals(cdPanel.getTitle()))) {
                collection.add(cdPanel);
                saveToCollectionFile(getCDCollection(0).indexOf(cdPanel), collectionList.indexOf(collection));
                break;
            }
        }
    }

    public void addToCollection(int collectionIndex, CDPanel cdPanel) {
        // TODO : ADD VALIDATION
        collectionList.get(collectionIndex).add(cdPanel);
    }

    public ArrayList<CDCollection> getCollectionList() {
        return collectionList;
    }

    // Remove CD from entire library
    public void removeCD(CDPanel cdPanel) {
        for (CDCollection collection : collectionList) {
            // Remove from all collections in list
            collection.remove(cdPanel);

            // Remove from all collections in file
            //removeFromCollectionFile(collection, cdPanel);

            refreshActivePanel(collectionList.indexOf(collection));
        }

        // Remove from cd_info file
    }
    // Remove CD from selected collection
    public void removeCD(CDPanel cdPanel, int currentTab) {
        // Remove from all collection in list
        collectionList.get(currentTab).remove(cdPanel);

        // Remove from collection in file
        removeFromCollectionFile(collectionList.get(currentTab), cdPanel);


        refreshActivePanel(currentTab);
    }

    public void removeFromCollectionFile(CDCollection collection, CDPanel cdPanel) {
        try (FileReader reader = new FileReader(COLLECTIONS_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject collectionArrayObject = (JSONObject) parser.parse(reader);
            JSONArray collectionsArray = (JSONArray) collectionArrayObject.get("collections");
            JSONObject collectionObject = (JSONObject) collectionsArray.get(collectionList.indexOf(collection) - 1);
            JSONArray cdIds = (JSONArray) collectionObject.get("cdIds");
            cdIds.remove(Long.valueOf(collectionList.get(0).indexOf(cdPanel)));

            try (FileWriter file = new FileWriter(COLLECTIONS_FILE.toFile())) {
                file.write(collectionArrayObject.toJSONString());
                file.flush();
            }

            System.out.println("Removed " + cdPanel.getTitle() + " from " + collection.getName());

        } catch (IOException e) {
            System.err.println("Error saving collection change to database: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("Error parsing collection database: " + e.getMessage());
        }
    }
    public void refreshActivePanel(int currentTab) {
        activePanel.removeAll();
        for (CDPanel cdPanel : getCDCollection(currentTab)) {
            activePanel.add(cdPanel);
        }

        activePanel.revalidate();
        activePanel.repaint();
    }

    public void loadDataFiles() {
        try {
            Files.createDirectories(DATA_DIR);
            Path collectionsFile = DATA_DIR.resolve("collections.json");
            Path cdInfoFile = DATA_DIR.resolve("cd_info.json");

            if (!Files.exists(collectionsFile)) {
                try (InputStream in = getClass().getResourceAsStream("/collections.json")) {
                    if (in != null) {
                        Files.copy(in, collectionsFile);
                    } else {
                        System.err.println("Could not find collections.json in resources");
                    }
                }
            }

            if (!Files.exists(cdInfoFile)) {
                try (InputStream in = getClass().getResourceAsStream("/cd_info.json")) {
                    if (in != null) {
                        Files.copy(in, cdInfoFile);
                    } else {
                        System.err.println("Could not find cd_info.json in resources");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error initializing data files: " + e.getMessage());
        }
    }
}
