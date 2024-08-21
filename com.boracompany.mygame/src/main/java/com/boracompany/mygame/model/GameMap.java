package com.boracompany.mygame.model;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_map") // Table name for the Map entity
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
}
