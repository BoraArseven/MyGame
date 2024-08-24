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

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;

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
			throw new PersistenceException("Failed to reset database", e);
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
		assertThrows(PersistenceException.class, () -> {
			gameMapDAO.addPlayerToMap(-1L, player); // -1L to simulate non-existing map
		});
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
		GameMapDAO gameMapDAOwithSpiedEmf = new GameMapDAO(spyEmf);

		// Create a GameMap object to update
		GameMap gameMap = new GameMap();
		gameMap.setName("Test Map");

		// Verify that the update method throws a PersistenceException due to the
		// simulated
		// PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOwithSpiedEmf.update(gameMap);
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
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		// Simulate finding a GameMap to trigger the remove operation
		GameMap gameMap = new GameMap();
		gameMap.setId(1L);
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);

		// Ensure the transaction is NOT active before the exception occurs
		Mockito.when(spyTransaction.isActive()).thenReturn(false);

		// Verify that the delete method throws a PersistenceException due to the
		// simulated
		// PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOWithSpiedEmf.delete(1L);
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
		GameMapDAO gameMapDAOwithSpiedEmf = new GameMapDAO(spyEmf);

		// Simulate finding a GameMap to trigger the persist operation
		GameMap gameMap = new GameMap();
		gameMap.setId(1L);
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);

		Player player = new Player();
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAOwithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction).rollback();
		// Assert that the exception message is exactly as expected
		assertEquals("Failed to add player to map", thrownException.getMessage());

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
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		// Verify that the addPlayerToMap method throws a PersistenceException due to
		// GameMap not being found
		Player player = new Player();
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert that the transaction was rolled back once
		Mockito.verify(spyTransaction, Mockito.times(1)).rollback();

		// Assert that the exception message contains the expected text
		assertEquals("Failed to add player to map", thrownException.getMessage());

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
		// Arrange
		EntityManagerFactory emfSpy = Mockito.spy(emf);
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(emfSpy);

		// Spy on the real EntityManager and Transaction
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		// Simulate GameMap not being found
		Mockito.doReturn(null).when(emSpy).find(GameMap.class, 1L);
		Player player = new Player();
		assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.removePlayerFromMap(1L, player);
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
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(emfspy);

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

		Long id = gameMap.getId();
		Player player = new Player();
		assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.removePlayerFromMap(id, player);
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
		GameMapDAO gameMapDAOwithSpiedEmf = new GameMapDAO(emfSpy);

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
		Long id = gameMap.getId();
		assertThrows(PersistenceException.class, () -> {

			gameMapDAOwithSpiedEmf.removePlayerFromMap(id, player);
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
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(emfSpy);

		// Spy on the real EntityManager and Transaction
		EntityManager emSpy = Mockito.spy(emfSpy.createEntityManager());
		EntityTransaction transactionSpy = Mockito.spy(emSpy.getTransaction());

		Mockito.when(emfSpy.createEntityManager()).thenReturn(emSpy);
		Mockito.doReturn(transactionSpy).when(emSpy).getTransaction();
		Mockito.when(transactionSpy.isActive()).thenReturn(true);

		// Simulate GameMap and Player not being found
		Mockito.doReturn(null).when(emSpy).find(GameMap.class, 1L);
		Mockito.doReturn(null).when(emSpy).find(Player.class, 1L);

		Player player = new Player();
		assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.removePlayerFromMap(1L, player);
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

		// Simulate finding a GameMap but throwing a PersistenceException during persist
		GameMap gameMap = new GameMap();
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.persist(Mockito.any(Player.class));

		// Ensure that the transaction is active
		Mockito.when(spyTransaction.isActive()).thenReturn(true);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);
		Player player = new Player();
		// Verify that the addPlayerToMap method throws a PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert that the transaction was rolled back
		Mockito.verify(spyTransaction, Mockito.times(1)).rollback();

		// Assert that the exception message contains the expected text
		assertEquals("Failed to add player to map", thrownException.getMessage());

		// Verify that the EntityManager is closed after the operation
		Mockito.verify(spyEm).close();
	}

	@Test
	void testAddPlayerToMap_TransactionNotActive() {
		// Spy on the real EntityManagerFactory and EntityManager
		EntityManagerFactory spyEmf = Mockito.spy(emf);
		EntityManager spyEm = Mockito.spy(spyEmf.createEntityManager());
		EntityTransaction spyTransaction = Mockito.spy(spyEm.getTransaction());

		// Set up the spy to use the real transaction
		Mockito.when(spyEmf.createEntityManager()).thenReturn(spyEm);
		Mockito.when(spyEm.getTransaction()).thenReturn(spyTransaction);

		// Simulate finding a GameMap but throwing a PersistenceException during persist
		GameMap gameMap = new GameMap();
		Mockito.when(spyEm.find(GameMap.class, 1L)).thenReturn(gameMap);
		Mockito.doThrow(new PersistenceException("Simulated Persistence Exception")).when(spyEm)
				.persist(Mockito.any(Player.class));

		// Ensure that the transaction is not active
		Mockito.when(spyTransaction.isActive()).thenReturn(false);

		// Use the spied EntityManagerFactory in the DAO
		GameMapDAO gameMapDAOWithSpiedEmf = new GameMapDAO(spyEmf);

		Player player = new Player();
		// Verify that the addPlayerToMap method throws a PersistenceException
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {

			gameMapDAOWithSpiedEmf.addPlayerToMap(1L, player);
		});

		// Assert that the transaction rollback was not called since the transaction is
		// not active
		Mockito.verify(spyTransaction, Mockito.never()).rollback();

		// Assert that the exception message contains the expected text
		assertEquals("Failed to add player to map", thrownException.getMessage());

		// Verify that the EntityManager is closed after the operation
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

		// Act & Assert: Try adding a null player to the map and assert that it throws
		// an exception
		PersistenceException thrownException = assertThrows(PersistenceException.class, () -> {
			gameMapDAO.addPlayerToMap(gameMap.getId(), null); // Pass null as the player
		});

		// Assert: Check the exception message
		assertEquals("Failed to add player to map", thrownException.getMessage());
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
	    public void testAddPlayerToMap_withNullMapId_throwsIllegalArgumentException() {
	        Player player = new Player();
	        player.setId(1L); // assuming this player already exists or has an ID

	        // Act & Assert
	        PersistenceException exception = assertThrows(PersistenceException.class, () -> {
	            gameMapDAO.addPlayerToMap(null, player);
	        });

	        // Check if the cause of the PersistenceException is IllegalArgumentException
	        assertTrue(exception.getCause() instanceof IllegalArgumentException);
	        assertTrue(exception.getCause().getMessage().contains("MapId can not be null"));
	    }
}
