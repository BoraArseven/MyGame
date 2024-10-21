package com.boracompany.mygame.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;
import com.boracompany.mygame.view.MainMenuView;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.persistence.EntityManagerFactory;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("App started");

        // Set default database properties
        String dbUrl = "jdbc:postgresql://localhost:5432/bora"; // Use localhost since DB is in Docker container
        String dbUser = null;
        String dbPassword = null;

        try {
            // Read the database user from a local file (e.g., "postgres_user.txt")
            dbUser = new String(Files.readAllBytes(Paths.get("postgres_user.txt"))).trim();
            // Read the database password from a local file (e.g., "postgres_password.txt")
            dbPassword = new String(Files.readAllBytes(Paths.get("postgres_password.txt"))).trim();
        } catch (IOException e) {
            LOGGER.error("Failed to read database credentials from files", e);
            return; // Exit the application if any secret cannot be read
        }

        LOGGER.info("DB URL: " + dbUrl);
        LOGGER.info("DB User: " + (dbUser != null ? dbUser : "Not provided"));
        LOGGER.info("DB Password Read: " + (dbPassword != null ? "Success" : "Failed"));

        // Check if all required database credentials are set
        if (dbUser == null || dbPassword == null || dbUrl == null) {
            LOGGER.error("Database connection properties are not properly set.");
            return; // Exit the application if any database property is not set
        }

        try {
            // **Initialize Hibernate, which will create the database if it does not exist**
            HibernateUtil.initialize(dbUrl, dbUser, dbPassword);
            LOGGER.info("Database initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database", e);
            return; // Exit the application if database initialization fails
        }

        // Get the EntityManagerFactory
        EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();

        // Create DAO instances
        GameMapDAO gameMapDAO = new GameMapDAO(emf);
        PlayerDAOIMPL playerDAO = new PlayerDAOIMPL(emf);

        // Create a Logger instance for GameController
        Logger gameControllerLogger = LogManager.getLogger(GameController.class);

        // Create the GameController instance
        GameController gameController = new GameController(playerDAO, gameMapDAO, gameControllerLogger);

        // Pass the GameController to the MainMenuView
        EventQueue.invokeLater(() -> {
            try {
                MainMenuView mainMenu = new MainMenuView();
                mainMenu.setGameController(gameController); // Set the GameController
                mainMenu.setVisible(true);
                LOGGER.info("Main Menu opened");
            } catch (Exception e) {
                LOGGER.error("Failed to open Main Menu", e);
            }
        });

        LOGGER.info("App Terminated");
    }
}
