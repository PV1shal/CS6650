import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.sql.*;
import java.util.List;

@WebServlet(name = "AlbumsServlet", value = "/AlbumsServlet")
public class AlbumsServlet extends HttpServlet {
    Gson gson = new Gson();

    private final String badConnectionError = new Gson().toJson(new Response("Bad Request", "Invalid Request"));
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getRequestURI();

        if(urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
            return;
        }

        String[] urlPaths = urlPath.split("/");

        if(!isUrlValid(urlPaths)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
        } else {
            try(Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement(Queries.sqlSelectQuery);
                statement.setInt(1, Integer.parseInt(urlPaths[3]));
                ResultSet resultSet = statement.executeQuery();
                AlbumInfo albumInfo = null;
                while (resultSet.next()) {
                    albumInfo = new AlbumInfo(
                            resultSet.getString("artist"),
                            resultSet.getString("title"),
                            resultSet.getInt("release_year"));
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(albumInfo));
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        int generatedKey;

        try (Connection connection = getConnection()) {
            List<FileItem> items = upload.parseRequest(request);
            byte[] imageBytes = new byte[0];
            AlbumInfo album = null;

            for (FileItem item : items) {
                switch (item.getFieldName()) {
                    case "profile": {
                        album = gson.fromJson(item.getString(), AlbumInfo.class);
                        break;
                    }
                    case "image": {
                        imageBytes = item.get();
                        break;
                    }
                    default:
                        break;
                }
            }

            PreparedStatement preparedStatement = connection.prepareStatement(Queries.sqlInsertQuery, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, album.getArtist());
            preparedStatement.setString(2, album.getTitle());
            preparedStatement.setInt(3, album.getYear());
            preparedStatement.setBlob(4, new ByteArrayInputStream(imageBytes));

            int affectedRows = preparedStatement.executeUpdate();

            if(affectedRows == 0) {
                throw new SQLException("Created Album Failed, no rows affected");
            }
            try(ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedKey = keys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
//            System.out.println("Key inserted: " + generatedKey);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(new Response("Album Created", "Album Created with ID: " + generatedKey, "Image Size: " + imageBytes.length + " bytes")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length >= 1;
    }


    public Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://albumdb.cfl1pikjmhpo.us-west-2.rds.amazonaws.com/hw2", "vishal", "Pstv!130619990809!");
    }
}