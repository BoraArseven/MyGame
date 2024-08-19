package com.boracompany.mygame.view;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import java.awt.Insets;
import javax.swing.JButton;

public class CreatePlayerView extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField;
	private JLabel lblDamage;
	private JLabel lblHealth;
	private JTextField DamageText;
	private JTextField HealthText;
	private JButton btnNewButton;
	private JLabel ErrorMessageLabel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CreatePlayerView frame = new CreatePlayerView();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
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
		gbl_contentPane.columnWidths = new int[]{0, 0, 0};
		gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		JLabel lblPlayername = new JLabel("Name:");
		GridBagConstraints gbc_lblPlayername = new GridBagConstraints();
		gbc_lblPlayername.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayername.anchor = GridBagConstraints.EAST;
		gbc_lblPlayername.gridx = 0;
		gbc_lblPlayername.gridy = 0;
		contentPane.add(lblPlayername, gbc_lblPlayername);
		
		textField = new JTextField();
		textField.setName("NameText");
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		contentPane.add(textField, gbc_textField);
		textField.setColumns(10);
		
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
		gbc_damageText.insets = new Insets(0, 0, 5, 0);
		gbc_damageText.fill = GridBagConstraints.HORIZONTAL;
		gbc_damageText.gridx = 1;
		gbc_damageText.gridy = 1;
		contentPane.add(DamageText, gbc_damageText);
		DamageText.setColumns(10);
		
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
		gbc_healthText.insets = new Insets(0, 0, 5, 0);
		gbc_healthText.fill = GridBagConstraints.HORIZONTAL;
		gbc_healthText.gridx = 1;
		gbc_healthText.gridy = 2;
		contentPane.add(HealthText, gbc_healthText);
		HealthText.setColumns(10);
		
		btnNewButton = new JButton("Create");
		btnNewButton.setEnabled(false);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridwidth = 2;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 3;
		contentPane.add(btnNewButton, gbc_btnNewButton);
		
		ErrorMessageLabel = new JLabel("");
		GridBagConstraints gbc_errorMessageLabel = new GridBagConstraints();
		gbc_errorMessageLabel.gridwidth = 2;
		gbc_errorMessageLabel.gridx = 0;
		gbc_errorMessageLabel.gridy = 4;
		contentPane.add(ErrorMessageLabel, gbc_errorMessageLabel);
	}

}
