package com.boracompany.mygame.Controller;

import com.boracompany.mygame.Model.GameMap;
import com.boracompany.mygame.Model.Player;
import com.boracompany.mygame.Model.PlayerBuilder;
import com.boracompany.mygame.ORM.PlayerDAOIMPL;
import com.boracompany.mygame.ORM.GameMapDAO;


import org.apache.logging.log4j.Logger;

public class GameController {

	private PlayerDAOIMPL playerDAO;
	private GameMapDAO gameMapDAO;
	private Logger logger; // Injected logger for better testability

	// Constructor with dependency injection
	public GameController(PlayerDAOIMPL playerDAO, GameMapDAO gameMapDAO, Logger logger) {
		this.playerDAO = playerDAO;
		this.gameMapDAO = gameMapDAO;
		this.logger = logger;
	}



	// Method to create a new player and add it to the database
	public Player createPlayer(String playerName, float health, float damage) {
		Player player = new PlayerBuilder().withName(playerName).withHealth(health).withDamage(damage).build();
		playerDAO.updatePlayer(player);
		logger.info("Player created: {}", player.getName());
		return player;
	}

	// Method to add a player to a map
	public void addPlayerToMap(Long mapId, Player player) {
		GameMap gameMap = gameMapDAO.findById(mapId);
		if (gameMap != null) {
			gameMapDAO.addPlayerToMap(mapId, player);
			gameMapDAO.update(gameMap);
			logger.info("Player {} added to map {}", player.getName(), gameMap.getName());
		} else {
			logger.error("Map with ID {} not found", mapId);
			throw new IllegalArgumentException("Map with ID " + mapId + " not found");
		}
	}

	// Method to remove a player from a map
	public void removePlayerFromMap(Long mapId, Player player) {
		GameMap gameMap = gameMapDAO.findById(mapId);
		if (gameMap != null && gameMap.getPlayers() != null && gameMap.getPlayers().contains(player)) {
			gameMapDAO.removePlayerFromMap(mapId, player);
			gameMapDAO.update(gameMap);
			logger.info("Player {} removed from map {}", player.getName(), gameMap.getName());
		} else {
			logger.error("Map with ID {} or player {} not found", mapId, player.getName());
			throw new IllegalArgumentException(
					"Map with ID " + mapId + " or player " + player.getName() + " not found");
		}
	}

	// Existing attack method
	public void attack(Player attacker, Player defender) {
		validatePlayers(attacker, defender); // This checks if attacker and defender are not null

		float damage = calculateDamage(attacker); // This should trigger error if damage is invalid

		float defenderHealth = defender.getHealth();
		logAttackInitiation(attacker, defender, damage, defenderHealth);

		float newHealth = calculateNewHealth(defenderHealth, damage);

		updateDefenderHealth(defender, newHealth);
	}

	// Helper methods for attack
	private void validatePlayers(Player attacker, Player defender) {
		if (attacker == null || defender == null) {
			logger.error("Attacker or defender is null.");
			throw new IllegalArgumentException("Attacker or defender is null.");
		}
	}

	private float calculateDamage(Player attacker) {
		float damage = attacker.getDamage();
		if (damage <= 0) {
			logger.error("Attack failed: Damage should be positive");
			throw new IllegalArgumentException("Damage should be positive");
		}
		return damage;
	}

	private void logAttackInitiation(Player attacker, Player defender, float damage, float defenderHealth) {
		logger.info("Attack initiated: Attacker: {} (Damage: {}), Defender: {} (Health: {})", attacker.getName(),
				damage, defender.getName(), defenderHealth);
	}

	private float calculateNewHealth(float defenderHealth, float damage) {
		return defenderHealth - damage;
	}
	

	private void updateDefenderHealth(Player defender, float newHealth) {
		if (newHealth > 0) {
			defender.setHealth(newHealth);
			logger.info("Attack successful: Defender: {}'s new health: {}", defender.getName(), newHealth);
		} else {
			defender.setHealth(0);
			defender.setAlive(false);
			logger.info("Attack successful: Defender: {} has been defeated (Health: 0, IsAlive: {})",
					defender.getName(), defender.Isalive());
		}
	}
}
