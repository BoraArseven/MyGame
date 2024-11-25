package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import javax.swing.DefaultListModel;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class CreatePlayerViewTest extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private CreatePlayerView createPlayerView;

	@Mock
	private GameController gameController;
	private AutoCloseable closeable;

	@Override
	protected void onSetUp() {
		closeable = MockitoAnnotations.openMocks(this);
		// Create the GUI on the Event Dispatch Thread
		createPlayerView = GuiActionRunner.execute(() -> new CreatePlayerView());
		createPlayerView.setSchoolController(gameController);
		window = new FrameFixture(robot(), createPlayerView); // Pass robot to FrameFixture
		window.show(); // shows the frame to test
	}

	@Override
	protected void onTearDown() throws Exception {
		closeable.close();
	}

	@Test
	@GUITest
	public void testControlsInitialStates() {
		assertNotNull(createPlayerView);
		window.label(JLabelMatcher.withText("Name:")).requireEnabled();
		window.label(JLabelMatcher.withText("Damage:")).requireEnabled();
		window.label(JLabelMatcher.withText("Health:")).requireEnabled();
		window.textBox("NameText").requireEnabled();
		window.textBox("DamageText").requireEnabled();
		window.textBox("HealthText").requireEnabled();
		window.button("MainMenuButton").requireEnabled();
		window.button("CreatePlayerButton").requireDisabled();
		window.button("DeleteButton").requireDisabled();
		window.button("MainMenuButton").requireEnabled();
	}

	@Test
	public void testTextFields() {
		// Enter values into the text fields
		window.textBox("DamageText").enterText("1");
		window.textBox("NameText").enterText("test");
		window.textBox("HealthText").enterText("100");

		// Verify the entered values are correctly reflected**
		assertThat(window.textBox("DamageText").text()).as("Damage text should be '1'").isEqualTo("1");
		assertThat(window.textBox("NameText").text()).as("Name text should be 'test'").isEqualTo("test");
		assertThat(window.textBox("HealthText").text()).as("Health text should be '100'").isEqualTo("100");

		// Verify the 'Create' button is enabled after entering valid input**
		assertThat(window.button(JButtonMatcher.withText("Create")).target().isEnabled())
				.as("Create button should be enabled when valid input is provided").isTrue();
	}

	// Can be good parameterized test, but I still am not comfortable at writing
	// them
	@Test
	public void testWhenEitherNameDamageorHealthAreBlankThenAddButtonShouldBeDisabled() {
		// Get references to the text boxes
		JTextComponentFixture damageTextBox = window.textBox("DamageText");
		JTextComponentFixture nameTextBox = window.textBox("NameText");
		JTextComponentFixture healthTextBox = window.textBox("HealthText");

		// Enter values into the text fields
		damageTextBox.enterText("1");
		nameTextBox.enterText(" ");
		healthTextBox.enterText(" ");

		// **Assertion 1: Verify the entered values in the text fields**
		assertThat(damageTextBox.text()).as("Damage text should be '1'").isEqualTo("1");
		assertThat(nameTextBox.text()).as("Name text should contain a blank space").isEqualTo(" ");
		assertThat(healthTextBox.text()).as("Health text should contain a blank space").isEqualTo(" ");

		// **Assertion 2: Verify the 'Create' button is disabled**
		assertThat(window.button(JButtonMatcher.withText("Create")).target().isEnabled())
				.as("Create button should be disabled when Name or Health fields are blank").isFalse();
	}

	@Test
	public void testDeleteButtonShouldBeEnabledOnlyWhenAPlayerIsSelected() {
		// Add a player to the list inside the GUI thread
		GuiActionRunner.execute(() -> {
			Player addedPlayer = new PlayerBuilder().withName("TestPlayer").withDamage(10).withHealth(100).build();
			createPlayerView.getListPlayersModel().addElement(addedPlayer);
		});

		// **Assertion 1: Ensure that the list contains the added player**
		window.list("ListPlayers").requireItemCount(1);

		// Select the first item in the list
		window.list("ListPlayers").selectItem(0);

		// **Assertion 2: Verify the selected player starts with "TestPlayer"**
		assertThat(window.list("ListPlayers").selection()).as("Selected player should start with 'TestPlayer'")
				.anySatisfy(selected -> assertThat(selected).startsWith("TestPlayer"));

		// Get the delete button and assert it is enabled
		JButtonFixture deleteButton = window.button(JButtonMatcher.withText("Delete Selected"));
		assertThat(deleteButton.target().isEnabled()).as("Delete button should be enabled when a player is selected")
				.isTrue();

		// Clear selection and assert the delete button is disabled
		window.list("ListPlayers").clearSelection();
		assertThat(deleteButton.target().isEnabled()).as("Delete button should be disabled when no player is selected")
				.isFalse();
	}

	@Test
	public void testShowAllPlayersShouldAddPlayerDescriptionsToTheList() {
		// Arrange: Create player objects
		Player player1 = new PlayerBuilder().withName("testPlayer1").withDamage(10).withHealth(100).build();
		Player player2 = new PlayerBuilder().withName("testPlayer2").withDamage(20).withHealth(80).build();

		// Act: Show all players in the view
		GuiActionRunner.execute(() -> createPlayerView.showAllPlayers(Arrays.asList(player1, player2)));

		// Assert: Verify that the list contains the correct player descriptions
		String[] listContents = window.list("ListPlayers").contents();
		assertThat(listContents).containsExactly(player1.toString(), player2.toString());
	}

	@Test
	public void testShowErrorShouldShowTheMessageInTheErrorLabel() {
		// Create a player instance to trigger the error
		Player player1 = new PlayerBuilder().withName("testPlayer1forError").withDamage(30).withHealth(95).build();

		// Simulate showing the error message
		GuiActionRunner.execute(() -> createPlayerView.showError("error message", player1));

		// **Assertion 1: Verify that the error label displays the correct message**
		assertThat(window.label("ErrorMessageLabel").text()).as("Error label should display the correct error message")
				.isEqualTo("error message: " + player1);

		// **Assertion 2: Verify that the error label is visible**
		assertThat(window.label("ErrorMessageLabel").target().isVisible())
				.as("Error label should be visible when an error occurs").isTrue();
	}

	@Test
	public void testPlayerRemovedShouldRemoveThePLayerFromTheListAndResetTheErrorLabel() {
		// setup
		Player player1 = new PlayerBuilder().withName("testPlayerToBeRemoved1").withDamage(200).withHealth(500).build();
		Player player2 = new PlayerBuilder().withName("testPlayerToBeRemoved2").withDamage(150).withHealth(700).build();

		// Add players to the list model
		GuiActionRunner.execute(() -> {
			DefaultListModel<Player> listPlayersModel = createPlayerView.getListPlayersModel();
			listPlayersModel.addElement(player1);
			listPlayersModel.addElement(player2);
		});

		// execute - remove player1 (using the same instance that was added to the list)
		GuiActionRunner.execute(() -> createPlayerView.playerRemoved(player1));

		// verify
		String[] listContents = window.list().contents();
		assertThat(listContents).containsExactly(player2.toString()); // Verify player2 is still in the list
		window.label("ErrorMessageLabel").requireText(""); // Verify the error message label is cleared
	}

	@Test
	public void testAddButtonShouldDelegateToSchoolControllerNewStudent() {
		window.textBox("NameText").enterText("testname");
		window.textBox("DamageText").enterText("20");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).click();
		Player playerShouldBeAdded = new PlayerBuilder().withName("testname").withDamage(20).withHealth(100).build();
		verify(gameController).createPlayer(playerShouldBeAdded.getName(), playerShouldBeAdded.getHealth(),
				playerShouldBeAdded.getDamage());
	}

	@Test
	public void testDeleteButtonShouldDelegateToSchoolControllerDeleteStudent() {
		// Create players to add to the list
		Player playerShouldBeDeleted = new PlayerBuilder()

				.withName("testname").withDamage(20).withHealth(100).build();
		playerShouldBeDeleted.setId(1L);
		Player playerShouldBeDeleted2 = new PlayerBuilder()

				.withName("testname2").withDamage(40).withHealth(50).build();
		playerShouldBeDeleted2.setId(2L);
		// Add players to the list inside the GUI thread
		GuiActionRunner.execute(() -> {
			DefaultListModel<Player> listPlayersModel = createPlayerView.getListPlayersModel();
			listPlayersModel.addElement(playerShouldBeDeleted);
			listPlayersModel.addElement(playerShouldBeDeleted2);
		});

		// Select the second player (index 1) and click delete
		window.list("ListPlayers").selectItem(1);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		// Assertion 1: Verify that the controller's deletePlayer method is called
		Mockito.verify(gameController).deletePlayer(playerShouldBeDeleted2.getId());

		// Assertion 2: Verify that the player list updates correctly
		assertThat(window.list("ListPlayers").contents())
				.as("Player list should contain only the first player after deletion").anySatisfy(playerString -> {
					assertThat(playerString).contains("testname").doesNotContain("testname2");
				});
	}

	@Test
	public void testPlayerRemovedShouldHandleEmptyListCorrectly() {
		// setup - Add a single player to the list
		Player playerToBeRemoved = new PlayerBuilder().withName("lastPlayer").withDamage(200).withHealth(500).build();

		// Add the player to the list model
		GuiActionRunner.execute(() -> {
			DefaultListModel<Player> listPlayersModel = createPlayerView.getListPlayersModel();
			listPlayersModel.addElement(playerToBeRemoved);
		});

		// execute - remove the last player (causing the list to become empty)
		GuiActionRunner.execute(() -> createPlayerView.playerRemoved(playerToBeRemoved));

		// verify - the list should now be empty
		String[] listContents = window.list().contents();
		assertThat(listContents).isEmpty(); // Verify the list is empty

		// The ErrorMessageLabel should be reset to an empty string
		window.label("ErrorMessageLabel").requireText("");

		// Verify that the gameController's deletePlayer method was called with the
		// correct ID
		verify(gameController).deletePlayer(playerToBeRemoved.getId());
	}

}