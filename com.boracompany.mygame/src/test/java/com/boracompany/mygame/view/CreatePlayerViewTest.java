package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

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

import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;

@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class CreatePlayerViewTest extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private CreatePlayerView createPlayerView;

	@Override
	protected void onSetUp() {
		// Create the GUI on the Event Dispatch Thread
		createPlayerView = GuiActionRunner.execute(() -> new CreatePlayerView());
		window = new FrameFixture(robot(), createPlayerView); // Pass robot to FrameFixture
		window.show(); // shows the frame to test
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
	        System.out.println("Player added: " + addedPlayer.getName());  // Debug statement
	        System.out.println("List player model size: " + createPlayerView.getListPlayersModel().getSize());  // Debug
	    });

	    // Now ensure that the list contains the added player before trying to select it
	    window.list("ListPlayers").requireItemCount(1);  // Ensures there is one item in the list
	    
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
	    assertThat(listContents)
	        .containsExactly(player1.toString(), player2.toString());
	}
}