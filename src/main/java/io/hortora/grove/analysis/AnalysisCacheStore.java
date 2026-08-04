package io.hortora.grove.analysis;

import io.hortora.grove.config.GroveConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AnalysisCacheStore {

    private static final Logger LOG = Logger.getLogger(AnalysisCacheStore.class.getName());

    private final String dbPath;

    @Inject
    public AnalysisCacheStore(GroveConfig config) {
        this.dbPath = config.analysisCache().path();
    }

    public AnalysisCacheStore(String dbPath) {
        this.dbPath = dbPath;
    }

    @PostConstruct
    void init() {
        try (Connection conn = connect()) {
            Statement st = conn.createStatement();
            st.execute("DROP TABLE IF EXISTS duplicate_pairs");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS analysis_cache (
                        analysis_type TEXT NOT NULL,
                        domain        TEXT NOT NULL,
                        result_json   TEXT NOT NULL,
                        entry_count   INTEGER NOT NULL,
                        analysed_at   TEXT NOT NULL,
                        PRIMARY KEY (analysis_type, domain)
                    )""");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize grove.db", e);
        }
    }

    public void cache(String analysisType, String domain, String resultJson, int entryCount) {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO analysis_cache (analysis_type, domain, result_json, entry_count, analysed_at) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, analysisType);
            ps.setString(2, domain);
            ps.setString(3, resultJson);
            ps.setInt(4, entryCount);
            ps.setString(5, Instant.now().toString());
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cache analysis results", e);
        }
    }

    public CacheEntry getCached(String analysisType, String domain) {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT result_json, entry_count, analysed_at FROM analysis_cache WHERE analysis_type = ? AND domain = ?");
            ps.setString(1, analysisType);
            ps.setString(2, domain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new CacheEntry(
                        rs.getString("result_json"),
                        rs.getInt("entry_count"),
                        Instant.parse(rs.getString("analysed_at")));
            }
            return null;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to read cache", e);
            return null;
        }
    }

    public void clearDomain(String domain) {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM analysis_cache WHERE domain = ? OR (analysis_type = 'cross-domain' AND domain = '__all__')");
            ps.setString(1, domain);
            ps.execute();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to clear domain cache", e);
        }
    }

    public void clearAll() {
        try (Connection conn = connect()) {
            conn.createStatement().execute("DELETE FROM analysis_cache");
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to clear all cache", e);
        }
    }

    private Connection connect() throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) {}
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement st = conn.createStatement();
        st.execute("PRAGMA journal_mode=WAL");
        st.execute("PRAGMA busy_timeout=5000");
        return conn;
    }
}
