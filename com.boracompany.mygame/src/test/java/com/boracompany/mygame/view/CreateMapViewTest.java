package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
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

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class CreateMapViewTest extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private CreateMapView createMapView;

	@Mock
	private GameController gameController;
	private AutoCloseable closeable;

	@Override
	protected void onSetUp() {
		closeable = MockitoAnnotations.openMocks(this);
		// Create the GUI on the Event Dispatch Thread
		createMapView = GuiActionRunner.execute(() -> new CreateMapView());
		createMapView.setGameController(gameController);
		window = new FrameFixture(robot(), createMapView); // Pass robot to FrameFixture
		window.show(); // shows the frame to test
	}

	@Override
	protected void onTearDown() throws Exception {
		closeable.close();
	}

	@Test
	@GUITest
	public void testControlsInitialStates() {
		assertNotNull(createMapView);
		window.label(JLabelMatcher.withText("Map Name:")).requireEnabled();
		window.textBox("NameText").requireEnabled();
		window.button("MainMenuButton").requireEnabled();
		window.button("CreateMapButton").requireDisabled();
		window.button("DeleteButton").requireDisabled();
	}

	@Test
	public void testTextFields() {
		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).requireEnabled();
	}

	@Test
	public void testWhenNameTextIsBlankThenCreateMapButtonShouldBeDisabled() {
		window.textBox("NameText").enterText(" ");
		window.button(JButtonMatcher.withText("Create Map")).requireDisabled();
	}

	@Test
	public void testDeleteButtonShouldBeEnabledOnlyWhenAMapIsSelected() {
		// Add a map to the list inside the GUI thread
		GuiActionRunner.execute(() -> {
			GameMap addedMap = new GameMap("TestMap");
			createMapView.getListMapsModel().addElement(addedMap);
		});

		// Ensure the list contains the added map
		window.list("ListMaps").requireItemCount(1);

		// Select the map and check if the delete button is enabled
		window.list("ListMaps").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).requireEnabled();

		// Clear selection and check if the delete button is disabled
		window.list("ListMaps").clearSelection();
		window.button(JButtonMatcher.withText("Delete Selected")).requireDisabled();
	}

	@Test
	public void testShowAllMapsShouldAddMapDescriptionsToTheList() {
		// Arrange: Create map objects
		GameMap map1 = new GameMap("TestMap1");
		GameMap map2 = new GameMap("TestMap2");

		// Act: Show all maps in the view
		GuiActionRunner.execute(() -> createMapView.showAllMaps(Arrays.asList(map1, map2)));

		// Assert: Verify that the list contains the correct map descriptions
		String[] listContents = window.list("ListMaps").contents();
		assertThat(listContents).containsExactly(map1.toString(), map2.toString());
	}

	@Test
	public void testShowErrorShouldShowTheMessageInTheErrorLabel() {
		GameMap map = new GameMap("ErrorMap");
		GuiActionRunner.execute(() -> createMapView.showError("Error message", map));
		window.label("ErrorMessageLabel").requireText("Error message: " + map.getName());
	}

	@Test
	public void testMapAddedShouldAddTheMapToTheListAndResetTheErrorLabel() {
		GameMap map = new GameMap("TestMap");
		GuiActionRunner.execute(() -> createMapView.mapAdded(map));
		String[] listContents = window.list().contents();
		assertThat(listContents).containsExactly(map.toString());
		window.label("ErrorMessageLabel").requireText("");
	}

	@Test
	public void testMapRemovedShouldRemoveTheMapFromTheListAndResetTheErrorLabel() {
		// setup
		GameMap map1 = new GameMap("Map1");
		GameMap map2 = new GameMap("Map2");

		// Add maps to the list model
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> listMapsModel = createMapView.getListMapsModel();
			listMapsModel.addElement(map1);
			listMapsModel.addElement(map2);
		});

		// execute - remove map1
		GuiActionRunner.execute(() -> createMapView.mapRemoved(map1));

		// verify
		String[] listContents = window.list().contents();
		assertThat(listContents).containsExactly(map2.toString()); // Verify map2 is still in the list
		window.label("ErrorMessageLabel").requireText(""); // Verify the error message label is cleared
	}

	@Test
	public void testCreateMapButtonShouldDelegateToGameControllerCreateMap() {
		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).click();

		// Create a map object
		GameMap mapShouldBeAdded = new GameMap("TestMap");

		// Verify that the GameController's createMap method is called with correct
		// arguments
		verify(gameController).createMap(mapShouldBeAdded.getName(), mapShouldBeAdded.getPlayers());
	}

	@Test
	public void testDeleteButtonShouldDelegateToGameControllerDeleteMap() {
		GameMap mapShouldBeDeleted = new GameMap("TestMap");

		// Add map to the list model
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> listMapsModel = createMapView.getListMapsModel();
			listMapsModel.addElement(mapShouldBeDeleted);
		});

		// Select the map in the list and click the delete button
		window.list("ListMaps").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		// Verify that the GameController's deleteMap method is called with correct ID
		verify(gameController).deleteMap(mapShouldBeDeleted.getId());
	}

	@Test
	public void testMapRemovedShouldHandleEmptyListCorrectly() {
		// setup - Add a single map to the list
		GameMap mapToBeRemoved = new GameMap("LastMap");

		// Add the map to the list model
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> listMapsModel = createMapView.getListMapsModel();
			listMapsModel.addElement(mapToBeRemoved);
		});

		// execute - remove the last map (causing the list to become empty)
		GuiActionRunner.execute(() -> createMapView.mapRemoved(mapToBeRemoved));

		// verify - the list should now be empty
		String[] listContents = window.list().contents();
		assertThat(listContents).isEmpty(); // Verify the list is empty

		// The ErrorMessageLabel should be reset to an empty string
		window.label("ErrorMessageLabel").requireText("");

		// Verify that the gameController's deleteMap method was called with the correct
		// ID
		verify(gameController).deleteMap(mapToBeRemoved.getId());
	}

	@Test
	@GUITest
	public void testRefreshMapList_ShouldPopulateListModel() {
		// Arrange: Mock the gameController to return a list of maps
		GameMap map1 = new GameMap("Map1");
		GameMap map2 = new GameMap("Map2");
		List<GameMap> maps = Arrays.asList(map1, map2);
		when(gameController.getAllMaps()).thenReturn(maps);

		// Act: Call refreshMapList() to refresh the list of maps
		GuiActionRunner.execute(() -> createMapView.refreshMapList());

		// Assert: Verify that the list model is populated with the correct maps
		String[] listContents = window.list("ListMaps").contents();
		assertThat(listContents).containsExactly(map1.toString(), map2.toString());
	}

	@Test
	@GUITest
	public void testRefreshMapList_ShouldHandleEmptyList() {
		// Arrange: Mock the gameController to return an empty list of maps
		when(gameController.getAllMaps()).thenReturn(Collections.emptyList());

		// Act: Call refreshMapList() to refresh the list of maps
		GuiActionRunner.execute(() -> createMapView.refreshMapList());

		// Assert: Verify that the list model is empty
		String[] listContents = window.list("ListMaps").contents();
		assertThat(listContents).isEmpty();
	}

	@Test
	@GUITest
	public void testRefreshMapList_ShouldShowErrorOnException() {
		// Arrange: Mock the gameController to throw an exception
		doThrow(new RuntimeException("Database error")).when(gameController).getAllMaps();

		// Act: Call refreshMapList() and trigger the exception
		GuiActionRunner.execute(() -> createMapView.refreshMapList());

		// Assert: Verify that the error message is displayed
		window.label("ErrorMessageLabel").requireText("Failed to refresh map list");
	}

	@Test
	@GUITest
	public void testMapAdded_ShouldShowError_WhenExceptionIsThrown() {
		// Arrange: Create a map and mock gameController to throw an exception
		GameMap map = new GameMap("TestMap");
		doThrow(new RuntimeException("Database error")).when(gameController).createMap(map.getName(), map.getPlayers());

		// Act: Call mapAdded() and trigger the exception
		GuiActionRunner.execute(() -> createMapView.mapAdded(map));

		// Assert: Verify that the error message is displayed
		window.label("ErrorMessageLabel").requireText("Failed to create map: " + map.getName());

		// Verify that createMap was called
		verify(gameController).createMap(map.getName(), map.getPlayers());

		// Assert that the map was not added to the list model
		DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
		boolean mapPresentInList = false;
		for (int i = 0; i < listModel.size(); i++) {
			if (listModel.getElementAt(i).equals(map)) {
				mapPresentInList = true;
				break;
			}
		}

		// Assert that the map is not present in the list model
		assertThat(mapPresentInList).isFalse();
	}

	@Test
	@GUITest
	public void testMapRemoved_ShouldShowError_WhenExceptionIsThrown() {
		// Arrange: Create a map and mock gameController to throw an exception
		GameMap map = new GameMap("TestMap");
		map.setId(1L);
		doThrow(new RuntimeException("Database error")).when(gameController).deleteMap(map.getId());

		// Act: Call mapRemoved() and trigger the exception
		GuiActionRunner.execute(() -> createMapView.mapRemoved(map));

		// Assert: Verify that the error message is displayed
		window.label("ErrorMessageLabel").requireText("Failed to remove map from the database: " + map.getName());

		// Verify that deleteMap was called
		verify(gameController).deleteMap(map.getId());

		// Assert that the map was removed from the list model
		DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
		boolean mapPresentInList = false;
		for (int i = 0; i < listModel.size(); i++) {
			if (listModel.getElementAt(i).equals(map)) {
				mapPresentInList = true;
				break;
			}
		}

		// Assert that the map is not present in the list model
		assertThat(mapPresentInList).isFalse();
	}

	@Test
	@GUITest
	public void testCreateMap_ShouldShowError_WhenExceptionIsThrown() {
		// Arrange: Spy on the GameController and force it to throw an exception when
		// createMap is called
		GameController spyGameController = Mockito.spy(gameController);

		// Inject the spy into the CreateMapView
		GuiActionRunner.execute(() -> {
			createMapView.setGameController(spyGameController);
		});

		// Simulate the GameController throwing an exception during map creation
		doThrow(new RuntimeException("Simulated failure")).when(spyGameController).createMap(anyString(), anyList());

		// Act: Try to create a map with valid data
		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).click();

		// Assert: Verify that the error message is displayed and the map is not added
		// to the list
		assertThat(window.label("ErrorMessageLabel").text()).contains("Failed to create map");
		assertThat(window.list("ListMaps").contents()).isEmpty(); // Ensure no map was added
	}
	@Test
	public void testMapAddedWithExceptionHandling() {
	    // Arrange: Set a valid map name
	    window.textBox("NameText").enterText("TestMap");

	    // Mock GameController to throw an exception during map creation to trigger the catch block
	    doThrow(new RuntimeException("Simulated failure")).when(gameController).createMap(anyString(), anyList());

	    // Act: Call mapAdded directly to simulate the map addition process
	    GuiActionRunner.execute(() -> {
	        // Simulate map addition in the GUI, which should trigger the catch block
	        createMapView.mapAdded(new GameMap("TestMap"));
	    });

	    // Assert: Verify that the error message now includes the name of the map being created
	    window.label("ErrorMessageLabel").requireText("Failed to create map: TestMap");

	    // Verify that the map was not added to the list model due to the exception
	    DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
	    assertThat(listModel.getSize()).isEqualTo(0);  // No map should be present in the list
	}
	@Test
	public void testCreateMap_EmptyNameText_ShouldShowError() {
	    // Arrange: Set nameText to an empty string
	    GuiActionRunner.execute(() -> {
	        createMapView.getNameText().setText("");  // Set empty input programmatically
	        createMapView.getCreateMapButton().setEnabled(true);  // Enable button to simulate click even if input is empty
	    });

	    // Act: Simulate button click to try creating a map with an empty name
	    GuiActionRunner.execute(() -> createMapView.getCreateMapButton().doClick());

	    // Assert: Verify that the error message is displayed properly
	    window.label("ErrorMessageLabel").requireText("Failed to create map: ");

	    // Verify the map was NOT added to the list
	    DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
	    assertThat(listModel.getSize()).isEqualTo(0);  // No map should be present in the list
	}


	@Test
	public void testCreateMap_ExceptionDuringMapCreation_ShouldShowError() {
	    // Arrange: Set a valid map name
	    window.textBox("NameText").enterText("TestMap");

	    // Mock the GameMap creation to throw an exception
	    doThrow(new RuntimeException("Simulated failure during map creation"))
	        .when(gameController).createMap(anyString(), anyList());

	    // Act: Click the Create Map button
	    window.button("CreateMapButton").click();

	    // Assert: Verify that the error message is displayed and includes the map name
	    window.label("ErrorMessageLabel").requireText("Failed to create map: TestMap");

	    // Verify the map was NOT added to the list
	    DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
	    assertThat(listModel.getSize()).isEqualTo(0);  // No map should be present in the list
	}
	@Test
	public void testShowErrorOnUIUpdateFailure() {
	    // Simulate an error message without involving the create map process
	    GuiActionRunner.execute(() -> createMapView.showError("Error updating UI", null));

	    // Assert: Verify that the error message is displayed properly
	    window.label("ErrorMessageLabel").requireText("Error updating UI");
	}
	@Test
	public void testCreateMap_ExceptionBeforeNameText_ShouldShowError() {
	    // Arrange: Set a valid map name
	    window.textBox("NameText").enterText("TestMap");

	    // Mock an exception during GameMap creation (e.g., failure before fetching map name)
	    doThrow(new RuntimeException("Simulated failure before nameText.getText()"))
	        .when(gameController).createMap(anyString(), anyList());

	    // Act: Click the Create Map button
	    window.button("CreateMapButton").click();

	    // Assert: Verify that the error message is displayed even when exception happens before fetching nameText
	    window.label("ErrorMessageLabel").requireText("Failed to create map: TestMap");

	    // Verify the map was NOT added to the list
	    DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
	    assertThat(listModel.getSize()).isEqualTo(0);  // No map should be present in the list
	}
	@Test
	public void testDeleteMap_NoMapSelected_ShouldShowError() {
	    // Arrange: Ensure no map is selected in the list
	    GuiActionRunner.execute(() -> {
	        createMapView.getListMapsModel().clear(); // Make sure the list is empty
	        createMapView.getDeleteButton().setEnabled(true); // Force enable the delete button for testing
	    });

	    // Act: Try to click the delete button when no map is selected
	    GuiActionRunner.execute(() -> createMapView.getDeleteButton().doClick());

	    // Assert: Verify that the error message "No map selected" is displayed
	    window.label("ErrorMessageLabel").requireText("No map selected");

	    // Verify that the map list remains empty
	    DefaultListModel<GameMap> listModel = createMapView.getListMapsModel();
	    assertThat(listModel.getSize()).isEqualTo(0);  // No map should be present in the list
	}


}
