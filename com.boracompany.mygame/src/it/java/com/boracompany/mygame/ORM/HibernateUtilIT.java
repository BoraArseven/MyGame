package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.orm.HibernateUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

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

	@Test
	public void testInitializeWhenDatabaseExists() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Initialize with the default database (which should already exist)
		HibernateUtil.initialize(validJdbcUrl, username, password);

		// Attempt to initialize again with the same parameters
		HibernateUtil.initialize(validJdbcUrl, username, password);

		// Verify that EntityManagerFactory is initialized
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");

		// If no exception is thrown, the test passes
	}

	@Test
	public void testDatabaseCreationWhenNotExists() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Generate a unique database name
		String uniqueDatabaseName = "testdb_" + System.currentTimeMillis();
		String jdbcUrlWithNewDb = validJdbcUrl.replace("testdb", uniqueDatabaseName);

		// Initialize HibernateUtil with the new database name
		HibernateUtil.initialize(jdbcUrlWithNewDb, username, password);

		// Verify that EntityManagerFactory is initialized
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");

		// Attempt to persist and retrieve an entity
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		try {
			tx.begin();

			// Create a new Player
			Player player = new Player();
			player.setName("Alice");

			// Persist the Player
			em.persist(player);

			tx.commit();

			// Retrieve the Player using the generated ID
			Player retrievedPlayer = em.find(Player.class, player.getId());
			assertNotNull(retrievedPlayer, "Retrieved player should not be null");
			assertEquals("Alice", retrievedPlayer.getName(), "Player name should match");

		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			fail("Exception occurred: " + e.getMessage());
		} finally {
			em.close();
		}
	}
	@Test
	public void testInitializeWithEmptyDatabaseName() {
	    // Close any existing EntityManagerFactory
	    HibernateUtil.close();

	    // Create a JDBC URL that ends with '/', resulting in an empty database name
	    String jdbcUrlWithEmptyDbName = validJdbcUrl.substring(0, validJdbcUrl.lastIndexOf('/') + 1);

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(jdbcUrlWithEmptyDbName, username, password);
	    });

	    String expectedMessage = "Invalid database name";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	public void testInitializeWithTooLongDatabaseName() {
	    // Close any existing EntityManagerFactory
	    HibernateUtil.close();

	    // Create a database name longer than 64 characters
	    String longDatabaseName = "a".repeat(65);
	    String jdbcUrlWithLongDbName = validJdbcUrl.replace("testdb", longDatabaseName);

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(jdbcUrlWithLongDbName, username, password);
	    });

	    String expectedMessage = "Invalid database name";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	public void testInitializeWithInvalidCharactersInDatabaseName() {
	    // Close any existing EntityManagerFactory
	    HibernateUtil.close();

	    // Use a database name with invalid characters
	    String invalidDatabaseName = "invalid-db-name!@#";
	    String jdbcUrlWithInvalidDbName = validJdbcUrl.replace("testdb", invalidDatabaseName);

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(jdbcUrlWithInvalidDbName, username, password);
	    });

	    String expectedMessage = "Invalid database name";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	public void testInitializeWithReservedKeywordDatabaseName() {
	    // Close any existing EntityManagerFactory
	    HibernateUtil.close();

	    // Use a reserved SQL keyword as the database name
	    String reservedKeywordDatabaseName = "SELECT";
	    String jdbcUrlWithReservedDbName = validJdbcUrl.replace("testdb", reservedKeywordDatabaseName);

	    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
	        HibernateUtil.initialize(jdbcUrlWithReservedDbName, username, password);
	    });

	    String expectedMessage = "Invalid database name";
	    String actualMessage = exception.getMessage();

	    assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	public void testInitializeWithValidDatabaseName() {
	    // Close any existing EntityManagerFactory
	    HibernateUtil.close();

	    // Use a valid database name
	    String validDatabaseName = "valid_db_name123";
	    String jdbcUrlWithValidDbName = validJdbcUrl.replace("testdb", validDatabaseName);

	    // Initialize HibernateUtil
	    HibernateUtil.initialize(jdbcUrlWithValidDbName, username, password);

	    // Verify that EntityManagerFactory is initialized
	    EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
	    assertNotNull(emf, "EntityManagerFactory should be initialized");

	    // Close the EntityManagerFactory
	    HibernateUtil.close();
	}

	
}
