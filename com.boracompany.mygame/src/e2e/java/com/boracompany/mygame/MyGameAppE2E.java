package com.boracompany.mygame;

import javax.swing.JFrame;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.assertj.swing.launcher.ApplicationLauncher.application;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
@Testcontainers
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

        // Wait for a short period to allow the window to appear
        TimeUnit.SECONDS.sleep(2);

        // Attempt to find the main menu window with a timeout
        logger.info("Attempting to find the Main Menu window.");
        try {
            window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
                @Override
                protected boolean isMatching(JFrame frame) {
                    return "Main Menu".equals(frame.getTitle()) && frame.isShowing();
                }
            }).withTimeout(10, TimeUnit.SECONDS).using(robot());

            if (window != null) {
                logger.info("Main Menu window found.");
            } else {
                logger.error("Main Menu window not found within the timeout.");
            }
        } catch (Exception e) {
            logger.error("Exception while finding the Main Menu window", e);
            throw e; // Rethrow to fail the test setup
        }
    }

    @Test
    public void testMainMenuOpensCorrectly() {
        logger.info("Starting test to check if main menu opens correctly");
        window.requireVisible();
        window.requireTitle("Main Menu");
        logger.info("Main menu opened correctly with title 'Main Menu'");
    }

    @Test
    public void testMainMenuButtonsArePresent() {
        logger.info("Starting test to check if all main menu buttons are present");

        // Verify that the "Create Map" button is present
        window.button("Create Map").requireVisible();

        // Verify that the "Create Player" button is present
        window.button("Create Player").requireVisible();

        // Verify that the "Add Players to Maps" button is present
        window.button("Add Players to Maps").requireVisible();

        // Verify that the "Play" button is present
        window.button("Play").requireVisible();

        logger.info("All main menu buttons are present and visible");
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
