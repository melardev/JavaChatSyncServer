package com.melardev.chat.db;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class JdbcService {

    private Connection conn;

    public boolean initH2Db() {
        try {
            Class.forName("org.h2.Driver");

            //conn = DriverManager.getConnection("jdbc:h2:file:C:\\Users\\melardev\\AppData\\Local\\Temp\\h2\\mydb2.db", "sa", "");
            //conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/D:/h2/mydb.db", "sa", "");
            //conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/D:/h2/mydb_server.db", "sa", "");

            String h2Path = "jdbc:h2:file:" + new File("").getAbsolutePath() + File.separator + "database/mydb.db;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1";
            conn = DriverManager.getConnection(h2Path, "sa", "");
            System.out.println("The database is located at " + h2Path);
            String sql = "DROP SCHEMA IF EXISTS chat_db";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            sql = "CREATE SCHEMA chat_db";
            stmt.execute(sql);

            stmt.execute("CREATE TABLE IF NOT EXISTS `chat_db`.`clients`"
                    + "( `id` INT NOT NULL AUTO_INCREMENT ,"
                    + "`username` VARCHAR(100) NOT NULL ,"
                    + "`password` VARCHAR(100) NOT NULL ,"
                    + "PRIMARY KEY (`id`))");

            // default credentials, bad security practice, but hey, it is a demo app...
            sql = String.format("INSERT INTO chat_db.clients(username, password)VALUES('admin', %s)",
                    new String(DigestUtils.sha1("password"), StandardCharsets.UTF_8));
            int insertedRows = stmt.executeUpdate(sql);
            ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next())
                System.out.println("Generated user" + rs.getInt(1));

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean initMySQLDb() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String userDb = System.getenv("DB_USER");
            if (userDb == null)
                userDb = "root";

            String passwordDb = System.getenv("DB_PASSWORD");
            if (passwordDb == null)
                passwordDb = "root";

            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306", userDb, passwordDb);

            String sql = "DROP DATABASE IF EXISTS chat_db";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            sql = "CREATE DATABASE chat_db";
            stmt.execute(sql);

            stmt.execute("CREATE TABLE IF NOT EXISTS `chat_db`.`clients`"
                    + "( `id` INT NOT NULL AUTO_INCREMENT ,"
                    + "`username` VARCHAR(100) NOT NULL ,"
                    + "`password` VARCHAR(100) NOT NULL ,"
                    + "PRIMARY KEY (`id`)) ENGINE = InnoDB");

            // default credentials, bad security practice, but hey, it is a demo app...
            sql = String.format("INSERT INTO chat_db.clients(username, password)VALUES('admin', '%s')",
                    DigestUtils.sha1Hex("password"));
            int insertedRows = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next())
                System.out.println("Generated user with id " + rs.getInt(1));

            stmt.execute("USE chat_db");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getHexStringFromBytes(byte[] passwords) {
        StringBuilder sb = new StringBuilder(passwords.length);
        for (byte c : passwords) {
            sb.append(Integer.toHexString((int) c & 0xff));
        }
        return sb.toString();
    }

    public boolean validCredentials(String username, String hashedPassword) {
        // conDB = DriverManager.getConnection("jdbc:mysql://localhost:3306/java_chat", userDb, passwordDb);

        // SQL injection
        // String sql = "select * from users where username='" + username + "' AND password='" + hashedPassword + "'";

        String sql = "select * from clients where username = ? AND password = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // SQL injection
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next())
                return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
