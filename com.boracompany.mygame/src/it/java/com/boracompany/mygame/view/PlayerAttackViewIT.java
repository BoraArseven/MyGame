package com.boracompany.mygame.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

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
	public void testAttackThrowsExceptionShouldShowErrorMessage() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			Player testPlayer1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20)
					.withIsAlive(true).build();
			Player testPlayer2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true)
					.build();

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

		doThrow(new RuntimeException("Test Exception")).when(spiedGameController).attack(any(Player.class),
				any(Player.class));

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

	@Test
	@GUITest
	public void testAttackWithBothAttackerAndDefenderSelected_ShouldAttackSuccessfully() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Arrange: Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			Player attackerPlayer = new PlayerBuilder().withName("AttackerPlayer").withHealth(100).withDamage(20)
					.withIsAlive(true).build();
			Player defenderPlayer = new PlayerBuilder().withName("DefenderPlayer").withHealth(80).withDamage(15)
					.withIsAlive(true).build();

			// Persist the map and players in the database
			em.persist(testMap);
			attackerPlayer.setMap(testMap);
			defenderPlayer.setMap(testMap);
			em.persist(attackerPlayer);
			em.persist(defenderPlayer);

			transaction.commit(); // Commit transaction
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh map and player lists in the GUI
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList(); // Refresh the map list
			playerAttackView.refreshPlayerLists(); // Refresh the attacker and defender player lists
		});

		// Select the map, attacker, and defender players
		window.list("mapList").selectItem(0); // Select the first (and only) map
		window.list("attackerList").selectItem(0); // Select the first player as the attacker
		window.list("defenderList").selectItem(1); // Select the second player as the defender

		// Act: Perform the attack
		window.button(JButtonMatcher.withText("Attack")).click();

		// Assert: Verify that the attack method on the GameController was called once
		verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));

		// Assert that there are no error messages displayed
		window.label("errorLabel").requireText("");

		// Verify that the appropriate logs are generated
		// This will ensure that both attacker and defender were non-null and the attack
		// occurred
		verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));
	}

	@Test
	@GUITest
	public void testAttackWithAttackerAndDefenderBothSelected_NoErrorOccurs() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Arrange: Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			Player attackerPlayer = new PlayerBuilder().withName("AttackerPlayer").withHealth(100).withDamage(20)
					.withIsAlive(true).build();
			Player defenderPlayer = new PlayerBuilder().withName("DefenderPlayer").withHealth(80).withDamage(15)
					.withIsAlive(true).build();

			// Persist the map and players in the database
			em.persist(testMap);
			attackerPlayer.setMap(testMap);
			defenderPlayer.setMap(testMap);
			em.persist(attackerPlayer);
			em.persist(defenderPlayer);

			transaction.commit(); // Commit transaction
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh map and player lists in the GUI
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList(); // Refresh the map list
			playerAttackView.refreshPlayerLists(); // Refresh the attacker and defender player lists
		});

		// Act: Select the map, attacker, and defender players
		window.list("mapList").selectItem(0); // Select the first (and only) map
		window.list("attackerList").selectItem(0); // Select the first player as the attacker
		window.list("defenderList").selectItem(1); // Select the second player as the defender

		// Click the Attack button
		window.button(JButtonMatcher.withText("Attack")).click();

		// Assert: Verify that the attack method on the GameController was called once
		verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));

		// Assert that there are no error messages displayed, indicating a successful
		// attack
		window.label("errorLabel").requireText("");

		// Assert that both attacker and defender were not null (implicitly verified by
		// the fact that attack was called)
		assertThat(window.list("attackerList").selection()).isNotNull();
		assertThat(window.list("defenderList").selection()).isNotNull();
	}

	@Test
	@GUITest
	public void testRefreshPlayerLists_SelectedDefenderNotInLivingPlayers() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		Player player1;
		Player player2;
		try {
			transaction.begin();

			// Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			player1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true).build();
			player2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true).build();

			em.persist(testMap);
			player1.setMap(testMap);
			player2.setMap(testMap);
			em.persist(player1);
			em.persist(player2);

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh map and player lists in the GUI
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Refresh player lists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// Select player2 as defender
		window.list("defenderList").selectItem(1); // Assuming player2 is at index 1

		// Verify that the selected defender is player2
		GuiActionRunner.execute(() -> {
			Player selectedDefender = playerAttackView.getDefenderList().getSelectedValue();
			assertThat(selectedDefender.getName()).isEqualTo("Player2");
		});
		ArrayList<Player> mockedlist = new ArrayList<>();
		mockedlist.add(player1);

		// Simulate that player2 is no longer in livingPlayers
		Mockito.doReturn(mockedlist).when(spiedGameController).getPlayersFromMap(Mockito.anyLong());

		// Call refreshPlayerLists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// After refreshing, the selected defender should be null
		GuiActionRunner.execute(() -> {
			Player selectedDefender = playerAttackView.getDefenderList().getSelectedValue();
			assertThat(selectedDefender).isNull();
		});
	}

	@Test
	@GUITest
	public void testSuccessfulAttackWhenAttackerAndDefenderAreBothSelected() {
		// Arrange: Set up map and players
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			GameMap testMap = new GameMap("TestMap");
			Player testAttacker = new PlayerBuilder().withName("Attacker").withHealth(100).withDamage(20)
					.withIsAlive(true).build();
			Player testDefender = new PlayerBuilder().withName("Defender").withHealth(80).withDamage(15)
					.withIsAlive(true).build();

			em.persist(testMap);
			testAttacker.setMap(testMap);
			testDefender.setMap(testMap);
			em.persist(testAttacker);
			em.persist(testDefender);

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Act: Refresh map and player lists in the view
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
			playerAttackView.refreshPlayerLists();
		});

		// Select map, attacker, and defender
		window.list("mapList").selectItem(0);
		window.list("attackerList").selectItem(0);
		window.list("defenderList").selectItem(1);

		// Assert: Verify that the attack button is enabled
		window.button(JButtonMatcher.withText("Attack")).requireEnabled();

		// Perform the attack by clicking the attack button
		window.button(JButtonMatcher.withText("Attack")).click();

		// Verify that the attack method was called with both selected players
		verify(spiedGameController, times(1)).attack(any(Player.class), any(Player.class));

		// Assert: Verify that there are no error messages displayed, indicating a
		// successful attack
		window.label("errorLabel").requireText("");
	}

	@Test
	@GUITest
	public void testAttackFailsWhenAttackerIsNullButDefenderExists() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Create and persist the map and defender player
			GameMap testMap = new GameMap("TestMap");
			Player testDefender = new PlayerBuilder().withName("Defender").withHealth(80f).withDamage(15f)
					.withIsAlive(true).build();

			em.persist(testMap); // Persist the map
			testDefender.setMap(testMap); // Associate the defender with the map
			em.persist(testDefender); // Persist the defender

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh the map and player lists in the view
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList(); // Refresh map list
		});

		// Select the map and then refresh the player lists
		window.list("mapList").selectItem(0);

		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists(); // Refresh player lists after selecting the map
		});

		// Ensure the player list is populated for defender only
		assertThat(playerAttackView.getDefenderListModel().getSize()).isEqualTo(1);

		// Select a defender in the defender list, but do not select an attacker
		window.list("defenderList").selectItem(0); // Select the defender only

		// Manually invoke the attackSelectedPlayers method to bypass the UI limitation
		// of the disabled button
		GuiActionRunner.execute(() -> {
			playerAttackView.attackSelectedPlayers();
		});

		// Verify that the error message is shown
		window.label("errorLabel").requireText("Attacker and defender must be selected.");

		// Verify that the attack method was never called since no attacker was selected
		verify(spiedGameController, times(0)).attack(any(), any());
	}

	@Test
	@GUITest
	public void testRefreshPlayerLists_SelectedAttackerInLivingPlayers() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		Player player1;

		try {
			transaction.begin();

			// Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			player1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true).build();
			Player player2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true)
					.build();

			em.persist(testMap);
			player1.setMap(testMap);
			player2.setMap(testMap);
			em.persist(player1);
			em.persist(player2);

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh map and player lists in the GUI
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Refresh player lists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// Select player1 as attacker
		window.list("attackerList").selectItem(0); // Assuming player1 is at index 0

		// Verify that the selected attacker is player1
		GuiActionRunner.execute(() -> {
			Player selectedAttacker = playerAttackView.getAttackerList().getSelectedValue();
			assertThat(selectedAttacker.getName()).isEqualTo("Player1");
		});

		// Call refreshPlayerLists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// After refreshing, the selected attacker should still be player1
		GuiActionRunner.execute(() -> {
			Player selectedAttacker = playerAttackView.getAttackerList().getSelectedValue();
			assertThat(selectedAttacker.getName()).isEqualTo("Player1");

			// Additional assertion to check that livingPlayers.contains(selectedAttacker)
			// is true
			List<Player> livingPlayers = playerAttackView.getLivingPlayers();
			assertThat(livingPlayers).contains(selectedAttacker);
		});
	}

	@Test
	@GUITest
	public void testRefreshPlayerLists_SelectedAttackerNotInLivingPlayers() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		Player player1;
		Player player2;

		try {
			transaction.begin();

			// Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			player1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true).build();
			player2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true).build();

			em.persist(testMap);
			player1.setMap(testMap);
			player2.setMap(testMap);
			em.persist(player1);
			em.persist(player2);

			transaction.commit();

			// Refresh entities to ensure IDs are populated
			em.refresh(player1);
			em.refresh(player2);
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Select the map and refresh lists initially
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
		});

		window.list("mapList").selectItem(0); // Select TestMap

		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists(); // Trigger initial refresh
		});

		// Verify initial living players
		GuiActionRunner.execute(() -> {
			List<Player> initialLivingPlayers = playerAttackView.getLivingPlayers();
			assertThat(initialLivingPlayers).isNotEmpty().contains(player1, player2); // Ensure both players are
																						// initially present
		});

		// Mock getPlayersFromMap to exclude player1
		List<Player> mockedLivingPlayers = new ArrayList<>();
		mockedLivingPlayers.add(player2); // Only player2 is in livingPlayers

		Mockito.doReturn(mockedLivingPlayers).when(spiedGameController).getPlayersFromMap(Mockito.anyLong());

		// Trigger refresh after mocking
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// Verify updated living players and selection
		GuiActionRunner.execute(() -> {
			List<Player> updatedLivingPlayers = playerAttackView.getLivingPlayers();
			assertThat(updatedLivingPlayers).isNotEmpty().doesNotContain(player1); // Ensure player1 is excluded
			Player selectedAttacker = playerAttackView.getAttackerList().getSelectedValue();
			assertThat(selectedAttacker).isNull(); // Attacker should be deselected
		});
	}

	@Test
	@GUITest
	public void testRefreshPlayerLists_SelectedDefenderInLivingPlayers() {
		// Arrange
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			// Create and persist the map and players
			GameMap testMap = new GameMap("TestMap");
			Player player1 = new PlayerBuilder().withName("Player1").withHealth(100).withDamage(20).withIsAlive(true)
					.build();
			Player player2 = new PlayerBuilder().withName("Player2").withHealth(80).withDamage(15).withIsAlive(true)
					.build();

			em.persist(testMap);
			player1.setMap(testMap);
			player2.setMap(testMap);
			em.persist(player1);
			em.persist(player2);

			transaction.commit();
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} finally {
			em.close();
		}

		// Refresh map and player lists in the GUI
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshMapList();
		});

		// Select the map
		window.list("mapList").selectItem(0);

		// Refresh player lists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// Select player2 as defender
		window.list("defenderList").selectItem(1); // Assuming player2 is at index 1

		// Verify that the selected defender is player2
		GuiActionRunner.execute(() -> {
			Player selectedDefender = playerAttackView.getDefenderList().getSelectedValue();
			assertThat(selectedDefender.getName()).isEqualTo("Player2");
		});

		// Call refreshPlayerLists
		GuiActionRunner.execute(() -> {
			playerAttackView.refreshPlayerLists();
		});

		// After refreshing, the selected defender should still be player2
		GuiActionRunner.execute(() -> {
			Player selectedDefender = playerAttackView.getDefenderList().getSelectedValue();
			assertThat(selectedDefender.getName()).isEqualTo("Player2");
		});
	}

	@Test
	public void testPlayerEqualsAndHashCode() {
		Player player1 = new Player();
		player1.setId(1L);
		player1.setName("Player1");

		Player player2 = new Player();
		player2.setId(1L);
		player2.setName("Player1");

		assertThat(player1).isEqualTo(player2).hasSameHashCodeAs(player2);
	}

	@Override
	protected void onTearDown() throws Exception {
		closeable.close();
	}
}
