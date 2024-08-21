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

    public boolean isAlive() {
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
    
    // normally id should only be enough since it is primary key, but I wanted to guarantee the behaviour.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Player player = (Player) obj;
        return damage == player.damage &&
               health == player.health &&
               id == player.id&&
               Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, damage, health, id);
    }

}
