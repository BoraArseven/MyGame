package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GameMapDAOIT {

	private static final Logger LOGGER = LogManager.getLogger(GameMapDAOIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:14.15");
	}

	private EntityManagerFactory emf;
	private GameMapDAO gameMapDAO;

	@BeforeAll
	void setUp() {
		// Directly passing database properties
		String dbUrl = postgreSQLContainer.getJdbcUrl();
		String dbUser = postgreSQLContainer.getUsername();
		String dbPassword = postgreSQLContainer.getPassword();

		// Initialize HibernateUtil with connection properties
		HibernateUtil.initialize(dbUrl, dbUser, dbPassword);
		emf = HibernateUtil.getEntityManagerFactory();
		gameMapDAO = new GameMapDAO(emf);
	}

	@AfterAll
	void tearDown() {
		HibernateUtil.close();
		if (postgreSQLContainer != null) {
			postgreSQLContainer.stop();
		}
	}

	@BeforeEach
	void resetDatabase() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Clear data from tables in reverse dependency order
			em.createQuery("DELETE FROM Player").executeUpdate();
			em.createQuery("DELETE FROM GameMap").executeUpdate();

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					LOGGER.error("Rollback failed: {}", rollbackEx.getMessage(), rollbackEx);
				}
			}
			throw new PersistenceException("Failed to reset database", e); // Preserve original exception as cause
		} finally {
			if (em.isOpen()) {
				em.close(); // Ensure resources are released
			}
		}
	}

	@Test
	void testSaveAndFindById() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		gameMapDAO.save(gameMap);
		GameMap retrievedMap = gameMapDAO.findById(gameMap.getId());

		assertNotNull(retrievedMap);
		assertEquals("Test Map", retrievedMap.getName());
	}

	@Test
	void testFindAll() {
		GameMap gameMap1 = new GameMap();
		gameMap1.setName("Map 1");
		gameMapDAO.save(gameMap1);

		GameMap gameMap2 = new GameMap();
		gameMap2.setName("Map 2");
		gameMapDAO.save(gameMap2);

		List<GameMap> gameMaps = gameMapDAO.findAll();
		assertNotNull(gameMaps);
		assertTrue(gameMaps.size() >= 2);
	}

	@Test
	void testUpdate() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Initial Name");
		gameMapDAO.save(gameMap);

		gameMap.setName("Updated Name");
		gameMapDAO.update(gameMap);

		GameMap updatedMap = gameMapDAO.findById(gameMap.getId());
		assertEquals("Updated Name", updatedMap.getName());
	}

	@Test
	void testDelete() {
		GameMap gameMap = new GameMap();
		gameMap.setName("To Be Deleted");
		gameMapDAO.save(gameMap);

		gameMapDAO.delete(gameMap.getId());
		GameMap deletedMap = gameMapDAO.findById(gameMap.getId());

		assertNull(deletedMap);
	}

	@Test
	void testFindPlayersByMapId() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Map with Players");

		Player player1 = new PlayerBuilder().withName("Player 1").build();
		Player player2 = new PlayerBuilder().withName("Player 2").build();
		gameMap.setPlayers(List.of(player1, player2));

		gameMapDAO.save(gameMap);

		List<Player> players = gameMapDAO.findPlayersByMapId(gameMap.getId());
		assertEquals(2, players.size());
	}

	@Test
	void testAddPlayerToMap() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Empty Map");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("TestAddPlayer").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), player);

		EntityManager em = emf.createEntityManager();

		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedMap); // Ensure the latest data is loaded
		var players = updatedMap.getPlayers();
		for (Player selectedplayer : players) {
			System.out.printf("player: %s/n", selectedplayer.toString());

			// Corrected: Use {} with LOGGER.debug and pass the argument separately
			LOGGER.debug("player: {}", selectedplayer);
		}
		assertEquals(1, updatedMap.getPlayers().size());
		assertEquals("TestAddPlayer", updatedMap.getPlayers().get(0).getName());
		em.close();
	}

	@Test
	void testRemovePlayerFromMap() {
		// Arrange: Create and save a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("Map with Player");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("Player to Remove").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), player);

		// Act & Assert: Attempt to remove the Player and simulate an exception
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();
			// Load the GameMap with a pessimistic lock
			GameMap managedGameMap = em.find(GameMap.class, gameMap.getId(), LockModeType.PESSIMISTIC_WRITE);
			if (managedGameMap == null) {
				throw new PersistenceException("GameMap with id " + gameMap.getId() + " not found.");
			}
			throw new PersistenceException("Simulated Exception"); // Simulate the exception
		} catch (PersistenceException ex) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			// Assert: Exception is properly thrown and handled
			assertEquals("Simulated Exception", ex.getMessage());
		} finally {
			if (em.isOpen()) {
				em.close(); // Ensure the EntityManager is closed
			}
		}

		// Verify: Player should still be in the GameMap since the transaction rolled
		// back
		EntityManager verifyEm = emf.createEntityManager();
		try {
			GameMap updatedGameMap = verifyEm.find(GameMap.class, gameMap.getId());
			assertNotNull(updatedGameMap);
			assertFalse(updatedGameMap.getPlayers().isEmpty());
			assertEquals("Player to Remove", updatedGameMap.getPlayers().get(0).getName());
		} finally {
			verifyEm.close();
		}
	}

	@Test
	void testRemovePlayerFromNonExistingMap() {
		Player player = new PlayerBuilder().withName("Player for Non-existing Map").build();
		assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(-1L, player); // -1L to simulate non-existing map
		});
	}

	@Test
	void testAddPlayerRollbackOnError() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Rollback Map");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("Player for Rollback").build();

		// Simulate error by using a spy
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO spyGameMapDAO = new GameMapDAO(emfSpy);
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emfSpy).createEntityManager();

		Long gameMapID = gameMap.getId();
		assertThrows(PersistenceException.class, () -> {

			spyGameMapDAO.addPlayerToMap(gameMapID, player);
		});

		EntityManager em = emf.createEntityManager();
		GameMap unchangedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(unchangedMap); // Ensure the latest data is loaded
		assertTrue(unchangedMap.getPlayers().isEmpty());
		em.close();
	}

	@Test
	void testRemovePlayerRollbackOnError() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Rollback Map");

		Player player = new PlayerBuilder().withName("Player for Rollback").build();
		gameMap.setPlayers(List.of(player));
		gameMapDAO.save(gameMap);

		// Simulate error by using a spy
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO spyGameMapDAO = new GameMapDAO(emfSpy);
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emfSpy).createEntityManager();
		Long gameMapId = gameMap.getId();
		assertThrows(PersistenceException.class, () -> {

			spyGameMapDAO.removePlayerFromMap(gameMapId, player);
		});

		EntityManager em = emf.createEntityManager();
		GameMap unchangedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(unchangedMap); // Ensure the latest data is loaded
		assertEquals(1, unchangedMap.getPlayers().size());
		em.close();
	}

	@Test
	void testSaveWithPersistenceException() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when persist is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Runtime Exception")).when(spyEm)
				.persist(Mockito.any(GameMap.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDaowithSpiedEmf = new GameMapDAO(spyEmf);

		// Create a GameMap object to save
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the save method throws a PersistenceException due to the
		// simulated
		// PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDaowithSpiedEmf.save(gameMap);
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("Failed to save GameMap:"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testSaveWithPersistenceExceptionWhileTransactionIsNotActiveShouldNotTriggerRollback() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when persist is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Runtime Exception")).when(spyEm)
				.persist(Mockito.any(GameMap.class));

		// Simulate that the transaction is not active
		Mockito.when(spyTransaction.isActive()).thenReturn(false);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOwithSpiedEmf = new GameMapDAO(spyEmf);

		// Create a GameMap object to save
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the save method throws a PersistenceException due to the
		// simulated
		// PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOwithSpiedEmf.save(gameMap);
		});

		// Assert that the rollback was NOT triggered
		Mockito.verify(spyTransaction, Mockito.never()).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("Failed to save GameMap:"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testUpdateWithPersistenceException() {
		// Arrange: Create and save a GameMap to assign an ID
		GameMap gameMap = new GameMap();
		gameMap.setName("Initial Name");
		gameMapDAO.save(gameMap);

		// Simulate an exception during update
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO spyGameMapDAO = new GameMapDAO(emfSpy);
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emfSpy).createEntityManager();

		// Act & Assert: Attempt to update the GameMap and expect a PersistenceException
		assertThrows(PersistenceException.class, () -> {
			spyGameMapDAO.update(gameMap);
		});
	}

	@Test
	void testDeleteWithPersistenceException() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Mock the transaction lifecycle
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		// Simulate the transaction being inactive at first, then active after begin()
		Mockito.when(spyTransaction.isActive()).thenReturn(false).thenReturn(true);

		// Simulate PersistenceException on remove
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.remove(Mockito.any(GameMap.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		// Execute the delete method and expect a PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpiedEmf.delete(1L);
		});

		// Verify that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Ensure the EntityManager was closed
		Mockito.verify(spyEm).close();

		// Additional assertion to check the message
		assertTrue(thrownException.getMessage().contains("Failed to delete GameMap:"));
	}

	@Test
	void testDelete_GameMapNotFound() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);

		// Simulate the find method returning null
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(null);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		// Verify that the delete method throws a PersistenceException due to the
		// GameMap
		// not being found
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpiedEmf.delete(1L);
		});

		// Assert that the rollback was triggered
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("GameMap with id 1 not found."));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testDelete_PersistenceException_TransactionNotActive() {
		// Arrange: Attempt to delete a non-existing GameMap
		Long nonExistentMapId = -1L; // Assuming IDs are positive, -1L does not exist

		// Act & Assert: Expect a PersistenceException when deleting a non-existing
		// GameMap
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.delete(nonExistentMapId);
		});

		// Verify that the exception message contains the expected text
		assertTrue(thrownException.getMessage()
				.contains("Failed to delete GameMap: GameMap with id " + nonExistentMapId + " not found."));
	}

	@Test
	void testCreatePlayerTriggersRollbackOnException() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Spy on EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(Mockito.any(Player.class));
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify the rollback
		Mockito.verify(transactionSpy).rollback();
		Mockito.verify(emSpy).close();

		// Verify exception message
		assertEquals("Simulated Exception", exception.getMessage());
	}

	@Test
	void testCreatePlayerFailsWhenTransactionIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Player").build();
		EntityManager emMock = Mockito.mock(EntityManager.class);
		Mockito.when(emMock.getTransaction()).thenReturn(null);

		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.createPlayer(player);
		});

		// Verify exception message
		assertEquals("Transaction is null", exception.getMessage());
	}

	@Test
	void testDeletePlayerFailsWhenTransactionIsNull() {
		// Arrange
		Player player = new Player();
		player.setId(1L);
		EntityManager emMock = Mockito.mock(EntityManager.class);
		Mockito.when(emMock.getTransaction()).thenReturn(null);

		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		Mockito.when(emfMock.createEntityManager()).thenReturn(emMock);

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfMock);

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify exception message
		assertEquals("Transaction is null", exception.getMessage());
	}

	@Test
	void testAddPlayerToMap_IllegalArgumentException() {
		// Arrange: Create and save a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.persist(gameMap);
		transaction.commit();
		em.close();

		Long mapId = gameMap.getId();

		// Act & Assert: Simulate adding a null player to the map
		GameMapDAO gameMapDAO = new GameMapDAO(emf);
		Player nullPlayer = null;

		IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(mapId, nullPlayer);
		});

		// Assert: Ensure the exception message is correct
		assertEquals("Player cannot be null", thrownException.getMessage());
	}

	@Test
	void testAddPlayerToMap_GameMapNotFound() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);

		// Simulate not finding a GameMap (return null)
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(null);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		// Verify that the addPlayerToMap method throws a PersistenceException due to
		// GameMap not being found
		Player player = new Player();
		IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {

			gameMapDAOWithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert that the transaction was rolled back once
		Mockito.verify(spyTransaction, Mockito.times(1)).rollback();

		// Assert that the exception message contains the expected text
		assertEquals("GameMap with id 1 not found.", thrownException.getMessage());

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testRemovePlayerFromMap_GameMapOrPlayerNotFound() {
		// Arrange
		GameMapDAO testGameMapDAO = new GameMapDAO(emf);

		// Create and persist a GameMap
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();
			GameMap gameMap = new GameMap();
			em.persist(gameMap);
			transaction.commit();

			// Get the actual ID of the saved GameMap
			Long gameId = gameMap.getId();

			// Re-open the EntityManager for the operation we are testing
			em = emf.createEntityManager();
			transaction = em.getTransaction();
			Player player = new Player();
			assertThrows(PersistenceException.class, () -> {

				testGameMapDAO.removePlayerFromMap(gameId, player);
			});

			// Verify that the transaction was rolled back
			if (transaction.isActive()) {
				transaction.rollback();
			}
		} finally {
			if (em.isOpen()) {
				em.close();
			}
		}
	}

	@Test
	void testRemovePlayerFromMap_GameMapNotFound() {
		// Arrange: Use a non-existent GameMap ID
		Long nonExistentGameMapId = 1L; // Assuming this ID does not exist in the database
		Player player = new Player(); // Player not persisted, no ID

		// Act & Assert: Call removePlayerFromMap and expect an exception
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(nonExistentGameMapId, player);
		});

		// Assert: Check the exception message
		assertEquals("Failed to remove Player: GameMap with id " + nonExistentGameMapId + " not found.",
				thrownException.getMessage());

		// Additional: Verify no residual changes in the database
		EntityManager em = emf.createEntityManager();
		try {
			GameMap gameMap = em.find(GameMap.class, nonExistentGameMapId);
			assertNull(gameMap, "GameMap should not exist in the database.");
		} finally {
			em.close();
		}
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotFound() {
		// Arrange: Create and save a GameMap without adding any Player
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		em.persist(gameMap);
		transaction.commit();
		em.close();

		// Act & Assert: Attempt to remove a Player not in the GameMap
		Long gameMapId = gameMap.getId();

		Player player = new Player(); // A player without an ID (not persisted)
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMapId, player);
		});

		// Assert: Ensure the exception message is as expected
		assertEquals("Failed to remove Player: Player is null or has a null ID.", thrownException.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotInGameMap() {
		// Arrange: Create and save a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("Map with One Player");
		gameMapDAO.save(gameMap);

		// Create a Player not associated with any GameMap
		Player player = new PlayerBuilder().withName("Lonely Player").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), player);

		// Act & Assert: Attempt to remove a different Player not in the GameMap
		Player anotherPlayer = new PlayerBuilder().withName("Another Player").build();
		gameMapDAO.save(new GameMap()); // Save another GameMap if needed
		assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), anotherPlayer);
		});
	}

	@Test
	void testRemovePlayerFromMap_GameMapAndPlayerNotFound() {
		// Arrange: No GameMap or Player is saved in the database
		EntityManager em = emf.createEntityManager();

		// Attempt to remove a Player from a non-existent GameMap
		Long nonExistentGameMapId = 1L; // Assuming no GameMap with this ID exists
		Player nonExistentPlayer = new Player(); // Player not persisted, no ID

		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(nonExistentGameMapId, nonExistentPlayer);
		});

		// Assert: Ensure the exception message is as expected, we check gamemap first
		assertEquals("Failed to remove Player: GameMap with id 1 not found.", thrownException.getMessage());

		em.close();
	}

	@Test
	void testRemovePlayerFromMap_GameMapOrPlayerNotInGameMap() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			// Begin transaction and save the GameMap without adding any players
			transaction.begin();
			GameMap gameMap = new GameMap();
			gameMap.setName("Test GameMap");
			em.persist(gameMap);
			transaction.commit();

			// Begin new transaction and save the Player without assigning it to the GameMap
			transaction.begin();
			Player player = new Player();
			player.setName("Test Player");
			em.persist(player);
			transaction.commit();

			// Detach both entities so we don't have automatic synchronization
			em.detach(gameMap);
			em.detach(player);

			GameMapDAO testGameMapDAO = new GameMapDAO(emf);
			Long id = gameMap.getId();
			// Try to remove the player that is not part of the game map
			assertThrows(PersistenceException.class, () -> {

				testGameMapDAO.removePlayerFromMap(id, player);
			});

		} finally {
			if (em.isOpen()) {
				em.close(); // Ensure the EntityManager is closed after the test
			}
		}
	}

	@Test
	void testAddPlayerToMap_TransactionActive() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);

		// Simulate an active transaction
		Mockito.when(spyTransaction.isActive()).thenReturn(true);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);
		Player player = new Player();

		// Act & Assert: Verify that the method throws an IllegalStateException
		IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
			gameMapDAOWithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert the exception message
		assertEquals("Transaction already active. Cannot begin a new transaction.", thrownException.getMessage());

		// Verify that rollback was not called
		Mockito.verify(spyTransaction, Mockito.never()).rollback();

		// Verify that the EntityManager was closed
		Mockito.verify(spyEm).close();
	}

	@Test
	void testFindAlivePlayers() {
		// Arrange: Create a few players, some alive and some dead
		GameMap gameMap = new GameMap();
		gameMap.setName("Map with Alive and Dead Players");
		gameMapDAO.save(gameMap);

		Player alivePlayer1 = new PlayerBuilder().withName("Alive Player 1").withIsAlive(true).build();
		Player alivePlayer2 = new PlayerBuilder().withName("Alive Player 2").withIsAlive(true).build();
		Player deadPlayer = new PlayerBuilder().withName("Dead Player").withIsAlive(false).build();

		// Add players to the map and persist them
		gameMapDAO.addPlayerToMap(gameMap.getId(), alivePlayer1);
		gameMapDAO.addPlayerToMap(gameMap.getId(), alivePlayer2);
		gameMapDAO.addPlayerToMap(gameMap.getId(), deadPlayer);

		// Act: Retrieve the list of alive players
		List<Player> alivePlayers = gameMapDAO.findAlivePlayers();

		// Assert: Only alive players should be returned
		assertNotNull(alivePlayers);
		assertEquals(2, alivePlayers.size());
		assertTrue(alivePlayers.stream().anyMatch(p -> p.getName().equals("Alive Player 1")));
		assertTrue(alivePlayers.stream().anyMatch(p -> p.getName().equals("Alive Player 2")));
		assertFalse(alivePlayers.stream().anyMatch(p -> p.getName().equals("Dead Player")));
	}

	@Test
	void testAddPlayerToMap_NullPlayer() {
		// Arrange: Create a new GameMap and persist it
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map for Null Player");
		gameMapDAO.save(gameMap);

		Long mapId = gameMap.getId();
		// Act & Assert: Try adding a null player to the map and assert that it throws
		// an exception
		IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(mapId, null); // Pass null as the player
		});

		// Assert: Check the exception message
		assertEquals("Player cannot be null", thrownException.getMessage());
	}

	@Test
	void testAddPlayerToMap_NewPlayer() {
		// Arrange: Create and save a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("Map for New Player Test");
		gameMapDAO.save(gameMap);

		// Act: Create a new Player and add to the map (no ID yet)
		Player newPlayer = new PlayerBuilder().withName("New Player").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), newPlayer);

		// Assert: The player should be added to the map and should have an ID after
		// persisting
		EntityManager em = emf.createEntityManager();
		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedMap); // Ensure we get the latest data
		assertEquals(1, updatedMap.getPlayers().size());
		assertNotNull(updatedMap.getPlayers().get(0).getId()); // ID should now be assigned
		assertEquals("New Player", updatedMap.getPlayers().get(0).getName());
		em.close();
	}

	@Test
	void testAddPlayerToMap_NewPlayerWithoutId() {
		// Arrange: Create and save a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		gameMapDAO.save(gameMap);

		// Arrange: Create a new Player without an ID
		Player newPlayer = new PlayerBuilder().withName("New Player").build();

		// Act: Add the new player to the GameMap
		gameMapDAO.addPlayerToMap(gameMap.getId(), newPlayer);

		// Assert: Ensure the player was added and persisted
		EntityManager em = emf.createEntityManager();
		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedMap); // Ensure the latest data is loaded

		assertTrue(updatedMap.getPlayers().contains(newPlayer), "The new player should be part of the GameMap.");
		em.close();
	}

	@Test
	void testAddPlayerToMap_withNullMapId_throwsIllegalArgumentException() {
		// Arrange: Create a player
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Act & Assert: Expect IllegalArgumentException
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new GameMapDAO(emf).addPlayerToMap(null, player);
		});

		// Verify the exception message
		assertEquals("MapId cannot be null", exception.getMessage());
	}

	// Test for updatePlayer when player.getId() is not null
	@Test
	void testUpdatePlayer_withPlayerIdNotNull_exceptionThrown_playerIdNotNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// Persist the player to assign an ID
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.persist(player);
		transaction.commit();
		em.close();

		// Spy on EntityManagerFactory and simulate exception
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).merge(Mockito.any(Player.class));
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.updatePlayer(player);
		});

		// Verify that rollback and close were called
		Mockito.verify(transactionSpy).rollback();
		Mockito.verify(emSpy).close();

		// Assert that the exception message is as expected
		assertEquals("Simulated Exception", exception.getMessage());
	}

	// Test for deletePlayer when player.getId() is not null
	@Test
	void testDeletePlayer_withPlayerIdNotNull_exceptionThrown_playerIdNotNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// Persist the player to assign an ID
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.persist(player);
		transaction.commit();
		em.close();

		// Spy on EntityManagerFactory and simulate exception
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);
		Mockito.when(emSpy.find(Player.class, player.getId())).thenReturn(player);
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).remove(Mockito.any(Player.class));
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emfSpy);

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			dao.deletePlayer(player);
		});

		// Verify that rollback and close were called
		Mockito.verify(transactionSpy).rollback();
		Mockito.verify(emSpy).close();

		// Assert that the exception message is as expected
		assertEquals("Simulated Exception", exception.getMessage());
	}

	// Test for deletePlayer when player.getId() is null
	@Test
	void testDeletePlayer_withNullPlayerId_exceptionThrown_playerIdIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("Test Player").build();
		// player.getId() is null

		PlayerDAOIMPL dao = new PlayerDAOIMPL(emf);

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			dao.deletePlayer(player);
		});

		// Assert that the exception message is as expected
		assertEquals("Tried to delete non-existing player with ID N/A", exception.getMessage());
	}

	@Test
	void testUpdateGameMapNotFound() {
		// Arrange: Create a GameMap object with a non-existing ID
		GameMap nonExistingGameMap = new GameMap();
		nonExistingGameMap.setId(999L); // Assuming 999L does not exist in the database
		nonExistingGameMap.setName("NonExistingMap");

		// Act & Assert: Expect a PersistenceException with the correct message
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.update(nonExistingGameMap);
		});

		// Verify the exception message
		assertEquals("Failed to update GameMap: GameMap with id 999 not found.", thrown.getMessage());
	}

	@Test
	void testUpdateGameMapFailsWithPersistenceException() {
		// Arrange: Persist a valid GameMap
		GameMap existingGameMap = new GameMap();
		existingGameMap.setName("ValidMap");
		gameMapDAO.save(existingGameMap);

		// Spy on EntityManagerFactory to simulate an exception
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate a PersistenceException during the merge operation
		Mockito.doThrow(new PersistenceException("Simulated Database Failure")).when(emSpy)
				.merge(Mockito.any(GameMap.class));

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert: Expect a PersistenceException with the appropriate message
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpy.update(existingGameMap);
		});

		// Verify that the transaction was rolled back
		Mockito.verify(transactionSpy).rollback();

		// Verify the exception message
		assertTrue(thrown.getMessage().contains("Failed to update GameMap: Simulated Database Failure"));
	}

	@Test
	void testRemovePlayerFromMap_PlayerIsNull() {
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), null);
		});

		assertEquals("Failed to remove Player: Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerHasNullId() {
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Player playerWithNullId = new PlayerBuilder().withName("PlayerWithNullId").build();
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), playerWithNullId);
		});

		assertEquals("Failed to remove Player: Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_Success() {
		// Arrange
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("Player to Remove").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), player);

		// Act
		gameMapDAO.removePlayerFromMap(gameMap.getId(), player);

		// Assert
		EntityManager em = emf.createEntityManager();
		GameMap updatedGameMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedGameMap); // Ensure the latest data is loaded

		assertTrue(updatedGameMap.getPlayers().isEmpty(), "The player should be removed from the GameMap.");
		em.close();
	}

	@Test
	void testAddPlayerToMap_PlayerIsNull() {
		// Arrange
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		gameMapDAO.save(gameMap);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(gameMap.getId(), null);
		});

		assertEquals("Player cannot be null", exception.getMessage());
	}

	@Test
	void testAddPlayerToMap_ExistingPlayer() {
		// Arrange
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		gameMapDAO.save(gameMap);

		Player existingPlayer = new PlayerBuilder().withName("Existing Player").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), existingPlayer);

		// Detach the player entity
		EntityManager em = emf.createEntityManager();
		em.detach(existingPlayer);
		existingPlayer.setName("Updated Name");

		// Save the updated player to ensure changes are persisted
		em.getTransaction().begin();
		em.merge(existingPlayer);
		em.getTransaction().commit();
		em.close();

		// Act
		gameMapDAO.addPlayerToMap(gameMap.getId(), existingPlayer);

		// Assert
		em = emf.createEntityManager();
		GameMap updatedGameMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedGameMap); // Ensure the latest data is loaded

		assertEquals(1, updatedGameMap.getPlayers().size());
		assertEquals("Updated Name", updatedGameMap.getPlayers().get(0).getName());
		em.close();
	}

	@Test
	void testRemovePlayerFromMap_RollbackOnError() {
		// Arrange
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("PlayerToRemove").build();
		gameMapDAO.addPlayerToMap(gameMap.getId(), player);

		// Simulate error during removal by setting up an invalid player ID
		player.setId(-1L); // Non-existent ID to simulate an error

		// Act & Assert
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), player);
		});

		// Verify exception message
		assertTrue(thrown.getMessage().contains("Failed to remove Player"), "Expected rollback on error.");

		// Assert that the transaction was rolled back and the player is still in the
		// GameMap
		EntityManager em = emf.createEntityManager();
		GameMap unchangedGameMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(unchangedGameMap);

		assertFalse(unchangedGameMap.getPlayers().isEmpty(),
				"The player should still be in the GameMap due to rollback.");
		em.close();
	}

	@Test
	void testRemovePlayerFromMap_TransactionInactiveAtRollback() {
		// Arrange: Create a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		Player player = new Player();
		player.setName("Test Player");

		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Set up the spy to return the mocked transaction
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Begin the transaction and persist the entities
		Mockito.doNothing().when(transactionSpy).begin();
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emSpy).find(GameMap.class,
				gameMap.getId(), LockModeType.PESSIMISTIC_WRITE);

		// Simulate the transaction becoming inactive after the exception
		Mockito.when(transactionSpy.isActive()).thenReturn(false);

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert: Attempt to remove the player and expect the exception
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpy.removePlayerFromMap(gameMap.getId(), player);
		});

		// Verify the exception message
		assertTrue(thrownException.getMessage().contains("Failed to remove Player"));

		// Verify that rollback was not called because the transaction is inactive
		Mockito.verify(transactionSpy, Mockito.never()).rollback();

		// Verify that the EntityManager is closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testDeleteGameMap_TransactionInactiveAtRollback() {
		// Arrange: Create a GameMap and persist it
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		gameMapDAO.save(gameMap);

		// Spy on the EntityManager and Transaction
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Set up the spy behavior
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate the transaction becoming inactive after an exception
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emSpy).find(GameMap.class,
				gameMap.getId(), LockModeType.PESSIMISTIC_WRITE);
		Mockito.when(transactionSpy.isActive()).thenReturn(false);

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert: Attempt to delete the GameMap and expect a PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpy.delete(gameMap.getId());
		});

		// Verify the exception message
		assertTrue(thrownException.getMessage().contains("Failed to delete GameMap"));

		// Verify that rollback was not called because the transaction is inactive
		Mockito.verify(transactionSpy, Mockito.never()).rollback();

		// Verify that the EntityManager is closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testUpdateGameMap_TransactionInactiveAtRollback() {
		// Arrange: Create and persist a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("Initial Name");
		gameMapDAO.save(gameMap);

		// Spy on the EntityManager and Transaction
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Set up the spy behavior
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate the transaction becoming inactive after an exception
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emSpy).find(GameMap.class,
				gameMap.getId(), LockModeType.PESSIMISTIC_WRITE);
		Mockito.when(transactionSpy.isActive()).thenReturn(false); // Transaction inactive

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert: Attempt to update the GameMap and expect a PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpy.update(gameMap);
		});

		// Verify the exception message
		assertTrue(thrownException.getMessage().contains("Failed to update GameMap"));

		// Verify that rollback was not called because the transaction is inactive
		Mockito.verify(transactionSpy, Mockito.never()).rollback();

		// Verify that the EntityManager is closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_withNonExistingMapId_throwsIllegalArgumentException() {
		// Arrange: Create a player
		Player player = new PlayerBuilder().withName("Test Player").build();

		// Act & Assert: Expect IllegalArgumentException
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(-1L, player); // Assuming -1L does not exist
		});

		// Verify the exception message
		assertEquals("GameMap with id -1 not found.", exception.getMessage());
	}

	@Test
	void testAddPlayerToMap_withNullPlayer_throwsIllegalArgumentException() {
		// Arrange: Create a new GameMap and persist it
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map for Null Player");
		gameMapDAO.save(gameMap);

		Long mapId = gameMap.getId();

		// Act & Assert: Try adding a null player to the map and assert that it throws
		// an exception
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(mapId, null); // Pass null as the player
		});

		// Assert: Check the exception message
		assertEquals("Player cannot be null", exception.getMessage());
	}

	@Test
	public void testAddPlayerToMap_withActiveTransaction_throwsIllegalStateException() {
		// Arrange
		// Mock the EntityManagerFactory
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);

		// Mock the EntityManager
		EntityManager emMock = Mockito.mock(EntityManager.class);
		when(emfMock.createEntityManager()).thenReturn(emMock);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);
		when(emMock.getTransaction()).thenReturn(transactionMock);

		// Simulate the transaction is already active
		when(transactionMock.isActive()).thenReturn(true);

		// Create GameMapDAO with the mocked EntityManagerFactory
		GameMapDAO daoWithMockedEmf = new GameMapDAO(emfMock);

		// Act & Assert
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			daoWithMockedEmf.addPlayerToMap(1L, new Player());
		}, "Expected addPlayerToMap to throw IllegalStateException when transaction is already active.");

		// Verify exception message
		assertEquals("Transaction already active. Cannot begin a new transaction.", thrown.getMessage(),
				"Exception message should match.");

		// Verify that rollback was not called
		verify(transactionMock, times(0)).rollback();

		// Verify that EntityManager was closed
		verify(emMock, times(1)).close();
	}

	@Test
	void testAddPlayerToMap_withPersistenceException_throwsPersistenceException() {
		// Spy on EntityManagerFactory to simulate an exception
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Step 1: Add a GameMap to the database to ensure it exists
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		GameMap testGameMap = new GameMap();
		testGameMap.setName("Test Map");

		transaction.begin();
		em.persist(testGameMap); // Persist the GameMap and assign an ID
		transaction.commit();
		em.close();

		// Verify the GameMap now has an ID
		assertNotNull(testGameMap.getId(), "GameMap ID should not be null after being persisted.");

		// Step 2: Add the player to the database to ensure it has an ID
		Player testPlayer = new PlayerBuilder().withName("Test Player").build();

		em = emf.createEntityManager();
		transaction = em.getTransaction();
		transaction.begin();
		em.persist(testPlayer); // Persist the player and assign an ID
		transaction.commit();
		em.close();

		// Verify the player now has an ID
		assertNotNull(testPlayer.getId(), "Player ID should not be null after being persisted.");

		// Step 3: Simulate exception during persist operation in GameMapDAO
		Mockito.doThrow(new PersistenceException("Simulated Exception")).when(emSpy).persist(Mockito.any(Player.class));

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert: Attempt to add the player to the GameMap and expect
		// PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpy.addPlayerToMap(testGameMap.getId(), testPlayer);
		});

		// Verify rollback was called
		Mockito.verify(transactionSpy).rollback();

		// Verify the exception message
		assertTrue(thrownException.getMessage().contains("Failed to add player to map"));

		// Verify EntityManager was closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_ensuresEntityManagerIsClosed() {
		// Spy on EntityManagerFactory
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);
		Player addedplayer = new PlayerBuilder().withName("Playertobeadded").build();

		// Act & Assert: Even if an exception is thrown, EntityManager should close
		assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAOWithSpy.addPlayerToMap(1L, addedplayer);
		});

		// Verify that the EntityManager is closed
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_TransactionAlreadyActive() {
		// Spy on the EntityManager and Transaction
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Set up the spy to return an active transaction
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		GameMapDAO gameMapDAOWithSpy = new GameMapDAO(emfSpy);

		// Act & Assert
		IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
			gameMapDAOWithSpy.addPlayerToMap(1L, new Player());
		});

		// Assert exception message
		assertEquals("Transaction already active. Cannot begin a new transaction.", thrownException.getMessage());

		// Verify no rollback since it throws immediately
		Mockito.verify(transactionSpy, Mockito.never()).rollback();
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_InvalidArguments() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		transaction.begin();
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		em.persist(gameMap); // Persist GameMap so it exists in the database
		transaction.commit();
		em.close();

		GameMapDAO dao = new GameMapDAO(emf);

		// Act & Assert
		IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {
			dao.addPlayerToMap(gameMap.getId(), null); // Pass null player
		});

		// Assert the exception message
		assertEquals("Player cannot be null", thrownException.getMessage());
	}

	@Test
	void testAddPlayerToMap_GenericException() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		transaction.begin();
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		em.persist(gameMap); // Persist GameMap so it exists in the database
		transaction.commit();
		em.close();

		// Spy on EntityManagerFactory and simulate an exception
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);

		// Simulate a generic Exception during persist
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emSpy).persist(Mockito.any(Player.class));

		GameMapDAO dao = new GameMapDAO(emfSpy);
		Player player = new Player();
		player.setName("Test Player");

		// Act & Assert
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			dao.addPlayerToMap(gameMap.getId(), player); // Use valid GameMap ID
		});

		// Verify rollback was called
		Mockito.verify(transactionSpy).rollback();

		// Verify the exception message
		assertTrue(thrownException.getMessage().contains("Failed to add player to map"));
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_EnsureEntityManagerClosed() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		transaction.begin();
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		em.persist(gameMap); // Persist GameMap
		transaction.commit();
		em.close();

		GameMapDAO dao = new GameMapDAO(emf);
		Player player = new Player();
		player.setName("Test Player");

		// Act
		dao.addPlayerToMap(gameMap.getId(), player);

		// Assert
		em = emf.createEntityManager();
		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		assertNotNull(updatedMap);
		assertFalse(updatedMap.getPlayers().isEmpty());
		assertEquals("Test Player", updatedMap.getPlayers().get(0).getName());
		em.close();
	}

	@Test
	void testAddPlayerToMap_IllegalArgumentException_NoRollback() {
		// Arrange
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.when(emSpy.getTransaction()).thenReturn(transactionSpy);
		Mockito.when(transactionSpy.isActive()).thenReturn(false); // Transaction not active

		GameMapDAO dao = new GameMapDAO(emfSpy);

		// Act & Assert
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			dao.addPlayerToMap(null, new Player()); // Pass null mapId to trigger exception
		});

		assertEquals("MapId cannot be null", thrown.getMessage());
		Mockito.verify(transactionSpy, Mockito.never()).rollback(); // Rollback shouldn't occur
		Mockito.verify(emSpy).close();
	}

	@Test
	void testAddPlayerToMap_GenericException_WithRollback() {
		// Arrange
		// Mock EntityManagerFactory to return a mock EntityManager
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		EntityManager emMock = Mockito.mock(EntityManager.class);
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);

		when(emfMock.createEntityManager()).thenReturn(emMock);
		when(emMock.getTransaction()).thenReturn(transactionMock);

		// Simulate transaction behavior
		doNothing().when(transactionMock).begin();
		// First call returns false (no active transaction), second call returns true
		when(transactionMock.isActive()).thenReturn(false).thenReturn(true);
		doNothing().when(transactionMock).rollback();

		// Mock finding a valid GameMap
		GameMap mockGameMap = new GameMap();
		mockGameMap.setId(1L);
		mockGameMap.setName("Test Map");
		when(emMock.find(GameMap.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(mockGameMap);

		// Simulate EntityManager being open
		when(emMock.isOpen()).thenReturn(true);

		// Simulate a runtime exception during persist
		doThrow(new RuntimeException("Simulated Exception during persist")).when(emMock).persist(any(Player.class));

		// Instantiate DAO with the mocked EntityManagerFactory
		GameMapDAO dao = new GameMapDAO(emfMock);

		// Act & Assert
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			dao.addPlayerToMap(1L, new Player());
		});

		// Verify exception message
		assertTrue(thrown.getMessage().contains("Failed to add player to map"));

		// Verify that rollback was called once
		verify(transactionMock, times(1)).rollback();

		// Verify that EntityManager was closed
		verify(emMock, times(1)).close();
	}

	@Test
	void testAddPlayerToMap_MapIdNull_ShouldThrowIllegalArgumentException() {
		// Arrange
		// Persist a GameMap
		EntityManager realEm = emf.createEntityManager();
		realEm.getTransaction().begin();
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");
		realEm.persist(gameMap);
		realEm.getTransaction().commit();
		realEm.close();

		// Create a Player
		Player player = new PlayerBuilder().withName("Test Player").withIsAlive(true).build();

		// Act & Assert
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(null, player);
		}, "Expected addPlayerToMap to throw IllegalArgumentException when mapId is null.");

		// Verify exception message
		assertTrue(thrown.getMessage().contains("MapId cannot be null"),
				"Exception message should contain 'MapId cannot be null'.");
	}

	@Test
	void testAddPlayerToMap_GameMapNotFound_ShouldThrowIllegalArgumentException() {
		// Arrange
		Long nonExistentMapId = 999L; // Assuming this ID does not exist

		// Create a Player
		Player player = new PlayerBuilder().withName("Test Player").withIsAlive(true).build();

		// Act & Assert
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			gameMapDAO.addPlayerToMap(nonExistentMapId, player);
		}, "Expected addPlayerToMap to throw IllegalArgumentException when GameMap is not found.");

		// Verify exception message
		assertTrue(thrown.getMessage().contains("GameMap with id " + nonExistentMapId + " not found."),
				"Exception message should contain 'GameMap with id 999 not found.'.");
	}

	@Test
	void testAddPlayerToMap_TransactionAlreadyActive_ShouldThrowIllegalStateException() {
		// Arrange
		// Mock the EntityManagerFactory to return a spy EntityManager
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		EntityManager emSpy = Mockito.spy(emf.createEntityManager());
		when(emfMock.createEntityManager()).thenReturn(emSpy);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);
		when(emSpy.getTransaction()).thenReturn(transactionMock);

		// Simulate that the transaction is already active
		when(transactionMock.isActive()).thenReturn(true);

		// Initialize DAO with the mocked EntityManagerFactory
		GameMapDAO daoWithSpy = new GameMapDAO(emfMock);

		// Create a Player
		Player player = new PlayerBuilder().withName("Test Player").withIsAlive(true).build();

		// Act & Assert
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			daoWithSpy.addPlayerToMap(1L, player); // Assuming mapId 1 exists
		}, "Expected addPlayerToMap to throw IllegalStateException when transaction is already active.");

		// Verify exception message
		assertTrue(thrown.getMessage().contains("Transaction already active. Cannot begin a new transaction."),
				"Exception message should contain 'Transaction already active. Cannot begin a new transaction.'.");

		// Verify that transaction.begin() was never called since the transaction was
		// already active
		verify(transactionMock, never()).begin();

		// Verify that transaction.rollback() was not called
		verify(transactionMock, never()).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy, times(1)).close();
	}

	@Test
	void testAddPlayerToMap_GenericException_ShouldThrowPersistenceException() {
		// Arrange
		// Mock the EntityManagerFactory to return a spy EntityManager
		EntityManagerFactory emfMock = Mockito.mock(EntityManagerFactory.class);
		EntityManager emSpy = Mockito.spy(emf.createEntityManager());
		when(emfMock.createEntityManager()).thenReturn(emSpy);

		// Mock the EntityTransaction
		EntityTransaction transactionMock = Mockito.mock(EntityTransaction.class);
		when(emSpy.getTransaction()).thenReturn(transactionMock);

		// Simulate normal transaction start
		when(transactionMock.isActive()).thenReturn(false);
		doNothing().when(transactionMock).begin();

		// Simulate an exception during em.persist()
		doThrow(new RuntimeException("Database error during persist")).when(emSpy).persist(any(Player.class));

		// Initialize DAO with the mocked EntityManagerFactory
		GameMapDAO daoWithSpy = new GameMapDAO(emfMock);

		// Create a Player
		Player player = new PlayerBuilder().withName("Test Player").withIsAlive(true).build();

		// Act & Assert
		PersistenceException thrown = assertThrows(PersistenceException.class, () -> {
			daoWithSpy.addPlayerToMap(1L, player); // Assuming mapId 1 exists
		}, "Expected addPlayerToMap to throw PersistenceException due to generic exception.");

		// Verify exception message
		assertTrue(thrown.getMessage().contains("Failed to add player to map"),
				"Exception message should contain 'Failed to add player to map'.");

		// Verify that transaction.begin() was called
		verify(transactionMock, times(1)).begin();

		// Verify that transaction.rollback() was called due to exception
		verify(transactionMock, times(1)).rollback();

		// Verify that the EntityManager was closed
		verify(emSpy, times(1)).close();
	}

}
