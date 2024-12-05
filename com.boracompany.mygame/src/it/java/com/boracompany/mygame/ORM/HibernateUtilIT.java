package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
		containerusarname = postgresContainer.getUsername();
		containerpassword = postgresContainer.getPassword();

		// Initialize HibernateUtil
		HibernateUtil.initialize(validJdbcUrl, containerusarname, containerpassword);
	}

	private String validJdbcUrl;
	private String containerusarname;
	private String containerpassword;

	@AfterEach
	public void tearDown() {
		// Close HibernateUtil
		HibernateUtil.close();
	}

	@Test
	void testEntityManagerFactoryInitialization() {
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");
	}

	@Test
	void testPersistAndRetrievePlayer() {
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
	void testInitializeWithNullParameters() {
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
		Exception exception = assertThrows(IllegalStateException.class, HibernateUtil::getEntityManagerFactory);
		String expectedMessage = "HibernateUtil is not initialized.";
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate uninitialized state");
	}

	@Test
	public void testCloseWhenEntityManagerFactoryIsNull() {
		// Ensure the entityManagerFactory is null by explicitly closing it
		HibernateUtil.close();

		// Call close() again, expecting no exceptions
		assertDoesNotThrow(() -> HibernateUtil.close(),
				"Calling close when entityManagerFactory is null should not throw exceptions");

		// Verify that entityManagerFactory is still null after calling close()
		Exception exception = assertThrows(IllegalStateException.class, () -> {
			HibernateUtil.getEntityManagerFactory();
		});

		String expectedMessage = "HibernateUtil is not initialized.";
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate uninitialized state");
	}

	@Test
	void testCreateDatabaseIfNotExistsSQLException() {
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
	void testInitializeWithInvalidDatabaseName() {
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
	void testInitializeWithNullDbPassword() {
		HibernateUtil.close(); // Ensure entityManagerFactory is null

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(validJdbcUrl, containerusarname, null);
		});

		String expectedMessage = "Database connection properties must not be null";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbPassword");
	}

	@Test
	void testInitializeWithNullDbUser() {
		HibernateUtil.close(); // Ensure entityManagerFactory is null

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(validJdbcUrl, null, containerpassword);
		});

		String expectedMessage = "Database connection properties must not be null";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbUser");
	}

	@Test
	void testInitializeWithNullDbUrl() {
		HibernateUtil.close(); // Ensure entityManagerFactory is null

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(null, containerusarname, containerpassword);
		});

		String expectedMessage = "Database connection properties must not be null";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate null dbUrl");
	}

	@Test
	void testInitializeWhenAlreadyInitialized() {
		// Ensure entityManagerFactory is initialized
		EntityManagerFactory emf1 = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf1, "EntityManagerFactory should be initialized");

		// Call initialize again with the same parameters
		HibernateUtil.initialize(validJdbcUrl, containerusarname, containerpassword);
		EntityManagerFactory emf2 = HibernateUtil.getEntityManagerFactory();

		// Verify that the same instance is returned (no reinitialization)
		assertSame(emf1, emf2, "EntityManagerFactory should not be reinitialized");

		// Now, call initialize with different parameters (should not reinitialize)
		String anotherValidJdbcUrl = validJdbcUrl; // Using the same URL for simplicity
		HibernateUtil.initialize(anotherValidJdbcUrl, containerusarname, containerpassword);
		EntityManagerFactory emf3 = HibernateUtil.getEntityManagerFactory();

		// Verify that the EntityManagerFactory has not changed
		assertSame(emf1, emf3, "EntityManagerFactory should not be reinitialized with different parameters");
	}

	/**
	 * Test concurrent initialization to cover the false branch of the inner if.
	 * Ensures thread-safe initialization.
	 */
	@Test
	void testInitializeConcurrentAccess() throws InterruptedException {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		final String jdbcUrl = validJdbcUrl;
		final String user = containerusarname;
		final String pass = containerpassword;

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
	void testInitializeWhenDatabaseExists() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Initialize with the default database (which should already exist)
		HibernateUtil.initialize(validJdbcUrl, containerusarname, containerpassword);

		// Attempt to initialize again with the same parameters
		HibernateUtil.initialize(validJdbcUrl, containerusarname, containerpassword);

		// Verify that EntityManagerFactory is initialized
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");

		// If no exception is thrown, the test passes
	}

	@Test
	void testDatabaseCreationWhenNotExists() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Generate a unique database name
		String uniqueDatabaseName = "testdb_" + System.currentTimeMillis();
		String jdbcUrlWithNewDb = validJdbcUrl.replace("testdb", uniqueDatabaseName);

		// Initialize HibernateUtil with the new database name
		HibernateUtil.initialize(jdbcUrlWithNewDb, containerusarname, containerpassword);

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
	void testInitializeWithEmptyDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Create a JDBC URL that ends with '/', resulting in an empty database name
		String jdbcUrlWithEmptyDbName = validJdbcUrl.substring(0, validJdbcUrl.lastIndexOf('/') + 1);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(jdbcUrlWithEmptyDbName, containerusarname, containerpassword);
		});

		String expectedMessage = "Invalid database name";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	void testInitializeWithTooLongDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Create a database name longer than 64 characters
		String longDatabaseName = "a".repeat(65);
		String jdbcUrlWithLongDbName = validJdbcUrl.replace("testdb", longDatabaseName);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(jdbcUrlWithLongDbName, containerusarname, containerpassword);
		});

		String expectedMessage = "Invalid database name";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	void testInitializeWithInvalidCharactersInDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Use a database name with invalid characters
		String invalidDatabaseName = "invalid-db-name!@#";
		String jdbcUrlWithInvalidDbName = validJdbcUrl.replace("testdb", invalidDatabaseName);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(jdbcUrlWithInvalidDbName, containerusarname, containerpassword);
		});

		String expectedMessage = "Invalid database name";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	void testInitializeWithReservedKeywordDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Use a reserved SQL keyword as the database name
		String reservedKeywordDatabaseName = "SELECT";
		String jdbcUrlWithReservedDbName = validJdbcUrl.replace("testdb", reservedKeywordDatabaseName);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			HibernateUtil.initialize(jdbcUrlWithReservedDbName, containerusarname, containerpassword);
		});

		String expectedMessage = "Invalid database name";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage), "Exception message should indicate invalid database name");
	}

	@Test
	void testInitializeWithValidDatabaseName() {
		// Close any existing EntityManagerFactory
		HibernateUtil.close();

		// Use a valid database name
		String validDatabaseName = "valid_db_name123";
		String jdbcUrlWithValidDbName = validJdbcUrl.replace("testdb", validDatabaseName);

		// Initialize HibernateUtil
		HibernateUtil.initialize(jdbcUrlWithValidDbName, containerusarname, containerpassword);

		// Verify that EntityManagerFactory is initialized
		EntityManagerFactory emf = HibernateUtil.getEntityManagerFactory();
		assertNotNull(emf, "EntityManagerFactory should be initialized");

		// Close the EntityManagerFactory
		HibernateUtil.close();
	}

}
