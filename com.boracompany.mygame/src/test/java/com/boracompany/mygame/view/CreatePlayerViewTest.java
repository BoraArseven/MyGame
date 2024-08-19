package com.boracompany.mygame.view;



import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JLabelMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
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

	@Test @GUITest
	public void testControlsInitialStates() {
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
	public void testInitialSetup() {

	}


}