package com.boracompany.mygame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.launcher.ApplicationLauncher.application;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.orm.HibernateUtil;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class MyGameAppE2E extends AssertJSwingJUnitTestCase {

	private static final Logger logger = LogManager.getLogger(MyGameAppE2E.class);
	private static PostgreSQLContainer<?> postgreSQLContainer;
	private FrameFixture window;

	@Override
	protected void onSetUp() throws Exception {
		logger.info("Setting up PostgreSQL container");

		// Initialize PostgreSQLContainer with default values
		postgreSQLContainer = new PostgreSQLContainer<>("postgres:13.3");
		postgreSQLContainer.start();
		logger.info("PostgreSQL container started at URL: {}", postgreSQLContainer.getJdbcUrl());

		String dbUrl = postgreSQLContainer.getJdbcUrl();
		String dbUser = postgreSQLContainer.getUsername();
		String dbPassword = postgreSQLContainer.getPassword();

		logger.debug("Database URL: {}", dbUrl);
		logger.debug("Database user: {}", dbUser);
		logger.debug("Database password: [PROTECTED]");

		// Launch the application with the dbUrl and other parameters as arguments
		logger.info("Launching application with dbUrl and credentials.");
		application("com.boracompany.mygame.main.Main")
				.withArgs("--dburl=" + dbUrl, "--dbuser=" + dbUser, "--dbpassword=" + dbPassword).start();

		// Use Awaitility to wait until the Main Menu window appears
		logger.info("Waiting for the Main Menu window to appear.");
		await().atMost(10, TimeUnit.SECONDS).until(() -> {
			try {
				window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
					@Override
					protected boolean isMatching(JFrame frame) {
						return "Main Menu".equals(frame.getTitle()) && frame.isShowing();
					}
				}).using(robot());
				return window != null;
			} catch (Exception e) {
				// Window not found yet
				return false;
			}
		});

		if (window != null) {
			logger.info("Main Menu window found.");
		} else {
			logger.error("Main Menu window not found within the timeout.");
			throw new AssertionError("Main Menu window not found within the timeout.");
		}
	}

	@Test
	public void testFullApplicationFlow() throws Exception {
		logger.info("Starting full application flow test");

		// Step 1: Open Create Map window and create a map
		logger.info("Step 1: Creating a new map");
		window.button("Create Map").click();
		robot().waitForIdle();

		FrameFixture createMapWindow = findWindowByTitle("Create Map");
		// Verify Create Map window is visible
		createMapWindow.requireVisible();

		// Enter map name
		createMapWindow.textBox("NameText").enterText("TestMap");
		// Click the "Create Map" button
		createMapWindow.button("CreateMapButton").click();
		// Click the "Main Menu" button to return
		createMapWindow.button("MainMenuButton").click();
		logger.debug("'Main Menu' button clicked to close 'Create Map' window.");
		robot().waitForIdle();

		// Use requireNotVisible() to ensure the window has closed
		createMapWindow.requireNotVisible();
		logger.info("Create Map window closed successfully.");

		// Re-find the main menu window since it may have been re-shown
		window = findWindowByTitle("Main Menu");
		// Ensure main menu is visible again
		window.requireVisible();

		// Step 2: Open Create Player window and create two players
		logger.info("Step 2: Creating Player1");
		window.button("Create Player").click();
		robot().waitForIdle();

		FrameFixture createPlayerWindow = findWindowByTitle("Create Player");
		createPlayerWindow.requireVisible();

		// Create Player 1
		createPlayerWindow.textBox("NameText").enterText("Player1");
		createPlayerWindow.textBox("DamageText").enterText("50");
		createPlayerWindow.textBox("HealthText").enterText("100");
		createPlayerWindow.button("CreatePlayerButton").click();
		robot().waitForIdle();

		// Verify that the text fields are cleared after creating Player 1
		createPlayerWindow.textBox("NameText").requireText("");
		createPlayerWindow.textBox("DamageText").requireText("");
		createPlayerWindow.textBox("HealthText").requireText("");

		// Create Player 2 in the same window
		logger.info("Step 2: Creating Player2");

		// Enter data for Player 2
		createPlayerWindow.textBox("NameText").enterText("Player2");
		createPlayerWindow.textBox("DamageText").enterText("60");
		createPlayerWindow.textBox("HealthText").enterText("90");
		createPlayerWindow.button("CreatePlayerButton").click();
		robot().waitForIdle();

		// Verify that the text fields are cleared after creating Player 2
		createPlayerWindow.textBox("NameText").requireText("");
		createPlayerWindow.textBox("DamageText").requireText("");
		createPlayerWindow.textBox("HealthText").requireText("");

		// Now click "Main Menu" to close the Create Player window
		createPlayerWindow.button("MainMenuButton").click();
		logger.debug("'Main Menu' button clicked to close 'Create Player' window.");
		robot().waitForIdle();

		// Use requireNotVisible() to ensure the window has closed
		createPlayerWindow.requireNotVisible();
		logger.info("Create Player window closed successfully.");

		// Re-find the main menu window since it may have been re-shown
		window = findWindowByTitle("Main Menu");
		// Ensure main menu is visible again
		window.requireVisible();

		// Step 3: Open Add Players to Maps window and add the two players to the
		// created map
		logger.info("Step 3: Adding Player1 and Player2 to TestMap");
		window.button("Add Players to Maps").click();
		robot().waitForIdle();

		FrameFixture addPlayersToMapsWindow = findWindowByTitle("Add Players to Maps");
		addPlayersToMapsWindow.requireVisible();

		// Select the map "TestMap"
		addPlayersToMapsWindow.list("mapList").selectItem("TestMap");
		robot().waitForIdle();

		// Add Player1 to the map using pattern matching
		addPlayersToMapsWindow.list("playerList").selectItem(Pattern.compile("^Player1.*"));
		addPlayersToMapsWindow.button("Add Selected Player to Map").click();
		robot().waitForIdle();

		// Select the map "TestMap" again
		addPlayersToMapsWindow.list("mapList").selectItem("TestMap");

		// Add Player2 to the map
		addPlayersToMapsWindow.list("playerList").selectItem(Pattern.compile("^Player2.*"));
		addPlayersToMapsWindow.button("Add Selected Player to Map").click();
		robot().waitForIdle();

		// Close Add Players to Maps window by clicking "Main Menu"
		addPlayersToMapsWindow.button("Main Menu").click();
		logger.debug("'Main Menu' button clicked to close 'Add Players to Maps' window.");
		robot().waitForIdle();

		// Use requireNotVisible() to ensure the window has closed
		addPlayersToMapsWindow.requireNotVisible();
		logger.info("Add Players to Maps window closed successfully.");

		// Re-find the main menu window
		window = findWindowByTitle("Main Menu");
		// Ensure main menu is visible again
		window.requireTitle("Main Menu");
		window.requireVisible();

		// Step 4: Press Play and attack Player1 to Player2
		logger.info("Step 4: Attacking Player2 with Player1");
		window.button("Play").click();
		robot().waitForIdle();

		FrameFixture playerAttackWindow = findWindowByTitle("Player Attack");
		playerAttackWindow.requireVisible();

		// Select the map "TestMap"
		playerAttackWindow.list("mapList").selectItem("TestMap");
		robot().waitForIdle();

        // Select attacker "Player1" using pattern matching
        playerAttackWindow.list("attackerList").selectItem(Pattern.compile("^Player1.*"));
		robot().waitForIdle();

        // Select defender "Player2"
        playerAttackWindow.list("defenderList").selectItem(Pattern.compile("^Player2.*"));
		robot().waitForIdle();

		// Perform the attack
		playerAttackWindow.button("btnAttack").click();
		logger.debug("'Attack' button clicked.");
		robot().waitForIdle();

		// Verify the attack result
		String[] defenderItems = playerAttackWindow.list("defenderList").contents();
		assertThat(defenderItems).anySatisfy(item -> {
			assertThat(item).contains("Player2").contains("Health: 40");
		});
		logger.info("Attack from 'Player1' to 'Player2' was successful.");

		// Close Player Attack window by clicking "Main Menu"
		playerAttackWindow.button("Main Menu").click();
		logger.debug("'Main Menu' button clicked to close 'Player Attack' window.");
		robot().waitForIdle();

		// Use requireNotVisible() to ensure the window has closed
		playerAttackWindow.requireNotVisible();
		logger.info("Player Attack window closed successfully.");

		// Re-find the main menu window
		window = findWindowByTitle("Main Menu");
		// Ensure main menu is visible again
		window.requireVisible();

		logger.info("Full application flow test completed successfully");
	}

	private FrameFixture findWindowByTitle(String title) {
		return WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
			@Override
			protected boolean isMatching(JFrame frame) {
				return title.equals(frame.getTitle()) && frame.isShowing();
			}
		}).withTimeout(10, TimeUnit.SECONDS).using(robot());
	}

	@Override
	protected void onTearDown() {
		logger.info("Tearing down PostgreSQL container and cleaning up window resources");
		HibernateUtil.close();
		if (postgreSQLContainer != null) {
			postgreSQLContainer.stop();
			logger.info("PostgreSQL container stopped");
		}
		if (window != null) {
			window.cleanUp();
			logger.info("Window resources cleaned up");
		}
	}
}
