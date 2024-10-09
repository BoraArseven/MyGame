package com.boracompany.mygame.orm;

import java.util.List;

import javax.annotation.Generated;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.model.Player;

public class PlayerDAOIMPL implements PlayerDAO {

	private static final String TRANSACTION_NULL_MESSAGE = "Transaction is null";
	
	private static final Logger LOGGER = LogManager.getLogger(PlayerDAOIMPL.class);
	private EntityManagerFactory emf;

	private EntityManager em;

	@Generated("exclude-from-coverage")
	public EntityManagerFactory getEmf() {
		return emf;
	}

	public PlayerDAOIMPL(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Override
	public List<Player> getAllPlayers() {
		em = emf.createEntityManager();
		List<Player> players = null;
		try {
			TypedQuery<Player> query = em.createQuery("SELECT p FROM Player p", Player.class);
			players = query.getResultList();
		} finally {
			em.close();
		}
		return players;
	}

	@Override
	public Player getPlayer(Long id) {
		em = emf.createEntityManager();
		Player player = null;
		try {
			player = em.find(Player.class, id);
		} finally {
			em.close();
		}
		return player;
	}

	@Override
	public void updatePlayer(Player player) throws IllegalStateException {
		em = emf.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			if ((transaction != null)) {
				transaction.begin();
				em.merge(player);
				transaction.commit();
			}
			// Handled NullpointerException thanks to jacoco's guidance.
			else {
				throw new IllegalStateException(TRANSACTION_NULL_MESSAGE);
			}
		} catch (RuntimeException e) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			LOGGER.error("An error occurred while trying to update player with ID {}: {}", player.getId(),
					e.getMessage(), e);
			throw e;
		}

		finally {
			em.close();
		}
	}

	@Override
	public void deletePlayer(Player player) {
		em = emf.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			if (transaction != null) {
				transaction.begin();
				Player managedPlayer = em.find(Player.class, player.getId());
				if (managedPlayer != null) {
					em.remove(managedPlayer);
					transaction.commit();
				} else {
					throw new IllegalStateException("Tried to delete non existing player");
				}
			} else {
				throw new IllegalStateException(TRANSACTION_NULL_MESSAGE);
			}

		} catch (RuntimeException e) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			LOGGER.error("An error occurred while trying to delete player with ID {}: {}", player.getId(),
					e.getMessage(), e);
			throw e;
		} finally {
			em.close();
		}
	}

	@Override
	public void createPlayer(Player player) throws IllegalStateException {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}

		em = emf.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			if (transaction != null) {
				transaction.begin();
				em.persist(player); // Use persist for creation
				transaction.commit();
			} else {
				throw new IllegalStateException(TRANSACTION_NULL_MESSAGE);
			}
		} catch (RuntimeException e) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			LOGGER.error("An error occurred while trying to create player with ID {}: {}", player.getId(),
					e.getMessage(), e);
			throw e;
		} finally {
			em.close();
		}
	}

}