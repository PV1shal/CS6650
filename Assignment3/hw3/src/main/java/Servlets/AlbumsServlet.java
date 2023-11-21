package Servlets;

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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import models.AlbumInfo;
import models.Response;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "Servlets.AlbumsServlet", value = "/Servlets.AlbumsServlet")
public class AlbumsServlet extends HttpServlet {
    Gson gson = new Gson();
    int counter = 1;

    String connectionString = "mongodb://ec2-54-68-149-246.us-west-2.compute.amazonaws.com:27017";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("hw3");
    MongoCollection<Document> collection = database.getCollection("albums");
    private final String badConnectionError = new Gson().toJson(new Response("Bad Request", "Invalid Request"));
//
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        response.setContentType("application/json");
//        String urlPath = request.getRequestURI();
//
//        if (urlPath == null || urlPath.isEmpty()) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write(badConnectionError);
//            return;
//        }
//
//        String[] urlPaths = urlPath.split("/");
//
//        if (!isUrlValid(urlPaths)) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write(badConnectionError);
//        } else {
//            String QueueName = "HW3";
//            ConnectionFactory factory = new ConnectionFactory();
//            factory.setHost("ec2-35-92-45-106.us-west-2.compute.amazonaws.com");
//            factory.setPort(5672);
//            Connection connection = null;
//            try {
//                connection = factory.newConnection();
//            } catch (TimeoutException e) {
//                throw new RuntimeException(e);
//            }
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                String message = new String(delivery.getBody(), "UTF-8");
//                System.out.println(" [x] Received '" + message + "'");
//            };
//            Channel channel = connection.createChannel();
//            channel.basicConsume(QueueName, true, deliverCallback, consumerTag -> {
//                System.out.println(" [x] Received '" + consumerTag + "'");
//            });
//            try {
//                channel.close();
//                connection.close();
//            } catch (TimeoutException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

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

            Document document = new Document("artist", album.getArtist()).append("title", album.getTitle()).append("year", album.getYear()).append("likes", album.getLikes()).append("image", imageBytes);
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