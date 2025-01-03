package main.gui;

import main.manager.CDFolder;
import main.manager.CDManager;
import main.manager.Track;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

// amazonq-ignore-next-line
public class CDPanel extends JPanel {
    private final String title;
    private final String artist;
    private final String genre;
    private final String date;
    private final ImageIcon cover;
    private final ArrayList<Track> tracks;
    private final CDManager cdManager;

    private JPopupMenu popupMenu;

    public CDPanel(String title, String artist, String genre, String date, ImageIcon cover, ArrayList<Track> tracks, CDManager cdManager, int currentTab) {
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.date = date;
        this.cover = cover;
        this.tracks = tracks;
        this.cdManager = cdManager;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        this.setPreferredSize(new Dimension(110, 135));

        JLabel coverLabel = new JLabel(new ImageIcon(this.cover
                .getImage()
                .getScaledInstance(96, 96, Image.SCALE_SMOOTH)));
        this.add(coverLabel, BorderLayout.NORTH);

        String titleText = this.title + " (" + this.date.substring(0, Math.min(this.date.length(), 4)) + ")";
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, titleLabel.getFont().getSize() - 1));
        this.setToolTipText(titleText + " - " + this.artist);

        this.add(titleLabel, BorderLayout.CENTER);
        JLabel artistLabel = new JLabel(this.artist);
        artistLabel.setFont(new Font(artistLabel.getFont().getName(), Font.ITALIC, artistLabel.getFont().getSize() - 1));
        this.add(artistLabel, BorderLayout.SOUTH);

        // Add mouse listeners
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // TODO fix cover image positioning

                if (e.getButton() == MouseEvent.BUTTON1) {
                    Dialog dialog = new JDialog();
                    dialog.setTitle(title);
                    dialog.setLayout(new BorderLayout(10, 10));

                    JSplitPane mainPanel = new JSplitPane();
                    mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
                    mainPanel.setPreferredSize(new Dimension(500, 400));
                    mainPanel.setDividerLocation(350);
                    mainPanel.setResizeWeight(1);
                    mainPanel.setDividerSize(2);
                    mainPanel.setContinuousLayout(true);

                    // Right side
                    JPanel albumInfo = new JPanel();
                    albumInfo.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                    albumInfo.setLayout(new BoxLayout(albumInfo, BoxLayout.Y_AXIS));
                    albumInfo.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
                    albumInfo.setAlignmentY(Component.TOP_ALIGNMENT);

                    ScaledImageLabel coverImage = new ScaledImageLabel(cover);
                    coverImage.setAlignmentY(Component.TOP_ALIGNMENT);
                    coverImage.setAlignmentX(Component.CENTER_ALIGNMENT);
                    coverImage.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                            BorderFactory.createEmptyBorder(0, 0, 1, 0)
                    ));
                    coverImage.setMinimumSize(new Dimension(135, 135));

                    coverImage.setMaximumSize(new Dimension(300, 300));
                    albumInfo.add(coverImage);
                    albumInfo.add(Box.createRigidArea(new Dimension(0, 10)));

                    JTable infoTable = getjTable();
                    infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
                    infoTable.getColumnModel().getColumn(0).setMaxWidth(50);
                    infoTable.setGridColor(UIManager.getColor("Panel.background"));

                    albumInfo.add(infoTable);
                    albumInfo.add(Box.createRigidArea(new Dimension(0, 10)));


                    // Left side
                    JPanel trackList = new JPanel();
                    trackList.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                    trackList.setLayout(new GridLayout());

                    JTable trackTable = new JTable() {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }

                        @Override
                        public boolean isFocusable() {
                            return false;
                        }

                        @Override
                        public boolean isRowSelected(int row) {
                            return false;
                        }

                        @Override
                        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                            Component comp = super.prepareRenderer(renderer, row, column);
                            if (!comp.getBackground().equals(getSelectionBackground())) {
                                Color alternateColor = new Color(240, 240, 240); // Light gray
                                comp.setBackground(row % 2 == 0 ? getBackground() : alternateColor);
                            }
                            return comp;
                        }
                    };
                    DefaultTableModel model = new DefaultTableModel();
                    model.addColumn("Title");
                    model.addColumn("Duration");
                    model.addRow(new Object[]{"<html><b>Title</b></html>", "<html><b>Duration</b></html>"});

                    for (Track track : tracks) {
                        model.addRow(new Object[]{track.getTitle(), track.getDuration()});
                    }

                    trackTable.setModel(model);
                    trackTable.setShowGrid(false);
                    trackTable.setIntercellSpacing(new Dimension(0, 0));
                    trackTable.getColumnModel().getColumn(0).setMinWidth(70);
                    trackTable.getColumnModel().getColumn(1).setMinWidth(70);
                    trackTable.getColumnModel().getColumn(1).setMaxWidth(70);

                    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
                    rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
                    trackTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);

                    trackList.add(trackTable);

                    mainPanel.setLeftComponent(trackList);
                    mainPanel.setRightComponent(albumInfo);

                    dialog.add(mainPanel, BorderLayout.CENTER);

                    // Size and position the dialog
                    dialog.pack();
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);

                } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                    // Show the popup menu at the mouse location
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            private JTable getjTable() {
                Object[][] data = {
                        {"Title", title},
                        {"Artist", artist},
                        {"Genre", genre},
                        {"Date", date}
                };
                String[] columnNames = {"", ""};
                return new JTable(data, columnNames) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }

                    @Override
                    public boolean isFocusable() {
                        return false;
                    }

                    @Override
                    public boolean isRowSelected(int row) {
                        return false;
                    }
                };
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                CDPanel.this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(BevelBorder.RAISED),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CDPanel.this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(BevelBorder.LOWERED),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
            }
        });
    }

    public String getTitle() {
        return title;
    }

    public void updateCDPanelPopupMenu(ArrayList<CDFolder> collectionList, int currentTab) {
        popupMenu = new JPopupMenu();

        // Favorite option
        if (currentTab != 1) {
            if (cdManager.getCDFolder(1).contains(this)) {
                JMenuItem unfavoriteItem = new JMenuItem("Unfavorite");
                unfavoriteItem.addActionListener(e1 -> {
                        cdManager.removeCD(this, 1);
                        cdManager.refreshActivePanel(currentTab);
                });
                popupMenu.add(unfavoriteItem);
            } else {
                JMenuItem favoriteItem = new JMenuItem("Favorite");
                favoriteItem.addActionListener(e1 ->
                        cdManager.addToFolder(1, this)
                );
                popupMenu.add(favoriteItem);
            }
        }

        // Add to collection submenu
        if (collectionList.size() > 2) {
            JMenu dynamicItems = new JMenu("Add to collection");
            for (int i = 2; i < collectionList.size(); i++) {
                JMenuItem collectionItem = new JMenuItem(collectionList.get(i).getName());

                if (collectionList.get(i).contains(this)) {
                    collectionItem.setForeground(Color.gray);
                    collectionItem.setEnabled(false);
                } else {
                    final int finalI = i;
                    collectionItem.addActionListener(e1 -> cdManager.addToFolder(collectionList.get(finalI).getName(), this));
                }
                dynamicItems.add(collectionItem);
            }
            popupMenu.add(dynamicItems);
        }


        // Remove Option
        String deleteOrRemove = currentTab == 0 ? "Delete" : "Remove";
        JMenuItem removeItem = new JMenuItem("<html><font color='#8B0000'>" + deleteOrRemove + "</font></html>");
        removeItem.addActionListener(e1 -> {
            if (currentTab == 0) {
                final String message = "Are you sure you want to delete " + title + " from your library?";
                final String title = "Confirm Deletion";
                int response = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    cdManager.removeCD(this);
                }
            } else {
                cdManager.removeCD(this, currentTab);
                cdManager.refreshActivePanel(currentTab);
            }
        });

        // Add all items to popup menu
        popupMenu.addSeparator();  // Adds a separation line
        popupMenu.add(removeItem);
    }
}
