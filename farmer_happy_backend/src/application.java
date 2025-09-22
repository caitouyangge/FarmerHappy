import repository.DatabaseManager;
import java.sql.Connection;

public class application {

    public static void main(String[] args) {
        System.out.println("农乐助农平台后端服务启动中...");

        // 初始化数据库管理器
        DatabaseManager dbManager = new DatabaseManager();

        // 初始化数据库和表
        dbManager.initializeDatabase();


        System.out.println("服务已启动，等待请求...");

        
    }
}
