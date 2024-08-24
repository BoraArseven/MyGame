package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.HibernateUtil;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class CreateMapViewIT extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private CreateMapView createMapView;
	private GameController gameController;
	private static EntityManagerFactory emf;
	private AutoCloseable closeable;

	private static final Logger logger = LogManager.getLogger(CreateMapViewIT.class);

	@Container
	public static PostgreSQLContainer<?> postgreSQLContainer = extracted().withDatabaseName("test").withUsername("test")
			.withPassword("test");

	private static PostgreSQLContainer<?> extracted() {
		return new PostgreSQLContainer<>("postgres:13.3");
	}

	@BeforeClass
	public static void setUpContainerAndEntityManagerFactory() {
		postgreSQLContainer.start();

		String jdbcUrl = postgreSQLContainer.getJdbcUrl();
		String username = postgreSQLContainer.getUsername();
		String password = postgreSQLContainer.getPassword();
		HibernateUtil.initialize(jdbcUrl, username, password);

		emf = HibernateUtil.getEntityManagerFactory();
	}

	@AfterClass
	public static void tearDownContainerAndEntityManagerFactory() {
		if (emf != null) {
			emf.close();
		}
		HibernateUtil.close();
		postgreSQLContainer.stop();
	}

	@Before
	public void onSetUp() {
		closeable = MockitoAnnotations.openMocks(this);

		GameMapDAO gameMapDAO = new GameMapDAO(emf);
		PlayerDAOIMPL playerDAO = new PlayerDAOIMPL(emf);
		gameController = new GameController(playerDAO, gameMapDAO, logger);

		createMapView = GuiActionRunner.execute(() -> {
			CreateMapView view = new CreateMapView();
			view.setGameController(gameController);
			return view;
		});

		resetDatabase();

		window = new FrameFixture(robot(), createMapView);
		window.show();
	}

	private void resetDatabase() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();
			em.createQuery("DELETE FROM GameMap").executeUpdate();
			em.createQuery("DELETE FROM Player").executeUpdate();
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
	public void testCreateMapAndDelete() {
		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).click();

		logger.debug("List contents after creation: " + Arrays.toString(window.list("ListMaps").contents()));

		assertThat(window.list("ListMaps").contents()).contains("TestMap");

		window.list("ListMaps").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		logger.debug("List contents after deletion: " + Arrays.toString(window.list("ListMaps").contents()));

		assertThat(window.list("ListMaps").contents()).doesNotContain("TestMap");
	}

	@Test
	@GUITest
	public void testShowAllMaps() {
		GameMap map1 = new GameMap("Map1");
		GameMap map2 = new GameMap("Map2");

		GuiActionRunner.execute(() -> createMapView.showAllMaps(Arrays.asList(map1, map2)));

		assertThat(window.list("ListMaps").contents()).containsExactly(map1.toString(), map2.toString());
	}

	@Test
	@GUITest
	public void testErrorHandling_MapCreationFails() {
		GameController spyGameController = Mockito.spy(gameController);

		GuiActionRunner.execute(() -> {
			createMapView.setGameController(spyGameController);
		});

		doThrow(new RuntimeException("Simulated failure")).when(spyGameController).createMap(anyString(),
				Mockito.anyList());

		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).click();

		assertThat(window.label("ErrorMessageLabel").text()).contains("Failed to create map");
		assertThat(window.list("ListMaps").contents()).isEmpty();
	}

	@Test
	@GUITest
	public void testDeleteMap_NoLongerExistsInBackend() {
		GameMap map = new GameMap("TestMap");

		GuiActionRunner.execute(() -> {
			gameController.createMap(map.getName(), map.getPlayers());
			map.setId(gameController.getAllMaps().get(0).getId());
			createMapView.mapAdded(map);
		});

		GuiActionRunner.execute(() -> gameController.deleteMap(map.getId()));

		window.list("ListMaps").selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();

		assertThat(window.label("ErrorMessageLabel").text())
				.contains("Failed to remove map from the database: TestMap");

		EntityManager em = emf.createEntityManager();
		try {
			List<GameMap> remainingMaps = em.createQuery("SELECT g FROM GameMap g WHERE g.id = :id", GameMap.class)
					.setParameter("id", map.getId()).getResultList();
			assertThat(remainingMaps).isEmpty();
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testCreateMapAndFieldsReset() {
		window.textBox("NameText").enterText("TestMap");
		window.button(JButtonMatcher.withText("Create Map")).click();

		assertThat(window.textBox("NameText").text()).isEmpty();
	}

	@Test
	@GUITest
	public void testRefreshMapList_ShouldDisplayAllMapsFromDatabase() {
		GameMap map1 = new GameMap("Map1");
		GameMap map2 = new GameMap("Map2");

		GuiActionRunner.execute(() -> {
			gameController.createMap(map1.getName(), map1.getPlayers());
			gameController.createMap(map2.getName(), map2.getPlayers());
		});

		GuiActionRunner.execute(() -> createMapView.refreshMapList());

		String[] listContents = window.list("ListMaps").contents();
		assertThat(listContents).containsExactly("Map1", "Map2");
	}

	@Test
	@GUITest
	public void testRefreshMapList_ShouldHandleDatabaseErrorGracefully() {
		GameController spyGameController = Mockito.spy(gameController);

		GuiActionRunner.execute(() -> {
			createMapView.setGameController(spyGameController);
		});

		doThrow(new RuntimeException("Database error")).when(spyGameController).getAllMaps();

		GuiActionRunner.execute(() -> createMapView.refreshMapList());

		window.label("ErrorMessageLabel").requireText("Failed to refresh map list");
		assertThat(window.list("ListMaps").contents()).isEmpty();
	}

	@Override
	protected void onTearDown() throws Exception {
		closeable.close();
	}
}
