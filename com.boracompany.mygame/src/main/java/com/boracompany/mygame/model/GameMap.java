package com.boracompany.mygame.model;

import jakarta.persistence.*;

import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "app_map") // Table name for the Map entity
@NamedEntityGraph(name = "GameMap.players", attributeNodes = @NamedAttributeNode("players"))
public class GameMap {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "map_id") // Foreign key in Player table to link back to the Map
	// by default, it is an empty arraylist
	private List<Player> players = new ArrayList<>();

	// Constructors
	public GameMap() {
	}

	public GameMap(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public GameMap(String name, List<Player> players) {
		this.name = name;
		this.players = players;
	}

	public GameMap(String name) {
		this.name = name;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	@Override
	public String toString() {
		return this.name; // Assuming the name field holds the map's name.
	}

	// Override equals method
	@ExcludeFromJacocoGeneratedReport
	  @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof GameMap)) return false;
	        GameMap gameMap = (GameMap) o;
	        return Objects.equals(id, gameMap.id);
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(id);
	    }
}
