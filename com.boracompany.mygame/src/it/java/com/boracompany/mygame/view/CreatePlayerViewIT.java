package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.boracompany.mygame.controller.GameController;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.PlayerDAOIMPL;
import com.boracompany.mygame.orm.HibernateUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class) // Use GUITestRunner for JUnit 4
public class CreatePlayerViewIT extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private CreatePlayerView createPlayerView;
	private GameController gameController;
	private static EntityManagerFactory emf;
	private AutoCloseable closeable;

	private static final Logger logger = LogManager.getLogger(CreatePlayerViewIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
	}

	@BeforeClass
	public static void setUpContainerAndEntityManagerFactory() {
		postgreSQLContainer.start();

		// Initialize Hibernate with the Testcontainer's JDBC URL
		String jdbcUrl = postgreSQLContainer.getJdbcUrl();
		String username = postgreSQLContainer.getUsername();
		String password = postgreSQLContainer.getPassword();
		HibernateUtil.initialize(jdbcUrl, username, password);

		// Create the EntityManagerFactory only once
		emf = HibernateUtil.getEntityManagerFactory();
	}

	@AfterClass
	public static void tearDownContainerAndEntityManagerFactory() {
		// Close EntityManagerFactory only once
		if (emf != null) {
			emf.close();
		}
		HibernateUtil.close();
		postgreSQLContainer.stop();
	}

	@Before
	public void onSetUp() {
		closeable = MockitoAnnotations.openMocks(this);

		// Initialize the GameController with real DAOs
		PlayerDAOIMPL playerDAO = new PlayerDAOIMPL(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emf);
		gameController = new GameController(playerDAO, gameMapDAO, logger);

		// Initialize the CreatePlayerView on the EDT
		createPlayerView = GuiActionRunner.execute(() -> {
			CreatePlayerView view = new CreatePlayerView();
			view.setSchoolController(gameController);
			return view;
		});

		// Clean the database before each test
		resetDatabase();

		// Initialize FrameFixture for testing the GUI
		window = new FrameFixture(robot(), createPlayerView);
		window.show();
	}
	@Test
    @GUITest
    public void testDeletePlayer_FailureDuringDeletion_ShowsErrorMessage() {
        // Arrange: Create a player via the controller to ensure it has an ID
        final Player[] playerHolder = new Player[1];

        // Create a spy of the GameController
        GameController spyGameController = Mockito.spy(gameController);

        // Inject the spy into the view
        GuiActionRunner.execute(() -> {
            createPlayerView.setSchoolController(spyGameController);
            Player p = spyGameController.createPlayer("Player1", 100, 10);
            createPlayerView.playerAdded(p); // Add the player with ID to the view
            playerHolder[0] = p; // Assign the player to the array
        });

        Player player = playerHolder[0]; // Retrieve the player after the lambda execution

        // Ensure player ID is not null
        assertNotNull(player.getId(), "Player ID should not be null");

        // Simulate an exception during deletion using anyLong()
        doThrow(new RuntimeException("Simulated deletion failure"))
            .when(spyGameController)
            .deletePlayer(Mockito.anyLong());

        // Act: Try to delete the player from the UI
        window.list("ListPlayers").selectItem(0);
        window.button(JButtonMatcher.withText("Delete Selected")).click();

        // Assert: Verify that the error message is displayed
        assertThat(window.label("ErrorMessageLabel").text())
                .contains("Failed to remove player from the database: " + player.getName());

        // The player should still be in the list since deletion failed
        assertThat(window.list("ListPlayers").contents()).containsExactly(player.toString());

        // The player should still be in the database
        EntityManager em = emf.createEntityManager();
        try {
            List<Player> remainingPlayers = em.createQuery("SELECT p FROM Player p WHERE p.id = :id", Player.class)
                    .setParameter("id", player.getId()).getResultList();
            assertThat(remainingPlayers).isNotEmpty(); // Player should still exist in the database
        } finally {
            em.close();
        }
    }
	private void resetDatabase() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();
			em.createQuery("DELETE FROM Player").executeUpdate();
			em.createQuery("DELETE FROM GameMap").executeUpdate();
			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw new PersistenceException("Failed to reset database", e);
		} finally {
			em.close();
		}
	}

	

	@Test
	@GUITest
	public void testShowAllPlayers() {
		// Arrange: Create players
		Player player1 = new PlayerBuilder().withName("Player1").withDamage(10).withHealth(100).build();
		Player player2 = new PlayerBuilder().withName("Player2").withDamage(20).withHealth(200).build();

		// Act: Show all players
		GuiActionRunner.execute(() -> createPlayerView.showAllPlayers(Arrays.asList(player1, player2)));

		// Assert: Players are displayed in the list
		assertThat(window.list("ListPlayers").contents()).containsExactly(player1.toString(), player2.toString());
	}

	@Override
	protected void onTearDown() throws Exception {
		// Close Mockito mocks
		closeable.close();
	}
	@Test
	@GUITest
	public void testCreatePlayerAndDelete() {
		// Act: Add a player via the view
		window.textBox("NameText").enterText("Player1");
		window.textBox("DamageText").enterText("10");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).click();

		// Debug: Print the list contents after the player is created
		logger.debug("List contents after creation: {}", (Object) window.list("ListPlayers").contents());

		// Assert: Player is added to the list
		assertThat(window.list("ListPlayers").contents()).contains("Player1, Health: 100.0, Damage: 10.0");

		// Select the player and delete it
		window.list("ListPlayers").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		// Debug: Print the list contents after the player is deleted
		logger.debug("List contents after deletion: {}", (Object) window.list("ListPlayers").contents());

		// Assert: Player is removed from the list
		assertThat(window.list("ListPlayers").contents()).doesNotContain("Player1");

		// Verify that the player is deleted from the database
		EntityManager em = emf.createEntityManager();
		try {
			List<Player> players = em.createQuery("SELECT p FROM Player p WHERE p.name = :name", Player.class)
					.setParameter("name", "Player1").getResultList();
			assertThat(players).isEmpty(); // Player should not exist in the database
		} finally {
			em.close();
		}
	}
	@Test
	@GUITest
	public void testCreateMultiplePlayers() {
		window.textBox("NameText").enterText("Player1");
		window.textBox("DamageText").enterText("10");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).click();
		window.textBox("NameText").enterText("Player2");
		window.textBox("DamageText").enterText("20");
		window.textBox("HealthText").enterText("200");
		window.button(JButtonMatcher.withText("Create")).click();

		// Debug: Print the list contents after the players are created
		logger.debug("List contents after multiple creations: {}", (Object) window.list("ListPlayers").contents());

		assertThat(window.list("ListPlayers").contents()).containsExactly("Player1, Health: 100.0, Damage: 10.0",
				"Player2, Health: 200.0, Damage: 20.0");

		EntityManager em = emf.createEntityManager();
		try {
			Player dbPlayer1 = em.createQuery("SELECT p FROM Player p WHERE p.name = :name", Player.class)
					.setParameter("name", "Player1").getSingleResult();
			Player dbPlayer2 = em.createQuery("SELECT p FROM Player p WHERE p.name = :name", Player.class)
					.setParameter("name", "Player2").getSingleResult();
			assertThat(dbPlayer1).isNotNull();
			assertThat(dbPlayer2).isNotNull();
			assertThat(dbPlayer1.getDamage()).isEqualTo(10);
			assertThat(dbPlayer2.getDamage()).isEqualTo(20);
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testInvalidInput_NonNumericDamageAndHealth() {

		window.textBox("NameText").enterText("Player1");
		window.textBox("DamageText").enterText("abc");
		window.textBox("HealthText").enterText("xyz");
		window.button(JButtonMatcher.withText("Create")).click();

		assertThat(window.label("ErrorMessageLabel").text()).contains("Failed to create player");
		assertThat(window.list("ListPlayers").contents()).isEmpty();

		EntityManager em = emf.createEntityManager();
		try {
			List<Player> players = em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
			assertThat(players).isEmpty();
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testDeleteNonExistentPlayer() {

		GuiActionRunner.execute(() -> {
			window.button(JButtonMatcher.withText("Delete Selected")).target().setEnabled(true);
		});

		window.button(JButtonMatcher.withText("Delete Selected")).click();

		assertThat(window.label("ErrorMessageLabel").text()).contains("No player selected");

		EntityManager em = emf.createEntityManager();
		try {
			List<Player> players = em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
			assertThat(players).isEmpty(); // No player should exist in the database
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testErrorHandling_PlayerCreationFails() {

		GameController spyGameController = Mockito.spy(gameController);

		// Inject the spy into the view
		GuiActionRunner.execute(() -> {
			createPlayerView.setSchoolController(spyGameController);
		});

		// Simulate a failure during player creation in the controller
		doThrow(new RuntimeException("Simulated failure")).when(spyGameController).createPlayer(anyString(), anyFloat(),
				anyFloat());

		window.textBox("NameText").enterText("Player1");
		window.textBox("DamageText").enterText("10");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).click();

		assertThat(window.label("ErrorMessageLabel").text()).contains("Failed to create player");
		assertThat(window.list("ListPlayers").contents()).isEmpty();

		EntityManager em = emf.createEntityManager();
		try {
			List<Player> players = em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
			assertThat(players).isEmpty();
		} finally {
			em.close();
		}
	}

	@Test
	public void testDeletePlayer_NoLongerExistsInBackend() {
		// Arrange: Create a player via the controller to ensure it has an ID
		final Player[] playerHolder = new Player[1];

		GuiActionRunner.execute(() -> {
			Player p = gameController.createPlayer("Player1", 100, 10);
			createPlayerView.playerAdded(p); // Add the player with ID to the view
			playerHolder[0] = p; // Assign the player to the array
		});

		Player player = playerHolder[0]; // Retrieve the player after the lambda execution

		// Simulate external deletion of the player from the backend
		GuiActionRunner.execute(() -> gameController.deletePlayer(player.getId()));

		// Try to delete the player from the UI after it has already been deleted
		window.list("ListPlayers").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		// Ensure the player is no longer in the database
		EntityManager em = emf.createEntityManager();
		try {
			List<Player> remainingPlayers = em.createQuery("SELECT p FROM Player p WHERE p.id = :id", Player.class)
					.setParameter("id", player.getId()).getResultList();
			assertThat(remainingPlayers).isEmpty(); // Player should not exist in the database
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testCreatePlayerAndFieldsReset() {

		window.textBox("NameText").enterText("Player1");
		window.textBox("DamageText").enterText("10");
		window.textBox("HealthText").enterText("100");
		window.button(JButtonMatcher.withText("Create")).click();

		assertThat(window.textBox("NameText").text()).isEmpty();
		assertThat(window.textBox("DamageText").text()).isEmpty();
		assertThat(window.textBox("HealthText").text()).isEmpty();
	}

	@Test
	public void testPlayerAddedShouldAddThePlayeToTheListAndResetTheErrorLabel() {
		Player player1 = new PlayerBuilder().withName("testPlayerToBeAdded").withDamage(250).withHealth(600).build();
		GuiActionRunner.execute(() -> createPlayerView.playerAdded(player1));
		String[] listContents = window.list().contents();
		assertThat(listContents).containsExactly(player1.toString());
		window.label("ErrorMessageLabel").requireText("");
	}

	@Test
	@GUITest
	public void testRefreshPlayerList_ShouldDisplayAllPlayersFromDatabase() {
		// Arrange: Add players to the database through the controller
		Player player1 = new PlayerBuilder().withName("Player1").withDamage(10).withHealth(100).build();
		Player player2 = new PlayerBuilder().withName("Player2").withDamage(20).withHealth(200).build();

		GuiActionRunner.execute(() -> {
			gameController.createPlayer(player1.getName(), player1.getHealth(), player1.getDamage());
			gameController.createPlayer(player2.getName(), player2.getHealth(), player2.getDamage());
		});

		// Act: Refresh the player list using the method to be tested
		GuiActionRunner.execute(() -> createPlayerView.refreshPlayerList());

		// Assert: Verify that the players from the database are displayed in the list
		String[] listContents = window.list("ListPlayers").contents();
		assertThat(listContents).containsExactly("Player1, Health: 100.0, Damage: 10.0",
				"Player2, Health: 200.0, Damage: 20.0"); // Should display both players
	}

	@Test
	@GUITest
	public void testRefreshPlayerList_ShouldHandleDatabaseErrorGracefully() {
		// Arrange: Spy on the game controller to simulate a failure when fetching
		// players
		GameController spyGameController = Mockito.spy(gameController);

		GuiActionRunner.execute(() -> {
			createPlayerView.setSchoolController(spyGameController);
		});

		doThrow(new RuntimeException("Database error")).when(spyGameController).getAllPlayers();

		// Act: Refresh the player list (this should trigger an error)
		GuiActionRunner.execute(() -> createPlayerView.refreshPlayerList());

		// Assert: Verify that the error message is displayed and the list is not
		// updated
		window.label("ErrorMessageLabel").requireText("Failed to refresh player list");
		assertThat(window.list("ListPlayers").contents()).isEmpty(); // The list should remain empty since an error
																		// occurred
	}

}
