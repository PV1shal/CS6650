package Servlets;

import RabbitMQ.RabbitMQConnectionFactory;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.InsertOneResult;
import com.rabbitmq.client.*;
import models.AlbumInfo;
import models.Response;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@WebServlet(name = "Servlets.ReviewsServlet", value = "/Servlets.ReviewsServlet", loadOnStartup = 1)
public class ReviewsServlet extends HttpServlet {

    private final String QUEUE_NAME = "HW3";

    String connectionString = "mongodb://ec2-54-68-149-246.us-west-2.compute.amazonaws.com:27017";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("hw3");
    MongoCollection<Document> likes = database.getCollection("likes");
    Gson gson = new Gson();
    int numThreads = 20;
    int basicQos = 50000;

    @Override
    public void init() throws ServletException {
        super.init();

        Runnable runnable = () -> {
            try {
                    Channel channel = RabbitMQConnectionFactory.getNewConnection().createChannel();
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                            try {
                                handleDelivery(delivery);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                    };
                    channel.basicQos(basicQos);
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

        for(int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

    public void handleDelivery(Delivery delivery) throws UnsupportedEncodingException {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

        String[] messageParts = message.split(",");
        String id = messageParts[0];
        int value = Integer.parseInt(messageParts[1]);
        Document updateDocument;

        updateDocument = new Document("$inc", new Document("likes", value));
        likes.updateOne(new Document("id", id), updateDocument, new UpdateOptions().upsert(true));
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String url = request.getPathInfo();
        String[] urlParts = url.split("/");

        if (urlParts.length != 3 || (Objects.equals(urlParts[1], "") || Objects.equals(urlParts[2], ""))) {
            response.setStatus(400);
            response.getWriter().write(
                    gson.toJson(
                            new Response("Review Creation Failed", "Invalid URL")
                    ));
            return;
        }

        String id = urlParts[2];

        Channel channel = RabbitMQConnectionFactory.getChannel();

        if (urlParts[1].equals("like")) {
            String likeJson = id + ",1";
            channel.basicPublish("", QUEUE_NAME, null, likeJson.getBytes());
        } else {
            String dislikeJson = id + ",-1";
            channel.basicPublish("", QUEUE_NAME, null, dislikeJson.getBytes());
        }

        response.setStatus(201);
        response.getWriter().write(
                gson.toJson(
                        new Response("Review Created", "Write successful")
                ));
    }
}
