import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Queries {

//    public static final String sqlSelectQuery = "SELECT * FROM albums;";
    public static final String sqlSelectQuery = "SELECT * FROM albums WHERE album_id = ?;";
    public static final String sqlInsertQuery = "INSERT INTO albums (artist, title, release_year, image) VALUES (?, ?, ?, ?)";
}
