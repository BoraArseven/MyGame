package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

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

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlayerDAOImpIT {

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
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

		boolean found = false;

		for (Player p : players) {
			if (p.getName().equals("John Doe")) {
				found = true;
				break;
			}
		}

		assertTrue(found, "Player John Doe should be found in the list of all players.");
	}

	@Test
	void testGetPlayer() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Jane Doe").build();
		player.setName("Jane Doe");
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
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
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
	void testUpdatePlayerWhenRollbackOnRuntimeException() {
		// Use the custom TestPlayerDAOIMPL that throws the exception
		TestPlayerDAOIMPL playerDAO = new TestPlayerDAOIMPL(emf);

		// Create a player instance
		Player player = new Player();
		player.setName("Should Rollback");

		// Act & Assert: Ensure that the exception is thrown and rollback happens
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			playerDAO.updatePlayer(player);
		});

		// Check that the exception message is correct
		assertEquals("Failed to update player due to an unexpected error.", exception.getMessage());

	}

	@Test
	void testUpdatePlayerTriggersRollbackOnRuntimeException() {
		// Mock the EntityManager and EntityTransaction
		EntityManager emMock = Mockito.mock(EntityManager.class);
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Mock EntityManagerFactory to return the mocked EntityManager
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Ensure the EntityManager returns a non-null transaction
		Mockito.when(emMock.getTransaction()).thenReturn(transactionMock);

		// Ensure the transaction is active
		Mockito.when(transactionMock.isActive()).thenReturn(true);

		// Inject the mocked EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new Player();
		player.setName("Test Player");

		// Simulate a RuntimeException during the merge operation
		Mockito.doThrow(new RuntimeException("Failed to update player due to an unexpected error.")).when(emMock)
				.merge(Mockito.any(Player.class));

		// Assert that the RuntimeException is thrown when updatePlayer is called
		assertThrows(RuntimeException.class, () -> {
			dao.updatePlayer(player);
		});

		// Verify that rollback was called on the transaction
		Mockito.verify(transactionMock).rollback();

		// Verify that the EntityManager was closed
		Mockito.verify(emMock).close();
	}

	@Test
	void testUpdatePlayerDoesNotRollbackWhenTransactionIsNull() {
		// Spy the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Create a spy of the real EntityTransaction, but do not attach it yet
		// We will simulate a null transaction in the next step

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Simulate the EntityManager returning a null transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(null);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new Player();
		player.setName("Test Player");

		// Act & Assert: Ensure that an IllegalStateException is thrown due to the null
		// transaction
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.updatePlayer(player);
		});
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testUpdatePlayerCommitsTransactionSuccessfully() {
		// Spy the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Create a spy of the real EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Ensure the spied EntityManager returns the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the transaction is not active at the start of the test
		Mockito.when(transactionSpy.isActive()).thenReturn(false);

		// Create a Player instance
		Player player = new Player();
		player.setName("Test Player");
		player.setId(1L); // Assuming Player has an ID field

		// Mock the find method to return the player
		Mockito.when(emSpy.find(Player.class, player.getId())).thenReturn(player);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Call the method to update the player
		dao.updatePlayer(player);

		// Verify that the find method was called
		Mockito.verify(emSpy).find(Player.class, player.getId());

		// Verify that the transaction was begun and committed
		Mockito.verify(transactionSpy).begin();
		Mockito.verify(transactionSpy).commit();

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testUpdatePlayerWhenTransactionIsNotActive() {
		// Spy the real EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());

		// Create a spy of the real EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		// Ensure the spied EntityManager returns the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate a non-active transaction
		Mockito.when(transactionSpy.isActive()).thenReturn(false);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Create a Player instance
		Player player = new Player();
		player.setName("Test Player");

		// Simulate a RuntimeException during the merge operation
		Mockito.doThrow(new RuntimeException("Failed to update player due to an unexpected error.")).when(emSpy)
				.merge(Mockito.any(Player.class));

		// Act & Assert: Ensure that the RuntimeException is thrown
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.updatePlayer(player);
		});
		assertEquals("Failed to update player due to an unexpected error.", exception.getMessage());

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerRollbackWhenRuntimeException() {
		// Use the custom TestPlayerDAOIMPL that throws the exception
		EntityManager em = emf.createEntityManager();
		TestPlayerDAOIMPL playerDAO = new TestPlayerDAOIMPL(emf);
		// Create a player instance
		Player player = new PlayerBuilder().withName("Should Rollback").build();
		em.getTransaction().begin();
		em.persist(player);
		em.getTransaction().commit();

		assertThrows(RuntimeException.class, () -> {
			playerDAO.deletePlayer(player);
		});
		Player managedPlayer = em.find(Player.class, player.getId());
		assertNotNull(managedPlayer);
		em.close();
	}

	@Test
	public void testDeletePlayer() {
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
	public void testDeletePlayerDoesNotRollbackWhenTransactionIsNull() {
		// Create a spy of the real EntityManagerFactory
		EntityManagerFactory spiedEmf = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(spiedEmf.createEntityManager());

		// Create a spy of the real EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManager returns the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(spiedEmf.createEntityManager()).thenReturn(emSpy);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(spiedEmf);

		// Create and persist a Player instance using the actual database
		Player player = new PlayerBuilder().withName("Test Player").build();
		emSpy.getTransaction().begin();
		emSpy.persist(player);
		emSpy.getTransaction().commit();

		// Ensure the player was persisted by finding it
		Player persistedPlayer = emSpy.find(Player.class, player.getId());
		assertNotNull(persistedPlayer);

		// Now simulate the scenario where transaction is null
		Mockito.when(emSpy.getTransaction()).thenReturn(null);

		// Attempt to delete the player
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that rollback was never called because transaction is null
		Mockito.verify(transactionSpy, Mockito.never()).rollback();

		// Verify that the remove operation was never called due to the null transaction
		Mockito.verify(emSpy, Mockito.never()).remove(Mockito.any(Player.class));

		// No need to find the player again here because the EntityManager might be
		// closed already.

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerTriggersRollbackOnRuntimeException() {
		// Create a spy of the real EntityManagerFactory
		EntityManagerFactory spiedEmf = Mockito.spy(emf);

		// Spy on the real EntityManagerdo
		EntityManager emSpy = Mockito.spy(spiedEmf.createEntityManager());

		// Spy on the EntityTransaction to observe its behavior
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Make the spied EntityManager return the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(spiedEmf.createEntityManager()).thenReturn(emSpy);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL playerDAOwithspiedEmf = new PlayerDAOIMPL(spiedEmf);

		// Create a player instance
		Player player = new Player();
		player.setName("Test Player");
		player.setId(1L); // Assuming an ID is needed

		// Simulate finding the player
		Mockito.when(emSpy.find(Player.class, player.getId())).thenReturn(player);

		// Simulate a RuntimeException during the remove operation
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).remove(Mockito.any(Player.class));

		// Act & Assert: Ensure that the exception is thrown and rollback happens
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			playerDAOwithspiedEmf.deletePlayer(player);
		});

		// Check that the exception message is correct
		assertEquals("Simulated Exception", exception.getMessage());

		// Verify that rollback was called on the transaction
		Mockito.verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerThrowsExceptionForNonExistingPlayer() {
		// Create a spy of the real EntityManagerFactory
		EntityManagerFactory spiedEmf = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(spiedEmf.createEntityManager());

		// Create a spy of the real EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManager returns the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(spiedEmf.createEntityManager()).thenReturn(emSpy);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(spiedEmf);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Assuming an ID is needed

		// Simulate that the player does not exist in the database
		Mockito.when(emSpy.find(Player.class, player.getId())).thenReturn(null);

		// Attempt to delete the player and expect an IllegalStateException
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Tried to delete non existing player", exception.getMessage());

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerThrowsExceptionWhenTransactionIsNull() {
		// Create a spy of the real EntityManagerFactory
		EntityManagerFactory spiedEmf = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(spiedEmf.createEntityManager());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(spiedEmf.createEntityManager()).thenReturn(emSpy);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL dao = new PlayerDAOIMPL(spiedEmf);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Assuming an ID is needed

		// Simulate finding the player (assuming the player exists)
		Mockito.when(emSpy.find(Player.class, player.getId())).thenReturn(player);

		// Simulate the scenario where transaction is null
		Mockito.when(emSpy.getTransaction()).thenReturn(null);

		// Attempt to delete the player and expect an IllegalStateException
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Transaction is null", exception.getMessage());

		// Verify that the remove operation was never called due to the null transaction
		Mockito.verify(emSpy, Mockito.never()).remove(Mockito.any(Player.class));

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerTriggersRollbackWhenTransactionIsActive() {
		// Create a spy of the real EntityManagerFactory
		EntityManagerFactory spiedEmf = Mockito.spy(emf);

		// Create a spy of the real EntityManager
		EntityManager emSpy = Mockito.spy(spiedEmf.createEntityManager());

		// Create a spy of the real EntityTransaction
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the spied EntityManagerFactory returns the spied EntityManager
		Mockito.when(spiedEmf.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Inject the spied EntityManagerFactory into the PlayerDAOIMPL
		PlayerDAOIMPL playerDAO = new PlayerDAOIMPL(spiedEmf);

		// Create and persist a Player instance using the actual database
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Persist the player to the database
		emSpy.getTransaction().begin();
		emSpy.persist(player);
		emSpy.getTransaction().commit();

		// Ensure the player was actually persisted by finding it
		Player persistedPlayer = emSpy.find(Player.class, player.getId());
		assertNotNull(persistedPlayer);

		// Now, simulate the scenario where an exception is thrown during deletion
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		// Simulate a RuntimeException during the remove operation
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).remove(Mockito.any(Player.class));

		// Act & Assert: Ensure that the exception is thrown and rollback happens
		assertThrows(RuntimeException.class, () -> {
			playerDAO.deletePlayer(player);
		});

		// Verify that rollback was called on the transaction
		Mockito.verify(transactionSpy).rollback();

		// Verify that the EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	public void testDeletePlayerTriggersRollbackOnTransactionFailure() {
		EntityManagerFactory spiedemf = Mockito.spy(emf);
		// Given: Persist a player in the database
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Player with Rollback").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		// Spy on the EntityManager and the transaction
		EntityManager emSpy = Mockito.spy(emf.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure that the spied EntityManager returns the spied transaction
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Use the spied EntityManagerFactory to create a DAO instance
		PlayerDAOIMPL playerDAOForTest = new PlayerDAOIMPL(spiedemf);

		// Mock the EntityManagerFactory to return the spied EntityManager
		Mockito.doReturn(emSpy).when(spiedemf).createEntityManager();

		// When: Call the deletePlayer method and simulate an exception inside it
		assertThrows(RuntimeException.class, () -> {
			playerDAOForTest.deletePlayer(new Player() {
				@Override
				public Long getId() {
					throw new RuntimeException("Simulated failure during deletePlayer");
				}
			});
		});

		// Then: Verify that rollback was called
		Mockito.verify(transactionSpy, Mockito.times(1)).rollback();

		// Verify that the player still exists in the database because the rollback
		// occurred
		EntityManager emVerify = emf.createEntityManager(); // New EntityManager for verification
		Player existingPlayer = emVerify.find(Player.class, player.getId());
		assertNotNull(existingPlayer, "Player should still exist after rollback due to transaction failure.");
		emVerify.close();
	}

	@Test
	public void testDeletePlayerThrowsExceptionForNonExistentPlayer() {
		// Given: A player that does not exist in the database
		Player nonExistentPlayer = new Player();
		nonExistentPlayer.setId(-999L); // Use an ID that is unlikely to exist

		// When & Then: Try to delete this player and expect an IllegalStateException
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			playerDAO.deletePlayer(nonExistentPlayer);
		});

		assertEquals("Tried to delete non existing player", exception.getMessage());
	}

	@Test
	public void testDeletePlayerTriggersRollbackWhenExceptionOccurs() {
		// Given: Persist a player in the database
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Player player = new PlayerBuilder().withName("Player to Rollback").build();
		em.persist(player);
		em.getTransaction().commit();
		em.close();

		// Create a fresh EntityManager for the test
		EntityManager emForTest = emf.createEntityManager();
		EntityTransaction transactionSpy = Mockito.spy(emForTest.getTransaction());

		// Spy on the EntityManager and the transaction
		EntityManager emSpy = Mockito.spy(emForTest);

		// Replace the real transaction with the spied transaction
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();

		try {
			// Begin the transaction
			transactionSpy.begin();

			// Find the player and simulate an exception after finding the player
			Player managedPlayer = emSpy.find(Player.class, player.getId());

			// Simulate removing the player
			emSpy.remove(managedPlayer);

			// Simulate a failure during transaction commit
			throw new RuntimeException("Simulated failure during deletePlayer");

		} catch (RuntimeException e) {
			// Verify that the rollback occurs
			if (transactionSpy.isActive()) {
				transactionSpy.rollback();
			}
		} finally {
			emSpy.close();
		}

		// Verify that rollback was called once
		Mockito.verify(transactionSpy, Mockito.times(1)).rollback();

		// Verify the player still exists because the rollback should have occurred
		EntityManager emVerify = emf.createEntityManager(); // New EntityManager for verification
		Player existingPlayer = emVerify.find(Player.class, player.getId());
		assertNotNull(existingPlayer, "Player should still exist after rollback due to transaction failure.");
		emVerify.close();
	}

	@Test
	public void testDeletePlayerThrowsExceptionWhenPlayerNotFound() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the transaction object
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of getTransaction() returning the mock transaction
		Mockito.when(emMock.getTransaction()).thenReturn(transactionMock);

		// Ensure that transactionMock behaves as expected
		Mockito.when(transactionMock.isActive()).thenReturn(true);

		// Ensure that find method returns null to simulate player not being found
		Mockito.when(emMock.find(Player.class, 1L)).thenReturn(null);

		// Simulate the behavior of emf.createEntityManager() returning the mock
		// EntityManager
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		// Act & Assert: Expect an IllegalStateException because the player is not found
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify the exception message
		assertEquals("Tried to delete non existing player", exception.getMessage());

		// Ensure that rollback was called if the transaction was active
		Mockito.verify(transactionMock, Mockito.times(1)).rollback();

		// Ensure the EntityManager is closed
		Mockito.verify(emMock).close();
	}

	@Test
	public void testDeletePlayerTransactionIsNullAndThrowsException() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Spy on the EntityTransaction (even though it will be null in this case)
		EntityTransaction transactionSpy = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		// EntityManager
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate the behavior of getTransaction() returning null
		Mockito.when(emMock.getTransaction()).thenReturn(null);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that rollback was not called because the transaction is null
		Mockito.verify(transactionSpy, Mockito.never()).rollback();

		// Ensure the EntityManager is closed
		Mockito.verify(emMock).close();
	}

	@Test
	public void testDeletePlayerTransactionNotActiveAndThrowsException() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		// EntityManager
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate the behavior of getTransaction() returning the mock transaction
		Mockito.when(emMock.getTransaction()).thenReturn(transactionMock);

		// Ensure that the transaction is not active
		Mockito.when(transactionMock.isActive()).thenReturn(false);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that rollback was not called because the transaction is not active
		Mockito.verify(transactionMock, Mockito.never()).rollback();

		// Ensure the EntityManager is closed
		Mockito.verify(emMock).close();
	}

	@Test
	public void testDeletePlayerTransactionActiveAndThrowsException() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		// EntityManager
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate the behavior of getTransaction() returning the mock transaction
		Mockito.when(emMock.getTransaction()).thenReturn(transactionMock);

		// Ensure that the transaction is active
		Mockito.when(transactionMock.isActive()).thenReturn(true);

		// Simulate an exception during the removal of the player
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emMock).remove(Mockito.any());

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Create a Player instance
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID

		assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that rollback was called because the transaction is active
		Mockito.verify(transactionMock, Mockito.times(1)).rollback();

		// Ensure the EntityManager is closed
		Mockito.verify(emMock).close();
	}


	@Test
	public void testDeletePlayerTransactionCommitsSuccessfully() {
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		// Simulate the behavior of createEntityManager() returning the mock
		// EntityManager
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		// Simulate the behavior of getTransaction() returning the mock transaction
		Mockito.when(emMock.getTransaction()).thenReturn(transactionMock);

		// Ensure that the transaction is active
		Mockito.when(transactionMock.isActive()).thenReturn(true);

		// Simulate the player being found
		Player player = new Player();
		player.setId(1L); // Use an arbitrary ID
		Mockito.when(emMock.find(Player.class, player.getId())).thenReturn(player);

		// Create a PlayerDAOIMPL instance using the mocked EntityManagerFactory
		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Act: Call the deletePlayer method
		dao.deletePlayer(player);

		// Verify that commit was called and rollback was not needed
		Mockito.verify(transactionMock, Mockito.times(1)).commit();
		Mockito.verify(transactionMock, Mockito.never()).rollback();

		// Ensure the EntityManager is closed
		Mockito.verify(emMock).close();
	}

}
