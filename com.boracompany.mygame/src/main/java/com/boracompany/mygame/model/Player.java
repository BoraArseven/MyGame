package com.boracompany.mygame.model;

import java.util.Objects;

import javax.persistence.*;

@Entity
@Table(name = "app_player") // Table name for the Player entity
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name = "default";

	private float health = 10;

	private float damage;

	@Column(name = "is_alive") // Maps to the corresponding column in the database
	private boolean isAlive = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "map_id")
	private GameMap map;

	// Constructors
	public Player() {
	}

	public Player(String name, float health, float damage, boolean isAlive) {
		this.name = name;
		this.health = health;
		this.damage = damage;
		this.isAlive = isAlive;
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

	public float getHealth() {
		return health;
	}

	public void setHealth(float health) {
		this.health = health;
	}

	public float getDamage() {
		return damage;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

	public GameMap getMap() {
		return map;
	}

	public void setMap(GameMap map) {
		this.map = map;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Player player = (Player) obj;
		return Objects.equals(id, player.id); // Only compare by ID if present
	}

	@Override
	public int hashCode() {
		return Objects.hash(id); // Hash based on ID only
	}

	@Override
	public String toString() {
		return this.getName() + ", Health: " + this.getHealth() + ", Damage: " + this.getDamage();
	}
}
