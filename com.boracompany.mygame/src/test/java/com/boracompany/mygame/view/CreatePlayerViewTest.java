package com.boracompany.mygame.view;

import static org.junit.Assert.assertNotNull;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

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
	}

	@Test
	public void testTextFields() {
		window.textBox("DamageText").enterText("1");
		window.textBox("NameText").enterText("test");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).requireEnabled();
	}

	
	// Can be good parameterized test, but I still am not comfortable at writing them
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
	
	
	
	
}