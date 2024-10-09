package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

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
@RunWith(org.assertj.swing.junit.runner.GUITestRunner.class)
public class PlayerAttackViewIT extends AssertJSwingJUnitTestCase {

	private FrameFixture window;
	private PlayerAttackView playerAttackView;
	private GameController spiedGameController;
	private static EntityManagerFactory emf;
	private AutoCloseable closeable;

	private static final Logger logger = LogManager.getLogger(PlayerAttackViewIT.class);

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

		PlayerDAOIMPL playerDAO = new PlayerDAOIMPL(emf);
		GameMapDAO gameMapDAO = new GameMapDAO(emf);
		spiedGameController = Mockito.spy(new GameController(playerDAO, gameMapDAO, logger));

		playerAttackView = GuiActionRunner.execute(() -> {
			PlayerAttackView view = new PlayerAttackView();
			view.setGameController(spiedGameController);
			return view;
		});

		resetDatabase();

		window = new FrameFixture(robot(), playerAttackView);
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
			throw new RuntimeException("Failed to reset database", e);
		} finally {
			em.close();
		}
	}

	@Test
	@GUITest
	public void testAttackShouldBePerformedSuccessfully() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			GameMap testMap = new GameMap("TestMap");
			Player testPlayer1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20)
					.withIsAlive(true).build();
			Player testPlayer2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true)
					.build();

			em.persist(testMap);
			testPlayer1.setMap(testMap);
			testPlayer2.setMap(testMap);
			em.persist(testPlayer1);
			em.persist(testPlayer2);

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
			playerAttackView.refreshPlayerLists();
			
		});

		window.list("mapList").selectItem(0);
		window.list("attackerList").selectItem(0);
		window.list("defenderList").selectItem(1);

		window.button(JButtonMatcher.withText("Attack")).click();

		verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));

		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAttackThrowsExceptionShouldShowErrorMessage() {
	    EntityManager em = emf.createEntityManager();
	    EntityTransaction transaction = em.getTransaction();

	    try {
	        transaction.begin();

	        // Create and persist the map and players
	        GameMap testMap = new GameMap("TestMap");
	        Player testPlayer1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true).build();
	        Player testPlayer2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true).build();

	        em.persist(testMap); // Persist the map
	        em.persist(testPlayer2);
	        em.persist(testPlayer1);
	        testPlayer1.setMap(testMap); // Assign the map to the player
	        testPlayer2.setMap(testMap); // Assign the map to the player
	        em.merge(testPlayer1); // Persist the player
	        em.merge(testPlayer2); // Persist the player
	        em.merge(testMap);
	        
	        transaction.commit(); // Commit the transaction
	    } catch (Exception e) {
	        if (transaction.isActive()) {
	            transaction.rollback();
	        }
	        throw e;
	    } finally {
	        em.close();
	    }

	    doThrow(new RuntimeException("Test Exception")).when(spiedGameController).attack(any(Player.class), any(Player.class));

	    // Refresh the map and player lists in the view
	    GuiActionRunner.execute(() -> {
	        playerAttackView.refreshMapList(); // Refresh map list
	        playerAttackView.refreshPlayerLists(); // Refresh player lists
	    });

	
	    // Select the map and players
	    window.list("mapList").selectItem(0); // Select the first map
	    window.list("attackerList").selectItem(0); // Select the first player
	    window.list("defenderList").selectItem(1); // Select the second player

	    // Perform the attack and trigger exception
	    window.button(JButtonMatcher.withText("Attack")).click();

	    // Verify that the attack method was called once and the exception was handled
	    verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));

	    // Verify that the error message is displayed correctly
	    window.label("errorLabel").requireText("Failed to perform attack.");
	}


	@Override
	protected void onTearDown() throws Exception {
		closeable.close();
	}
}
