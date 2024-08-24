package com.boracompany.mygame.view;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.swing.DefaultListModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class) // Use GUITestRunner for JUnit 4
public class AddPlayersToMapsViewIT extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private AddPlayersToMapsView addPlayersToMapsView;
	private GameController spiedGameController;
	private static EntityManagerFactory emf;
	private AutoCloseable closeable;

	private static final Logger logger = LogManager.getLogger(AddPlayersToMapsViewIT.class);

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
		spiedGameController = Mockito.spy(new GameController(playerDAO, gameMapDAO, logger));

		// Initialize the AddPlayersToMapsView on the EDT
		addPlayersToMapsView = GuiActionRunner.execute(() -> {
			AddPlayersToMapsView view = new AddPlayersToMapsView();
			view.setGameController(spiedGameController);
			return view;
		});

		// Clean the database before each test
		resetDatabase();

		// Initialize FrameFixture for testing the GUI
		window = new FrameFixture(robot(), addPlayersToMapsView);
		window.show();
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
	public void testNoPlayerOrMapSelected() {
		// Add a map and a player to the database first
		GuiActionRunner.execute(() -> {
			EntityManager em = emf.createEntityManager();
			EntityTransaction transaction = em.getTransaction();
			try {
				transaction.begin();
				GameMap testMap = new GameMap("TestMap");
				em.persist(testMap);
				Player testPlayer = new PlayerBuilder().withName("TestPlayer").build();
				em.persist(testPlayer);
				transaction.commit();
			} catch (Exception e) {
				if (transaction.isActive())
					transaction.rollback();
				throw e;
			} finally {
				em.close();
			}
		});

		// Ensure neither player nor map is selected in the UI
		GuiActionRunner.execute(() -> {
			DefaultListModel<GameMap> mapListModel = addPlayersToMapsView.getMapListModel();
			DefaultListModel<Player> playerListModel = addPlayersToMapsView.getPlayerListModel();
			mapListModel.addElement(new GameMap("TestMap"));
			playerListModel.addElement(new PlayerBuilder().withName("TestPlayer").build());
		});

		// Simulate selecting neither player nor map
		window.list("mapList").clearSelection();
		window.list("playerList").clearSelection();

		// Click the button
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify that the controller's method was not called
		Mockito.verify(spiedGameController, Mockito.never()).addPlayerToMap(anyLong(), any(Player.class));

		// Verify the error message is shown, null since button is disabled
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddPlayerToMapSuccessfully() {
		// Add a map and a player to the database
		
			EntityManager em = emf.createEntityManager();
			EntityTransaction transaction = em.getTransaction();
			try {
				transaction.begin();
				GameMap testMap = new GameMap("TestMap");
				em.persist(testMap);
				Player testPlayer = new PlayerBuilder().withName("TestPlayer").build();
				em.persist(testPlayer);
				transaction.commit();
			} catch (Exception e) {
				if (transaction.isActive())
					transaction.rollback();
				throw e;
			} finally {
				em.close();
			}
		

		// Refresh the view's map and player lists to reflect the state of the database
		GuiActionRunner.execute(() -> {
			addPlayersToMapsView.refreshMapList(); // Ensures mapList is refreshed
			addPlayersToMapsView.refreshPlayerList(); // Ensures playerList is refreshed
		});

		// Verify that the lists are not empty
		assertThat(window.list("mapList").target().getModel().getSize()).isGreaterThan(0);
		assertThat(window.list("playerList").target().getModel().getSize()).isGreaterThan(0);

		// Select the map and player from the lists in the UI
		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);

		// Click the button to add player to the map
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the player has been associated with the map in the database
		EntityManager em2 = emf.createEntityManager();
		try {
			GameMap dbMap = em2.createQuery("SELECT g FROM GameMap g WHERE g.name = :name", GameMap.class)
					.setParameter("name", "TestMap").getSingleResult();

			// Force a refresh to make sure the players list is up to date
			em2.refresh(dbMap);

			// Debugging info: log the players in the map
			System.out.println("Players in TestMap: " + dbMap.getPlayers());

			// Assert that the players list now contains "TestPlayer"
			assertThat(dbMap.getPlayers()).extracting(Player::getName).containsExactly("TestPlayer");
		} finally {
			em2.close();
		}

		// Assert that the error label is empty, indicating success
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAddPlayerToMapErrorHandling() {
		// Add a map and a player to the database
		GuiActionRunner.execute(() -> {
			EntityManager em = emf.createEntityManager();
			EntityTransaction transaction = em.getTransaction();
			try {
				transaction.begin();
				GameMap testMap = new GameMap("TestMap");
				em.persist(testMap);
				Player testPlayer = new PlayerBuilder().withName("TestPlayer").build();
				em.persist(testPlayer);
				transaction.commit();
			} catch (Exception e) {
				if (transaction.isActive())
					transaction.rollback();
				throw e;
			} finally {
				em.close();
			}
		});

		// Retrieve the map and player from the database and populate list models
		GuiActionRunner.execute(() -> {
			EntityManager em = emf.createEntityManager();
			try {
				GameMap testMap = em.createQuery("SELECT g FROM GameMap g WHERE g.name = :name", GameMap.class)
						.setParameter("name", "TestMap").getSingleResult();
				Player testPlayer = em.createQuery("SELECT p FROM Player p WHERE p.name = :name", Player.class)
						.setParameter("name", "TestPlayer").getSingleResult();

				DefaultListModel<GameMap> mapListModel = addPlayersToMapsView.getMapListModel();
				mapListModel.addElement(testMap);

				DefaultListModel<Player> playerListModel = addPlayersToMapsView.getPlayerListModel();
				playerListModel.addElement(testPlayer);
			} finally {
				em.close();
			}
		});

		GuiActionRunner.execute(() -> addPlayersToMapsView.setGameController(spiedGameController));
		doThrow(new IllegalStateException("Test Exception")).when(spiedGameController).addPlayerToMap(anyLong(),
				any(Player.class));

		// Select the map and player from the lists in the UI
		window.list("mapList").selectItem(0);
		window.list("playerList").selectItem(0);

		// Click the button to add player to the map
		window.button(JButtonMatcher.withText("Add Selected Player to Map")).click();

		// Verify the error message in the UI
		window.label("errorLabel").requireText("Failed to add player to map: TestMap");

		// Verify that the player was not added to the map in the database
		EntityManager em = emf.createEntityManager();
		try {
			GameMap dbMap = em.createQuery("SELECT g FROM GameMap g WHERE g.name = :name", GameMap.class)
					.setParameter("name", "TestMap").getSingleResult();
			assertThat(dbMap.getPlayers()).isEmpty();
		} finally {
			em.close();
		}
	}

	@Override
	protected void onTearDown() throws Exception {
		// Close Mockito mocks
		closeable.close();
	}
}
