package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Before;
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
	public void testInitialSetup() {

	}


}