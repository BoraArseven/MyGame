package com.boracompany.mygame.controller;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.model.PlayerBuilder;
import com.boracompany.mygame.orm.GameMapDAO;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

import java.util.List;
import org.apache.logging.log4j.Logger;

public class GameController {

    private static final String NOT_FOUND_MESSAGE = " not found";
    private static final String MAP_WITH_ID = "Map with ID ";
    private static final String ERROR_MAPNOTFOUND = "Map with ID {} not found";

    private final PlayerDAOIMPL playerDAO;
    private final GameMapDAO gameMapDAO;
    private final Logger logger;

    // A single lock for all operations (simplifies thread safety, but reduces concurrency)
    private final Object lock = new Object();

    // Constructor with dependency injection
    public GameController(PlayerDAOIMPL playerDAO, GameMapDAO gameMapDAO, Logger logger) {
        this.playerDAO = playerDAO;
        this.gameMapDAO = gameMapDAO;
        this.logger = logger;
    }

    public Player createPlayer(String playerName, float health, float damage) {
        synchronized (lock) {
            validatePlayerAttributes(health, damage);
            Player player = new PlayerBuilder().withName(playerName).withHealth(health).withDamage(damage).build();
            logger.info("Player created: {}", playerName);
            return playerDAO.createPlayer(player); // Ensure this returns the persisted Player with 'id'
        }
    }

    private void validatePlayerAttributes(float health, float damage) {
        if (health <= 0) {
            logger.error("Player creation failed: Health must be greater than 0.");
            throw new IllegalArgumentException("Health must be greater than 0.");
        }
        if (damage <= 0) {
            logger.error("Player creation failed: Damage must be greater than 0.");
            throw new IllegalArgumentException("Damage must be greater than 0.");
        }
    }

    // Method to add a player to a map
    public void addPlayerToMap(Long mapId, Player player) {
        synchronized (lock) {
            GameMap gameMap = gameMapDAO.findById(mapId);
            if (gameMap != null) {
                gameMapDAO.addPlayerToMap(mapId, player);
                gameMapDAO.update(gameMap);
                // Logging was previously incorrect: Using correct placeholder usage
                logger.info("Player {} added to map {}", player.getName(), gameMap.getName());
            } else {
                logger.error(ERROR_MAPNOTFOUND, mapId);
                throw new IllegalArgumentException(MAP_WITH_ID + mapId + NOT_FOUND_MESSAGE);
            }
        }
    }

    // Method to remove a player from a map
    public void removePlayerFromMap(Long mapId, Player player) {
        synchronized (lock) {
            GameMap gameMap = gameMapDAO.findById(mapId);
            if (gameMap != null && gameMap.getPlayers() != null && gameMap.getPlayers().contains(player)) {
                gameMapDAO.removePlayerFromMap(mapId, player);
                gameMapDAO.update(gameMap);
                logger.info("Player {} removed from map {}", player.getName(), gameMap.getName());
            } else {
                logger.error("Map with ID {} or player {} not found", mapId, player.getName());
                throw new IllegalArgumentException(
                        MAP_WITH_ID + mapId + " or player " + player.getName() + NOT_FOUND_MESSAGE);
            }
        }
    }

    // Existing attack method
    public void attack(Player attacker, Player defender) {
        synchronized (lock) {
            validatePlayers(attacker, defender);
            validateAlive(attacker);

            float damage = calculateDamage(attacker);

            float defenderHealth = defender.getHealth();
            logAttackInitiation(attacker, defender, damage, defenderHealth);

            float newHealth = calculateNewHealth(defenderHealth, damage);

            updateDefenderHealth(defender, newHealth);
        }
    }

    private void validateAlive(Player attacker) {
        if (attacker.getHealth() <= 0 || !attacker.isAlive()) {
            logger.error("Attack failed: Attacker {} is not eligible to attack.", attacker.getName());
            throw new IllegalArgumentException("Attacker is not eligible to attack.");
        }
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
        defender.setHealth(Math.max(newHealth, 0));
        defender.setAlive(newHealth > 0);
        if (newHealth > 0) {
            logger.info("Attack successful: Defender: {}'s new health: {}", defender.getName(), newHealth);
        } else {
            logger.info("Attack successful: Defender: {} has been defeated (Health: 0, IsAlive: {})",
                    defender.getName(), defender.isAlive());
        }

        try {
            playerDAO.updatePlayer(defender);
        } catch (Exception e) {
            logger.error("Failed to update defender {} in the database", defender.getName(), e);
            throw new IllegalStateException("Could not update defender in the database", e);
        }
    }

    public void deletePlayer(Long playerId) {
        synchronized (lock) {
            if (playerId == null) {
                logger.error("Player ID is null, cannot delete player.");
                throw new IllegalArgumentException("Player ID must not be null.");
            }

            Player player = playerDAO.getPlayer(playerId);
            if (player != null) {
                try {
                    playerDAO.deletePlayer(player);
                    logger.info("Player {} with ID {} deleted successfully.", player.getName(), playerId);
                } catch (RuntimeException e) {
                    logger.error("Failed to delete player with ID {}", playerId);
                    throw new IllegalStateException("Could not delete player with ID " + playerId, e);
                }
            } else {
                logger.error("Player with ID {} not found", playerId);
                throw new IllegalArgumentException("Player with ID " + playerId + NOT_FOUND_MESSAGE);
            }
        }
    }

    public List<Player> getAllPlayers() {
        synchronized (lock) {
            try {
                List<Player> players = playerDAO.getAllPlayers();
                List<Player> alivePlayers = players.stream().filter(Player::isAlive).toList();
                logger.info("Retrieved {} alive players from the database.", alivePlayers.size());
                return alivePlayers;
            } catch (RuntimeException e) {
                logger.error("Failed to retrieve all players from the database", e);
                throw new IllegalStateException("Could not retrieve players from the database", e);
            }
        }
    }

    public void deleteMap(Long mapId) {
        synchronized (lock) {
            if (mapId == null) {
                logger.error("Map ID is null, cannot delete map.");
                throw new IllegalArgumentException("Map ID must not be null.");
            }

            GameMap map = gameMapDAO.findById(mapId);
            if (map != null) {
                try {
                    gameMapDAO.delete(mapId);
                    logger.info("Map {} with ID {} deleted successfully.", map.getName(), mapId);
                } catch (RuntimeException e) {
                    logger.error("Failed to delete map with ID {}", mapId);
                    throw new IllegalStateException("Could not delete map with ID " + mapId, e);
                }
            } else {
                logger.error(ERROR_MAPNOTFOUND, mapId);
                throw new IllegalArgumentException(MAP_WITH_ID + mapId + NOT_FOUND_MESSAGE);
            }
        }
    }

    // Method to create a new map and add it to the database
    public GameMap createMap(String mapName, List<Player> players) {
        synchronized (lock) {
            GameMap gameMap = new GameMap(mapName, players);
            try {
                gameMapDAO.save(gameMap);
                logger.info("Map created: {}", gameMap.getName());
                return gameMap;
            } catch (Exception e) {
                logger.error("Failed to create map: {}", mapName, e);
                throw new IllegalStateException("Could not create map: " + mapName, e);
            }
        }
    }

    public List<GameMap> getAllMaps() {
        synchronized (lock) {
            try {
                List<GameMap> maps = gameMapDAO.findAll();
                logger.info("Retrieved {} maps from the database.", maps.size());
                return maps;
            } catch (RuntimeException e) {
                logger.error("Failed to retrieve all maps from the database", e);
                throw new IllegalStateException("Could not retrieve maps from the database", e);
            }
        }
    }

    public List<Player> getPlayersFromMap(Long mapId) {
        synchronized (lock) {
            if (mapId == null) {
                logger.error("Map ID is null, cannot retrieve players.");
                throw new IllegalArgumentException("Map ID must not be null.");
            }

            GameMap gameMap = gameMapDAO.findById(mapId);
            if (gameMap != null) {
                List<Player> alivePlayers = gameMap.getPlayers().stream().filter(Player::isAlive).toList();
                logger.info("Retrieved {} alive players from map {}", alivePlayers.size(), gameMap.getName());
                return alivePlayers;
            } else {
                logger.error(ERROR_MAPNOTFOUND, mapId);
                throw new IllegalArgumentException(MAP_WITH_ID + mapId + NOT_FOUND_MESSAGE);
            }
        }
    }
}
