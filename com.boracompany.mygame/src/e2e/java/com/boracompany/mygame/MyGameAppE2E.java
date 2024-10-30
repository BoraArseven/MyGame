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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.FileNotFoundException;
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
    
    private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
	}

    // Helper method to read the file content as a String from classpath
    private String readFileContent(String filename) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                throw new FileNotFoundException("File not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    @Override
    protected void onSetUp() throws Exception {
        logger.info("Setting up PostgreSQL container with values from text files");

        // Read database credentials from text files
        String dbName = readFileContent("postgres_db.txt");
        String dbUser = readFileContent("postgres_user.txt");
        String dbPassword = readFileContent("postgres_password.txt");

        logger.debug("Database name: {}, User: {}, Password: {}", dbName, dbUser, dbPassword);
        
        

        // Initialize the PostgreSQLContainer with values from the files
        postgreSQLContainer = extracted()
                .withDatabaseName(dbName)
                .withUsername(dbUser)
                .withPassword(dbPassword);

        postgreSQLContainer.start();
        logger.info("PostgreSQL container started at URL: {}", postgreSQLContainer.getJdbcUrl());

        String dbUrl = postgreSQLContainer.getJdbcUrl();
        logger.debug("Database URL: {}", dbUrl);

        // Start the application with the dbUrl as an argument
        logger.info("Launching application with dbUrl: {}", dbUrl);
        application("com.boracompany.mygame.main.Main")
                .withArgs("--dburl=" + dbUrl)
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
        
        // Verify that the window is visible
        window.requireVisible();
        
        // Verify that the window has the correct title
        window.requireTitle("Main Menu");
        
        logger.info("Main menu opened correctly with title 'Main Menu'");
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
