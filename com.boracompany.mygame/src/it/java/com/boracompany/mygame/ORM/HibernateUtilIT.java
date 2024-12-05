package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.orm.HibernateUtil;

@Testcontainers
public class HibernateUtilIT {

	@Container
	private static PostgreSQLContainer<?> postgresContainer = extracted().withDatabaseName("testdb")
			.withUsername("testuser").withPassword("testpass");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:14.15");
	}

	@BeforeEach
	public void setUp() {
		// Ensure the container is started
		if (!postgresContainer.isRunning()) {
			postgresContainer.start();
		}


        // Get connection details from the container
        validJdbcUrl = postgresContainer.getJdbcUrl();
        username = postgresContainer.getUsername();
        password = postgresContainer.getPassword();

		// Initialize HibernateUtil
		HibernateUtil.initialize(validJdbcUrl, username, password);
	}
	 private String validJdbcUrl;
	    private String username;
	    private String password;

	@AfterEach
	public void tearDown() {
		// Close HibernateUtil
		HibernateUtil.close();
	}

	@Test
	public void testEntityManagerFactoryInitialization() {
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");
	}

	@Test
	public void testPersistAndRetrievePlayer() {
		EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
		EntityTransaction tx = em.getTransaction();

		try {
			tx.begin();

			// Create a new Player
			Player player = new Player();
			player.setName("John Doe");

			// Persist the Player
			em.persist(player);

			tx.commit();

			// Retrieve the Player using the generated ID
			Player retrievedPlayer = em.find(Player.class, player.getId());
			assertNotNull(retrievedPlayer, "Retrieved player should not be null");
			assertEquals("John Doe", retrievedPlayer.getName(), "Player name should match");

		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			fail("Exception occurred: " + e.getMessage());
		} finally {
			em.close();
		}
	}

	@Test
	public void testDatabaseCreation() {
		// Close the existing EntityManagerFactory
		HibernateUtil.close();

		// Attempt to connect to a non-existent database
		String nonExistentDbName = "nonexistentdb";
		String nonExistentDbUrl = postgresContainer.getJdbcUrl().replace("testdb", nonExistentDbName);
		String username = postgresContainer.getUsername();
		String password = postgresContainer.getPassword();

		// Initialize HibernateUtil with the non-existent database
		HibernateUtil.initialize(nonExistentDbUrl, username, password);

		// Now the database should be created
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized with new database");

		// Try to persist an entity
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		try {
			tx.begin();

			// Create a new Player
			Player player = new Player();
			player.setName("Jane Doe");

			// Persist the Player
			em.persist(player);

			tx.commit();

			// Retrieve the Player
			Player retrievedPlayer = em.find(Player.class, player.getId());
			assertNotNull(retrievedPlayer, "Retrieved player should not be null");
			assertEquals("Jane Doe", retrievedPlayer.getName(), "Player name should match");

		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			fail("Exception occurred: " + e.getMessage());
		} finally {
			em.close();
		}
	}

	@Test
	public void testInitializeWithNullParameters() {
		// Ensure that the entityManagerFactory is null
		HibernateUtil.close();

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(null, null, null);
		});

		String expectedMessage = "Database connection properties must not be null";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null properties");
	}

	@Test
	public void testGetEntityManagerFactoryBeforeInitialization() {
		HibernateUtil.close(); // Ensure it's not initialized
		Exception exception = assertThrows(IllegalStateException.class, () -> {
			HibernateUtil.getEntityManagerFactory();
		});

		String expectedMessage = "HibernateUtil is not initialized.";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate uninitialized state");
	}

	@Test
	public void testCloseWhenEntityManagerFactoryIsNull() {
		// Ensure entityManagerFactory is null
		HibernateUtil.close();

		// Call close() again
		HibernateUtil.close();

		// If no exception is thrown, the test passes
	}

	@Test
	public void testCreateDatabaseIfNotExistsSQLException() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Provide invalid credentials or URL to cause SQLException
		String invalidJdbcUrl = "jdbc:postgresql://invalidhost:5432/testdb";
		String username = "invaliduser";
		String password = "invalidpass";

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(invalidJdbcUrl, username, password);
		});

		String expectedMessage = "Failed to create database";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage),
				"Exception message should indicate failure to create database");
	}

	@Test
	public void testInitializeWithInvalidDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Use an invalid database name
		String invalidDbName = "invalid-db-name!@#"; // Contains invalid characters
		String invalidDbUrl = postgresContainer.getJdbcUrl().replace("testdb", invalidDbName);
		String username = postgresContainer.getUsername();
		String password = postgresContainer.getPassword();

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(invalidDbUrl, username, password);
		});

		String expectedMessage = "Invalid database name";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	public void testIsValidDatabaseName() {
		// Valid database names
		assertTrue(HibernateUtil.isValidDatabaseName("valid_db_name"),
				"Should accept valid database name with underscores");
		assertTrue(HibernateUtil.isValidDatabaseName("validDbName123"), "Should accept alphanumeric database name");

		// Invalid database names
		assertFalse(HibernateUtil.isValidDatabaseName("invalid-db-name"), "Should reject database name with hyphens");
		assertFalse(HibernateUtil.isValidDatabaseName(""), "Should reject empty database name");
		assertFalse(HibernateUtil.isValidDatabaseName(null), "Should reject null database name");
		assertFalse(
				HibernateUtil.isValidDatabaseName(
						"verylongdatabasenamethatisdefinitelymorethansixtyfourcharacterslongandshouldfail"),
				"Should reject database name longer than 64 characters");
	}

	
	@Test
	public void testInitializeWithNullDbPassword() {
	    HibernateUtil.close(); // Ensure entityManagerFactory is null

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(validJdbcUrl, username, null);
	    });

	    String expectedMessage = "Database connection properties must not be null";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbPassword");
	}

	@Test
	public void testInitializeWithNullDbUser() {
	    HibernateUtil.close(); // Ensure entityManagerFactory is null

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(validJdbcUrl, null, password);
	    });

	    String expectedMessage = "Database connection properties must not be null";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbUser");
	}

	@Test
	public void testInitializeWithNullDbUrl() {
	    HibernateUtil.close(); // Ensure entityManagerFactory is null

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(null, username, password);
	    });

	    String expectedMessage = "Database connection properties must not be null";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbUrl");
	}
	 @Test
	    public void testInitializeWhenAlreadyInitialized() {
	        // Ensure entityManagerFactory is initialized
	        EntityManagerFactory emf1 = HibernateUtil.getEntityManagerFactory();
	        assertNotNull(emf1, "EntityManagerFactory should be initialized");

	        // Call initialize again with the same parameters
	        HibernateUtil.initialize(validJdbcUrl, username, password);
	        EntityManagerFactory emf2 = HibernateUtil.getEntityManagerFactory();

	        // Verify that the same instance is returned (no reinitialization)
	        assertSame(emf1, emf2, "EntityManagerFactory should not be reinitialized");

	        // Now, call initialize with different parameters (should not reinitialize)
	        String anotherValidJdbcUrl = validJdbcUrl; // Using the same URL for simplicity
	        HibernateUtil.initialize(anotherValidJdbcUrl, username, password);
	        EntityManagerFactory emf3 = HibernateUtil.getEntityManagerFactory();

	        // Verify that the EntityManagerFactory has not changed
	        assertSame(emf1, emf3, "EntityManagerFactory should not be reinitialized with different parameters");
	    }

	    /**
	     * Test concurrent initialization to cover the false branch of the inner if.
	     * Ensures thread-safe initialization.
	     */
	    @Test
	    public void testInitializeConcurrentAccess() throws InterruptedException {
	        // Close any existing EntityManagerFactory
	        HibernateUtil.close();

	        final String jdbcUrl = validJdbcUrl;
	        final String user = username;
	        final String pass = password;

	        // Create a latch to synchronize threads
	        CountDownLatch latch = new CountDownLatch(1);

	        // Thread 1: Initializes HibernateUtil
	        Thread thread1 = new Thread(() -> {
	            HibernateUtil.initialize(jdbcUrl, user, pass);
	            latch.countDown(); // Signal that initialization is complete
	        });

	        // Thread 2: Attempts to initialize HibernateUtil concurrently
	        Thread thread2 = new Thread(() -> {
	            try {
	                // Wait for thread1 to start initializing
	                latch.await(5, TimeUnit.SECONDS);

	                // Call initialize; entityManagerFactory should already be initialized
	                HibernateUtil.initialize(jdbcUrl, user, pass);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                fail("Thread was interrupted");
	            }
	        });

	        // Start both threads
	        thread1.start();
	        thread2.start();

	        // Wait for both threads to finish
	        thread1.join();
	        thread2.join();

	        // Verify that entityManagerFactory is initialized
	        assertNotNull(HibernateUtil.getEntityManagerFactory(), "EntityManagerFactory should be initialized");
	    }


	
}
