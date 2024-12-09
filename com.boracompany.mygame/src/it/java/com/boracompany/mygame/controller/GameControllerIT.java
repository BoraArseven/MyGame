package com.boracompany.mygame.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

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

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameControllerIT {

	private static Logger logger = LogManager.getLogger(GameControllerIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:14.15");
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
		gameMapDAO = spy(new GameMapDAO(emf));
		playerDAO = spy(new PlayerDAOIMPL(emf));

		// Initialize the PlayerBuilder
		playerBuilder = new PlayerBuilder();
		// Spy on the logger with inline mocking
		logger = spy(logger);
		// Initialize the GameController with the spied logger
		controller = new GameController(playerDAO, gameMapDAO, logger);

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
		EntityManager em1 = emf.createEntityManager();
		// Arrange: Create a new player and a game map
		Player addedPlayer = playerBuilder.withDamage(10).withHealth(20).withName("AddedPlayer1").build();
		EntityTransaction tx1 = em1.getTransaction();
		try {
			tx1.begin();
			em1.persist(addedPlayer);

			tx1.commit();
			em1.refresh(addedPlayer);
		} catch (Exception e) {
			if (tx1.isActive()) {
				tx1.rollback();
			}
			throw e;
		} finally {
			em1.close();
		}

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

		logger.info("Player {} successfully added to map {}", addedPlayer.getName(), map.getName());
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

		// Try to remove the player from a non-existent GameMap (ID 999)
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(999L, player); // Use a non-existent GameMap ID
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Failed to remove Player: GameMap with id 999 not found.", thrown.getMessage());

		logger.info("Test completed: testRemovePlayerFromMap_GameMapIsNull");
	}

	@Test
	void testRemovePlayerFromMap_PlayerIsNull() {
		// Arrange: Create and persist a GameMap
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		Long gameMapId = gameMap.getId();
		// Try to remove a null player from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {

			gameMapDAO.removePlayerFromMap(gameMapId, null);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Failed to remove Player: Player is null or has a null ID.", thrown.getMessage());
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
		// Try to remove the player with null ID from the map
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {

			gameMapDAO.removePlayerFromMap(gameId, playerWithNullId);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Failed to remove Player: Player is null or has a null ID.", thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotInGameMap() {
		// Arrange: Create and persist a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		// Create a new Player and persist it using createPlayer to assign an ID
		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.createPlayer(player); // Use createPlayer instead of updatePlayer

		// Verify that the player has been assigned an ID
		assertNotNull(player.getId(), "Player ID should not be null after creation.");

		Long gameMapId = gameMap.getId();

		// Act & Assert: Attempt to remove the player from a GameMap it's not associated
		// with
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMapId, player);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Failed to remove Player: Expected GameMap not found or Player not in this GameMap.", thrown.getMessage());
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
		assertEquals("Failed to remove Player: Expected GameMap not found or Player not in this GameMap.",
				thrown.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_PlayerExistsButNotInGameMap() {
		// Arrange: Create and persist a GameMap and a Player
		GameMap gameMap = new GameMap();
		gameMap.setName("TestMap");
		gameMapDAO.save(gameMap);

		// Create a new Player and persist it using createPlayer to assign an ID
		Player player = new PlayerBuilder().withName("TestPlayer").build();
		playerDAO.createPlayer(player); // Use createPlayer instead of updatePlayer

		// Verify that the player has been assigned an ID
		assertNotNull(player.getId(), "Player ID should not be null after creation.");

		Long gameMapId = gameMap.getId();

		// Act & Assert: Attempt to remove the player from a GameMap it's not associated
		// with
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			gameMapDAO.removePlayerFromMap(gameMapId, player);
		});

		// Assert: Verify that the exception message is as expected
		assertEquals("Failed to remove Player: Expected GameMap not found or Player not in this GameMap.", thrown.getMessage());
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
	void testCreatePlayer() {
		// Arrange
		String playerName = "TestPlayer";
		float health = 100f;
		float damage = 50f;
		Player expectedPlayer = new PlayerBuilder().withName(playerName).withHealth(health).withDamage(damage).build();

		// Act
		Player createdPlayer = controller.createPlayer(playerName, health, damage);

		// Assert
		assertEquals(expectedPlayer.getName(), createdPlayer.getName());
		assertEquals(expectedPlayer.getHealth(), createdPlayer.getHealth());
		assertEquals(expectedPlayer.getDamage(), createdPlayer.getDamage());

		// Verify that updatePlayer was called on playerDAO
		verify(playerDAO).createPlayer(createdPlayer);
	}

	@Test
	void testCreatePlayer_ValidAttributes() {
		// Arrange: Setup valid parameters
		String playerName = "ValidPlayer";
		float health = 100;
		float damage = 50;

		// Act: Create a valid player
		Player result = controller.createPlayer(playerName, health, damage);

		// Assert: Verify the player was created successfully
		assertNotNull(result);
		assertEquals(playerName, result.getName());
		assertEquals(health, result.getHealth(), 0.01);
		assertEquals(damage, result.getDamage(), 0.01);
		assertTrue(result.isAlive());

		// Verify logger call
		verify(logger).info("Player created: {}", playerName);
	}

	@Test
	void testCreatePlayerBoundaryConditions() {
		String playerName = "BoundaryPlayer";

		// Valid boundary: health = 1, damage = 1
		Player player = controller.createPlayer(playerName, 1, 1);
		assertNotNull(player);
		assertEquals(1, player.getHealth(), 0.01);
		assertEquals(1, player.getDamage(), 0.01);
		assertTrue(player.isAlive());

		// Invalid boundary: health = 0
		assertThrows(IllegalArgumentException.class, () -> {
			controller.createPlayer(playerName, 0, 50); // Health = 0
		});

		// Invalid boundary: damage = 0
		assertThrows(IllegalArgumentException.class, () -> {
			controller.createPlayer(playerName, 100, 0); // Damage = 0
		});
	}

	@Test
	void testCreatePlayer_SetsIsAliveCorrectly() {
		// Arrange
		String playerName = "AlivePlayer";
		float health = 100;
		float damage = 50;

		// Act
		Player player = controller.createPlayer(playerName, health, damage);

		// Assert
		assertNotNull(player, "Player should not be null.");
		assertEquals(playerName, player.getName(), "Player's name should match the input.");
		assertEquals(health, player.getHealth(), 0.01, "Player's health should match the input.");
		assertEquals(damage, player.getDamage(), 0.01, "Player's damage should match the input.");
		assertTrue(player.isAlive(), "Player's isAlive should be explicitly set to true by withIsAlive(true).");
	}

	@Test
	void testDeletePlayer_PlayerNotFound() {
		// Arrange: Try to delete a non-existent player
		Long nonExistentPlayerId = 999L;

		// Expect an IllegalArgumentException
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

		Long playerId = player.getId();
		// Act & Assert: Expect IllegalStateException with "EntityManagerFactory is
		// closed" message
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			controller.deletePlayer(playerId);
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
		logger.info("Retrieved {} players from the database.", players.size());
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
		logger.info("Retrieved {} players from the database.", players.size());
	}

	@Test
	void testGetAllPlayersThrowsExceptionWhenDBFails() {
		// Arrange: Simulate a failure by closing the EntityManagerFactory
		HibernateUtil.close();

		// Expect an IllegalStateException
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
		Player attacker = playerBuilder.resetBuilder().withName("StrongAttacker").withHealth(100).withDamage(70)
				.build();
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

		// Assert: Check that the defender's health and alive status are updated in the
		// database
		EntityManager em = emf.createEntityManager();
		try {
			Player updatedDefender = em.find(Player.class, defender.getId());
			assertNotNull(updatedDefender, "Defender should exist in the database.");

			float expectedHealth = 0.0f; // Defender's health cannot be negative
			assertEquals(expectedHealth, updatedDefender.getHealth(), 0.01, "Defender's health should be 0.");

			// Defender should be marked as not alive
			assertFalse(updatedDefender.isAlive(), "Defender should be marked as not alive.");

			logger.info("Defender's health after attack: {}, isAlive: {}", updatedDefender.getHealth(),
					updatedDefender.isAlive());
		} finally {
			em.close();
		}
	}

	@Test
	void testConcurrentAttacksOnDefender() throws InterruptedException {
		// Create and persist one defender with large health
		Player defender = playerBuilder.resetBuilder().withName("ConcurrentDefender").withHealth(1000f).withDamage(10f)
				.withIsAlive(true).build();
		playerDAO.createPlayer(defender);

		// Create attackers
		int attackerCount = 20;
		float attackDamage = 10f;
		Player[] attackers = new Player[attackerCount];
		for (int i = 0; i < attackerCount; i++) {
			attackers[i] = playerBuilder.resetBuilder().withName("Attacker" + i).withHealth(100f)
					.withDamage(attackDamage).withIsAlive(true).build();
			playerDAO.createPlayer(attackers[i]);
		}

		// No need to create a map for this test, attacks do not require maps
		// But if your business logic requires it, persist a map and add players.

		// Prepare concurrency tools
		ExecutorService executor = Executors.newFixedThreadPool(attackerCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(attackerCount);

		for (Player attacker : attackers) {
			executor.submit(() -> {
				try {
					startLatch.await(); // Wait until all threads are ready
					controller.attack(attacker, defender);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		// Start all attackers simultaneously
		startLatch.countDown();
		// Wait for all to complete
		doneLatch.await();
		executor.shutdown();

		// After all attacks, verify defender's health in DB
		// Expected damage = 20 attackers * 10 damage = 200 total damage
		float expectedHealth = 1000f - 200f;

		// Refresh the defender from the DB to ensure we have the latest state
		EntityManager em = emf.createEntityManager();
		try {
			Player updatedDefender = em.find(Player.class, defender.getId());
			assertEquals(expectedHealth, updatedDefender.getHealth(), 0.01f,
					"Defender health should reflect all concurrent damage");
			assertEquals(expectedHealth > 0, updatedDefender.isAlive(),
					"Defender's alive status does not match expected health condition.");
		} finally {
			em.close();
		}
	}

	@Test
	void testConcurrentAddPlayersToMap() throws InterruptedException {
		// Create and persist one map
		GameMap gameMap = new GameMap();
		gameMap.setName("ConcurrentMap");
		gameMapDAO.save(gameMap);

		// Create players to add
		int playerCount = 50;
		Player[] players = new Player[playerCount];
		for (int i = 0; i < playerCount; i++) {
			players[i] = playerBuilder.resetBuilder().withName("ConcurrentPlayer" + i).withHealth(100f).withDamage(10f)
					.withIsAlive(true).build();
			playerDAO.createPlayer(players[i]);
		}

		ExecutorService executor = Executors.newFixedThreadPool(playerCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(playerCount);

		for (Player p : players) {
			executor.submit(() -> {
				try {
					startLatch.await();
					controller.addPlayerToMap(gameMap.getId(), p);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		doneLatch.await();
		executor.shutdown();

		// After all insertions, check how many players ended up in the map
		EntityManager em = emf.createEntityManager();
		try {
			GameMap updatedMap = em.find(GameMap.class, gameMap.getId());
			em.refresh(updatedMap);
			int finalCount = updatedMap.getPlayers().size();
			assertEquals(playerCount, finalCount, "All players should be added even under concurrent conditions");
		} finally {
			em.close();
		}
	}

	@Test
	void testDeletePlayer_ConcurrentDeletion() throws InterruptedException {
		// Arrange: Create and persist a player
		Player player = playerBuilder.withName("ConcurrentPlayer").withDamage(50).withHealth(150).build();
		playerDAO.createPlayer(player);
		Long playerId = player.getId();

		// Prepare concurrency tools
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					controller.deletePlayer(playerId);
				} catch (Exception e) {
					// Ignore exceptions for this test
				} finally {
					latch.countDown();
				}
			});
		}

		// Wait for all threads to complete
		latch.await();
		executor.shutdown();

		// Assert: Ensure the player is deleted
		EntityManager em = emf.createEntityManager();
		Player deletedPlayer = em.find(Player.class, playerId);
		assertNull(deletedPlayer);
		em.close();

		// Verify that deletePlayer was called at least once
		verify(playerDAO, Mockito.atLeastOnce()).deletePlayer(player);
	}

}
