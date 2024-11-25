package com.boracompany.mygame.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;
import com.boracompany.mygame.view.MainMenuView;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.awt.EventQueue;

import javax.persistence.EntityManagerFactory;

@Command(name = "MyGame", mixinStandardHelpOptions = true, version = "1.0", description = "Starts the MyGame application")
public class Main implements Runnable {

	private static final Logger LOGGER = LogManager.getLogger(Main.class);

	@Option(names = { "--dburl" }, description = "PostgreSQL JDBC URL", required = true)
	private String dbUrl;

	@Option(names = { "--dbuser" }, description = "PostgreSQL database user", required = true)
	private String dbUser = "postgres";

	@Option(names = { "--dbpassword" }, description = "PostgreSQL database password", required = true)
	private String dbPassword;

	public static void main(String[] args) {
		LOGGER.info("App started");

		// Parse command-line arguments using Picocli
		int exitCode = new CommandLine(new Main()).execute(args);

		LOGGER.info("App Terminated with exit code: {}", exitCode);
	}

	@Override
	public void run() {
		LOGGER.info("DB URL: {}", dbUrl);
		LOGGER.info("DB User: {}", dbUser);
		LOGGER.info("DB Password: [PROTECTED]");

		// Check if all required database credentials are set
		if (dbUser == null || dbPassword == null || dbUrl == null) {
			LOGGER.error("Database connection properties are not properly set.");
			return; // Exit the application if any database property is not set
		}

		try {
			// Initialize Hibernate with the provided configurations
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
	}
}
