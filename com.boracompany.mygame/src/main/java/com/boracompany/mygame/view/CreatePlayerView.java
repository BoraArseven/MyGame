package com.boracompany.mygame.view;

import java.awt.Color;
import java.awt.EventQueue;
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

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;

public class CreatePlayerView extends JFrame implements IPlayerView {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField nameText;
	private JLabel lblDamage;
	private JLabel lblHealth;
	private JTextField damageText;
	private JTextField healthText;
	private JButton createPlayerButton;
	private JLabel errorMessageLabel;
	private JButton mainMenuButton;
	private JLabel lblPlayerlist;
	private JScrollPane scrollPane;
	private JButton deleteButton;

	private transient GameController gameController;

	public void setGameController(GameController gameController) {
		this.gameController = gameController;
		refreshPlayerList();
	}

	private JList<Player> list;
	private DefaultListModel<Player> listPlayersModel;

	DefaultListModel<Player> getListPlayersModel() {
		return listPlayersModel;
	}

	/**
	 * Create the frame.
	 */
	@Generated("Swing Designer")
	public CreatePlayerView() {

		extracted();

		JLabel lblPlayername = new JLabel("Name:");
		GridBagConstraints gbc_lblPlayername = new GridBagConstraints();
		gbc_lblPlayername.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayername.anchor = GridBagConstraints.EAST;
		gbc_lblPlayername.gridx = 0;
		gbc_lblPlayername.gridy = 0;
		contentPane.add(lblPlayername, gbc_lblPlayername);

		KeyAdapter btnAddEnabler = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				createPlayerButton.setEnabled(!nameText.getText().trim().isEmpty()
						&& !damageText.getText().trim().isEmpty() && !healthText.getText().trim().isEmpty());
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

		lblDamage = new JLabel("Damage:");
		GridBagConstraints gbc_lblDamage = new GridBagConstraints();
		gbc_lblDamage.anchor = GridBagConstraints.EAST;
		gbc_lblDamage.insets = new Insets(0, 0, 5, 5);
		gbc_lblDamage.gridx = 0;
		gbc_lblDamage.gridy = 1;
		contentPane.add(lblDamage, gbc_lblDamage);

		damageText = new JTextField();
		damageText.setName("DamageText");
		GridBagConstraints gbc_damageText = new GridBagConstraints();
		gbc_damageText.insets = new Insets(0, 0, 5, 5);
		gbc_damageText.fill = GridBagConstraints.HORIZONTAL;
		gbc_damageText.gridx = 1;
		gbc_damageText.gridy = 1;
		contentPane.add(damageText, gbc_damageText);
		damageText.setColumns(10);

		damageText.addKeyListener(btnAddEnabler);

		lblHealth = new JLabel("Health:");
		GridBagConstraints gbc_lblHealth = new GridBagConstraints();
		gbc_lblHealth.anchor = GridBagConstraints.EAST;
		gbc_lblHealth.insets = new Insets(0, 0, 5, 5);
		gbc_lblHealth.gridx = 0;
		gbc_lblHealth.gridy = 2;
		contentPane.add(lblHealth, gbc_lblHealth);

		healthText = new JTextField();
		healthText.setName("HealthText");
		GridBagConstraints gbc_healthText = new GridBagConstraints();
		gbc_healthText.insets = new Insets(0, 0, 5, 5);
		gbc_healthText.fill = GridBagConstraints.HORIZONTAL;
		gbc_healthText.gridx = 1;
		gbc_healthText.gridy = 2;
		contentPane.add(healthText, gbc_healthText);
		healthText.setColumns(10);
		healthText.addKeyListener(btnAddEnabler);
		createPlayerButton = new JButton("Create");
		createPlayerButton.setName("CreatePlayerButton");
		createPlayerButton.setEnabled(false);

		createPlayerButton.addActionListener(e -> {
			try {
				// Build the player using the PlayerBuilder
				Player player = new PlayerBuilder().withName(nameText.getText())
						.withHealth(Float.parseFloat(healthText.getText()))
						.withDamage(Float.parseFloat(damageText.getText())).build();

				// Add the player to the UI list
				playerAdded(player);

				// Clear the text fields after successful player creation
				nameText.setText("");
				damageText.setText("");
				healthText.setText("");

				// Optionally disable the Create button until the fields are filled again
				createPlayerButton.setEnabled(false);

			} catch (Exception ex) {
				// Handle any exceptions (e.g., parsing or validation errors)
				showError("Failed to create player", null);
			}
		});

		GridBagConstraints gbc_createPlayerButton = new GridBagConstraints();
		gbc_createPlayerButton.insets = new Insets(0, 0, 5, 5);
		gbc_createPlayerButton.gridwidth = 2;
		gbc_createPlayerButton.gridx = 0;
		gbc_createPlayerButton.gridy = 3;
		contentPane.add(createPlayerButton, gbc_createPlayerButton);

		lblPlayerlist = new JLabel("PlayerList:");
		GridBagConstraints gbc_lblPlayerlist = new GridBagConstraints();
		gbc_lblPlayerlist.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayerlist.gridx = 0;
		gbc_lblPlayerlist.gridy = 4;
		contentPane.add(lblPlayerlist, gbc_lblPlayerlist);

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridheight = 3;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 4;
		contentPane.add(scrollPane, gbc_scrollPane);

		listPlayersModel = new DefaultListModel<>();
		list = new JList<>(listPlayersModel);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				deleteButton.setEnabled(list.getSelectedIndex() != -1);
			}
		});
		list.setName("ListPlayers");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setColumnHeaderView(list);

		mainMenuButton = new JButton("MainMenu");
		mainMenuButton.setName("MainMenuButton");
		// mainMenuButton.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// }});
		GridBagConstraints gbc_mainMenuButton = new GridBagConstraints();
		gbc_mainMenuButton.insets = new Insets(0, 0, 5, 5);
		gbc_mainMenuButton.gridx = 0;
		gbc_mainMenuButton.gridy = 7;
		contentPane.add(mainMenuButton, gbc_mainMenuButton);

		deleteButton = new JButton("Delete Selected");
		deleteButton.setName("DeleteButton");
		deleteButton.setEnabled(false);
		deleteButton.addActionListener(e -> {
			Player selectedPlayer = list.getSelectedValue();
			if (selectedPlayer != null) {
				// Call playerRemoved() if a player is selected
				playerRemoved(selectedPlayer);
			} else {
				// Show error message when no player is selected
				showError("No player selected", null);
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
		mainMenuButton.addActionListener(e -> {
			navigateToMainMenu();
		});

	}

	@Generated("Swing Designer")
	private void extracted() {
		setTitle("Create Player");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
	}

	@ExcludeFromJacocoGeneratedReport
	private void navigateToMainMenu() {
		// Create an instance of MainMenuView and make it visible
		MainMenuView mainMenuView = new MainMenuView();
		mainMenuView.setGameController(gameController);
		mainMenuView.setVisible(true);

		// Dispose of the current CreatePlayerView window
		dispose();
	}

	@Override
	public void showError(String errorMessage, Player player) {

		errorMessageLabel.setForeground(Color.RED);
		if (player != null) {

			errorMessageLabel.setText(errorMessage + ": " + player);

		} else {
			errorMessageLabel.setText(errorMessage);
		}
	}

	@Override
	public void showAllPlayers(List<Player> players) {
		players.stream().forEach(listPlayersModel::addElement);

	}

	@Override
	public void playerAdded(Player player) {
		resetErrorLabel();
		try {
			// Try to add the player to the database
			gameController.createPlayer(player.getName(), player.getHealth(), player.getDamage());

			// If the database operation is successful, add the player to the view's list
			listPlayersModel.addElement(player);
		} catch (Exception ex) {
			// If there is an error, show an error message and don't add the player to the
			// list
			showError("Failed to create player", player);
		}
	}

	private void resetErrorLabel() {
		errorMessageLabel.setText("");
	}

	@Override
	public void playerRemoved(Player player) {

		// Remove the player from the database
		try {
			gameController.deletePlayer(player.getId());
		} catch (Exception ex) {
			showError("Failed to remove player from the database: " + player.getName(), player);
			return;
		}
		resetErrorLabel();
		listPlayersModel.removeElement(player); // This should remove the player from the list model
		// Update the selection
		if (!listPlayersModel.isEmpty()) {
			list.setSelectedIndex(0); // Optionally select the first remaining player
		}
	}

	public void setSchoolController(GameController gameController) {
		this.gameController = gameController;
	}

	public void refreshPlayerList() {
		try {
			// Fetch the list of players from the GameController
			List<Player> players = gameController.getAllPlayers();

			// Ensure updates are made on the Event Dispatch Thread (EDT)
			EventQueue.invokeLater(() -> {
				// Clear the current contents of the list model
				listPlayersModel.clear();

				// Add the players to the list model
				for (Player player : players) {
					listPlayersModel.addElement(player);
				}
			});
		} catch (Exception e) {
			// Handle exceptions and optionally show an error message
			showError("Failed to refresh player list", null);
		}
	}

}
