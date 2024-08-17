package com.boracompany.mygame.main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.boracompany.mygame.controller.GameController;

public class Main {
	private static final Logger LOGGER = LogManager.getLogger(GameController.class);
	// this will activate view in future
	public static void main(String[] args) {
		LOGGER.info("App started");
		System.out.println("Hello World!");
		LOGGER.info("App Terminated");
	}
}
