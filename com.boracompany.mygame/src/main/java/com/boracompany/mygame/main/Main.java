package com.boracompany.mygame.main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
	private static final Logger LOGGER = LogManager.getLogger(Main.class);
	// this will activate view in future
	public static void main(String[] args) {
		LOGGER.info("App started");
		LOGGER.info("Hello World!");
		LOGGER.info("App Terminated");
	}
}
