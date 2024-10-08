package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import javax.swing.DefaultListModel;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class AddPlayersToMapsViewTest extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private AddPlayersToMapsView addPlayersToMaps;

	@Mock
	private GameController mockGameController;

	@Override
	protected void onSetUp() {

		MockitoAnnotations.openMocks(this);
		addPlayersToMaps = GuiActionRunner.execute(() -> new AddPlayersToMapsView());
		mockGameController = Mockito.mock(GameController.class);
		addPlayersToMaps.setGameController(mockGameController);
		window = new FrameFixture(robot(), addPlayersToMaps);
		window.show(); // shows the frame to test
	}

	@Test
	@GUITest
	public void testButtonEnabledWhenMapAndPlayerSelected() {
		// Set up models for lists
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("Player1").withHealth(100).withDamage(50).build());
		});

		// Select a map and a player, and assert that the button is enabled
		window.list("mapList").selectItem(0); // Corrected the name to "mapList"
		window.list("playerList").selectItem(0); // Corrected the name to "playerList"
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).requireEnabled(); // Should now be enabled
	}

	@Test
	@GUITest
	public void testButtonDisabledWhenOnlyOneSelectionMade() {
		// Set up models for lists
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("Player1").withHealth(100).withDamage(50).build());
		});

		// Select a map but no player, and assert that the button remains disabled
		window.list("mapList").selectItem(0); // Corrected the name to "mapList"
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).requireDisabled(); // Button should still
																								// be disabled

		// Deselect the map and select a player, button should remain disabled
		window.list("mapList").clearSelection(); // Corrected the name to "mapList"
		window.list("playerList").selectItem(0); // Corrected the name to "playerList"
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).requireDisabled(); // Button should still
																								// be disabled
	}

	@Test
	@GUITest
	public void testButtonReDisabledAfterDeselecting() {
		// Set up models for lists
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("Player1").withHealth(100).withDamage(50).build());
		});

		// Select a map and a player, and assert that the button is enabled
		window.list("mapList").selectItem(0); // Corrected the name to "mapList"
		window.list("playerList").selectItem(0); // Corrected the name to "playerList"
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).requireEnabled(); // Should now be enabled

		// Deselect player and assert that the button is disabled again
		window.list("playerList").clearSelection(); // Corrected the name to "playerList"
		window.list("mapList").clearSelection(); // Corrected the name to "mapList"
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).requireDisabled(); // Should be disabled
	}

	@Test
	@GUITest
	public void testErrorMessageLabelShouldBeInitiallyEmpty() {
		// Assert that the error label is initially empty
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testSelectingItemsInBothListsShouldRetainSelections() {
		// Set up models for lists
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("Player1").withHealth(100).withDamage(50).build());
		});

		// Select a map and a player
		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);

		// Retrieve selected indices using the target JList method inside
		// GuiActionRunner
		int selectedMapIndex = GuiActionRunner.execute(() -> window.list("mapList").target().getSelectedIndex());
		int selectedPlayerIndex = GuiActionRunner.execute(() -> window.list("playerList").target().getSelectedIndex());

		// Verify the selected indices
		assertThat(selectedMapIndex).isEqualTo(0);
		assertThat(selectedPlayerIndex).isEqualTo(0);
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMap() {
		// Set up the test data
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Select a map and player
		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);

		// Click the button to add the player to the map
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's addPlayerToMap method was called with the correct
		// arguments
		verify(mockGameController).addPlayerToMap(1L, new PlayerBuilder().withName("TestPlayer").build());
	}

	@Test
	@GUITest
	public void testButtonShouldNotBeClickableWhenBothItemsIsNotSelecteds() {
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));
		});

		// Clear any selection from the player list to simulate the error condition
		window.list("mapList").selectItem(0); // Only map is selected
		window.list("playerList").clearSelection(); // Player is not selected

		// Click the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Check the error message in the error label
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapSuccess() {
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		verify(mockGameController).addPlayerToMap(1L, new PlayerBuilder().withName("TestPlayer").build());
		window.label("errorLabel").requireText(""); // No error should be shown
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapErrorHandling() {
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		doThrow(new IllegalStateException("Test Exception")).when(mockGameController).addPlayerToMap(anyLong(),
				any(Player.class));

		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		window.label("errorLabel").requireText("Failed to add player to map: TestMap");
	}

	@Test
	@GUITest
	public void testAddPlayerToMapNotTriggeredWhenMapOrPlayerNotSelected() {
		// Set up the test data
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap("Map1"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("Player1").build());
		});

		// Case 1: Map is selected, Player is not selected
		window.list("mapList").selectItem(0);
		window.list("playerList").clearSelection(); // No player selected

		// Try clicking the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Case 2: Player is selected, Map is not selected
		window.list("mapList").clearSelection(); // No map selected
		window.list("playerList").selectItem(0);

		// Try clicking the button again
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Case 3: Neither Map nor Player is selected
		window.list("mapList").clearSelection();
		window.list("playerList").clearSelection();

		// Try clicking the button again
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapWhenMapAndPlayerAreNull() {
		// Set up the test data with empty models (i.e., no selection available)
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			mapListModel.clear();
			playerListModel.clear();
		});

		// Simulate no selection made in both lists
		window.list("mapList").clearSelection();
		window.list("playerList").clearSelection();

		// Try clicking the button, which should be disabled or should not trigger the
		// action
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called since both map and player are
		// null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Check that the error label remains empty because there was no action
		// attempted
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapWhenBothMapAndPlayerAreNotNull() {
		// Set up the test data with a map and player both selected
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Select both a map and player (both are not null)
		window.list("mapList").selectItem(0); // Select map
		window.list("playerList").selectItem(0); // Select player

		// Click the button to add the player to the map
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's addPlayerToMap method was called with the correct
		// arguments
		verify(mockGameController).addPlayerToMap(1L, new PlayerBuilder().withName("TestPlayer").build());

		// Check that the error label remains empty because the operation was successful
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapWhenMapIsNotNullAndPlayerIsNull() {
		// Set up the test data with a map selected but no player selected
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.clear();
		});

		// Select a map but no player (map is not null, player is null)
		window.list("mapList").selectItem(0); // Select map
		window.list("playerList").clearSelection(); // No player selected

		// Try clicking the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called because selectedPlayer is null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Check that the error label remains empty because there was no action
		// attempted
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapWhenMapIsNullAndPlayerIsNotNull() {
		// Set up the test data with no map selected but a player is available and
		// selected
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.clear();

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// No map selected, but player is selected (map is null, player is not null)
		window.list("mapList").clearSelection(); // No map selected
		window.list("playerList").selectItem(0); // Select player

		// Try clicking the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called because selectedMap is null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Check that the error label remains empty because there was no action
		// attempted
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddSelectedPlayerToMapWhenBothMapAndPlayerAreNull() {
		// Set up the test data with no map or player selected
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.clear();

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.clear();
		});

		// No map selected, no player selected (both map and player are null)
		window.list("mapList").clearSelection(); // No map selected
		window.list("playerList").clearSelection(); // No player selected

		// Try clicking the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the controller's method was NOT called because both selectedMap and
		// selectedPlayer are null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Check that the error label remains empty because there was no action
		// attempted
		window.label("errorLabel").requireText("");
	}

	@Test
	public void testAddSelectedPlayerToMapWhenBothSelectedMapAndPlayerAreNotNull() {
		// Set up the test data in the list models
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Simulate selecting a map and a player
		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);

		// Call the method directly to bypass UI event triggers
		GuiActionRunner.execute(() -> addPlayersToMaps.addSelectedPlayerToMap());

		// Verify the controller's addPlayerToMap method was called with the correct
		// arguments
		verify(mockGameController).addPlayerToMap(1L, new PlayerBuilder().withName("TestPlayer").build());
	}

	@Test
	public void testAddSelectedPlayerToMapWhenSelectedMapIsNotNullAndSelectedPlayerIsNull() {
		// Set up the test data in the list models
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Simulate selecting only a map (and no player)
		window.list("mapList").selectItem(0);
		window.list("playerList").clearSelection();

		// Call the method directly to bypass UI event triggers
		GuiActionRunner.execute(() -> addPlayersToMaps.addSelectedPlayerToMap());

		// Verify the controller's method was NOT called because selectedPlayer is null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));
	}

	@Test
	public void testAddSelectedPlayerToMapWhenSelectedMapIsNullAndSelectedPlayerIsNotNull() {
		// Set up the test data in the list models
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Simulate selecting only a player (and no map)
		window.list("mapList").clearSelection();
		window.list("playerList").selectItem(0);

		// Call the method directly to bypass UI event triggers
		GuiActionRunner.execute(() -> addPlayersToMaps.addSelectedPlayerToMap());

		// Verify the controller's method was NOT called because selectedMap is null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));
	}

	@Test
	public void testAddSelectedPlayerToMapWhenBothSelectedMapAndPlayerAreNull() {
		// Set up the test data in the list models
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMaps.getMapListModel();
			mapListModel.addElement(new GameMap(1L, "TestMap"));

			DefaultListModel<Player> playerListModel = addPlayersToMaps.getPlayerListModel();
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Simulate selecting neither map nor player
		window.list("mapList").clearSelection();
		window.list("playerList").clearSelection();

		// Call the method directly to bypass UI event triggers
		GuiActionRunner.execute(() -> addPlayersToMaps.addSelectedPlayerToMap());

		// Verify the controller's method was NOT called because both are null
		verify(mockGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));
	}

	@Test
	public void testRefreshMapListHandlesException() {
		// Simulate the gameController throwing an exception when getAllMaps() is called
		doThrow(new RuntimeException("Test Exception")).when(mockGameController).getAllMaps();

		// Call the method
		GuiActionRunner.execute(() -> addPlayersToMaps.refreshMapList());

		// Verify that the error was logged and handled properly (you may need to verify
		// via log messages if logger is mocked)
		// Since we don't have access to the logger output directly in the test, we
		// assume the exception is caught
		// and doesn't crash the program. You can verify it via mock loggers if they are
		// available.

		// You can verify the list remains cleared after the exception
		assertThat(addPlayersToMaps.getMapListModel().getSize()).isEqualTo(0); // The map list should be empty
	}

	@Test
	public void testRefreshPlayerListHandlesException() {
		// Simulate the gameController throwing an exception when getAllPlayers() is
		// called
		doThrow(new RuntimeException("Test Exception")).when(mockGameController).getAllPlayers();

		// Call the method
		GuiActionRunner.execute(() -> addPlayersToMaps.refreshPlayerList());

		// Verify that the error was logged and handled properly (you may need to verify
		// via log messages if logger is mocked)
		// Again, we assume that the exception is caught and does not crash the program.

		// You can verify the list remains cleared after the exception
		assertThat(addPlayersToMaps.getPlayerListModel().getSize()).isEqualTo(0); // The player list should be empty
	}

}
