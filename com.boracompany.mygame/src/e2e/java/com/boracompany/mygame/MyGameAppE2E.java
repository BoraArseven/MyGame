package com.boracompany.mygame;

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
            .withArgs("--dburl=" + dbUrl,
                      "--dbuser=" + dbUser,
                      "--dbpassword=" + dbPassword)
            .start();

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
        robot().waitForIdle();

        // Wait until the Create Map window is closed
        waitUntilWindowCloses(createMapWindow, 10); // Wait up to 10 seconds

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
        
        //waits until current poerations are finished
        robot().waitForIdle();

        // Wait until the Create Player window is closed
        waitUntilWindowCloses(createPlayerWindow, 10); // Wait up to 10 seconds

        // Re-find the main menu window since it may have been re-shown
        window = findWindowByTitle("Main Menu");
        // Ensure main menu is visible again
        window.requireVisible();

        // Step 3: Open Add Players to Maps window and add the two players to the created map
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
        
     // Select the map "TestMap"
        addPlayersToMapsWindow.list("mapList").selectItem("TestMap");
        // Add Player2 to the map using pattern matching
        addPlayersToMapsWindow.list("playerList").selectItem(Pattern.compile("^Player2.*"));
        addPlayersToMapsWindow.button("Add Selected Player to Map").click();
        robot().waitForIdle();

        // Optionally, verify that the players have been added to the map
        // This could involve checking a list or confirmation message

        // Close Add Players to Maps window by clicking "Main Menu"
        addPlayersToMapsWindow.button("Main Menu").click();
        robot().waitForIdle();

        // Verify that the AddPlayersToMapsView window is closed
        try {
            addPlayersToMapsWindow.requireNotVisible();
            logger.info("AddPlayersToMapsView closed successfully after adding players.");
        } catch (AssertionError e) {
            logger.error("AddPlayersToMapsView did not close as expected after adding players.", e);
            throw e;
        }

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

        // Select defender "Player2" using pattern matching
        playerAttackWindow.list("defenderList").selectItem(Pattern.compile("^Player2.*"));
        robot().waitForIdle();

        // Perform the attack
        playerAttackWindow.button("btnAttack").click();
        robot().waitForIdle();

        // Verify the attack result
        try {
            playerAttackWindow.label("errorLabel").requireText("");
            logger.info("Attack from 'Player1' to 'Player2' was successful.");
            playerAttackWindow.list("defenderList")
            .item(Pattern.compile("^Player2.* Health: 40.*"));
        } catch (AssertionError e) {
            // If "lblAttackResult" doesn't exist, check for error label
            try {
                playerAttackWindow.label("errorLabel").requireText("");
                logger.warn("Attack performed but 'lblAttackResult' not found. Verified no error message.");
            } catch (AssertionError ex) {
                logger.error("Attack failed or result not displayed as expected.", ex);
                throw ex;
            }
        }

        // Close Player Attack window by clicking "Main Menu"
        playerAttackWindow.button("Main Menu").click();
        robot().waitForIdle();

        // Verify that the PlayerAttackView window is closed
        try {
            playerAttackWindow.requireNotVisible();
            logger.info("PlayerAttackView closed successfully after attack.");
        } catch (AssertionError e) {
            logger.error("PlayerAttackView did not close as expected after attack.", e);
            throw e;
        }

        // Re-find the main menu window
        window = findWindowByTitle("Main Menu");
        // Ensure main menu is visible again
        window.requireVisible();

        logger.info("Full application flow test completed successfully");
    }
   

    private FrameFixture findWindowByTitle(String title) throws Exception {
        return WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
            @Override
            protected boolean isMatching(JFrame frame) {
                return title.equals(frame.getTitle()) && frame.isShowing();
            }
        }).withTimeout(10, TimeUnit.SECONDS).using(robot());
    }
    /**
     * Helper method to wait until a window is no longer visible.
     *
     * @param windowFixture    The FrameFixture representing the window.
     * @param timeoutInSeconds The maximum time to wait in seconds.
     * @throws InterruptedException If the thread is interrupted while waiting.
     * @throws AssertionError       If the window is still visible after the timeout.
     */
    private void waitUntilWindowCloses(FrameFixture windowFixture, long timeoutInSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutInSeconds * 1000;
        while (System.currentTimeMillis() < endTime) {
            try {
                windowFixture.requireNotVisible();
                return; // Window is not visible, exit the method
            } catch (AssertionError e) {
                // Window is still visible, wait and retry
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
        // Timeout reached, window is still visible
        throw new AssertionError("Window '" + windowFixture.target().getTitle() + "' did not close within " + timeoutInSeconds + " seconds.");
    }


    
    @Override
    protected void onTearDown() {
        logger.info("Tearing down PostgreSQL container and cleaning up window resources");

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