package com.boracompany.mygame.view;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import com.boracompany.mygame.controller.GameController;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.annotation.Generated;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MainMenuView extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private transient GameController gameController; // Add this line

    public MainMenuView() {
        initialize();
    }

    // Setter for GameController
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
        
    }

    @Generated("Swing Designer")
    private void initialize() {
        setTitle("Main Menu");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[] { 0, 0, 0, 0, 0, 0 };
        gbl_contentPane.rowHeights = new int[] { 0, 0 };
        gbl_contentPane.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_contentPane.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        contentPane.setLayout(gbl_contentPane);

        JButton btnCreateMap = new JButton("Create Map");
        btnCreateMap.setName("Create Map");
        btnCreateMap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CreateMapView createMapView = new CreateMapView();
                createMapView.setGameController(gameController); // Pass the GameController
                createMapView.setVisible(true);
                dispose(); // Close the Main Menu window
            }
        });
        GridBagConstraints gbc_btnCreateMap = new GridBagConstraints();
        gbc_btnCreateMap.gridwidth = 2;
        gbc_btnCreateMap.insets = new Insets(0, 0, 0, 5);
        gbc_btnCreateMap.gridx = 0;
        gbc_btnCreateMap.gridy = 0;
        contentPane.add(btnCreateMap, gbc_btnCreateMap);

        JButton btnCreatePlayer = new JButton("Create Player");
        btnCreatePlayer.setName("Create Player");
        btnCreatePlayer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateToCreatePlayerView();
            }

            private void navigateToCreatePlayerView() {
                CreatePlayerView createPlayerView = new CreatePlayerView();
                createPlayerView.setGameController(gameController); // Pass the GameController
                createPlayerView.setVisible(true);
                dispose(); // Close the Main Menu window
            }
        });
        GridBagConstraints gbc_btnCreatePlayer = new GridBagConstraints();
        gbc_btnCreatePlayer.insets = new Insets(0, 0, 0, 5);
        gbc_btnCreatePlayer.gridx = 2;
        gbc_btnCreatePlayer.gridy = 0;
        contentPane.add(btnCreatePlayer, gbc_btnCreatePlayer);

        JButton btnAddPlayersTo = new JButton("Add Players to Maps");
        btnAddPlayersTo.setName("Add Players to Maps");
        btnAddPlayersTo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AddPlayersToMapsView addPlayersToMapsView = new AddPlayersToMapsView();
                addPlayersToMapsView.setGameController(gameController); // Pass the GameController
                addPlayersToMapsView.setVisible(true);
                dispose(); // Close the Main Menu window
            }
        });
        GridBagConstraints gbc_btnAddPlayersTo = new GridBagConstraints();
        gbc_btnAddPlayersTo.insets = new Insets(0, 0, 0, 5);
        gbc_btnAddPlayersTo.gridx = 3;
        gbc_btnAddPlayersTo.gridy = 0;
        contentPane.add(btnAddPlayersTo, gbc_btnAddPlayersTo);

        JButton btnPlay = new JButton("Play");
        btnPlay.setName("Play");
        btnPlay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateToPlayerAttackView();
            }

            private void navigateToPlayerAttackView() {
                PlayerAttackView playerAttackView = new PlayerAttackView();
                playerAttackView.setGameController(gameController); // Pass the GameController
                playerAttackView.setVisible(true);
                dispose(); // Close the Main Menu window
            }
        });
        GridBagConstraints gbc_btnPlay = new GridBagConstraints();
        gbc_btnPlay.gridx = 4;
        gbc_btnPlay.gridy = 0;
        contentPane.add(btnPlay, gbc_btnPlay);
    }
}
