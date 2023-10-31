import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.*;
import java.util.List;

@WebServlet(name = "AlbumsServlet", value = "/AlbumsServlet")
public class AlbumsServlet extends HttpServlet {
    Gson gson = new Gson();
    int counter = 1;

    String connectionString = "mongodb://ec2-54-68-149-246.us-west-2.compute.amazonaws.com:27017";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("hw2");
    MongoCollection<Document> collection = database.getCollection("albums");
    private final String badConnectionError = new Gson().toJson(new Response("Bad Request", "Invalid Request"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getRequestURI();

        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
            return;
        }

        String[] urlPaths = urlPath.split("/");

        if (!isUrlValid(urlPaths)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
        } else {
            try {
                Document album = collection.find(new Document("_id", new ObjectId(urlPaths[3]))).first();
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(album));
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

        try {
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

            Document document = new Document("artist", album.getArtist()).append("title", album.getTitle()).append("year", album.getYear()).append("image", imageBytes);
            InsertOneResult result = collection.insertOne(document);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(
                    gson.toJson(
                            new Response("Album Created", "Album Created with ID: " + result.getInsertedId().toString(), "Image Size: " + imageBytes.length + " bytes")
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length >= 1;
    }
}