package com.boracompany.mygame.orm;

import java.util.List;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

public interface IGameMapDAO {

    void save(GameMap gameMap);

    GameMap findById(Long id);

    List<GameMap> findAll();

    void update(GameMap gameMap);

    void delete(Long id);

    List<Player> findPlayersByMapId(Long mapId);

    void addPlayerToMap(Long mapId, Player player);

    void removePlayerFromMap(Long mapId, Player player);

	List<Player> findAlivePlayers();
}