package com.boracompany.mygame.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.annotation.Generated;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.boracompany.mygame.controller.GameController;
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

	private transient GameController gameController;

	private JList<GameMap> list;
	private DefaultListModel<GameMap> listMapsModel;

	DefaultListModel<GameMap> getListMapsModel() {
		return listMapsModel;
	}

	private static final transient Logger mapViewLogger = LogManager.getLogger(CreateMapView.class);

	/**
	 * Create the frame.
	 */
	@Generated("Swing Designer")
	public CreateMapView() {

		createWindow();

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
				getCreateMapButton().setEnabled(!getNameText().getText().trim().isEmpty());
			}
		};

		setNameText(new JTextField());
		getNameText().addKeyListener(btnAddEnabler);
		getNameText().setName("NameText");
		GridBagConstraints gbc_nameText = new GridBagConstraints();
		gbc_nameText.insets = new Insets(0, 0, 5, 5);
		gbc_nameText.fill = GridBagConstraints.HORIZONTAL;
		gbc_nameText.gridx = 1;
		gbc_nameText.gridy = 0;
		contentPane.add(getNameText(), gbc_nameText);
		getNameText().setColumns(10);

		setCreateMapButton(new JButton("Create Map"));
		getCreateMapButton().setEnabled(false);
		getCreateMapButton().setName("CreateMapButton");
		createMapButton.addActionListener(e -> {
			try {
				// Validate the input
				if (nameText.getText().trim().isEmpty()) {
					throw new IllegalArgumentException("Map name cannot be empty");
				}

				// Build the map using the GameMap constructor
				String mapName = nameText.getText().trim();

				GameMap mapToCreate = new GameMap(mapName);
				// Call the GameController to create the map (persist it)
				mapAdded(mapToCreate);

				// Optionally disable the Create button until the fields are filled again
				createMapButton.setEnabled(false);

			} catch (Exception ex) {
				// Handle any exceptions (e.g., validation errors)
				showError("Failed to create map: " + nameText.getText(), null);
				mapViewLogger.error("Failed to create map", ex);
			}
		});

		GridBagConstraints gbc_createMapButton = new GridBagConstraints();
		gbc_createMapButton.insets = new Insets(0, 0, 5, 5);
		gbc_createMapButton.gridwidth = 2;
		gbc_createMapButton.gridx = 0;
		gbc_createMapButton.gridy = 2;
		contentPane.add(getCreateMapButton(), gbc_createMapButton);

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
				getDeleteButton().setEnabled(list.getSelectedIndex() != -1);
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
		// Add action listener for mainMenuButton
		mainMenuButton.addActionListener(e -> {
			navigateToMainMenu();
		});
		setDeleteButton(new JButton("Delete Selected"));
		getDeleteButton().setName("DeleteButton");
		getDeleteButton().setEnabled(false);
		getDeleteButton().addActionListener(e -> {
		    GameMap selectedMap = getListMaps().getSelectedValue();
		    try {
		        if (selectedMap != null) {
		            gameController.deleteMap(selectedMap.getId());
		            refreshMapList();
		        } else {
		            errorMessageLabel.setText("No map selected to delete.");
		        }
		    } catch (Exception ex) {
		        errorMessageLabel.setText("Failed to remove map from the database: " + selectedMap.getName());
		    }
		});


		GridBagConstraints gbc_deleteButton = new GridBagConstraints();
		gbc_deleteButton.insets = new Insets(0, 0, 5, 5);
		gbc_deleteButton.gridx = 1;
		gbc_deleteButton.gridy = 7;
		contentPane.add(getDeleteButton(), gbc_deleteButton);

		errorMessageLabel = new JLabel("");
		errorMessageLabel.setName("ErrorMessageLabel");
		GridBagConstraints gbc_errorMessageLabel = new GridBagConstraints();
		gbc_errorMessageLabel.gridwidth = 2;
		gbc_errorMessageLabel.gridx = 1;
		gbc_errorMessageLabel.gridy = 8;
		contentPane.add(errorMessageLabel, gbc_errorMessageLabel);
	}

	private void createWindow() {
		setTitle("Create Map");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gblcontentPane = new GridBagLayout();
		gblcontentPane.columnWidths = new int[] { 0, 0, 0 };
		gblcontentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gblcontentPane.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gblcontentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gblcontentPane);
	}

	private void navigateToMainMenu() {
		// Dispose of the current CreateMapView window
		dispose();

		// Open the MainMenuView
		MainMenuView mainMenu = new MainMenuView();
		mainMenu.setGameController(gameController);
		mainMenu.setVisible(true);
	}

	public void showError(String errorMessage, GameMap map) {
		errorMessageLabel.setForeground(Color.RED);
		if (map != null) {
			errorMessageLabel.setText(errorMessage + ": " + map.getName());
		} else {
			errorMessageLabel.setText(errorMessage);
		}
	}
	public JList<GameMap> getListMaps() {
	    return list;
	}

	public void showAllMaps(List<GameMap> maps) {
		maps.forEach(listMapsModel::addElement);
	}

	public void mapAdded(GameMap map) {
		resetErrorLabel();
		try {
			// Ensure the correct map name is used
			String mapName = map.getName();
			mapViewLogger.info("Map Name: {}", mapName); // Debugging step

			// Add the map to the database
			gameController.createMap(mapName, map.getPlayers());
			// Refresh the map list to include the newly created map with a valid ID
			refreshMapList();
			// Reset the text field nametext
			nameText.setText("");
		} catch (Exception ex) {
			showError("Failed to create map", map);
			mapViewLogger.error("Failed to create map", ex);
		}
	}

	public void mapRemoved(GameMap map) {
		resetErrorLabel();
		listMapsModel.removeElement(map);

		try {
			// Remove the map from the database
			gameController.deleteMap(map.getId());
		} catch (Exception ex) {
			// Fixed: map.getName() is already included in the error message, no need to
			// append it again
			showError("Failed to remove map from the database: " + map.getName(), null);
			mapViewLogger.error("Failed to remove map from the database", ex);
			return;
		}

		// Optionally select the first remaining map
		if (!listMapsModel.isEmpty()) {
			list.setSelectedIndex(0);
		}
	}

	private void resetErrorLabel() {
		errorMessageLabel.setText("");
	}

	public void setGameController(GameController gameController) {
		this.gameController = gameController;
		refreshMapList(); // Fetch and display the existing maps
	}

	public void refreshMapList() {
		try {
			// Fetch the list of maps from the GameController
			List<GameMap> maps = gameController.getAllMaps();

			// Clear the current list model
			listMapsModel.clear();

			// Add the maps to the list model
			maps.forEach(listMapsModel::addElement);
		} catch (Exception e) {
			showError("Failed to refresh map list", null);
			mapViewLogger.error("Failed to refresh map list", e);
		}
	}

	public JTextField getNameText() {
		return nameText;
	}

	public void setNameText(JTextField nameText) {
		this.nameText = nameText;
	}

	public JButton getCreateMapButton() {
		return createMapButton;
	}

	public void setCreateMapButton(JButton createMapButton) {
		this.createMapButton = createMapButton;
	}

	public JButton getDeleteButton() {
		return deleteButton;
	}

	public void setDeleteButton(JButton deleteButton) {
		this.deleteButton = deleteButton;
	}
}
