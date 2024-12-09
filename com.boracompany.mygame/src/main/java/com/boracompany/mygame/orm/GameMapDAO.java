package com.boracompany.mygame.orm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.util.List;

public class GameMapDAO implements IGameMapDAO {

	private static final Logger logger = LogManager.getLogger(GameMapDAO.class);

	private EntityManagerFactory entityManagerFactory;

	public GameMapDAO(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	private final String GAMEMAP_WITH_ID = "GameMap with id ";
	private final String NOT_FOUND = " not found.";

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
		EntityTransaction transaction = entityManager.getTransaction();

		try {
			logger.info("Updating GameMap: {}", gameMap.getName());
			transaction.begin();

			// Acquire a PESSIMISTIC_WRITE lock on the GameMap
			GameMap managedGameMap = entityManager.find(GameMap.class, gameMap.getId(), LockModeType.PESSIMISTIC_WRITE);
			if (managedGameMap == null) {
				throw new PersistenceException(GAMEMAP_WITH_ID + gameMap.getId() + NOT_FOUND);
			}

			// Update fields as necessary
			managedGameMap.setName(gameMap.getName());
			// ... update other fields as needed

			entityManager.merge(managedGameMap);
			transaction.commit();
			logger.info("GameMap {} updated successfully.", managedGameMap.getName());
		} catch (PersistenceException e) {
			if (transaction.isActive()) {
				transaction.rollback();
				logger.error("Failed to update GameMap: {}", e.getMessage());
			}
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

			// Acquire a PESSIMISTIC_WRITE lock on the GameMap
			GameMap gameMap = entityManager.find(GameMap.class, id, LockModeType.PESSIMISTIC_WRITE);

			if (gameMap != null) {
				entityManager.remove(gameMap);
				transaction.commit(); // Commit the transaction if everything is fine
				logger.info("GameMap with ID {} deleted successfully.", id);
			} else {
				logger.warn("GameMap with ID {} not found.", id);
				throw new PersistenceException(GAMEMAP_WITH_ID + id + NOT_FOUND);
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

	public void addPlayerToMap(Long mapId, Player player) {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		boolean isTransactionOwner = false;

		try {
			if (!transaction.isActive()) {
				transaction.begin();
				isTransactionOwner = true;
			} else {
				throw new IllegalStateException("Transaction already active. Cannot begin a new transaction.");
			}

			// Validate mapId
			if (mapId == null) {
				throw new IllegalArgumentException("MapId cannot be null");
			}

			// Find the GameMap by ID with a PESSIMISTIC_WRITE lock
			GameMap gameMap = em.find(GameMap.class, mapId, LockModeType.PESSIMISTIC_WRITE);
			if (gameMap == null) {
				throw new IllegalArgumentException(GAMEMAP_WITH_ID + mapId + NOT_FOUND);
			}

			if (player == null) {
				throw new IllegalArgumentException("Player cannot be null");
			}

			// Persist or merge the player entity
			if (player.getId() == null) {
				em.persist(player);
			} else {
				em.merge(player);
			}

			// Add the player to the map's player list
			gameMap.getPlayers().add(player);

			// Save changes to the GameMap
			em.merge(gameMap);

			// Commit the transaction
			transaction.commit();
		} catch (IllegalArgumentException | IllegalStateException e) {
			if (isTransactionOwner && transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		} catch (Exception e) {
			if (isTransactionOwner && transaction.isActive()) {
				transaction.rollback();
			}
			throw new PersistenceException("Failed to add player to map", e);
		} finally {

			em.close();

		}
	}

	@Override
	public void removePlayerFromMap(Long mapId, Player player) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();

		try {
			transaction.begin();

			// Find the GameMap with a PESSIMISTIC WRITE lock and validate it exists
			GameMap gameMap = entityManager.find(GameMap.class, mapId, LockModeType.PESSIMISTIC_WRITE);
			if (gameMap == null) {
				throw new PersistenceException(GAMEMAP_WITH_ID + mapId + NOT_FOUND);
			}

			// Validate that the player's ID is not null before attempting to load the
			// player
			if (player == null || player.getId() == null) {
				logger.warn("Player is null or has a null ID.");
				throw new PersistenceException("Player is null or has a null ID.");
			}

			// Find the managed player entity with a PESSIMISTIC WRITE lock
			Player managedPlayer = entityManager.find(Player.class, player.getId(), LockModeType.PESSIMISTIC_WRITE);
			if (managedPlayer == null || !gameMap.getPlayers().contains(managedPlayer)) {
				logger.warn("Expected GameMap not found or Player not in this GameMap.");
				throw new PersistenceException("Expected GameMap not found or Player not in this GameMap.");
			}

			// Remove the player from the map and update the entities
			logger.info("Removing Player {} from GameMap with ID: {}", player.getName(), mapId);
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
			throw new PersistenceException("Failed to remove Player: " + e.getMessage(), e);
		} finally {
			entityManager.close();
			logger.info("EntityManager closed after removing Player from GameMap.");
		}
	}

	@Override
	public List<Player> findAlivePlayers() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			return entityManager.createQuery("SELECT p FROM Player p WHERE p.isAlive = true", Player.class)
					.getResultList();
		} finally {
			entityManager.close();
		}
	}

}
