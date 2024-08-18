package com.boracompany.mygame.orm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

	// Private constructor to prevent instantiation
	private HibernateUtil() {
	}

	// Static inner class for lazy initialization and thread safety
	private static class SingletonHelper {
		private static final HibernateUtil INSTANCE = new HibernateUtil();
	}

	// Public method to provide access to the singleton instance
	public static HibernateUtil getInstance() {
		return SingletonHelper.INSTANCE;
	}

	//Double-checked locker pattern
	// Static method to initialize the EntityManagerFactory
	// We call whenever we want an emf, if there is no emf, it will create, otherwise does nothing, we simply call emf after the call of this method.
	public static void initialize(String dbUrl, String dbUser, String dbPassword) {
		if (entityManagerFactory == null) {
			// synchronized ensures that only one thread can access that at the same time.
			synchronized (HibernateUtil.class) {
				if (entityManagerFactory == null) {
					getInstance().initializeInternal(dbUrl, dbUser, dbPassword);
				}
			}
		}
	}

	private void initializeInternal(String dbUrl, String dbUser, String dbPassword) {
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
		if (entityManagerFactory == null) {
			throw new IllegalStateException("HibernateUtil is not initialized.");
		}
		return entityManagerFactory;
	}

	public static void close() {
		if (entityManagerFactory != null) {
			synchronized (HibernateUtil.class) {
				if (entityManagerFactory != null) {
					entityManagerFactory.close();
					entityManagerFactory = null;
				}
			}
		}
	}

	private PersistenceUnitInfo createPersistenceUnitInfo() {
		return new HibernatePersistenceUnitInfo("my-persistence-unit", Player.class, GameMap.class);
	}

	private void createDatabaseIfNotExists(String baseDbUrl, String user, String password, String databaseName) {
		try (Connection conn = DriverManager.getConnection(baseDbUrl, user, password)) {
			if (!databaseExists(conn, databaseName)) {
				createDatabase(conn, databaseName);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to create database: " + databaseName, e);
		}
	}

	private boolean databaseExists(Connection conn, String databaseName) throws SQLException {
		String query = "SELECT 1 FROM pg_database WHERE datname = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setString(1, databaseName);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}

	private void createDatabase(Connection conn, String databaseName) throws SQLException {
		// Validate the database name to ensure it only contains valid characters
		if (!isValidDatabaseName(databaseName)) {
			throw new IllegalArgumentException("Invalid database name: " + databaseName);
		}
		// Safely construct and execute the CREATE DATABASE statement
		String sql = "CREATE DATABASE " + databaseName;
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
		}
	}

	// \\w means [a-zA-Z0-9_]
	private boolean isValidDatabaseName(String databaseName) {
		return databaseName != null && databaseName.matches("^\\w{1,64}$");
	}
}
