package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 数据库配置管理类
 * 负责从配置文件加载数据库连接信息
 */
public class DatabaseConfig {
    private static final String LOCAL_CONFIG_FILE = "/database-local.properties";
    private static final String DEFAULT_CONFIG_FILE = "/database.properties";
    
    private static DatabaseConfig instance;
    private Properties properties;
    
    private DatabaseConfig() {
        loadProperties();
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    private void loadProperties() {
        properties = new Properties();
        
        // 首先尝试加载本地配置文件
        InputStream localStream = getClass().getResourceAsStream(LOCAL_CONFIG_FILE);
        if (localStream != null) {
            try {
                properties.load(localStream);
                System.out.println("已加载本地数据库配置文件: " + LOCAL_CONFIG_FILE);
                return;
            } catch (IOException e) {
                System.err.println("加载本地配置文件失败: " + e.getMessage());
            } finally {
                try {
                    localStream.close();
                } catch (IOException e) {
                    System.err.println("关闭本地配置文件流失败: " + e.getMessage());
                }
            }
        }
        
        // 如果本地配置文件不存在，加载默认配置文件
        InputStream defaultStream = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE);
        if (defaultStream != null) {
            try {
                properties.load(defaultStream);
                System.out.println("已加载默认数据库配置文件: " + DEFAULT_CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("加载默认配置文件失败: " + e.getMessage());
                // 使用硬编码的默认值作为后备方案
                setDefaultValues();
            } finally {
                try {
                    defaultStream.close();
                } catch (IOException e) {
                    System.err.println("关闭默认配置文件流失败: " + e.getMessage());
                }
            }
        } else {
            System.err.println("未找到配置文件，使用默认值");
            setDefaultValues();
        }
    }
    
    private void setDefaultValues() {
        properties.setProperty("db.url", "jdbc:mysql://localhost:3306/");
        properties.setProperty("db.name", "farmer_happy");
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "");
        properties.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    }
    
    public String getUrl() {
        return properties.getProperty("db.url", "jdbc:mysql://localhost:3306/");
    }
    
    public String getDatabaseName() {
        return properties.getProperty("db.name", "farmer_happy");
    }
    
    public String getUsername() {
        return properties.getProperty("db.username", "root");
    }
    
    public String getPassword() {
        return properties.getProperty("db.password", "");
    }
    
    public String getDriver() {
        return properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    }
    
    public String getFullUrl() {
        return getUrl() + getDatabaseName();
    }
    
    public int getMaxConnections() {
        return Integer.parseInt(properties.getProperty("db.max.connections", "10"));
    }
    
    public int getMinConnections() {
        return Integer.parseInt(properties.getProperty("db.min.connections", "2"));
    }
    
    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("db.connection.timeout", "30000"));
    }
    
    /**
     * 重新加载配置文件
     */
    public void reload() {
        loadProperties();
    }
}
