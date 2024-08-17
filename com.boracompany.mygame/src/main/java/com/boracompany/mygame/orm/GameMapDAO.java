package com.boracompany.mygame.orm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import java.util.List;

public class GameMapDAO implements IGameMapDAO {

	private static final Logger logger = LogManager.getLogger(GameMapDAO.class);

	private EntityManagerFactory entityManagerFactory;

	public GameMapDAO(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void save(GameMap gameMap) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();

		try {
			logger.info("Saving GameMap: {}", gameMap.getName());
			transaction.begin();
			entityManager.persist(gameMap);
			transaction.commit(); // Commit only if everything goes well
			logger.info("GameMap {} saved successfully.", gameMap.getName());
		} catch (PersistenceException e) {
			if (transaction.isActive()) {
				transaction.rollback(); // Rollback if an exception occurs before commit
				logger.error("Transaction rolled back due to error while saving GameMap: {}", e.getMessage());
			}
			throw new PersistenceException("Failed to save GameMap: " + e.getMessage(), e);
		} finally {
			entityManager.close(); // Ensure the EntityManager is closed
			logger.info("EntityManager closed after saving GameMap.");
		}
	}

	@Override
	public GameMap findById(Long id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			logger.info("Finding GameMap by ID: {}", id);
			return entityManager.find(GameMap.class, id);
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after finding GameMap by ID.");
		}
	}

	@Override
	public List<GameMap> findAll() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			logger.info("Finding all GameMaps.");
			return entityManager.createQuery("FROM GameMap", GameMap.class).getResultList();
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after finding all GameMaps.");
		}
	}

	@Override
	public void update(GameMap gameMap) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			logger.info("Updating GameMap: {}", gameMap.getName());
			entityManager.getTransaction().begin();
			entityManager.merge(gameMap);
			entityManager.getTransaction().commit();
			logger.info("GameMap {} updated successfully.", gameMap.getName());
		} catch (PersistenceException e) {
			entityManager.getTransaction().rollback();
			logger.error("Failed to update GameMap: {}", e.getMessage());
			throw new PersistenceException("Failed to update GameMap: " + e.getMessage(), e);
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after updating GameMap.");
		}
	}

	@Override
	public void delete(Long id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();

		try {
			logger.info("Deleting GameMap by ID: {}", id);
			transaction.begin();
			GameMap gameMap = entityManager.find(GameMap.class, id);

			if (gameMap != null) {
				entityManager.remove(gameMap);
				transaction.commit(); // Commit the transaction if everything is fine
				logger.info("GameMap with ID {} deleted successfully.", id);
			} else {
				logger.warn("GameMap with ID {} not found.", id);
				throw new PersistenceException("GameMap with id " + id + " not found.");
			}
		} catch (PersistenceException e) {
			if (transaction.isActive()) {
				transaction.rollback(); // Rollback the transaction if an exception occurs
				logger.error("Transaction rolled back due to error while deleting GameMap: {}", e.getMessage());
			}
			throw new PersistenceException("Failed to delete GameMap: " + e.getMessage(), e);
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after deleting GameMap.");
		}
	}

	@Override
	public List<Player> findPlayersByMapId(Long mapId) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			logger.info("Finding players by GameMap ID: {}", mapId);
			return entityManager.createQuery("SELECT p FROM Player p WHERE p.map.id = :mapId", Player.class)
					.setParameter("mapId", mapId).getResultList();
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after finding players by GameMap ID.");
		}
	}

	@Override
	public void addPlayerToMap(Long mapId, Player player) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();

		try {
			transaction.begin();
			GameMap gameMap = entityManager.find(GameMap.class, mapId);

			if (gameMap == null) {
				throw new PersistenceException("GameMap with id " + mapId + " not found.");
			}

			player.setMap(gameMap);
			entityManager.persist(player);
			transaction.commit();
		} catch (PersistenceException e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			logger.error("Error occurred, transaction rolled back: {}", e.getMessage());
			throw e; // Rethrow the exception after rollback
		} finally {
			entityManager.close();
		}
	}

	@Override
	public void removePlayerFromMap(Long mapId, Player player) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			// Find the GameMap and validate it exists
			GameMap gameMap = entityManager.find(GameMap.class, mapId);

			// Validate that the player's ID is not null before attempting to load the
			// player
			if (player == null || player.getId() == null) {
				logger.warn("Player is null or has a null ID.");
				throw new PersistenceException("Player is null or has a null ID.");
			}

			// Find the managed player entity
			Player managedPlayer = entityManager.find(Player.class, player.getId());

			// Validate the GameMap and Player exist and the Player is in the GameMap
			if (gameMap == null || managedPlayer == null || !gameMap.getPlayers().contains(managedPlayer)) {
				logger.warn("Expected GameMap or Player not found");
				throw new PersistenceException("Expected GameMap not found or Player not in this GameMap.");
			}

			// Remove the player from the map and update the entities
			logger.info("Removing Player {} from GameMap with ID: {}", player.getName(), mapId);
			transaction.begin();
			gameMap.getPlayers().remove(managedPlayer);
			managedPlayer.setMap(null);
			entityManager.merge(gameMap);
			entityManager.merge(managedPlayer);
			transaction.commit();
			logger.info("Player {} removed from GameMap {} successfully.", player.getName(), gameMap.getName());

		} catch (PersistenceException e) {
			if (transaction.isActive()) {
				transaction.rollback();
				logger.error("Transaction rolled back due to error while removing Player: {}", e.getMessage());
			}
			throw e;
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after removing Player from GameMap.");
		}
	}

}
