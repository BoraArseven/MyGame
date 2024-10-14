package com.boracompany.mygame.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

public class AddPlayersToMapsView extends JFrame {

	private static final Logger LOGGER = LogManager.getLogger(AddPlayersToMapsView.class);
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JList<GameMap> mapList;
	private JList<Player> playerList;
	private JButton btnAddSelectedPlayer;

	private JLabel errorLabel;

	private DefaultListModel<GameMap> mapListModel;
	private DefaultListModel<Player> playerListModel;

	private GameController gameController; // Add a reference to GameController

	/**
	 * Launch the application.
	 */
	@ExcludeFromJacocoGeneratedReport
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AddPlayersToMapsView frame = new AddPlayersToMapsView();
					frame.setVisible(true);
				} catch (Exception e) {
					throw e;
				}
			}
		});
	}

	public GameController getGameController() {
		return gameController;
	}

	public void setGameController(GameController gameController) {
		this.gameController = gameController;
	}

	/**
	 * Create the frame.
	 */
	public AddPlayersToMapsView() { // Pass GameController to constructor

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblMaps = new JLabel("Maps:");
		GridBagConstraints gbc_lblMaps = new GridBagConstraints();
		gbc_lblMaps.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaps.gridx = 0;
		gbc_lblMaps.gridy = 1;
		contentPane.add(lblMaps, gbc_lblMaps);

		JLabel lblPlayers = new JLabel("Players:");
		GridBagConstraints gbc_lblPlayers = new GridBagConstraints();
		gbc_lblPlayers.insets = new Insets(0, 0, 5, 0);
		gbc_lblPlayers.gridx = 1;
		gbc_lblPlayers.gridy = 1;
		contentPane.add(lblPlayers, gbc_lblPlayers);

		mapListModel = new DefaultListModel<>();
		mapList = new JList<>(mapListModel);
		mapList.setName("mapList");
		mapList.setPreferredSize(new Dimension(200, 150)); // Set the size of the lists
		GridBagConstraints gbc_mapList = new GridBagConstraints();
		gbc_mapList.insets = new Insets(0, 0, 5, 5);
		gbc_mapList.fill = GridBagConstraints.BOTH;
		gbc_mapList.gridx = 0;
		gbc_mapList.gridy = 2;
		contentPane.add(mapList, gbc_mapList);

		playerListModel = new DefaultListModel<>();
		playerList = new JList<>(playerListModel);
		playerList.setName("playerList");
		playerList.setPreferredSize(new Dimension(200, 150)); // Set the size of the lists
		GridBagConstraints gbc_playerList = new GridBagConstraints();
		gbc_playerList.insets = new Insets(0, 0, 5, 0);
		gbc_playerList.fill = GridBagConstraints.BOTH;
		gbc_playerList.gridx = 1;
		gbc_playerList.gridy = 2;
		contentPane.add(playerList, gbc_playerList);

		JButton btnMainMenu = new JButton("Main Menu");
		GridBagConstraints gbc_btnMainMenu = new GridBagConstraints();
		gbc_btnMainMenu.insets = new Insets(0, 0, 5, 5);
		gbc_btnMainMenu.gridx = 0;
		gbc_btnMainMenu.gridy = 3;
		contentPane.add(btnMainMenu, gbc_btnMainMenu);

		btnAddSelectedPlayer = new JButton("Add Selected Player to Map");
		btnAddSelectedPlayer.setName("Add Selected Player to Map");
		btnAddSelectedPlayer.setEnabled(false); // Initially disabled
		GridBagConstraints gbc_btnAddSelectedPlayer = new GridBagConstraints();
		gbc_btnAddSelectedPlayer.insets = new Insets(0, 0, 5, 0);
		gbc_btnAddSelectedPlayer.gridx = 1;
		gbc_btnAddSelectedPlayer.gridy = 3;
		contentPane.add(btnAddSelectedPlayer, gbc_btnAddSelectedPlayer);

		JLabel errorLabel = new JLabel("");
		errorLabel.setName("errorLabel");
		errorLabel.setForeground(Color.RED);
		this.errorLabel = errorLabel;
		GridBagConstraints gbc_errorLabel = new GridBagConstraints();
		gbc_errorLabel.gridx = 1;
		gbc_errorLabel.gridy = 4;
		contentPane.add(errorLabel, gbc_errorLabel);

		// Add selection listeners for map and player lists
		mapList.addListSelectionListener(e -> updateButtonState());
		playerList.addListSelectionListener(e -> updateButtonState());

		// Add action listener for adding the player to the map
		btnAddSelectedPlayer.addActionListener(e -> {
			// Get selected map and player
			addSelectedPlayerToMap();
		});
	}

	// Method to update the state of the button
	private void updateButtonState() {
		boolean isMapSelected = !mapList.isSelectionEmpty();
		boolean isPlayerSelected = !playerList.isSelectionEmpty();
		btnAddSelectedPlayer.setEnabled(isMapSelected && isPlayerSelected);
	}

	// Method to add the selected player to the selected map using GameController
	protected void addSelectedPlayerToMap() {

		GameMap selectedMap = mapList.getSelectedValue();
		Player selectedPlayer = playerList.getSelectedValue();

		if (selectedMap != null && selectedPlayer != null) {
			try {
				// Add player to map via the controller
				gameController.addPlayerToMap(selectedMap.getId(), selectedPlayer);

				// Log success
				LOGGER.info("Player {} added to map {}", selectedPlayer.getName(), selectedMap.getName());

				// Clear error message
				errorLabel.setText("");

				// Refresh the view after successful addition
				refreshMapList();
				refreshPlayerList();

			} catch (Exception e) {
				// Show error message if something goes wrong
				errorLabel.setText("Failed to add player to map: " + selectedMap.getName());
				
				LOGGER.error("Failed to add player to map: " + selectedMap.getName() + e.getStackTrace(), e);
			}
		}
	}

	// Method to refresh the map list using the GameController (clear and repopulate)
	protected void refreshMapList() {
	    DefaultListModel<GameMap> mapListModel = getMapListModel();
	    
	    try {
	        // Clear the list model before repopulating
	        mapListModel.clear();
	        
	        // Fetch all maps using the GameController and repopulate the list
	        List<GameMap> allMaps = gameController.getAllMaps();
	        for (GameMap map : allMaps) {
	            mapListModel.addElement(map);
	        }
	    } catch (Exception e) {
	        LOGGER.error("Failed to refresh map list", e);
	    }
	}

	// Method to refresh the player list using the GameController (clear and repopulate)
	protected void refreshPlayerList() {
	    DefaultListModel<Player> playerListModel = getPlayerListModel();
	    
	    try {
	        // Clear the list model before repopulating
	        playerListModel.clear();
	        
	        // Fetch all players using the GameController and repopulate the list
	        List<Player> allPlayers = gameController.getAllPlayers();
	        for (Player player : allPlayers) {
	            playerListModel.addElement(player);
	        }
	    } catch (Exception e) {
	        LOGGER.error("Failed to refresh player list", e);
	    }
	}


	public DefaultListModel<GameMap> getMapListModel() {
		return mapListModel;
	}

	public DefaultListModel<Player> getPlayerListModel() {
		return playerListModel;
	}

}
