package com.boracompany.mygame.view;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;
import com.boracompany.mygame.model.GameMap;

public class CreateMapView extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField nameText;
	private JLabel lblMapList;
	private JButton createMapButton;
	private JLabel errorMessageLabel;
	private JButton mainMenuButton;

	private JScrollPane scrollPane;
	private JButton deleteButton;

	private GameController gameController;

	private JList<GameMap> list;
	private DefaultListModel<GameMap> listMapsModel;

	DefaultListModel<GameMap> getListMapsModel() {
		return listMapsModel;
	}

	/**
	 * Launch the application.
	 */
	@ExcludeFromJacocoGeneratedReport
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@ExcludeFromJacocoGeneratedReport
			public void run() {
				try {
					CreateMapView frame = new CreateMapView();
					frame.setVisible(true);
				} catch (Exception e) {
					throw e;
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CreateMapView() {

		setTitle("Create Map");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblMapName = new JLabel("Map Name:");
		GridBagConstraints gbc_lblMapName = new GridBagConstraints();
		gbc_lblMapName.insets = new Insets(0, 0, 5, 5);
		gbc_lblMapName.anchor = GridBagConstraints.EAST;
		gbc_lblMapName.gridx = 0;
		gbc_lblMapName.gridy = 0;
		contentPane.add(lblMapName, gbc_lblMapName);

		KeyAdapter btnAddEnabler = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				createMapButton.setEnabled(!nameText.getText().trim().isEmpty());
			}
		};

		nameText = new JTextField();
		nameText.addKeyListener(btnAddEnabler);
		nameText.setName("NameText");
		GridBagConstraints gbc_nameText = new GridBagConstraints();
		gbc_nameText.insets = new Insets(0, 0, 5, 5);
		gbc_nameText.fill = GridBagConstraints.HORIZONTAL;
		gbc_nameText.gridx = 1;
		gbc_nameText.gridy = 0;
		contentPane.add(nameText, gbc_nameText);
		nameText.setColumns(10);

		createMapButton = new JButton("Create Map");
		createMapButton.setEnabled(false);
		createMapButton.setName("CreateMapButton");
		createMapButton.addActionListener(e -> {
			try {
				// Build the map using the GameMap constructor
				GameMap map = new GameMap(nameText.getText());

				// Add the map to the UI list
				mapAdded(map);

				// Clear the text field after successful map creation
				nameText.setText("");

				// Optionally disable the Create button until the fields are filled again
				createMapButton.setEnabled(false);

			} catch (Exception ex) {
				// Handle any exceptions (e.g., validation errors)
				showError("Failed to create map", null);
			}
		});

		GridBagConstraints gbc_createMapButton = new GridBagConstraints();
		gbc_createMapButton.insets = new Insets(0, 0, 5, 5);
		gbc_createMapButton.gridwidth = 2;
		gbc_createMapButton.gridx = 0;
		gbc_createMapButton.gridy = 2;
		contentPane.add(createMapButton, gbc_createMapButton);

		lblMapList = new JLabel("Map List:");
		GridBagConstraints gbc_lblMapList = new GridBagConstraints();
		gbc_lblMapList.insets = new Insets(0, 0, 5, 5);
		gbc_lblMapList.gridx = 0;
		gbc_lblMapList.gridy = 3;
		contentPane.add(lblMapList, gbc_lblMapList);

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridheight = 3;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 3;
		contentPane.add(scrollPane, gbc_scrollPane);

		listMapsModel = new DefaultListModel<>();
		list = new JList<>(listMapsModel);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				deleteButton.setEnabled(list.getSelectedIndex() != -1);
			}
		});
		list.setName("ListMaps");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(list);

		mainMenuButton = new JButton("Main Menu");
		mainMenuButton.setName("MainMenuButton");
		// Add action listener for mainMenuButton
		GridBagConstraints gbc_mainMenuButton = new GridBagConstraints();
		gbc_mainMenuButton.insets = new Insets(0, 0, 5, 5);
		gbc_mainMenuButton.gridx = 0;
		gbc_mainMenuButton.gridy = 7;
		contentPane.add(mainMenuButton, gbc_mainMenuButton);

		deleteButton = new JButton("Delete Selected");
		deleteButton.setName("DeleteButton");
		deleteButton.setEnabled(false);
		deleteButton.addActionListener(e -> {
			GameMap selectedMap = list.getSelectedValue();
			if (selectedMap != null) {
				// Call mapRemoved() if a map is selected
				mapRemoved(selectedMap);
			} else {
				// Show error message when no map is selected
				showError("No map selected", null);
			}
		});

		GridBagConstraints gbc_deleteButton = new GridBagConstraints();
		gbc_deleteButton.insets = new Insets(0, 0, 5, 5);
		gbc_deleteButton.gridx = 1;
		gbc_deleteButton.gridy = 7;
		contentPane.add(deleteButton, gbc_deleteButton);

		errorMessageLabel = new JLabel("");
		errorMessageLabel.setName("ErrorMessageLabel");
		GridBagConstraints gbc_errorMessageLabel = new GridBagConstraints();
		gbc_errorMessageLabel.gridwidth = 2;
		gbc_errorMessageLabel.gridx = 1;
		gbc_errorMessageLabel.gridy = 8;
		contentPane.add(errorMessageLabel, gbc_errorMessageLabel);
	}

	public void showError(String errorMessage, GameMap map) {
		errorMessageLabel.setForeground(Color.RED);
		if (map != null) {
			errorMessageLabel.setText(errorMessage + ": " + map.getName());
		} else {
			errorMessageLabel.setText(errorMessage);
		}
	}

	public void showAllMaps(List<GameMap> maps) {
		maps.forEach(listMapsModel::addElement);
	}

	public void mapAdded(GameMap map) {
		resetErrorLabel();
		try {
			// Add the map to the database
			gameController.createMap(map.getName(), map.getPlayers());

			// If successful, add the map to the view's list
			listMapsModel.addElement(map);
		} catch (Exception ex) {
			// Handle error and do not add map to the list
			showError("Failed to create map", map);
		}
	}

	public void mapRemoved(GameMap map) {
        resetErrorLabel();
        listMapsModel.removeElement(map);

        try {
            // Remove the map from the database
            gameController.deleteMap(map.getId());
        } catch (Exception ex) {
            // Fixed: map.getName() is already included in the error message, no need to append it again
            showError("Failed to remove map from the database: " + map.getName(), null);
            return;
        }
    

	// Optionally select the first remaining map
	if(!listMapsModel.isEmpty())

	{
		list.setSelectedIndex(0);
	}
	}

	private void resetErrorLabel() {
        errorMessageLabel.setText("");
    }

	public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

	public void refreshMapList() {
        try {
            // Fetch the list of maps from the GameController
            List<GameMap> maps = gameController.getAllMaps();

            EventQueue.invokeLater(() -> {
                // Clear the current list model
                listMapsModel.clear();

                // Add the maps to the list model
                maps.forEach(listMapsModel::addElement);
            });
        } catch (Exception e) {
            showError("Failed to refresh map list", null);
        }
    }
}
