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
		window.textBox("DamageText").enterText("1");
		window.textBox("NameText").enterText("test");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).requireEnabled();
	}

	// Can be good parameterized test, but I still am not comfortable at writing
	// them
	@Test
	public void testWhenEitherNameDamageorHealthAreBlankThenAddButtonShouldBeDisabled() {
		JTextComponentFixture damageTextBox = window.textBox("DamageText");
		JTextComponentFixture nameTextBox = window.textBox("NameText");
		JTextComponentFixture healthTextBox = window.textBox("HealthText");

		damageTextBox.enterText("1");
		nameTextBox.enterText(" ");
		healthTextBox.enterText(" ");
		window.button(JButtonMatcher.withText("Create")).requireDisabled();

	}

	@Test
	public void testDeleteButtonShouldBeEnabledOnlyWhenAPlayerIsSelected() {
		// Add a player to the list inside the GUI thread
		GuiActionRunner.execute(() -> {
			Player addedPlayer = new PlayerBuilder().withName("TestPlayer").withDamage(10).withHealth(100).build();
			createPlayerView.getListPlayersModel().addElement(addedPlayer);
			System.out.println("Player added: " + addedPlayer.getName()); // Debug statement
			System.out.println("List player model size: " + createPlayerView.getListPlayersModel().getSize()); // Debug
		});

		// Now ensure that the list contains the added player before trying to select it
		window.list("ListPlayers").requireItemCount(1); // Ensures there is one item in the list

		// Select the first item in the list
		window.list("ListPlayers").selectItem(0);

		// Check if the delete button is enabled when a player is selected
		JButtonFixture deleteButton = window.button(JButtonMatcher.withText("Delete Selected"));
		deleteButton.requireEnabled();

		// Clear selection and check if the delete button is disabled again
		window.list("ListPlayers").clearSelection();
		deleteButton.requireDisabled();
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
		Player player1 = new PlayerBuilder().withName("testPlayer1forError").withDamage(30).withHealth(95).build();
		GuiActionRunner.execute(() -> createPlayerView.showError("error message", player1));
		window.label("ErrorMessageLabel").requireText("error message: " + player1);
	}

	@Test
	public void testStudentAddedShouldAddTheStudentToTheListAndResetTheErrorLabel() {
		Player player1 = new PlayerBuilder().withName("testPlayerToBeAdded").withDamage(250).withHealth(600).build();
		GuiActionRunner.execute(() -> createPlayerView.playerAdded(player1));
		String[] listContents = window.list().contents();
		assertThat(listContents).containsExactly(player1.toString());
		window.label("ErrorMessageLabel").requireText("");
	}

	@Test
	public void testStudentRemovedShouldRemoveTheStudentFromTheListAndResetTheErrorLabel() {
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
		Player playerShouldBeDeleted = new PlayerBuilder().withName("testname").withDamage(20).withHealth(100).build();
		Player playerShouldBeDeleted2 = new PlayerBuilder().withName("testname2").withDamage(40).withHealth(50).build();
		GuiActionRunner.execute(() -> {
			DefaultListModel<Player> listStudentsModel = createPlayerView.getListPlayersModel();
			listStudentsModel.addElement(playerShouldBeDeleted);
			listStudentsModel.addElement(playerShouldBeDeleted2);
		});
		window.list("ListPlayers").selectItem(1);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

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