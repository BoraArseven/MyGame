package com.boracompany.mygame.model;

//Builder class is not expected to be failed, so removed from coverage.
public class PlayerBuilder {

    private Player player;

    public PlayerBuilder() {
        this.player = new Player();
    }

    public Player build() {
        return player;
    }

    public PlayerBuilder withName(String name) {
        player.setName(name);
        return this;
    }

    public PlayerBuilder withHealth(float health) {
        player.setHealth(health);
        return this;
    }

    public PlayerBuilder withDamage(float damage) {
        player.setDamage(damage);
        return this;
    }

    public PlayerBuilder withIsAlive(boolean isAlive) {
        player.setAlive(isAlive);
        return this;
    }
    public PlayerBuilder resetBuilder() {
    	this.player = new Player();
    	return this;
    }
}