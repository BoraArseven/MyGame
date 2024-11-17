package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class PlayerAttackViewTest extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private PlayerAttackView playerAttackView;

	public PlayerAttackView getPlayerAttackView() {
		return playerAttackView;
	}

	public void setPlayerAttackView(PlayerAttackView playerAttackView) {
		this.playerAttackView = playerAttackView;
	}

	@Mock
	private GameController mockGameController;

	@Override
	protected void onSetUp() throws Exception {
		// Initialize Mockito mocks
		MockitoAnnotations.openMocks(this);

		// Create the UI component and inject the mock controller
		GuiActionRunner.execute(() -> {
			playerAttackView = new PlayerAttackView();
			playerAttackView.setGameController(mockGameController);
			return playerAttackView;
		});

		// Initialize the AssertJ Swing window fixture
		window = new FrameFixture(robot(), playerAttackView);
		window.show(); // Show the window to be tested
	}

	@BeforeEach
	public void reset() {
		GuiActionRunner.execute(() -> playerAttackView.resetErrorLabel());
	}

	@Test
	@GUITest
	public void testMapListAndPlayerListsPopulate() {
		// Mock the GameController behavior for getAllMaps() and getAllPlayers()
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testPlayer1 = new Player("Player1", 100, 20, true);
		Player testPlayer2 = new Player("Player2", 80, 15, true);

		// Associate players with the selected map
		testPlayer1.setMap(testMap);
		testPlayer2.setMap(testMap);

		// Mock the GameController's getAllMaps() and getAllPlayers() methods
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(anyLong())).thenReturn(Arrays.asList(testPlayer1, testPlayer2));

		// Inject the mock controller into the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
		});

		// Populate the map list in the UI
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = playerAttackView.getMapListModel();
			mapListModel.addElement(testMap);
		});

		// Select the map, which should trigger a call to getAllPlayers
		window.list("mapList").selectItem(0);

		// Verify that getAllPlayers() was called once when the map was selected
		verify(mockGameController, times(1)).getPlayersFromMap(testMap.getId());

		// Verify that the player lists are populated correctly
		DefaultListModel<Player> attackerListModel = playerAttackView.getAttackerListModel();
		DefaultListModel<Player> defenderListModel = playerAttackView.getDefenderListModel();

		assertThat(attackerListModel.getSize()).isEqualTo(2); // Ensure the attacker list has players
		assertThat(defenderListModel.getSize()).isEqualTo(2); // Ensure the defender list has players
		assertThat(attackerListModel.getElementAt(0)).isEqualTo(testPlayer1);
		assertThat(attackerListModel.getElementAt(1)).isEqualTo(testPlayer2);
		assertThat(defenderListModel.getElementAt(0)).isEqualTo(testPlayer1);
		assertThat(defenderListModel.getElementAt(1)).isEqualTo(testPlayer2);
	}

	@Test
	@GUITest
	public void testAttackButtonEnabledWhenPlayersSelected() {
		// Set up the initial map and player list
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testPlayer1 = new Player("Player1", 100, 20, true);
		Player testPlayer2 = new Player("Player2", 80, 15, true);

		// Associate players with the map
		testPlayer1.setMap(testMap);
		testPlayer2.setMap(testMap);

		// Mock the GameController behavior for getAllMaps() and getAllPlayers()
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testPlayer1, testPlayer2));

		// Inject the mock controller into the view and populate the map and player
		// lists
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map, attacker, and defender in the UI
		window.list("mapList").selectItem(0);
		GuiActionRunner.execute(() -> {
			playerAttackView.getAttackerListModel().addElement(testPlayer1);
			playerAttackView.getDefenderListModel().addElement(testPlayer2);
		});

		// Simulate player selections in the UI
		window.list("attackerList").selectItem(0); // Select the first attacker
		window.list("defenderList").selectItem(0); // Select the first defender

		// Verify that the attack button is enabled when players are selected
		window.button(JButtonMatcher.withText("Attack")).requireEnabled();

		// Use the utility function to find the button and verify its state
		JButtonFixture attackButton = window.button(JButtonMatcher.withText("Attack"));
		assertNotNull(attackButton, "Attack button should exist");
		assertTrue(attackButton.isEnabled(), "Attack button should be enabled");
	}

	@Test
	@GUITest
	public void testAttackButtonDisabledWhenNoPlayersSelected() {
		// Set up the initial map and player list
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testPlayer1 = new Player("Player1", 100, 20, true);
		Player testPlayer2 = new Player("Player2", 80, 15, true);
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testPlayer1, testPlayer2));

		GuiActionRunner.execute(() -> {
			playerAttackView.getMapListModel().addElement(testMap);
			playerAttackView.getAttackerListModel().addElement(testPlayer1);
			playerAttackView.getDefenderListModel().addElement(testPlayer2);
		});

		// Select the map without selecting any players
		window.list("mapList").selectItem(0);

		// Verify that no attacker and defender are selected
		assertThat(window.list("attackerList").selection()).isEmpty();
		assertThat(window.list("defenderList").selection()).isEmpty();

		// Verify that the attack button is disabled when no players are selected
		window.button(JButtonMatcher.withText("Attack")).requireDisabled();

	}

	@Test
	@GUITest
	public void testAttackButtonDisabledWhenNeitherAttackerNorDefenderSelected() {
		// Set up a mock map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		Player testDefender = new Player("Defender", 80, 15, true);
		testAttacker.setMap(testMap);
		testDefender.setMap(testMap);
		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testAttacker, testDefender));
		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});
		// Select the map but do not select the attacker or defender
		window.list("mapList").selectItem(0);

		// Additional assertions to verify no attacker or defender is selected
		assertThat(window.list("attackerList").selection()).isEmpty();
		assertThat(window.list("defenderList").selection()).isEmpty();

		// Verify that the attack button is disabled
		window.button(JButtonMatcher.withText("Attack")).requireDisabled();
	}

	@Test
	@GUITest
	public void testSelfAttackIsNotAllowed() {
		// Set up the initial map and player list
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testPlayer = new Player("Player1", 100, 20, true);

		// Associate the player with the map
		testPlayer.setMap(testMap);

		// Mock the GameController behavior for getAllMaps() and getAllPlayers()
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Collections.singletonList(testPlayer));

		// Inject the mock controller into the view and populate the map and player
		// lists
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
			playerAttackView.getAttackerListModel().addElement(testPlayer);
			playerAttackView.getDefenderListModel().addElement(testPlayer);
		});

		// Select the same player as both the attacker and defender
		window.list("mapList").selectItem(0);
		window.list("attackerList").selectItem(0);
		window.list("defenderList").selectItem(0);

		// Click the attack button
		window.button(JButtonMatcher.withText("Attack")).click();

		// Verify that the error message is shown and no attack is performed
		window.label("errorLabel").requireText("A player cannot attack itself.");
		verify(mockGameController, times(0)).attack(testPlayer, testPlayer); // Ensure the attack method is never called
	}

	@Test
	@GUITest
	public void testNoMapSelected_PlayerListsNotUpdated() {
		// Mock the GameController behavior for getAllMaps and getAllPlayers
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testPlayer1 = new Player("Player1", 100, 20, true);

		// Mock the GameController
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Collections.singletonList(testPlayer1));

		// Inject the mock controller and add the map to the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Ensure no map is selected
		window.list("mapList").clearSelection();

		// Verify that getAllPlayers is NOT called and player lists are not populated
		verify(mockGameController, times(0)).getAllPlayers();
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	@GUITest
	public void testMapSelected_NoPlayersAssociated() {
		// Mock the GameController behavior for getAllMaps and getAllPlayers
		GameMap testMap = new GameMap(1L, "TestMap");

		// Mock the GameController
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Collections.emptyList()); // No players
																											// associated

		// Inject the mock controller and add the map to the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Verify that getAllPlayers was called, but no players are added to the lists
		verify(mockGameController, times(1)).getPlayersFromMap(testMap.getId());
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	@GUITest
	public void testRefreshPlayerLists_NoMapSelected() {
		// Ensure no map is selected
		GuiActionRunner.execute(() -> playerAttackView.getMapList().clearSelection());

		// Try to refresh player lists
		playerAttackView.refreshPlayerLists();

		// Verify that getAllPlayers is NOT called
		verify(mockGameController, times(0)).getAllPlayers();

		// Ensure the player lists remain empty
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	public void testPlayerWithoutMap() {
		GameMap selectedMap = new GameMap(1L, "TestMap");
		Player playerWithoutMap = new Player("Player1", 100, 20, true);
		playerWithoutMap.setMap(null); // Player without a map

		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(selectedMap));
		when(mockGameController.getAllPlayers()).thenReturn(Collections.singletonList(playerWithoutMap));

		// Inject the mock controller into the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(selectedMap);
		});

		// Select the map, which should trigger a call to getAllPlayers and refresh the
		// player lists
		window.list("mapList").selectItem(0);

		// Verify that no players were added to the attacker or defender lists since
		// playerWithoutMap has no map
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	public void testPlayerWithDifferentMap() {
		GameMap selectedMap = new GameMap(1L, "TestMap");
		GameMap differentMap = new GameMap(2L, "DifferentMap");
		Player playerWithDifferentMap = new Player("Player1", 100, 20, true);
		playerWithDifferentMap.setMap(differentMap); // Player with a different map

		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(selectedMap));
		when(mockGameController.getAllPlayers()).thenReturn(Collections.singletonList(playerWithDifferentMap));

		// Inject the mock controller into the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(selectedMap);
		});

		// Select the map, which should trigger a call to getAllPlayers and refresh the
		// player lists
		window.list("mapList").selectItem(0);

		// Verify that no players were added to the attacker or defender lists since the
		// player's map doesn't match the selected map
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	public void testPlayerNotAlive() {
		GameMap selectedMap = new GameMap(1L, "TestMap");
		Player deadPlayer = new Player("Player1", 0, 20, false); // Dead player
		deadPlayer.setMap(selectedMap); // Player belongs to the selected map but is not alive

		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(selectedMap));
		when(mockGameController.getAllPlayers()).thenReturn(Collections.singletonList(deadPlayer));

		// Inject the mock controller into the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(selectedMap);
		});

		// Select the map, which should trigger a call to getAllPlayers and refresh the
		// player lists
		window.list("mapList").selectItem(0);

		// Verify that no players were added to the attacker or defender lists since the
		// player is not alive
		assertThat(playerAttackView.getAttackerListModel().getSize()).isZero();
		assertThat(playerAttackView.getDefenderListModel().getSize()).isZero();
	}

	@Test
	public void testPlayerWithMatchingMapAndAlive() {
		GameMap selectedMap = new GameMap(1L, "TestMap");
		Player alivePlayer = new Player("Player1", 100, 20, true); // Alive player
		alivePlayer.setMap(selectedMap); // Player belongs to the selected map

		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(selectedMap));
		when(mockGameController.getPlayersFromMap(selectedMap.getId()))
				.thenReturn(Collections.singletonList(alivePlayer));

		// Inject the mock controller into the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(selectedMap);
		});

		// Select the map, which should trigger a call to getAllPlayers and refresh the
		// player lists
		window.list("mapList").selectItem(0);

		// Verify that the player is added to the attacker and defender lists since they
		// belong to the selected map and are alive
		assertThat(playerAttackView.getAttackerListModel().getSize()).isEqualTo(1);
		assertThat(playerAttackView.getDefenderListModel().getSize()).isEqualTo(1);
		assertThat(playerAttackView.getAttackerListModel().getElementAt(0)).isEqualTo(alivePlayer);
		assertThat(playerAttackView.getDefenderListModel().getElementAt(0)).isEqualTo(alivePlayer);
	}

	@Test
	@GUITest
	public void testAttackButtonEnabledWhenBothAttackerAndDefenderSelected() {
		// Set up a mock map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		Player testDefender = new Player("Defender", 80, 15, true);
		testAttacker.setMap(testMap);
		testDefender.setMap(testMap);
		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testAttacker, testDefender));
		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});
		// Select the map, attacker, and defender in the UI
		window.list("mapList").selectItem(0);
		GuiActionRunner.execute(() -> {
			playerAttackView.getAttackerListModel().addElement(testAttacker);
			playerAttackView.getDefenderListModel().addElement(testDefender);
		});
		// Simulate selecting both the attacker and defender
		window.list("attackerList").selectItem(0);
		window.list("defenderList").selectItem(0);

		// Additional assertions to verify the selected attacker and defender
		assertThat(window.list("attackerList").selection()[0]).isEqualTo(testAttacker.toString());
		assertThat(window.list("defenderList").selection()[0]).isEqualTo(testDefender.toString());

		// Verify that the attack button is enabled
		window.button(JButtonMatcher.withText("Attack")).requireEnabled();
	}

	@Test
	@GUITest
	public void neitherAttackerNorDefenderSelected() {
		// Set up a mock map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		Player testDefender = new Player("Defender", 80, 15, true);
		testAttacker.setMap(testMap);
		testDefender.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testAttacker, testDefender));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map but do not select the attacker or defender
		window.list("mapList").selectItem(0);

		// Verify that the attack button is disabled using Fest-Assert
		window.button(JButtonMatcher.withText("Attack")).requireDisabled();

		// Additional assertion: Verify programmatically that the button is disabled
		JButton attackButton = window.button(JButtonMatcher.withText("Attack")).target();
		assertFalse(attackButton.isEnabled(), "Attack button should be disabled");
	}

	@Test
	@GUITest
	public void testAttackButtonDisabledWhenOnlyAttackerSelected() {
		// Set up a mock map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		Player testDefender = new Player("Defender", 80, 15, true);
		testAttacker.setMap(testMap);
		testDefender.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testAttacker, testDefender));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Simulate selecting only the attacker
		window.list("mapList").selectItem(0);
		GuiActionRunner.execute(() -> {
			playerAttackView.getAttackerListModel().addElement(testAttacker);
			playerAttackView.getDefenderListModel().addElement(testDefender);
		});
		window.list("attackerList").selectItem(0);

		// Verify that the attack button is disabled using Fest-Assert
		window.button(JButtonMatcher.withText("Attack")).requireDisabled();

		// Additional assertion: Verify programmatically that the button is disabled
		JButton attackButton = window.button(JButtonMatcher.withText("Attack")).target();
		assertFalse(attackButton.isEnabled(), "Attack button should be disabled when only the attacker is selected");
	}

	@Test
	@GUITest
	public void testAttackButtonDisabledWhenOnlyDefenderSelected() {
		// Set up a mock map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		Player testDefender = new Player("Defender", 80, 15, true);
		testAttacker.setMap(testMap);
		testDefender.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getAllPlayers()).thenReturn(Arrays.asList(testAttacker, testDefender));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Simulate selecting only the defender
		window.list("mapList").selectItem(0);
		GuiActionRunner.execute(() -> {
			playerAttackView.getAttackerListModel().addElement(testAttacker);
			playerAttackView.getDefenderListModel().addElement(testDefender);
		});
		window.list("defenderList").selectItem(0);

		// Verify that the attack button is disabled using Fest-Assert
		window.button(JButtonMatcher.withText("Attack")).requireDisabled();

		// Additional assertion: Verify programmatically that the button is disabled
		JButton attackButton = window.button(JButtonMatcher.withText("Attack")).target();
		assertFalse(attackButton.isEnabled(), "Attack button should be disabled when only the defender is selected");

	}

	@Test
	@GUITest
	public void testAttackShouldNotBePerformedIfAttackerOrDefenderIsNull() {
		// Mocking setup for attacker and defender
		Player testPlayer = new Player("Player1", 100, 20, true);
		GameMap testMap = new GameMap(1L, "TestMap");
		testPlayer.setMap(testMap);

		// Mock GameController behavior
		when(mockGameController.getAllPlayers()).thenReturn(Collections.singletonList(testPlayer));
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));

		// Populate map and players in the UI thread
		GuiActionRunner.execute(() -> {
			playerAttackView.getMapListModel().addElement(testMap);
			playerAttackView.getAttackerListModel().addElement(testPlayer);
			playerAttackView.getDefenderListModel().addElement(testPlayer);
		});

		// Ensure the list has items before selecting
		assertThat(window.list("attackerList").target().getModel().getSize()).isPositive();
		assertThat(window.list("defenderList").target().getModel().getSize()).isPositive();

		// Select items
		window.list("attackerList").selectItem(0); // Select the first attacker
		window.list("defenderList").selectItem(0); // Select the first defender

		// Assert that the attack is not performed (test for null conditions)
		window.button(JButtonMatcher.withText("Attack")).click();
		verify(mockGameController, times(0)).attack(any(), any());
	}

	@Test
	public void testAttackThrowsExceptionShouldShowErrorMessage() {
		// Setup players and map
		Player player1 = new PlayerBuilder().withName("Player1").withHealth(100f).withDamage(20f).withIsAlive(true)
				.build();
		player1.setId(1L);
		Player player2 = new PlayerBuilder().withName("Player2").withHealth(80f).withDamage(15f).withIsAlive(true)
				.build();
		player2.setId(2L);

		GameMap testMap = new GameMap("TestMap");
		testMap.setPlayers(Arrays.asList(player1, player2));

		// Mock the GameController to return the players when getAllPlayers() is called
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Arrays.asList(player1, player2));

		// Populate the map and player list on the EDT
		// Populate the map and player list on the EDT
		GuiActionRunner.execute(() -> {
			playerAttackView.getMapListModel().addElement(testMap);

			// Ensure lists are updated after adding elements
			playerAttackView.getMapList().revalidate();
			playerAttackView.getMapList().repaint();

			// Select the map in the view to trigger player list refresh
			playerAttackView.getMapList().setSelectedIndex(0);

		});
		// Now select the map and players in the test window
		window.list("mapList").selectItem(0); // Select the first map in the list
		GuiActionRunner.execute(() -> { // Call the method to refresh player lists based on the selected map
			playerAttackView.refreshPlayerLists();

			// Ensure player lists are updated after the refresh
			playerAttackView.getAttackerList().revalidate();
			playerAttackView.getAttackerList().repaint();
			playerAttackView.getDefenderList().revalidate();
			playerAttackView.getDefenderList().repaint();
		});
		// Mock the GameController behavior to throw an exception when attack is called
		doThrow(new IllegalStateException("Attack failed")).when(mockGameController).attack(player1, player2);

		window.list("attackerList").selectItem(0); // Select Player1 as attacker
		window.list("defenderList").selectItem(1); // Select Player2 as defender

		// Simulate clicking the attack button
		window.button("btnAttack").click();

		// Verify that the attack method was called on the controller
		verify(mockGameController).attack(player1, player2);

		// Assert that the error message is displayed after the exception
		window.label("errorLabel").requireText("Failed to perform attack.");
	}

	@Test
	public void testAttackShouldBePerformedSuccessfully() {
		// Setup players and map
		Player player1 = new PlayerBuilder().withName("Player1").withHealth(100f).withDamage(20f).build();
		player1.setId(1L);
		Player player2 = new PlayerBuilder().withName("Player2").withHealth(80f).withDamage(15f).build();
		player2.setId(2L);

		GameMap testMap = new GameMap("TestMap");
		testMap.setPlayers(Arrays.asList(player1, player2));

		// Mocking the game controller behavior
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Arrays.asList(player1, player2));

		// Populate map list
		GuiActionRunner.execute(() -> {
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Interact with UI components via AssertJ Swing
		window.list("mapList").selectItem(0); // Select the first map in the list
		window.list("attackerList").selectItem(0); // Select Player1 as attacker
		window.list("defenderList").selectItem(1); // Select Player2 as defender

		// Simulate clicking the attack button
		window.button("btnAttack").click();

		// Verify that the attack method was called
		verify(mockGameController).attack(player1, player2);
	}

	@Test
	@GUITest
	public void testAttackFailsWhenNoPlayersSelected() {
		// Setup the map and players
		GameMap testMap = new GameMap(1L, "TestMap");
		Player player1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true)
				.build();
		Player player2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true)
				.build();

		// Associate players with the map
		player1.setMap(testMap);
		player2.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Arrays.asList(player1, player2));

		// Populate the map and player lists in the UI
		GuiActionRunner.execute(() -> {
			playerAttackView.getMapListModel().addElement(testMap);
			playerAttackView.getAttackerListModel().addElement(player1);
			playerAttackView.getDefenderListModel().addElement(player2);

			// Manually enable the attack button
			playerAttackView.getBtnAttack().setEnabled(true);
		});

		// Select the map but do not select any players from attacker or defender list
		window.list("mapList").selectItem(0);

		// Ensure no player is selected
		window.list("attackerList").clearSelection();
		window.list("defenderList").clearSelection();

		// Invoke the attack method directly (bypassing UI checks)
		GuiActionRunner.execute(() -> {
			playerAttackView.attackSelectedPlayers();
		});

		// Assert that the correct error message is displayed
		window.label("errorLabel").requireText("Attacker and defender must be selected.");

		// Verify that the attack method was never called since no players were selected
		verify(mockGameController, times(0)).attack(any(), any());
	}

	@Test
	@GUITest
	public void testAttackFailsWhenDefenderIsNullButAttackerExists() {
		// Set up the map and attacker
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		testAttacker.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Collections.singletonList(testAttacker));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Ensure that the attacker list is populated
		assertThat(playerAttackView.getAttackerListModel().getSize()).isEqualTo(1);

		// Ensure that no attacker is selected yet
		assertThat(window.list("attackerList").selection()).isEmpty();

		// Select the attacker
		window.list("attackerList").selectItem(0);

		// Attempt to perform the attack without selecting a defender
		window.button(JButtonMatcher.withText("Attack")).click();

		// Verify that the "Attack" button is disabled
		window.button("btnAttack").requireDisabled();

		// Verify that the attack method was not called
		verify(mockGameController, times(0)).attack(any(), any());
		assertThat(window.label("errorLabel").text()).isEmpty();

	}

	@Test
	@GUITest
	public void testRefreshMapListChangesLabelWhenThereIsException() {
		// Mock the behavior of GameController to throw an exception
		when(mockGameController.getAllMaps()).thenThrow(new IllegalStateException("Failed to refresh map list."));

		// Ensure refreshMapList() is called on the EDT
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
		});

		// Check label text after calling refreshMapList
		window.label("errorLabel").requireText("Failed to refresh map list.");

		// Additional assertion: Verify programmatically that the label contains the
		// expected text
		JLabel errorLabel = window.label("errorLabel").target();
		assertNotNull(errorLabel, "Error label should exist");
		assertEquals("Failed to refresh map list.", errorLabel.getText());
	}

	@Test
	@GUITest
	public void testAttackFailsWhenAttackerIsNullButDefenderExists() {
		// Set up the map and defender
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testDefender = new Player("Defender", 80, 15, true);
		testDefender.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Collections.singletonList(testDefender));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Ensure that the attacker list is populated
		assertThat(playerAttackView.getAttackerListModel().getSize()).isEqualTo(1);

		// Ensure that no attacker is selected
		assertThat(window.list("attackerList").selection()).isEmpty();

		// Select the defender
		window.list("defenderList").selectItem(0);

		// Manually enable the attack button for testing (if necessary)
		GuiActionRunner.execute(() -> {
			playerAttackView.getBtnAttack().setEnabled(true);
		});

		// Attempt to perform the attack
		window.button(JButtonMatcher.withText("Attack")).click();

		// Verify that the attack method was not called
		verify(mockGameController, times(0)).attack(any(), any());

		// Verify that the correct error message is displayed
		window.label("errorLabel").requireText("Attacker and defender must be selected.");
	}

	@Test
	@GUITest
	public void testAttackFailsWhenDefenderNotSelected() {
		// Set up the map and attacker
		GameMap testMap = new GameMap(1L, "TestMap");
		Player testAttacker = new Player("Attacker", 100, 20, true);
		testAttacker.setMap(testMap);

		// Mock the GameController behavior
		when(mockGameController.getAllMaps()).thenReturn(Collections.singletonList(testMap));
		when(mockGameController.getPlayersFromMap(testMap.getId())).thenReturn(Collections.singletonList(testAttacker));

		// Inject mock controller and set up the view
		GuiActionRunner.execute(() -> {
			playerAttackView.setGameController(mockGameController);
			playerAttackView.getMapListModel().addElement(testMap);
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Ensure that the attacker list is populated
		assertThat(playerAttackView.getAttackerListModel().getSize()).isEqualTo(1);

		// Simulate selecting an attacker programmatically
		GuiActionRunner.execute(() -> {
			playerAttackView.getAttackerList().setSelectedIndex(0); // Select the attacker
		});

		// Do not select any defender (defenderList remains unselected)

		// Directly invoke the method to bypass the disabled button
		GuiActionRunner.execute(() -> {
			playerAttackView.attackSelectedPlayers();
		});

		// Verify the error message
		JLabel errorLabel = window.label("errorLabel").target();
		assertThat(errorLabel.getText()).isEqualTo("Attacker and defender must be selected.");

		// Verify the warning log is generated (optionally, use LogCaptor or verify
		// logger)
	}

}
