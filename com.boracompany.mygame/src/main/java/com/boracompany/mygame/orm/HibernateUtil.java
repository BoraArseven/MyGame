package com.boracompany.mygame.orm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.jpa.HibernatePersistenceProvider;

import com.boracompany.mygame.main.ExcludeFromJacocoGeneratedReport;
import com.boracompany.mygame.model.GameMap;
import com.boracompany.mygame.model.Player;

public class HibernateUtil {

	private static EntityManagerFactory entityManagerFactory;

	// Private constructor to prevent instantiation
	private HibernateUtil() {
	}

	// Static inner class for lazy initialization and thread safety

	// Double-checked locker pattern
	// Static method to initialize the EntityManagerFactory
	// We call whenever we want an emf, if there is no emf, it will create,
	// otherwise does nothing, we simply call emf after the call of this method.
	@ExcludeFromJacocoGeneratedReport
	public static void initialize(String dbUrl, String dbUser, String dbPassword) {
		if (entityManagerFactory == null) {
			// synchronized ensures that only one thread can access that at the same time.
			synchronized (HibernateUtil.class) {
				if (entityManagerFactory == null) {
					HibernateUtil.initializeInternal(dbUrl, dbUser, dbPassword);
				}
			}
		}
	}

	private static void initializeInternal(String dbUrl, String dbUser, String dbPassword) {
		Map<String, Object> properties = new HashMap<>();
		if (dbUrl == null || dbUser == null || dbPassword == null) {
			throw new IllegalArgumentException("Database connection properties must not be null");
		}

		// Extract the database name from the URL
		String cleanDbUrl = dbUrl.split("\\?")[0]; // Remove query parameters
		String databaseName = cleanDbUrl.substring(cleanDbUrl.lastIndexOf('/') + 1);
		String baseDbUrl = cleanDbUrl.substring(0, cleanDbUrl.lastIndexOf('/')) + "/postgres";

		// Check and create database if it doesn't exist
		createDatabaseIfNotExists(baseDbUrl, dbUser, dbPassword, databaseName);

		// Use standard JPA properties
		properties.put("jakarta.persistence.jdbc.url", dbUrl);
		properties.put("jakarta.persistence.jdbc.user", dbUser);
		properties.put("jakarta.persistence.jdbc.password", dbPassword);
		properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver"); // Include if necessary

		// Hibernate-specific properties

		properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		properties.put("hibernate.hbm2ddl.auto", "update");
		properties.put("hibernate.show_sql", "true");

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

	@ExcludeFromJacocoGeneratedReport
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

	private static PersistenceUnitInfo createPersistenceUnitInfo() {
		return new HibernatePersistenceUnitInfo("my-persistence-unit", Player.class, GameMap.class);
	}

	private static void createDatabaseIfNotExists(String baseDbUrl, String user, String password, String databaseName) {
		try (Connection conn = DriverManager.getConnection(baseDbUrl, user, password)) {
			if (!databaseExists(conn, databaseName)) {
				createDatabase(conn, databaseName);
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("Failed to create database: " + databaseName, e);
		}
	}

	private static boolean databaseExists(Connection conn, String databaseName) throws SQLException {
		String query = "SELECT 1 FROM pg_database WHERE datname = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setString(1, databaseName);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}

	private static void createDatabase(Connection conn, String databaseName) throws SQLException {
		// Validate the database name
		if (!isValidDatabaseName(databaseName)) {
			throw new IllegalArgumentException("Invalid database name: " + databaseName);
		}

		// Construct the SQL statement with the validated database name
		String sql = "CREATE DATABASE \"" + databaseName + "\"";

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		}
	}

	@ExcludeFromJacocoGeneratedReport
	private static boolean isValidDatabaseName(String databaseName) {
		// Safeguard for null, even if it’s considered unreachable
		if (databaseName == null) {
			return false;
		}

		// Ensure the database name length is between 1 and 64 characters
		if (databaseName.isEmpty() || databaseName.length() > 64) {
			return false;
		}

		// Allow only letters, digits, and underscores
		if (!databaseName.matches("^\\w+$")) {
			return false;
		}

		// Disallow SQL reserved keywords (optional but recommended)
		String lowerCaseName = databaseName.toLowerCase();
		Set<String> reservedKeywords = Set.of("select", "insert", "update", "delete", "create", "drop", "alter",
				"table", "database", "from", "where", "join", "on", "group", "by", "having", "order", "limit"
		// Add more keywords as needed
		);

		return !reservedKeywords.contains(lowerCaseName);
	}

}
