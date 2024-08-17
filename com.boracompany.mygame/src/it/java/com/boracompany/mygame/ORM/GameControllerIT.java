package com.boracompany.mygame.ORM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameControllerIT {

	private static final Logger LOGGER = LogManager.getLogger(GameControllerIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
	}

	private EntityManagerFactory emf;
	private GameMapDAO gameMapDAO;
	private PlayerDAOIMPL playerDAO;

	private GameController controller;
	private PlayerBuilder playerBuilder;

	@BeforeAll
	void setUpAll() {
		postgreSQLContainer.start();

		// Directly passing database properties
		String dbUrl = postgreSQLContainer.getJdbcUrl();
		String dbUser = postgreSQLContainer.getUsername();
		String dbPassword = postgreSQLContainer.getPassword();

		// Initialize HibernateUtil with connection properties
		HibernateUtil.initialize(dbUrl, dbUser, dbPassword);
		emf = HibernateUtil.getEntityManagerFactory();
	}

	@BeforeEach
	void setUp() {

		// Initialize DAOs with the EntityManagerFactory
		gameMapDAO = new GameMapDAO(emf);
		playerDAO = new PlayerDAOIMPL(emf);

		// Initialize the PlayerBuilder
		playerBuilder = new PlayerBuilder();

		// Spy on the GameController
		controller = spy(new GameController(playerDAO, gameMapDAO, LOGGER));

		// Reset database before each test
		resetDatabase();
	}

	@AfterAll
	static void tearDownAll() {
		HibernateUtil.close();
		if (postgreSQLContainer != null) {
			postgreSQLContainer.stop();
		}
	}

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
	void testAddPlayersToMapFromController() {
		// Arrange: Create a new player and a game map
		Player addedPlayer = playerBuilder.withDamage(10).withHealth(20).withName("AddedPlayer1").build();
		GameMap map = new GameMap();
		map.setName("TestMap");

		// Persist the map in the database
		gameMapDAO.save(map);

		// Act: Add the player to the map using the controller
		controller.addPlayerToMap(map.getId(), addedPlayer);

		// Assert: Access the players collection within an active transaction
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			GameMap retrievedMap = gameMapDAO.findById(map.getId());
			assertNotNull(retrievedMap);

			// Access the players collection while the session is still open
			assertFalse(retrievedMap.getPlayers().isEmpty());
			assertEquals("AddedPlayer1", retrievedMap.getPlayers().get(0).getName());
			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		LOGGER.info("Player {} successfully added to map {}", addedPlayer.getName(), map.getName());
	}

	@Test
	void testRemovePlayerFromMap_GameMapIsNull() {
		// Arrange: Create and persist a Player without associating it with a GameMap
		Player player = new PlayerBuilder().withName("TestPlayer").build();

		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();
			em.persist(player); // Persist the player into the database
			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw new RuntimeException("Failed to persist player", e);
		} finally {
			em.close();
		}

		// Act & Assert: Try to remove the player from a non-existent GameMap (ID 999)
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(999L, player); // Use a non-existent GameMap ID
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Expected GameMap not found or Player not in this GameMap.", thrown.getMessage());

		LOGGER.info("Test completed: testRemovePlayerFromMap_GameMapIsNull");
	}

	@Test
	void testRemovePlayerFromMap_PlayerIsNull() {
		// Arrange: Create and persist a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		// Act & Assert: Try to remove a null player from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), null);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerHasNullId() {
		// Arrange: Create and persist a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		// Create a Player with null ID
		Player playerWithNullId = new PlayerBuilder().withName("TestPlayerWithNullId").build();

		// Act & Assert: Try to remove the player with null ID from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), playerWithNullId);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotInGameMap() {
		// Arrange: Create and persist a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.updatePlayer(player); // Persist the player separately, not in the game map

		// Arrange
		Long gameMapId = gameMap.getId();

		// Act & Assert: Try to remove a null player from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
		    gameMapDAO.removePlayerFromMap(gameMapId, null);
		});
		// Assert: Verify that the exception message is as expected
		assertEquals("Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_SuccessfulRemoval() {
		// Arrange: Create and persist a GameMap and a Player, and associate the player
		// with the GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.updatePlayer(player); // Persist the player
		gameMapDAO.addPlayerToMap(gameMap.getId(), player); // Add the player to the map

		// Act: Remove the player from the map
		gameMapDAO.removePlayerFromMap(gameMap.getId(), player);

		// Assert: Ensure the player was successfully removed from the map
		EntityManager em = emf.createEntityManager();
		GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
		em.refresh(updatedMap); // Ensure the latest data is loaded
		assertTrue(updatedMap.getPlayers().isEmpty(), "Player was not successfully removed from the map");
		em.close();
	}

	@Test
	void testRemovePlayerFromMap_ManagedPlayerIsNull() {
		// Arrange: Create and persist a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		// Create a Player object with a valid ID but do not persist it
		Player player = new PlayerBuilder().withName("TestPlayer").build();
		player.setId(999L);
	
		// Arrange
		Long gameMapId = gameMap.getId();

	
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
		    gameMapDAO.removePlayerFromMap(gameMapId, player);
		});


		// Assert: Verify that the exception message is as expected
		assertEquals("Expected GameMap not found or Player not in this GameMap.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerExistsButNotInGameMap() {
		// Arrange: Create and persist a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.updatePlayer(player); // Persist the player separately

		// Act & Assert: Try to remove the player from the map where the player is not
		// present
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMap.getId(), player);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Player is null or has a null ID.", thrown.getMessage());
	}

}
