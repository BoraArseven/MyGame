package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

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

import com.boracompany.mygame.Model.GameMap;
import com.boracompany.mygame.Model.Player;
import com.boracompany.mygame.Model.PlayerBuilder;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GameMapDAOIT {

	private static final Logger LOGGER = LogManager.getLogger(GameMapDAOIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
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
			// Delete all data from tables
			em.createQuery("DELETE FROM Player").executeUpdate();
			em.createQuery("DELETE FROM GameMap").executeUpdate();
			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw new RuntimeException("Failed to reset database", e);
		} finally {
			em.close();
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
			System.out.printf("player: {}", selectedplayer.toString());
			LOGGER.debug("player: {}", selectedplayer.toString());
		}
		assertEquals(1, updatedMap.getPlayers().size());
		assertEquals("TestAddPlayer", updatedMap.getPlayers().get(0).getName());
		em.close();
	}

	@Test
	void testRemovePlayerFromMap() {
		GameMap gameMap = new GameMap();
		gameMap.setName("Map with One Player");

		Player player = new PlayerBuilder().withName("Lonely Player").build();
		gameMap.setPlayers(List.of(player));
		gameMapDAO.save(gameMap);

		gameMapDAO.removePlayerFromMap(gameMap.getId(), player);

		EntityManager em = emf.createEntityManager();
		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedMap); // Ensure the latest data is loaded
		assertEquals(0, updatedMap.getPlayers().size());
		em.close();
	}

	@Test
	void testAddPlayerToNonExistingMap() {
		Player player = new PlayerBuilder().withName("Player for Non-existing Map").build();
		assertThrows(RuntimeException.class, () -> {
			gameMapDAO.addPlayerToMap(-1L, player); // -1L to simulate non-existing map
		});
	}

	@Test
	void testRemovePlayerFromNonExistingMap() {
		Player player = new PlayerBuilder().withName("Player for Non-existing Map").build();
		assertThrows(RuntimeException.class, () -> {
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
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emfSpy).createEntityManager();

		assertThrows(RuntimeException.class, () -> {
			spyGameMapDAO.addPlayerToMap(gameMap.getId(), player);
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
		Mockito.doThrow(new RuntimeException("Simulated Exception")).when(emfSpy).createEntityManager();

		assertThrows(RuntimeException.class, () -> {
			spyGameMapDAO.removePlayerFromMap(gameMap.getId(), player);
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
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Create a GameMap object to save
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the save method throws a RuntimeException due to the simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.save(gameMap);
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
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Create a GameMap object to save
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the save method throws a RuntimeException due to the simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.save(gameMap);
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
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when merge is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.merge(Mockito.any(GameMap.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Create a GameMap object to update
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the update method throws a RuntimeException due to the simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.update(gameMap);
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("Failed to update GameMap:"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testDeleteWithPersistenceException() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when remove is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.remove(Mockito.any(GameMap.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO GameMapDAOwithSpiedEmf = new GameMapDAO(spyEmf);

		// Ensure the transaction is active before the exception occurs
		Mockito.when(spyTransaction.isActive()).thenReturn(true);

		// Verify that the delete method throws a RuntimeException due to the simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			GameMapDAOwithSpiedEmf.delete(1L);
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("Failed to delete GameMap:"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
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
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Verify that the delete method throws a RuntimeException due to the GameMap
		// not being found
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.delete(1L);
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
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when remove is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.remove(Mockito.any(GameMap.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Simulate finding a GameMap to trigger the remove operation
		GameMap gameMap = new GameMap();
		gameMap.setId(1L);
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);

		// Ensure the transaction is NOT active before the exception occurs
		Mockito.when(spyTransaction.isActive()).thenReturn(false);

		// Verify that the delete method throws a RuntimeException due to the simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.delete(1L);
		});

		// Assert that the transaction was NOT rolled back since it was not active
		Mockito.verify(spyTransaction, Mockito.never()).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("Failed to delete GameMap: Simulated Persistence Exception"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testAddPlayerToMap_PersistenceException() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction but simulate a
		// PersistenceException when persist is called
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.persist(Mockito.any(Player.class));

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Simulate finding a GameMap to trigger the persist operation
		GameMap gameMap = new GameMap();
		gameMap.setId(1L);
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);

		// Verify that the addPlayerToMap method throws a RuntimeException due to the
		// simulated
		// PersistenceException
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.addPlayerToMap(1L, new Player());
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage()
				.contains("Failed to add player to GameMap: Simulated Persistence Exception"));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
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
		GameMapDAO gameMapDAO = new GameMapDAO(spyEmf);

		// Verify that the addPlayerToMap method throws a RuntimeException due to
		// GameMap not being found
		RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.addPlayerToMap(1L, new Player());
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Assert that the exception message contains the expected text
		assertTrue(thrownException.getMessage().contains("GameMap with id 1 not found."));

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testRemovePlayerFromMap_GameMapOrPlayerNotFound() {
		// Arrange
		GameMapDAO gameMapDAO = new GameMapDAO(emf);

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

			assertThrows(RuntimeException.class, () -> {
				gameMapDAO.removePlayerFromMap(gameId, new Player());
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
		// Arrange
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emfSpy);

		// Spy on the real EntityManager and Transaction
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		// Simulate GameMap not being found
		Mockito.doReturn(null).when(emSpy).find(GameMap.class, 1L);

		assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(1L, new Player());
		});

		// Verify that the transaction was rolled back
		Mockito.verify(transactionSpy).rollback();

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(emSpy).close();
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotFound() {
		// Arrange
		EntityManagerFactory emfspy = Mockito.spy(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emfspy);

		// Begin transaction and save the GameMap, but not the Player
		EntityManager em = emfspy.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		transaction.begin();
		GameMap gameMap = new GameMap();
		em.persist(gameMap);
		transaction.commit();
		em.close();

		// Re-open the EntityManager for the operation we are testing
		EntityManager emspy = Mockito.spy(emf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(emspy.getTransaction());

		// Spy on the real EntityManager and Transaction

		Mockito.when(emfspy.createEntityManager()).thenReturn(emspy);
		Mockito.doReturn(spyTransaction).when(emspy).getTransaction();
		Mockito.when(spyTransaction.isActive()).thenReturn(true);

		assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), new Player());
		});

		// Verify that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(emspy).close();
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotInGameMap() {
		// Arrange
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emfSpy);

		// Create and persist a GameMap and a Player (player is not part of the map)
		EntityManager em = emfSpy.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		// Persist GameMap in the first transaction
		transaction.begin();
		GameMap gameMap = new GameMap();
		em.persist(gameMap);
		transaction.commit();
		em.close();

		// Create a new EntityManager and Transaction to persist the Player
		em = emfSpy.createEntityManager();
		transaction = em.getTransaction();
		transaction.begin();
		Player player = new Player();
		player.setId(1L);
		player.setMap(null); // Ensure the player is not in any map
		em.merge(player);
		transaction.commit();
		em.close();

		// Re-open the EntityManager for the operation we are testing
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		// Ensure the EntityManagerFactory returns our spied EntityManager
		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), player);
		});

		// Verify that the transaction was rolled back
		Mockito.verify(transactionSpy).rollback();

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(emSpy).close();
	}

	@Test
	void testRemovePlayerFromMap_GameMapAndPlayerNotFound() {
		// Arrange
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emfSpy);

		// Spy on the real EntityManager and Transaction
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		// Simulate GameMap and Player not being found
		Mockito.doReturn(null).when(emSpy).find(GameMap.class, 1L);
		Mockito.doReturn(null).when(emSpy).find(Player.class, 1L);

		assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(1L, new Player());
		});

		// Verify that the transaction was rolled back
		Mockito.verify(transactionSpy).rollback();

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(emSpy).close();
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


			GameMapDAO gameMapDAO = new GameMapDAO(emf);

			// Act & Assert: Try to remove the player that is not part of the game map
			assertThrows(RuntimeException.class, () -> {
				gameMapDAO.removePlayerFromMap(gameMap.getId(), player);
			});

		} finally {
			if (em.isOpen()) {
				em.close(); // Ensure the EntityManager is closed after the test
			}
		}
	}

}
