package com.boracompany.mygame.view;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;
import com.boracompany.mygame.model.Player;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import java.awt.Insets;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class CreatePlayerView extends JFrame implements PlayerView {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField NameText;
	private JLabel lblDamage;
	private JLabel lblHealth;
	private JTextField DamageText;
	private JTextField HealthText;
	private JButton createPlayerButton;
	private JLabel ErrorMessageLabel;
	private JButton mainMenuButton;
	private JLabel lblPlayerlist;

	private JScrollPane scrollPane;
	private JButton DeleteButton;

	private GameController gameController;

	private JList<Player> list;
	private DefaultListModel<Player> listPlayersModel;

	DefaultListModel<Player> getListPlayersModel() {
		return listPlayersModel;
	}

	/**
	 * Launch the application.
	 */
	@ExcludeFromJacocoGeneratedReport
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CreatePlayerView frame = new CreatePlayerView();
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
	public CreatePlayerView() {

		setTitle("Create Player");
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
				createPlayerButton.setEnabled(!NameText.getText().trim().isEmpty()
						&& !DamageText.getText().trim().isEmpty() && !HealthText.getText().trim().isEmpty());
			}
		};

		NameText = new JTextField();
		NameText.addKeyListener(btnAddEnabler);

		NameText.setName("NameText");
		GridBagConstraints gbc_nameText = new GridBagConstraints();
		gbc_nameText.insets = new Insets(0, 0, 5, 5);
		gbc_nameText.fill = GridBagConstraints.HORIZONTAL;
		gbc_nameText.gridx = 1;
		gbc_nameText.gridy = 0;
		contentPane.add(NameText, gbc_nameText);
		NameText.setColumns(10);

		lblDamage = new JLabel("Damage:");
		GridBagConstraints gbc_lblDamage = new GridBagConstraints();
		gbc_lblDamage.anchor = GridBagConstraints.EAST;
		gbc_lblDamage.insets = new Insets(0, 0, 5, 5);
		gbc_lblDamage.gridx = 0;
		gbc_lblDamage.gridy = 1;
		contentPane.add(lblDamage, gbc_lblDamage);

		DamageText = new JTextField();
		DamageText.setName("DamageText");
		GridBagConstraints gbc_damageText = new GridBagConstraints();
		gbc_damageText.insets = new Insets(0, 0, 5, 5);
		gbc_damageText.fill = GridBagConstraints.HORIZONTAL;
		gbc_damageText.gridx = 1;
		gbc_damageText.gridy = 1;
		contentPane.add(DamageText, gbc_damageText);
		DamageText.setColumns(10);

		DamageText.addKeyListener(btnAddEnabler);

		lblHealth = new JLabel("Health:");
		GridBagConstraints gbc_lblHealth = new GridBagConstraints();
		gbc_lblHealth.anchor = GridBagConstraints.EAST;
		gbc_lblHealth.insets = new Insets(0, 0, 5, 5);
		gbc_lblHealth.gridx = 0;
		gbc_lblHealth.gridy = 2;
		contentPane.add(lblHealth, gbc_lblHealth);

		HealthText = new JTextField();
		HealthText.setName("HealthText");
		GridBagConstraints gbc_healthText = new GridBagConstraints();
		gbc_healthText.insets = new Insets(0, 0, 5, 5);
		gbc_healthText.fill = GridBagConstraints.HORIZONTAL;
		gbc_healthText.gridx = 1;
		gbc_healthText.gridy = 2;
		contentPane.add(HealthText, gbc_healthText);
		HealthText.setColumns(10);
		HealthText.addKeyListener(btnAddEnabler);
		createPlayerButton = new JButton("Create");
		createPlayerButton.setName("CreatePlayerButton");
		createPlayerButton.setEnabled(false);
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
				DeleteButton.setEnabled(list.getSelectedIndex() != -1);
			}
		});
		list.setName("ListPlayers");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setColumnHeaderView(list);

		mainMenuButton = new JButton("MainMenu");
		mainMenuButton.setName("MainMenuButton");
		mainMenuButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		GridBagConstraints gbc_mainMenuButton = new GridBagConstraints();
		gbc_mainMenuButton.insets = new Insets(0, 0, 5, 5);
		gbc_mainMenuButton.gridx = 0;
		gbc_mainMenuButton.gridy = 7;
		contentPane.add(mainMenuButton, gbc_mainMenuButton);

		DeleteButton = new JButton("Delete Selected");
		DeleteButton.setName("DeleteButton");
		DeleteButton.setEnabled(false);
		DeleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		GridBagConstraints gbc_deleteButton = new GridBagConstraints();
		gbc_deleteButton.insets = new Insets(0, 0, 5, 5);
		gbc_deleteButton.gridx = 1;
		gbc_deleteButton.gridy = 7;
		contentPane.add(DeleteButton, gbc_deleteButton);

		ErrorMessageLabel = new JLabel("");
		ErrorMessageLabel.setName("ErrorMessageLabel");
		GridBagConstraints gbc_errorMessageLabel = new GridBagConstraints();
		gbc_errorMessageLabel.gridwidth = 2;
		gbc_errorMessageLabel.gridx = 1;
		gbc_errorMessageLabel.gridy = 8;
		contentPane.add(ErrorMessageLabel, gbc_errorMessageLabel);
	}

	@Override
	public void showError(String errorMessage, Player player) {
		// TODO Auto-generated method stub
		ErrorMessageLabel.setText(errorMessage + ": " + player);
	}

	@Override
	public void showAllPlayers(List<Player> players) {
		players.stream().forEach(listPlayersModel::addElement);

	}

	@Override
	public void playerAdded(Player player) {
		listPlayersModel.addElement(player);
		resetErrorLabel();
		gameController.createPlayer(player.getName(), player.getHealth(), player.getDamage());
	}

	private void resetErrorLabel() {
		ErrorMessageLabel.setText(" ");
	}

	@Override
	public void playerRemoved(Player player) {
		// TODO Auto-generated method stub
		listPlayersModel.removeElement(player);
		resetErrorLabel();
	}

	public void setSchoolController(GameController gameController) {
		this.gameController = gameController;
	}

}
