package com.boracompany.mygame;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

class TestGameController {
	private static final Logger LOGGER = LogManager.getLogger(TestGameController.class);
	PlayerBuilder builder;

	private Logger logger; // Mock logger
	private GameController controllerSpy;
	private PlayerDAOIMPL playerDAOMock;
	private GameMapDAO gameMapDAOMock;
	private GameController gameControllerwithMocks;

	@BeforeEach
	void setup() {
		// Initialize the PlayerBuilder
		builder = new PlayerBuilder();

		// Mock the Logger
		logger = mock(Logger.class);

		// Mock DAOs
		playerDAOMock = mock(PlayerDAOIMPL.class);
		gameMapDAOMock = mock(GameMapDAO.class);

		// Spy on the GameController with the mock logger
		controllerSpy = spy(new GameController(playerDAOMock, gameMapDAOMock, logger));

		// GameController with mocks and the mock logger
		gameControllerwithMocks = new GameController(playerDAOMock, gameMapDAOMock, logger);

	}

	@Test
	void testWhenAttackingDefendingPlayerisNullThrowsException() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = null;

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Attacker or defender is null.", exception.getMessage());
	}

	@Test
	void testwhenAttackingAttackingPLayerisNullThrowsException() {
		Player attacker = null;
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(30).build();

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Attacker or defender is null.", exception.getMessage());
	}

	@Test
	void testWhenAttackingBothPLayersareNullThrowsException() {
		Player attacker = null;
		Player defender = null;

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Attacker or defender is null.", exception.getMessage());
	}

	@Test
	void AttackerReducesHealthOfDefender() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(30).build();

		gameControllerwithMocks.attack(attacker, defender);

		attacker.setDamage(5);
		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(15, defender.getHealth());
	}

	@Test
	void AttackerReducesHealthOfDefenderNotMinus() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(10).build();

		gameControllerwithMocks.attack(attacker, defender);

		attacker.setDamage(5);
		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(0, defender.getHealth());
	}

	@Test
	void DefenderDiesIfHealthsmallerthanzero() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(10).build();

		gameControllerwithMocks.attack(attacker, defender);

		attacker.setDamage(5);
		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(0, defender.getHealth());
		assertEquals(false, defender.Isalive());
	}

	@Test
	void DefenderNotDiesIfHealthbiggerthanzero() {
		Player attacker = builder.resetBuilder().withDamage(5).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();

		LOGGER.debug("Attacker created with damage: {}", attacker.getDamage());

		gameControllerwithMocks.attack(attacker, defender);

		LOGGER.debug("Defender's health after first attack: {}", defender.getHealth());

		attacker.setDamage(15);

		LOGGER.debug("Attacker's damage updated to: {}", attacker.getDamage());

		gameControllerwithMocks.attack(attacker, defender);

		LOGGER.debug("Defender's health after second attack: {}", defender.getHealth());

		assertEquals(30, defender.getHealth());
		assertEquals(true, defender.Isalive());
	}

	@Test
	void DamageShouldBePositive() {
		Player attacker = builder.resetBuilder().withDamage(-5).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();

		LOGGER.debug("Attacker created with damage: {}", attacker.getDamage());

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Damage should be positive", exception.getMessage());
	}

	@Test
	void DamageShouldBeNonZero() {
		Player attacker = builder.resetBuilder().withDamage(0).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();

		LOGGER.debug("Attacker created with damage: {}", attacker.getDamage());

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Damage should be positive", exception.getMessage());
	}

	@Test
	void MaximumDamageHandling() {
		Player attacker = builder.resetBuilder().withDamage(Float.MAX_VALUE).withName("Attacker").withHealth(30)
				.build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();
		gameControllerwithMocks.attack(attacker, defender);
		assertEquals(0, defender.getHealth());
		assertEquals(false, defender.Isalive());
	}

	@ParameterizedTest
	@MethodSource("provideAttackScenarios")
	void attackerDealsDamage(Player attacker, Player defender, int expectedHealth, boolean expectedIsAlive) {
		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(expectedHealth, defender.getHealth());
		assertEquals(expectedIsAlive, defender.Isalive());
	}

	private static Stream<Arguments> provideAttackScenarios() {
		return Stream.of(
				Arguments.of(new PlayerBuilder().withDamage(50).withName("Attacker").withHealth(30).build(),
						new PlayerBuilder().resetBuilder().withDamage(10).withName("Defender").withHealth(50).build(),
						0, false),
				Arguments.of(
						new PlayerBuilder().resetBuilder().withDamage(60).withName("Attacker").withHealth(30).build(),
						new PlayerBuilder().resetBuilder().withDamage(10).withName("Defender").withHealth(50).build(),
						0, false),
				Arguments.of(
						new PlayerBuilder().resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build(),
						new PlayerBuilder().resetBuilder().withDamage(10).withName("Defender").withHealth(50).build(),
						40, true));
	}

	@Test
	void TestLoggingForAttackSuccess() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(20).build();

		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(10, defender.getHealth());
		// This test ensures the attack success condition is covered and indirectly the
		// log message.
	}

	@Test
	void TestDefenderHealthBoundaryAtZero() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(10).build();

		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(0, defender.getHealth());
		assertEquals(false, defender.Isalive()); // Ensure that defender is marked as dead.
		assertEquals("Defender", defender.getName()); // Check that name retrieval works after setting health to 0.
	}

	@Test
	void TestDamageZeroShouldFail() {
		Player attacker = builder.resetBuilder().withDamage(0).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});
		assertEquals("Damage should be positive", exception.getMessage());
		assertEquals("Attacker", attacker.getName()); // Ensure name retrieval works after invalid attack.
	}

	// Ensure attacking with positive damage works
	@Test
	void TestAttackingWithPositiveDamage() {
		Player attacker = builder.resetBuilder().withDamage(10).withName("Attacker").withHealth(30).build();
		Player defender = builder.resetBuilder().withDamage(10).withName("Defender").withHealth(50).build();

		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(40, defender.getHealth()); // Health should decrease
		assertTrue(defender.Isalive()); // Defender should still be alive
	}



	@Test
	void testValidatePlayersWithNullAttacker() {
		Player defender = mock(Player.class);
		// This should throw an IllegalArgumentException
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(null, defender);
		});
		assertTrue(exception.getMessage().contains("Attacker or defender is null"));
	}

	@Test
	void testValidatePlayersWithNullDefender() {
		Player attacker = mock(Player.class);
		// This should throw an IllegalArgumentException
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, null);
		});
		assertTrue(exception.getMessage().contains("Attacker or defender is null"));
	}


	@Test
	void testCalculateDamageWithZeroValue() {
		Player attacker = mock(Player.class);
		when(attacker.getDamage()).thenReturn(0.0f);
		// This should throw an IllegalArgumentException
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, mock(Player.class));
		});
		assertTrue(exception.getMessage().contains("Damage should be positive"));
	}

	@Test
	void testCalculateDamageWithNegativeValue() {
		Player attacker = mock(Player.class);
		when(attacker.getDamage()).thenReturn(-5.0f);
		// This should throw an IllegalArgumentException
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, mock(Player.class));
		});
		assertTrue(exception.getMessage().contains("Damage should be positive"));
	}

	@Test
	void testAttackSuccess() {
		// Create Players using the builder pattern
		Player attacker = new PlayerBuilder().resetBuilder().withName("Attacker").withDamage(100).withIsAlive(true)
				.build();
		Player defender = new PlayerBuilder().resetBuilder().withName("Defender").withDamage(50).withIsAlive(true)
				.withHealth(150).build();

		// Run the attack method
		controllerSpy.attack(attacker, defender);

		// Verify that the public behavior of the GameController is correct
		assertEquals(50, defender.getHealth());
		assertTrue(defender.Isalive());

		// Verify that logging occurred with the correct messages
		verify(logger).info(anyString(), eq("Attacker"), eq(100f), eq("Defender"), eq(150f));
		verify(logger).info(anyString(), eq("Defender"), eq(50f));
	}

	@Test
	void testAttackWithZeroDamageThrowsException() {
		// Create Players using the builder pattern
		Player attacker = new PlayerBuilder().resetBuilder().withName("Attacker").withDamage(0).withIsAlive(true)
				.build();
		Player defender = new PlayerBuilder().resetBuilder().withName("Defender").withDamage(50).withIsAlive(true)
				.withHealth(150).build();

		// Test that the exception is thrown when damage is zero
		assertThrows(IllegalArgumentException.class, () -> controllerSpy.attack(attacker, defender));

		// Verify that the error logging occurred with the correct message
		verify(logger).error("Attack failed: Damage should be positive");
	}

	@Test
	void testNullPlayerThrowsException() {
		// Create Players with one null player
		Player attacker = null;
		Player defender = new PlayerBuilder().resetBuilder().withName("Defender").withDamage(50).withIsAlive(true)
				.withHealth(150).build();

		// Test that the exception is thrown when attacker is null
		assertThrows(IllegalArgumentException.class, () -> controllerSpy.attack(attacker, defender));

		// Verify that the error logging occurred with the correct message
		verify(logger).error("Attacker or defender is null.");
	}

	@Test
	void testDefenderDefeated() {
		// Create Players using the builder pattern
		Player attacker = new PlayerBuilder().resetBuilder().withName("Attacker").withDamage(200).withIsAlive(true)
				.build();
		Player defender = new PlayerBuilder().resetBuilder().withName("Defender").withDamage(50).withIsAlive(true)
				.withHealth(150).build();

		// Run the attack method
		controllerSpy.attack(attacker, defender);

		// Verify the defender's health and alive status
		assertEquals(0, defender.getHealth());
		assertFalse(defender.Isalive());

		// Verify that the correct logging occurred
		verify(logger).info("Attack successful: Defender: {} has been defeated (Health: 0, IsAlive: {})",
				eq("Defender"), eq(false));

	}

	@Test
	void testCalculateDamageWithPositiveValueAndZeroSubstitution() {
		Player attacker = builder.resetBuilder().withDamage(1).build();
		Player defender = builder.resetBuilder().withHealth(100).build();

		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(99, defender.getHealth()); // Ensure damage is reduced by 1
	}

	@Test
	void testUpdateDefenderHealthCallsSetAlive() {
		Player attacker = builder.resetBuilder().withDamage(100).build();
		Player defender = spy(builder.resetBuilder().withHealth(50).withIsAlive(true).build());

		gameControllerwithMocks.attack(attacker, defender);

		assertEquals(0, defender.getHealth()); // Ensure defender's health is 0
		verify(defender).setAlive(false); // Verify setAlive(false) is called
		assertFalse(defender.Isalive()); // Ensure defender is not alive
	}

	@Test
	void testDefenderHealthIsNotSetTo1WhenHealthIsZero() {
		// Create attacker and defender
		Player attacker = builder.resetBuilder().withDamage(50).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Call the attack method, which should reduce defender's health to exactly 0
		gameControllerwithMocks.attack(attacker, defender);

		// Assert that the defender's health is exactly 0, not 1
		assertEquals(0, defender.getHealth());
		assertFalse(defender.Isalive()); // Ensure defender is dead
	}

	@Test
	void testIsAliveCalledCorrectlyWhenDefenderDies() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(100).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify that setAlive() and isAlive() are called
		Player defenderSpy = spy(defender);

		// Call the attack method, which should kill the defender
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify that setAlive(false) was called
		verify(defenderSpy).setAlive(false);

		// Optionally, check the alive status after the attack
		assertFalse(defenderSpy.Isalive());

	}

	@Test
	void testDefenderHealthReducedToZero() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(100).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Perform the attack
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify the method invocation order
		InOrder inOrder = Mockito.inOrder(defenderSpy);

		// First, setAlive(false) should be called
		inOrder.verify(defenderSpy).setAlive(false);

		// After that, isAlive() should be called
		inOrder.verify(defenderSpy).Isalive();

		// Verify that the defender's health is correctly set to 0
		assertEquals(0, defenderSpy.getHealth(), 0.0);
	}

	@Test
	void testDefenderHealthReducedBelowZero() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(100).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Perform the attack
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify that the defender's health is correctly set to 0
		assertEquals(0, defenderSpy.getHealth(), 0.0);

		// Verify that setAlive(false) was called once
		verify(defenderSpy, times(1)).setAlive(false);

		// Verify that isAlive() was called exactly once
		verify(defenderSpy, times(1)).Isalive();
	}

	@Test
	void testDefenderHealthReducedToZeroAndBelow() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(50).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Create the GameController

		// Perform the attack which should reduce health to zero
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify that the defender's health is exactly 0
		assertEquals(0, defenderSpy.getHealth(), 0.0);

		// Verify that setAlive(false) was called exactly once
		verify(defenderSpy, times(1)).setAlive(false);

		// Verify that isAlive() was called exactly once
		verify(defenderSpy, times(1)).Isalive();

		// Now perform an overkill attack to ensure health does not go negative
		attacker.setDamage(100);
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Ensure health is still 0 and isAlive() is still false
		assertEquals(0, defenderSpy.getHealth(), 0.0);
		assertFalse(defenderSpy.Isalive());
	}

	@Test
	void testLoggingWhenDefenderDefeated() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(100).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Run the attack method
		controllerSpy.attack(attacker, defender);

		// Verify that the correct logging occurred
		verify(logger).info("Attack successful: Defender: {} has been defeated (Health: 0, IsAlive: {})",
				eq("Defender"), eq(false));
	}

	@Test
	void testHealthExactlyAtZeroAfterAttack() {
		Player attacker = builder.resetBuilder().withDamage(50).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Perform the attack
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify the method invocation order using InOrder
		InOrder inOrder = Mockito.inOrder(defenderSpy);

		// First, setAlive(false) should be called
		inOrder.verify(defenderSpy).setAlive(false);

		// After that, isAlive() should be called
		inOrder.verify(defenderSpy).Isalive();

		// Verify that the defender's health is correctly set to 0
		assertEquals(0, defenderSpy.getHealth(), 0.0);
	}

	@Test
	void testOverkillAttack() {
		// Create attacker with overkill damage
		Player attacker = builder.resetBuilder().withDamage(200).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Perform the attack
		gameControllerwithMocks.attack(attacker, defender);

		// Ensure defender's health is 0 and they are dead
		assertEquals(0, defender.getHealth());
		assertFalse(defender.Isalive());
	}

	@Test
	void testDefenderHealthExactlyOne() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(49).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(50).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Perform the attack which should reduce health to exactly 1
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify that the defender's health is exactly 1 and not considered defeated
		assertEquals(1, defenderSpy.getHealth(), 0.0);

		// Ensure defender is still alive
		assertTrue(defenderSpy.Isalive());

		// Verify that setAlive(false) was never called because defender is still alive
		verify(defenderSpy, times(0)).setAlive(false);

		// Ensure correct logging for the attack with health remaining
		verify(logger).info("Attack successful: Defender: {}'s new health: {}", eq("Defender"), eq(1f));
	}

	@Test
	void testDefenderHealthReducesToZeroFromOne() {
		// Create attacker and defender using the builder
		Player attacker = builder.resetBuilder().withDamage(1).withName("Attacker").withHealth(100).build();
		Player defender = builder.resetBuilder().withName("Defender").withHealth(1).build();

		// Spy on the defender object to verify method calls
		Player defenderSpy = spy(defender);

		// Perform the attack which should reduce health to exactly 0
		gameControllerwithMocks.attack(attacker, defenderSpy);

		// Verify that the defender's health is exactly 0
		assertEquals(0, defenderSpy.getHealth(), 0.0);

		// Ensure defender is marked as dead
		assertFalse(defenderSpy.Isalive());

		// Verify that setAlive(false) was called because defender is dead
		verify(defenderSpy, times(1)).setAlive(false);

		// Ensure correct logging for the attack resulting in death
		verify(logger).info("Attack successful: Defender: {} has been defeated (Health: 0, IsAlive: {})",
				eq("Defender"), eq(false));
	}

	@Test
	void testCreatePlayer() {
		// Arrange
		String playerName = "TestPlayer";
		float health = 100f;
		float damage = 50f;
		Player expectedPlayer = new PlayerBuilder().withName(playerName).withHealth(health).withDamage(damage).build();

		// Act
		Player createdPlayer = gameControllerwithMocks.createPlayer(playerName, health, damage);

		// Assert
		assertEquals(expectedPlayer.getName(), createdPlayer.getName());
		assertEquals(expectedPlayer.getHealth(), createdPlayer.getHealth());
		assertEquals(expectedPlayer.getDamage(), createdPlayer.getDamage());

		// Verify that updatePlayer was called on playerDAO
		verify(playerDAOMock).updatePlayer(any(Player.class));
	}

	@Test
	void testAddPlayerToMap() {
		// Arrange
		Long mapId = 1L;
		Player player = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		GameMap mockGameMap = mock(GameMap.class);
		when(gameMapDAOMock.findById(mapId)).thenReturn(mockGameMap);

		// Act
		gameControllerwithMocks.addPlayerToMap(mapId, player);

		// Assert
		verify(gameMapDAOMock).addPlayerToMap(mapId, player);
		verify(gameMapDAOMock).update(mockGameMap);
	}

	@Test
	void testAddPlayerToMap_MapNotFound() {
		// Arrange
		Long mapId = 1L;
		Player player = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		when(gameMapDAOMock.findById(mapId)).thenReturn(null);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.addPlayerToMap(mapId, player);
		});

		assertEquals("Map with ID 1 not found", exception.getMessage());
		verify(gameMapDAOMock, never()).addPlayerToMap(anyLong(), any(Player.class));
	}

	@Test
	void testRemovePlayerFromMap() {
		// Arrange
		Long mapId = 1L;
		Player player = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		GameMap mockGameMap = mock(GameMap.class);
		when(gameMapDAOMock.findById(mapId)).thenReturn(mockGameMap);
		when(mockGameMap.getPlayers()).thenReturn(List.of(player));

		// Act
		gameControllerwithMocks.removePlayerFromMap(mapId, player);

		// Assert
		verify(gameMapDAOMock).removePlayerFromMap(mapId, player);
		verify(gameMapDAOMock).update(mockGameMap);
	}

	@Test
	void testRemovePlayerFromMap_MapNotFound() {
		// Arrange
		Long mapId = 1L;
		Player player = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		when(gameMapDAOMock.findById(mapId)).thenReturn(null);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.removePlayerFromMap(mapId, player);
		});

		// Update the expected message to match the actual message
		assertEquals("Map with ID 1 or player TestPlayer not found", exception.getMessage());
	}

	@Test
	void testAttack_KillDefender() {
		// Arrange
		Player attacker = new PlayerBuilder().withName("Attacker").withHealth(100f).withDamage(200f).build();
		Player defender = new PlayerBuilder().withName("Defender").withHealth(100f).build();

		// Act
		gameControllerwithMocks.attack(attacker, defender);

		// Assert
		assertEquals(0f, defender.getHealth());
		assertFalse(defender.Isalive());
		verifyNoInteractions(playerDAOMock, gameMapDAOMock);
	}

	@Test
	void testAttack_DefenderNull() {
		// Arrange
		Player attacker = new PlayerBuilder().withName("Attacker").withHealth(100f).withDamage(20f).build();
		Player defender = null;

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});

		assertEquals("Attacker or defender is null.", exception.getMessage());
		verifyNoInteractions(playerDAOMock, gameMapDAOMock);
	}

	@Test
	void testValidatePlayers_NullAttacker() {
		Player defender = mock(Player.class);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(null, defender);
		});

		assertEquals("Attacker or defender is null.", exception.getMessage());
		verify(logger).error("Attacker or defender is null.");
	}

	@Test
	void testValidatePlayers_NullDefender() {
		Player attacker = mock(Player.class);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, null);
		});

		assertEquals("Attacker or defender is null.", exception.getMessage());
		verify(logger).error("Attacker or defender is null.");
	}

	@Test
	void testCalculateDamage_NegativeDamage() {
		Player attacker = mock(Player.class);
		when(attacker.getDamage()).thenReturn(-10f);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, mock(Player.class));
		});

		assertEquals("Damage should be positive", exception.getMessage());
		verify(logger).error("Attack failed: Damage should be positive");
	}

	// Test for addPlayerToMap with invalid mapId
	@Test
	void testAddPlayerToMapWithInvalidMapId() {
		// Arrange
		Long invalidMapId = 1L;
		Player player = builder.resetBuilder().withName("TestPlayer").withHealth(100).withDamage(10).build();

		// Mock the behavior to return null for an invalid mapId
		when(gameMapDAOMock.findById(invalidMapId)).thenReturn(null);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.addPlayerToMap(invalidMapId, player);
		});

		assertEquals("Map with ID 1 not found", exception.getMessage());
	}

	// Test for removePlayerFromMap with invalid mapId
	@Test
	void testRemovePlayerFromMapWithInvalidMapId() {
		// Arrange
		Long invalidMapId = 1L;
		Player player = builder.resetBuilder().withName("TestPlayer").withHealth(100).withDamage(10).build();

		// Mock the behavior to return null for an invalid mapId
		when(gameMapDAOMock.findById(invalidMapId)).thenReturn(null);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.removePlayerFromMap(invalidMapId, player);
		});

		assertEquals("Map with ID 1 or player TestPlayer not found", exception.getMessage());
	}

	// Test for removePlayerFromMap with player not in map
	@Test
	void testRemovePlayerFromMapWithPlayerNotInMap() {
		// Arrange
		Long mapId = 1L;
		GameMap gameMap = new GameMap();
		Player playerNotInMap = builder.resetBuilder().withName("TestPlayer").withHealth(100).withDamage(10).build();

		// Mock the behavior to return a valid map but without the player
		when(gameMapDAOMock.findById(mapId)).thenReturn(gameMap);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.removePlayerFromMap(mapId, playerNotInMap);
		});

		assertEquals("Map with ID 1 or player TestPlayer not found", exception.getMessage());
	}

	// Test for attack with null attacker
	@Test
	void testAttackWithNullAttacker() {
		// Arrange
		Player defender = builder.resetBuilder().withName("Defender").withHealth(100).withDamage(10).build();

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(null, defender);
		});

		assertEquals("Attacker or defender is null.", exception.getMessage());
	}

	// Test for attack with null defender
	@Test
	void testAttackWithNullDefender() {
		// Arrange
		Player attacker = builder.resetBuilder().withName("Attacker").withHealth(100).withDamage(10).build();

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, null);
		});

		assertEquals("Attacker or defender is null.", exception.getMessage());
	}

	// Test for attack with damage less than or equal to 0
	@Test
	void testAttackWithInvalidDamage() {
		// Arrange
		Player attacker = builder.resetBuilder().withName("Attacker").withHealth(100).withDamage(0).build(); // Invalid
																												// damage:
																												// 0
		Player defender = builder.resetBuilder().withName("Defender").withHealth(100).withDamage(10).build();

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.attack(attacker, defender);
		});

		assertEquals("Damage should be positive", exception.getMessage());
	}

	@Test
	void testRemovePlayerFromMap_GameMapPlayersIsNull() {
		// Arrange
		Player player = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		Long mapId = 1L; // Valid map ID

		// Mock gameMapDAO to return a map with null players list
		GameMap mockGameMap = mock(GameMap.class);
		when(mockGameMap.getPlayers()).thenReturn(null);
		when(gameMapDAOMock.findById(mapId)).thenReturn(mockGameMap);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.removePlayerFromMap(mapId, player);
		});

		assertEquals("Map with ID 1 or player TestPlayer not found", exception.getMessage());
		verify(logger).error("Map with ID {} or player {} not found", mapId, player.getName());
	}

	@Test
	void testRemovePlayerFromMap_PlayerNotInMap() {
		// Arrange
		Player playerNotInMap = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		Long mapId = 1L; // Valid map ID

		// Mock gameMapDAO to return a map with an empty players list
		GameMap mockGameMap = mock(GameMap.class);
		when(mockGameMap.getPlayers()).thenReturn(new ArrayList<>());
		when(gameMapDAOMock.findById(mapId)).thenReturn(mockGameMap);

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			gameControllerwithMocks.removePlayerFromMap(mapId, playerNotInMap);
		});

		assertEquals("Map with ID 1 or player TestPlayer not found", exception.getMessage());
		verify(logger).error("Map with ID {} or player {} not found", mapId, playerNotInMap.getName());
	}

	@Test
	void testRemovePlayerFromMap_PlayerSuccessfullyRemoved() {
		// Arrange
		Player playerToRemove = new PlayerBuilder().withName("TestPlayer").withHealth(100f).withDamage(50f).build();
		Long mapId = 1L; // Valid map ID

		// Mock gameMapDAO to return a map with the player in the players list
		GameMap mockGameMap = mock(GameMap.class);
		List<Player> playersList = new ArrayList<>();
		playersList.add(playerToRemove);
		when(mockGameMap.getPlayers()).thenReturn(playersList);
		when(gameMapDAOMock.findById(mapId)).thenReturn(mockGameMap);

		gameControllerwithMocks.removePlayerFromMap(mapId, playerToRemove);

		verify(gameMapDAOMock).removePlayerFromMap(mapId, playerToRemove);
		verify(gameMapDAOMock).update(mockGameMap);
		verify(logger).info("Player {} removed from map {}", playerToRemove.getName(), mockGameMap.getName());
	}

}
