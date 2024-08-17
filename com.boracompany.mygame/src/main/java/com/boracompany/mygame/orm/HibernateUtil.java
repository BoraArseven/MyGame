package com.boracompany.mygame.orm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

public class HibernateUtil {

	private static EntityManagerFactory entityManagerFactory;

	public static void initialize(String dbUrl, String dbUser, String dbPassword) {
		Map<String, Object> properties = new HashMap<>();
		if (dbUrl == null || dbUser == null || dbPassword == null) {
			throw new RuntimeException("Database connection properties not set");
		}

		// Extract the database name from the URL
		String cleanDbUrl = dbUrl.split("\\?")[0]; // Remove query parameters
		String databaseName = cleanDbUrl.substring(cleanDbUrl.lastIndexOf('/') + 1);
		String baseDbUrl = cleanDbUrl.substring(0, cleanDbUrl.lastIndexOf('/')) + "/postgres";

		// Check and create database if it doesn't exist
		createDatabaseIfNotExists(baseDbUrl, dbUser, dbPassword, databaseName);

		properties.put(AvailableSettings.URL, dbUrl);
		properties.put(AvailableSettings.USER, dbUser);
		properties.put(AvailableSettings.PASS, dbPassword);
		properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
		properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		properties.put(AvailableSettings.SHOW_SQL, "true");

		// HikariCP settings
		properties.put("hibernate.hikari.connectionTimeout", "20000");
		properties.put("hibernate.hikari.minimumIdle", "10");
		properties.put("hibernate.hikari.maximumPoolSize", "20");
		properties.put("hibernate.hikari.idleTimeout", "300000");
		properties.put("hibernate.hikari.maxLifetime", "1800000");
		properties.put("hibernate.hikari.poolName", "MyHikariCP");

		entityManagerFactory = new HibernatePersistenceProvider()
				.createContainerEntityManagerFactory(createPersistenceUnitInfo(), properties);
	}

	public static EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public static void close() {
		entityManagerFactory.close();
	}

	private static PersistenceUnitInfo createPersistenceUnitInfo() {
		return new HibernatePersistenceUnitInfo("my-persistence-unit", Player.class, GameMap.class);
	}

	private static void createDatabaseIfNotExists(String baseDbUrl, String user, String password, String databaseName) {
		try (Connection conn = DriverManager.getConnection(baseDbUrl, user, password)) {
			if (!databaseExists(conn, databaseName)) {
				createDatabase(conn, databaseName);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to create database: " + databaseName, e);
		}
	}

	private static boolean databaseExists(Connection conn, String databaseName) throws SQLException {
		String query = "SELECT 1 FROM pg_database WHERE datname = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			// Set the databaseName parameter
			pstmt.setString(1, databaseName);

			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}

	// parameteried queries is not supported when creating databases
	private static void createDatabase(Connection conn, String databaseName) throws SQLException {
		// Basic input validation to ensure that the database name contains only allowed
		// characters
		if (!isValidDatabaseName(databaseName)) {
			throw new IllegalArgumentException("Invalid database name: " + databaseName);
		}

		// Safely construct and execute the CREATE DATABASE statement
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE DATABASE " + databaseName);
		}
	}

	// A helper method to validate the database name
	private static boolean isValidDatabaseName(String databaseName) {
		// Allow only alphanumeric characters and underscores
		return databaseName != null && databaseName.matches("[a-zA-Z0-9_]+");
	}
}
