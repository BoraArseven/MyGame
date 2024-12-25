package com.boracompany.mygame.orm;

import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.model.Player;

public class PlayerDAOIMPL implements PlayerDAO {
	public static final String TRANSACTION_NULL_MESSAGE = "Transaction is null";
	private static final Logger LOGGER = LogManager.getLogger(PlayerDAOIMPL.class);
	private EntityManagerFactory emf;

	public PlayerDAOIMPL(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Override
	public List<Player> getAllPlayers() {
		EntityManager em = emf.createEntityManager();
		try {
			return em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
		} finally {
			em.close();
		}
	}

	@Override
	public Player getPlayer(Long id) {
		EntityManager em = emf.createEntityManager();
		try {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin(); // Start a transaction
			Player player = em.find(Player.class, id, LockModeType.PESSIMISTIC_READ);
			transaction.commit(); // Commit the transaction
			return player;
		} catch (RuntimeException e) {
			em.getTransaction().rollback(); // Rollback in case of failure
			throw e;
		} finally {
			em.close();
		}
	}

	@Override
	public Player createPlayer(Player player) throws IllegalStateException {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		return executeInTransaction(em -> {
			em.persist(player);
			return player;
		}, "An error occurred while trying to create player with ID {}: {}",
				player.getId() != null ? player.getId() : "N/A");
	}

	@Override
	public void updatePlayer(Player player) throws IllegalStateException {
		executeInTransaction(em -> {
			if (player.getId() == null) {
				throw new IllegalStateException("Player ID cannot be null.");
			}
			Player managedPlayer = em.find(Player.class, player.getId(), LockModeType.PESSIMISTIC_WRITE);
			if (managedPlayer == null) {
				throw new IllegalStateException("Player with ID '" + player.getId() + "' not found.");
			}
			em.merge(player);
			return null;
		}, "An error occurred while trying to update player with ID {}: {}",
				player.getId() != null ? player.getId() : "N/A");
	}

	@Override
	public void deletePlayer(Player player) {
		executeInTransaction(em -> {
			if (player.getId() == null) {
				throw new IllegalStateException("Tried to delete non-existing player with ID N/A");
			}

			Player managedPlayer = em.find(Player.class, player.getId(), LockModeType.PESSIMISTIC_WRITE);
			if (managedPlayer != null) {
				em.remove(managedPlayer);
			} else {
				throw new IllegalStateException("Tried to delete non-existing player with ID " + player.getId());
			}
			return null;
		}, "An error occurred while trying to delete player with ID {}: {}",
				player.getId() != null ? player.getId() : "N/A");
	}

	/**
	 * Executes a database operation within a transaction.
	 *
	 * @param <R>          The type of the result.
	 * @param action       The database operation to execute, which returns a
	 *                     result.
	 * @param errorMessage The error message to log if an exception occurs.
	 * @param errorArgs    Arguments for the error message placeholders.
	 * @return The result of the database operation.
	 * @throws IllegalStateException if the transaction cannot be started.
	 */
	private <R> R executeInTransaction(Function<EntityManager, R> action, String errorMessage, Object... errorArgs) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			if (transaction != null) {
				transaction.begin();
				R result = action.apply(em);
				transaction.commit();
				return result;
			} else {
				throw new IllegalStateException(TRANSACTION_NULL_MESSAGE);
			}
		} catch (RuntimeException e) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			LOGGER.error(errorMessage, errorArgs);
			LOGGER.error("Exception occurred:", e);
			throw e;
		} finally {
			em.close();
		}
	}
}
