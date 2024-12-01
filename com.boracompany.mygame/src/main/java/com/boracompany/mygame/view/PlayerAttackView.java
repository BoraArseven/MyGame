package com.boracompany.mygame.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Generated;
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

public class PlayerAttackView extends JFrame {

	private static final Logger LOGGER = LogManager.getLogger(PlayerAttackView.class);

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JList<GameMap> mapList;
	private JList<Player> attackerList;
	private JList<Player> defenderList;
	private JButton btnAttack;
	private JButton btnMainMenu;
	private JLabel errorLabel;

	private DefaultListModel<GameMap> mapListModel;
	private DefaultListModel<Player> attackerListModel;
	private DefaultListModel<Player> defenderListModel;

	private transient GameController gameController;

	public void setGameController(GameController gameController) {
		this.gameController = gameController;
		// Re-attach the action listener to ensure it uses the updated gameController

		refreshMapList();
		refreshPlayerLists();
	}

	/**
	 * Create the frame.
	 */
	@Generated("Swing Designer")
	public PlayerAttackView() {
		extracted();

		// Map List at the top
		JLabel lblMaps = new JLabel("Select Map:");
		GridBagConstraints gbc_lblMaps = new GridBagConstraints();
		gbc_lblMaps.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaps.gridx = 0;
		gbc_lblMaps.gridy = 0;
		contentPane.add(lblMaps, gbc_lblMaps);

		mapListModel = new DefaultListModel<>();
		mapList = new JList<>(mapListModel);
		mapList.setName("mapList");
		mapList.setPreferredSize(new Dimension(200, 100)); // Set a fixed size
		GridBagConstraints gbc_mapList = new GridBagConstraints();
		gbc_mapList.insets = new Insets(0, 0, 5, 5);
		gbc_mapList.fill = GridBagConstraints.BOTH; // Make sure it fills the space
		gbc_mapList.gridx = 1;
		gbc_mapList.gridy = 0;
		gbc_mapList.gridwidth = 2; // Span across two columns to ensure visibility
		contentPane.add(mapList, gbc_mapList);

		// Attacker and Defender labels
		JLabel lblAttacker = new JLabel("Attacker:");
		GridBagConstraints gbc_lblAttacker = new GridBagConstraints();
		gbc_lblAttacker.insets = new Insets(0, 0, 5, 5);
		gbc_lblAttacker.gridx = 0;
		gbc_lblAttacker.gridy = 1;
		contentPane.add(lblAttacker, gbc_lblAttacker);

		JLabel lblDefender = new JLabel("Defender:");
		GridBagConstraints gbc_lblDefender = new GridBagConstraints();
		gbc_lblDefender.insets = new Insets(0, 0, 5, 0);
		gbc_lblDefender.gridx = 1;
		gbc_lblDefender.gridy = 1;
		contentPane.add(lblDefender, gbc_lblDefender);

		// Attacker List
		attackerListModel = new DefaultListModel<>();
		attackerList = new JList<>(attackerListModel);
		attackerList.setName("attackerList");
		attackerList.setPreferredSize(new Dimension(200, 150)); // Set size
		GridBagConstraints gbc_attackerList = new GridBagConstraints();
		gbc_attackerList.insets = new Insets(0, 0, 5, 5);
		gbc_attackerList.fill = GridBagConstraints.BOTH;
		gbc_attackerList.gridx = 0;
		gbc_attackerList.gridy = 2;
		contentPane.add(attackerList, gbc_attackerList);

		// Defender List
		defenderListModel = new DefaultListModel<>();
		defenderList = new JList<>(defenderListModel);
		defenderList.setName("defenderList");
		defenderList.setPreferredSize(new Dimension(200, 150)); // Set size
		GridBagConstraints gbc_defenderList = new GridBagConstraints();
		gbc_defenderList.insets = new Insets(0, 0, 5, 0);
		gbc_defenderList.fill = GridBagConstraints.BOTH;
		gbc_defenderList.gridx = 1;
		gbc_defenderList.gridy = 2;
		contentPane.add(defenderList, gbc_defenderList);

		// Main Menu Button
		btnMainMenu = new JButton("Main Menu");
		btnMainMenu.setName("Main Menu");
		GridBagConstraints gbc_btnMainMenu = new GridBagConstraints();
		gbc_btnMainMenu.insets = new Insets(0, 0, 5, 5);
		gbc_btnMainMenu.gridx = 0;
		gbc_btnMainMenu.gridy = 3;
		contentPane.add(btnMainMenu, gbc_btnMainMenu);

		// Attack button
		btnAttack = new JButton("Attack");
		btnAttack.setName("btnAttack");
		btnAttack.setEnabled(false); // Initially disabled
		GridBagConstraints gbc_btnAttack = new GridBagConstraints();
		gbc_btnAttack.insets = new Insets(0, 0, 5, 0);
		gbc_btnAttack.gridx = 1;
		gbc_btnAttack.gridy = 3;
		
		btnAttack.addActionListener(e -> 
		attackSelectedPlayers());
		contentPane.add(btnAttack, gbc_btnAttack);

		// Error label
		errorLabel = new JLabel("");
		errorLabel.setName("errorLabel");
		errorLabel.setForeground(Color.RED);
		GridBagConstraints gbc_errorLabel = new GridBagConstraints();
		gbc_errorLabel.gridx = 1;
		gbc_errorLabel.gridy = 4;
		contentPane.add(errorLabel, gbc_errorLabel);

		// Add Listeners
		mapList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) { // Ensures that the method is only called once after selection is finalized
				refreshPlayerLists();
			}
		});
		attackerList.addListSelectionListener(e -> updateButtonState());
		defenderList.addListSelectionListener(e -> updateButtonState());

		// Main Menu Button Action
		btnMainMenu.addActionListener(e -> navigateToMainMenu());

	}

	@Generated("Swing Designer")
	private void extracted() {
		setTitle("Player Attack");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0, 1.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);
	}

	// Method to handle main menu navigation
	@ExcludeFromJacocoGeneratedReport
	protected void navigateToMainMenu() {
		// Create an instance of MainMenuView and make it visible
		MainMenuView mainMenuView = new MainMenuView();
		mainMenuView.setGameController(gameController);
		mainMenuView.setVisible(true);

		// Dispose of the current CreatePlayerView window
		dispose();
	}

	// Refresh the player lists when a map is selected
	protected void refreshPlayerLists() {
		GameMap selectedMap = mapList.getSelectedValue();

		if (selectedMap != null) {
			// Create a mutable copy of the list
			List<Player> livingPlayers = new ArrayList<>(gameController.getPlayersFromMap(selectedMap.getId()));

			// Preserve the currently selected attacker and defender
			Player selectedAttacker = attackerList.getSelectedValue();
			Player selectedDefender = defenderList.getSelectedValue();

			// Sort the list of players to maintain consistent order
			livingPlayers.sort(Comparator.comparing(Player::getName));

			attackerListModel.clear();
			defenderListModel.clear();

			for (Player player : livingPlayers) {
				attackerListModel.addElement(player);
				defenderListModel.addElement(player);
			}

			// Reselect the previously selected attacker and defender, if they still exist
			// in the list
			if (selectedAttacker != null && livingPlayers.contains(selectedAttacker)) {
				attackerList.setSelectedValue(selectedAttacker, true);
			}
			if (selectedDefender != null && livingPlayers.contains(selectedDefender)) {
				defenderList.setSelectedValue(selectedDefender, true);
			}

			// Update the state of the attack button
			updateButtonState();
		}
	}

	// Method to update the state of the attack button
	void updateButtonState() {
		boolean isAttackerSelected = !attackerList.isSelectionEmpty();
		boolean isDefenderSelected = !defenderList.isSelectionEmpty();
		btnAttack.setEnabled(isAttackerSelected && isDefenderSelected);
	}

	// Method to perform an attack between the selected attacker and defender
	protected void attackSelectedPlayers() {
		// Get the selected attacker and defender from the lists

		Player attacker = attackerList.getSelectedValue();
		Player defender = defenderList.getSelectedValue();

		// Check if both attacker and defender are selected
		if (attacker != null) {
			if (defender != null) {

				// Check if the attacker and defender are the same player
				if (attacker.equals(defender)) {
					// Prevent self-attack and show the appropriate error message
					errorLabel.setText("A player cannot attack itself.");
					LOGGER.warn("Attempted self-attack: Player {}", attacker.getName());
					return; // Exit early to prevent the attack
				}

				try {
					// Attempt to perform the attack through the game controller
					gameController.attack(attacker, defender);

					// Log the successful attack
					LOGGER.info("Player {} attacked Player {}", attacker.getName(), defender.getName());

					// Clear any existing error message
					errorLabel.setText("");

					// Refresh the player lists after the attack to reflect updated states
					refreshPlayerLists();
				} catch (Exception e) {
					// Handle any exception that occurs during the attack
					errorLabel.setText("Failed to perform attack.");
					LOGGER.error("Attack failed", e);
				}
			} else {
				errorLabel.setText("Attacker and defender must be selected.");
				LOGGER.warn("Defender not selected while attacker {} is selected.", attacker.getName());
			}
		} else {
			// Optionally, handle cases where no attacker or defender is selected
			errorLabel.setText("Attacker and defender must be selected.");
			LOGGER.warn("Attacker not selected.");
		}
	}

	public JList<Player> getAttackerList() {
		return attackerList;
	}

	@ExcludeFromJacocoGeneratedReport
	public void setAttackerList(JList<Player> attackerList) {
		this.attackerList = attackerList;
	}

	public JList<Player> getDefenderList() {
		return defenderList;
	}

	protected JButton getBtnAttack() {
		return btnAttack;
	}

	@ExcludeFromJacocoGeneratedReport
	public void setDefenderList(JList<Player> defenderList) {
		this.defenderList = defenderList;
	}

	@ExcludeFromJacocoGeneratedReport
	public void setMapList(JList<GameMap> mapList) {
		this.mapList = mapList;
	}

	public DefaultListModel<GameMap> getMapListModel() {
		return mapListModel;
	}

	public DefaultListModel<Player> getAttackerListModel() {
		return attackerListModel;
	}

	public DefaultListModel<Player> getDefenderListModel() {
		return defenderListModel;
	}

	public JList<GameMap> getMapList() {
		return mapList;
	}

	@ExcludeFromJacocoGeneratedReport
	void resetErrorLabel() {
		errorLabel.setText(""); // Clear the error label
	}

	public List<Player> getLivingPlayers() {
		return Collections.list(attackerListModel.elements());
	}

	@ExcludeFromJacocoGeneratedReport
	public JLabel getErrorLabel() {
		return errorLabel;
	}

	public void refreshMapList() {
		try {
			// Fetch the list of maps from the GameController
			List<GameMap> maps = gameController.getAllMaps();

			// Clear the current map list model
			mapListModel.clear();

			// Populate the map list model with the fetched maps
			for (GameMap map : maps) {
				mapListModel.addElement(map);
			}
		} catch (Exception e) {
			// Handle any exception that might occur
			errorLabel.setText("Failed to refresh map list.");
			LOGGER.error("Failed to refresh map list", e);
		}
	}

}
