package com.boracompany.mygame.view;

import java.util.List;

import com.boracompany.mygame.model.Player;

public interface PlayerView {

	public void showError(String errorMessage, Player player);

	public void showAllPlayers(List<Player> players);

	void playerAdded(Player player);

	void playerRemoved(Player player);
}
