package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlayerDAOImpIT {

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:14.15");
	}

	private EntityManagerFactory emf;
	private PlayerDAOIMPL playerDAO;

	@BeforeAll
	void setUp() {
		// Directly passing database properties
		String dbUrl = postgreSQLContainer.getJdbcUrl();
		String dbUser = postgreSQLContainer.getUsername();
		String dbPassword = postgreSQLContainer.getPassword();

		// Initialize HibernateUtil with connection properties
		HibernateUtil.initialize(dbUrl, dbUser, dbPassword);
		emf = HibernateUtil.getEntityManagerFactory();
		playerDAO = new PlayerDAOIMPL(emf);
	}

	@AfterAll
	void tearDown() {
		HibernateUtil.close();
		if (postgreSQLContainer != null) {
			postgreSQLContainer.stop();
		}
	}

	@Test
	void testGetAllPlayers() {
		EntityManager em = emf.createEntityManager();

		em.getTransaction().begin();
		Player player = new Player();
		player.setName("John Doe");
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		List<Player> players = playerDAO.getAllPlayers();
		assertNotNull(players);
		assertFalse(players.isEmpty());

		boolean found = players.stream().anyMatch(p -> "John Doe".equals(p.getName()));
		assertTrue(found, "Player John Doe should be found in the list of all players.");
	}

	@Test
	void testGetPlayer() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Jane Doe").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		Player retrievedPlayer = playerDAO.getPlayer(player.getId());
		assertNotNull(retrievedPlayer);
		assertEquals("Jane Doe", retrievedPlayer.getName());
	}

	@Test
	void testUpdatePlayer() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new Player();
		player.setName("Initial Name");
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		player.setName("Updated Name");
		playerDAO.updatePlayer(player);

		Player updatedPlayer = playerDAO.getPlayer(player.getId());
		assertNotNull(updatedPlayer);
		assertEquals("Updated Name", updatedPlayer.getName());
	}

	@Test
	void testUpdatePlayerCommitsTransactionSuccessfully() {
		// Arrange
		Player player = new Player();
		player.setName("Test Player");

		// Persist the player to get an ID
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		// Update the player
		player.setName("Updated Name");
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emf);

		// Act
		dao.updatePlayer(player);

		// Assert
		em = emf.createEntityManager();
		Player updatedPlayer = em.find(Player.class, player.getId());
		assertEquals("Updated Name", updatedPlayer.getName());
		em.close();
	}

	@Test
	void testDeletePlayer() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("To Be Deleted").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		playerDAO.deletePlayer(player);

		Player deletedPlayer = playerDAO.getPlayer(player.getId());
		assertNull(deletedPlayer);
	}

	@Test
	void testDeletePlayerDoesNotRollbackWhenTransactionIsNull() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a real EntityManager and persist a player
		EntityManager emPersist = emfSpy.createEntityManager();
		emPersist.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Test Player").build();
		emPersist.persist(player);
		emPersist.getTransaction().commit();
		emPersist.close();

		// Now, create a new spied EntityManager for the delete operation
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Mock getTransaction() to return null
		when(emSpy.getTransaction()).thenReturn(null);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Ensure that the DAO uses the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Attempt to delete the player, expecting an IllegalStateException
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that the remove operation was never called due to the null transaction
		verify(emSpy, never()).remove(any(Player.class));

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayerTriggersRollbackOnRuntimeException() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a player instance and persist it
		Player player = new PlayerBuilder().withName("Test Player").build();
		emSpy.getTransaction().begin();
		emSpy.persist(player);
		emSpy.getTransaction().commit();

		// Simulate a RuntimeException during the remove operation
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).remove(any(Player.class));

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that the exception message is correct
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the transaction
		verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayerThrowsExceptionForNonExistingPlayer() {
		// Arrange
		Player nonExistentPlayer = new Player();
		nonExistentPlayer.setId(-999L); // Use an ID that does not exist

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.deletePlayer(nonExistentPlayer);
		});

		// Verify the exception message
		assertEquals("Tried to delete non-existing player with ID -999", exception.getMessage());
	}

	@Test
	void testDeletePlayerThrowsExceptionWhenTransactionIsNull() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Simulate the EntityManager returning a null transaction
		when(emSpy.getTransaction()).thenReturn(null);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L);

		// Simulate finding the player
		when(emSpy.find(Player.class, player.getId())).thenReturn(player);

		// Attempt to delete the player
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that the remove operation was never called due to the null transaction
		verify(emSpy, never()).remove(any(Player.class));

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayer_withNullPlayerId_exceptionThrown_playerIdIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// player.getId() is null

		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spied EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Use doReturn() to stub getTransaction() to return null without invoking the
		// real method
		doReturn(null).when(emSpy).getTransaction();

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert: Expect an IllegalStateException due to null transaction
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that 'rollback()' was never called since transaction is null
		verify(transactionSpy, never()).rollback();

		// Verify that 'remove()' was never called due to the null transaction
		verify(emSpy, never()).remove(any(Player.class));

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayer_withPlayerIdNotNull_exceptionThrown_playerIdNotNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		player.setId(1L); // Assign a non-null ID

		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Stub the persist method to throw an exception using doThrow()
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(any(Player.class));

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify that rollback was called
		verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();

		// Assert that the exception message is as expected
		assertEquals("Simulated Exception", exception.getMessage());
	}

	@Test
	void testCreatePlayer_withNullPlayerId_exceptionThrown_playerIdIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// player.getId() is null

		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Stub the persist method to throw an exception using doThrow()
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(any(Player.class));

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify that rollback and close were called
		verify(transactionSpy).rollback();
		verify(emSpy).close();

		// Assert that the exception message is as expected
		assertEquals("Simulated Exception", exception.getMessage());
	}

	@Test
	void testDeletePlayerTransactionIsNullAndThrowsException() {
		// Arrange
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spied EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Stub getTransaction() to return null without invoking the real method
		doReturn(null).when(emSpy).getTransaction();

		// Ensure that createEntityManager() returns the spied EntityManager
		doReturn(emSpy).when(emfSpy).createEntityManager();

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert: Expect an IllegalStateException due to null transaction
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Print the exception message for debugging
		System.out.println("Exception Message: " + exception.getMessage());

		// Verify the exception message (adjust based on actual message)
		assertEquals(PlayerDAOIMPL.TRANSACTION_NULL_MESSAGE, exception.getMessage());

		// Verify that 'remove()' was never called due to null transaction
		verify(emSpy, never()).remove(any(Player.class));

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayerTransactionNotActiveAndThrowsException() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate the transaction being not active
		when(transactionSpy.isActive()).thenReturn(false);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that rollback was not called because the transaction is not active
		verify(transactionSpy, never()).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayerTransactionActiveAndThrowsException() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a real EntityManager and persist a player
		EntityManager emPersist = emfSpy.createEntityManager();
		emPersist.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Test Player").build();
		emPersist.persist(player);
		emPersist.getTransaction().commit();
		emPersist.close();

		// Now, create a new spied EntityManager for the delete operation
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Ensure the spied EntityManager returns the spied transaction
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Stub the remove method to throw a RuntimeException
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).remove(any(Player.class));

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the transaction
		verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testDeletePlayerTransactionCommitsSuccessfully() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a real EntityManager for setup (persisting the player)
		EntityManager emPersist = emfSpy.createEntityManager();
		emPersist.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Test Player").build();
		emPersist.persist(player);
		emPersist.getTransaction().commit();
		emPersist.close();

		// Now, create a new spied EntityManager for the delete operation
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spied EntityManagerFactory return the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Make the spied EntityManager return the spied EntityTransaction
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the transaction is not active before DAO starts
		when(transactionSpy.isActive()).thenReturn(false);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act: Call the deletePlayer method
		dao.deletePlayer(player);

		// Verify that the transaction was begun and committed
		verify(transactionSpy).begin();
		verify(transactionSpy).commit();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayerSuccessfully() {
		Player player = new PlayerBuilder().withName("NewPlayer").withDamage(50).withHealth(100).build();
		playerDAO.createPlayer(player);

		EntityManager em = emf.createEntityManager();
		Player persistedPlayer = em.find(Player.class, player.getId());
		assertNotNull(persistedPlayer, "Player should be successfully created and found in the database");
		assertEquals("NewPlayer", persistedPlayer.getName());
		em.close();
	}

	@Test
	void testCreatePlayerFailsDueToTransactionIssue() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spied EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Ensure the spied EntityManager returns the spied transaction
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Simulate a RuntimeException during the persist operation
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(any(Player.class));

		// Act & Assert: Expect an IllegalStateException due to the simulated exception
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify the exception message
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the transaction
		verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayerCommitsSuccessfully() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Create a Player instance
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Call the method to create the player
		dao.createPlayer(player);

		// Verify that the transaction was begun and committed
		verify(transactionSpy).begin();
		verify(transactionSpy).commit();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayerThrowsExceptionForNullPlayer() {
		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			playerDAO.createPlayer(null);
		});

		assertEquals("Player cannot be null", exception.getMessage());
	}

	@Test
	void testCreatePlayerTriggersRollbackOnRuntimeException() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Spy on the EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spies return each other
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the transaction begins properly and is active
		doNothing().when(transactionSpy).begin();
		when(transactionSpy.isActive()).thenReturn(true);

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Simulate a RuntimeException during the persist operation
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(any(Player.class));

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify the exception message
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the transaction
		verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayerThrowsExceptionWhenTransactionIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// player.getId() is null or set as needed

		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spied EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Spy on the EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Use doReturn() to stub getTransaction() to return null without invoking the
		// real method
		doReturn(null).when(emSpy).getTransaction();

		// Inject the spied EntityManagerFactory into the DAO
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert: Expect an IllegalStateException due to null transaction
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify the exception message
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that 'rollback()' was never called since transaction is null
		verify(transactionSpy, never()).rollback();

		// Verify that 'persist()' was never called due to the null transaction
		verify(emSpy, never()).persist(any(Player.class));

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testCreatePlayerWithNonNullInactiveTransaction() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the transaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate a non-null, inactive transaction
		when(emMock.getTransaction()).thenReturn(transactionMock);
		when(transactionMock.isActive()).thenReturn(false);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Act: Call the method to create the player
		dao.createPlayer(player);

		// Verify that the transaction was started and committed
		verify(transactionMock).begin();
		verify(transactionMock).commit();

		// Verify that the EntityManager was closed
		verify(emMock).close();
	}

	@Test
	void testCreatePlayerTriggersRollbackWhenTransactionIsActive() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the transaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate an active transaction
		when(emMock.getTransaction()).thenReturn(transactionMock);
		when(transactionMock.isActive()).thenReturn(true);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Simulate a RuntimeException during the persist operation
		doThrow(new RuntimeException("Simulated Exception")).when(emMock).persist(any(Player.class));

		// Act & Assert: Ensure that the exception is thrown and rollback happens
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Check that the exception message is correct
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the active transaction
		verify(transactionMock).rollback();

		// Verify that the EntityManager was closed
		verify(emMock).close();
	}

	@Test
	void testUpdatePlayerThrowsExceptionWhenPlayerNotFound() {
		// Arrange
		Player player = new PlayerBuilder().withName("Non-Existent Player").build();
		player.setId(999L); // Non-existent ID

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.updatePlayer(player);
		});
		assertEquals("Player with ID '999' not found.", exception.getMessage());
	}

	@Test
	void testDeletePlayerThrowsExceptionForNullId() {
		// Arrange
		Player player = new PlayerBuilder().withName("Player with Null ID").build();

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.deletePlayer(player);
		});
		assertEquals("Tried to delete non-existing player with ID N/A", exception.getMessage());
	}

	@Test
	void testDeletePlayerThrowsExceptionWhenPlayerNotFound() {
		// Arrange
		Player player = new PlayerBuilder().withName("Non-Existent Player").build();
		player.setId(999L); // Non-existent ID

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.deletePlayer(player);
		});
		assertEquals("Tried to delete non-existing player with ID 999", exception.getMessage());
	}

	@Test
	void testUpdatePlayerSuccessfully() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		// Persist a player into the database
		Player player = new PlayerBuilder().withName("Original Name").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		// Update the player's name
		player.setName("Updated Name");

		// Act
		playerDAO.updatePlayer(player);

		// Assert
		em = emf.createEntityManager();
		Player updatedPlayer = em.find(Player.class, player.getId());
		assertNotNull(updatedPlayer);
		assertEquals("Updated Name", updatedPlayer.getName());
		em.close();
	}

	@Test
	void testUpdatePlayerLogsCorrectErrorArgsWhenIdIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		player.setId(null); // Simulate a null ID

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.updatePlayer(player);
		});

		assertEquals("Player ID cannot be null.", exception.getMessage());
	}



	@Test
	void testGetPlayerSuccessfully() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Existing Player").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		// Act
		Player retrievedPlayer = playerDAO.getPlayer(player.getId());

		// Assert
		assertNotNull(retrievedPlayer);
		assertEquals(player.getId(), retrievedPlayer.getId());
		assertEquals(player.getName(), retrievedPlayer.getName());
	}

	@Test
	void testGetPlayerNotFound() {
		// Act
		Player retrievedPlayer = playerDAO.getPlayer(999L); // Non-existent ID

		// Assert
		assertNull(retrievedPlayer, "Expected null when the player is not found");
	}

	@Test
	void testGetPlayerRollsBackOnException() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate an exception during the find operation
		doThrow(new RuntimeException("Simulated Exception")).when(emSpy).find(eq(Player.class), any(),
				eq(LockModeType.PESSIMISTIC_READ));

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.getPlayer(1L);
		});

		// Verify rollback was called
		verify(transactionSpy).rollback();

		// Verify the exception message
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that the EntityManager was closed
		verify(emSpy).close();
	}

	@Test
	void testEntityManagerClosedEvenOnSuccess() {
		// Spy on the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		when(emfSpy.createEntityManager()).thenReturn(emSpy);
		when(emSpy.getTransaction()).thenReturn(transactionSpy);

		Player player = new PlayerBuilder().withName("Test Player").build();

		// Persist the player for setup
		emSpy.getTransaction().begin();
		emSpy.persist(player);
		emSpy.getTransaction().commit();

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act
		Player retrievedPlayer = dao.getPlayer(player.getId());

		// Assert
		assertNotNull(retrievedPlayer);

		// Verify the EntityManager was closed
		verify(emSpy).close();
	}

}
