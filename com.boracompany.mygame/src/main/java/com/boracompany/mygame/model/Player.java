package com.boracompany.mygame.model;

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
    
    private boolean isalive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id")
    private GameMap map;

    // Constructors
    public Player() {}

    public Player(String name, float health, float damage, boolean isalive) {
        this.name = name;
        this.health = health;
        this.damage = damage;
        this.isalive = isalive;
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

    public boolean Isalive() {
        return isalive;
    }

    public void setAlive(boolean isalive) {
        this.isalive = isalive;
    }

    public GameMap getMap() {
        return map;
    }

    public void setMap(GameMap map) {
        this.map = map;
    }
}
