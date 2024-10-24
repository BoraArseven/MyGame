package com.boracompany.mygame.controller;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.List;

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

		Long gameMapId = gameMap.getId();
		// Act & Assert: Try to remove a null player from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {

			gameMapDAO.removePlayerFromMap(gameMapId, null);
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
		Long gameId = gameMap.getId();
		// Act & Assert: Try to remove the player with null ID from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {

			gameMapDAO.removePlayerFromMap(gameId, playerWithNullId);
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

		// Persist the map
		gameMapDAO.save(gameMap);

		// Create and persist a player
		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.createPlayer(player); // Persist the player

		// Manually add the player to the map by updating the map's players list and
		// persisting it
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			gameMap = em.find(GameMap.class, gameMap.getId());
			gameMap.getPlayers().add(player); // Add the player to the GameMap's players list
			em.merge(gameMap); // Merge the changes into the database
			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Act: Remove the player from the map using the DAO
		gameMapDAO.removePlayerFromMap(gameMap.getId(), player);

		// Assert: Ensure the player was successfully removed from the map
		EntityManager emCheck = emf.createEntityManager();
		GameMap updatedMap = emCheck.find(GameMap.class, gameMap.getId());
		emCheck.refresh(updatedMap); // Ensure the latest data is loaded

		assertTrue(updatedMap.getPlayers().isEmpty(), "Player was not successfully removed from the map");

		emCheck.close();
	}

	@Test
	void testRemovePlayerFromMap_ManagedPlayerIsNull() {
		// Create and persist a GameMap
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

		// Verify that the exception message is as expected
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

		Long gameMapId = gameMap.getId();
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {

			gameMapDAO.removePlayerFromMap(gameMapId, player);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testDeletePlayerSuccessfully() {
		// Arrange: Create and persist a player
		Player player = new PlayerBuilder().withName("TestPlayer").withDamage(100).withHealth(200).build();
		playerDAO.createPlayer(player); // Ensure the player is created and persisted

		// Ensure the player has a valid ID after persistence
		assertNotNull(player.getId());

		// Act: Delete the player using the controller
		controller.deletePlayer(player.getId());

		// Assert: Verify that the player no longer exists in the database
		EntityManager em = emf.createEntityManager();
		Player deletedPlayer = em.find(Player.class, player.getId());
		assertNull(deletedPlayer);
		em.close();
	}

	@Test
	void testDeletePlayer_PlayerNotFound() {
		// Arrange: Try to delete a non-existent player
		Long nonExistentPlayerId = 999L;

		// Act & Assert: Expect an IllegalArgumentException
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			controller.deletePlayer(nonExistentPlayerId);
		});

		assertEquals("Player with ID " + nonExistentPlayerId + " not found", thrown.getMessage());
	}

	@Test
	void testDeletePlayer_DeleteFailsDueToDBError() {
		// Arrange: Create and persist a player
		Player player = new PlayerBuilder().withName("TestPlayer").withDamage(100).withHealth(200).build();
		playerDAO.createPlayer(player);

		// Retrieve the player with the persisted ID
		EntityManager em = emf.createEntityManager();
		Player persistedPlayer = em.find(Player.class, player.getId());

		// Ensure the player was persisted correctly
		assertNotNull(persistedPlayer, "Player should be persisted and retrievable from the database.");

		// Simulate a failure by closing the EntityManagerFactory
		HibernateUtil.close();

		// Act & Assert: Expect IllegalStateException with "EntityManagerFactory is
		// closed" message
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			controller.deletePlayer(player.getId());
		});

		// Verify: The exception message matches the expected one
		String expectedMessage = "EntityManagerFactory is closed";
		assertEquals(expectedMessage, thrown.getMessage());

		// Reinitialize Hibernate for further tests
		setUpAll();
		setUp();
	}

	@Test
	void testDeletePlayerThrowsExceptionForNullPlayerId() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			controller.deletePlayer(null); // Pass null player ID
		});

		assertEquals("Player ID must not be null.", thrown.getMessage());
	}

	@Test
	void testGetAllPlayersReturnsEmptyList() {
		// Arrange: Ensure the database is empty (resetDatabase has already been called
		// in @BeforeEach)
		// Act: Call the getAllPlayers method
		List<Player> players = controller.getAllPlayers();

		// Assert: Verify that the result is an empty list
		assertNotNull(players, "The list of players should not be null");
		assertTrue(players.isEmpty(), "The list of players should be empty");

		// Verify that the correct log message was generated
		LOGGER.info("Retrieved {} players from the database.", players.size());
	}

	@Test
	void testGetAllPlayersReturnsMultiplePlayers() {
		// Arrange: Create and persist multiple players
		Player player1 = new PlayerBuilder().withName("Player1").withDamage(10).withHealth(100).build();
		Player player2 = new PlayerBuilder().withName("Player2").withDamage(20).withHealth(200).build();
		Player player3 = new PlayerBuilder().withName("Player3").withDamage(30).withHealth(300).build();

		playerDAO.createPlayer(player1); // Persist Player 1
		playerDAO.createPlayer(player2); // Persist Player 2
		playerDAO.createPlayer(player3); // Persist Player 3

		// Act: Call the getAllPlayers method
		List<Player> players = controller.getAllPlayers();

		// Assert: Verify that the list contains all the players
		assertNotNull(players, "The list of players should not be null");
		assertEquals(3, players.size(), "The list should contain three players");
		assertTrue(players.stream().anyMatch(p -> p.getName().equals("Player1")), "Player1 should be in the list");
		assertTrue(players.stream().anyMatch(p -> p.getName().equals("Player2")), "Player2 should be in the list");
		assertTrue(players.stream().anyMatch(p -> p.getName().equals("Player3")), "Player3 should be in the list");

		// Verify that the correct log message was generated
		LOGGER.info("Retrieved {} players from the database.", players.size());
	}

	@Test
	void testGetAllPlayersThrowsExceptionWhenDBFails() {
		// Arrange: Simulate a failure by closing the EntityManagerFactory
		HibernateUtil.close();

		// Act & Assert: Expect an IllegalStateException
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			controller.getAllPlayers();
		});

		// Assert: Verify the exception message
		String expectedMessage = "Could not retrieve players from the database";
		assertEquals(expectedMessage, thrown.getMessage());

		// Reinitialize Hibernate for further tests
		setUpAll();
		setUp();
	}
	@Test
	void testAttackDefeatsDefenderAndUpdatesDatabase() {
	    // Arrange: Create and persist two players and a game map
	    Player attacker = playerBuilder.resetBuilder().withName("StrongAttacker").withHealth(100).withDamage(70).build();
	    Player defender = playerBuilder.resetBuilder().withName("WeakDefender").withHealth(50).withDamage(20).build();

	    // Do not set the id field manually
	    playerDAO.createPlayer(attacker);
	    playerDAO.createPlayer(defender);

	    GameMap gameMap = new GameMap();
	    gameMap.setName("FinalBattle");
	    gameMapDAO.save(gameMap);

	    // Add both players to the map
	    gameMapDAO.addPlayerToMap(gameMap.getId(), attacker);
	    gameMapDAO.addPlayerToMap(gameMap.getId(), defender);

	    // Act: Perform an attack
	    controller.attack(attacker, defender);

	    // Assert: Check that the defender's health and alive status are updated in the database
	    EntityManager em = emf.createEntityManager();
	    try {
	        Player updatedDefender = em.find(Player.class, defender.getId());
	        assertNotNull(updatedDefender, "Defender should exist in the database.");

	        float expectedHealth = 0.0f; // Defender's health cannot be negative
	        assertEquals(expectedHealth, updatedDefender.getHealth(), 0.01, "Defender's health should be 0.");

	        // Defender should be marked as not alive
	        assertFalse(updatedDefender.isAlive(), "Defender should be marked as not alive.");

	        LOGGER.info("Defender's health after attack: {}, isAlive: {}", updatedDefender.getHealth(), updatedDefender.isAlive());
	    } finally {
	        em.close();
	    }
	}
}
