package Servlets;

import RabbitMQ.RabbitMQConnectionFactory;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "Servlets.ReviewsServlet", value = "/Servlets.ReviewsServlet", loadOnStartup = 1)
public class ReviewsServlet extends HttpServlet {

    private final String QUEUE_NAME = "HW3";

    String connectionString = "mongodb://ec2-54-68-149-246.us-west-2.compute.amazonaws.com:27017";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("hw3");
    MongoCollection<Document> likes = database.getCollection("likes");
    Gson gson = new Gson();
    private ExecutorService executorService;

    @Override
    public void init() throws ServletException {
        super.init();
        int numThreads = 10;
//        executorService = Executors.newFixedThreadPool(numThreads);

        Runnable runnable = () -> {
            try {
                Channel channel = RabbitMQConnectionFactory.getConnection().createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    try {
                        handleDelivery(delivery);
//                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                };
                channel.basicQos(1000);
                channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
    }

    public void handleDelivery(Delivery delivery) throws UnsupportedEncodingException {
        String message = new String(delivery.getBody(), "UTF-8");
        Document doc = Document.parse(message);
        Document album = likes.find(new Document("albumId", doc.getString("id"))).first();
        if (album == null) {
            likes.insertOne(new Document("albumId", doc.getString("id")).append("likes", doc.getInteger("value")));
        } else {
            if (doc.getInteger("value") == 1) {
                likes.updateOne(
                        new Document("albumId", doc.getString("id")),
                        new Document("$inc", new Document("likes", 1))
                );
            } else {
                likes.updateOne(
                        new Document("albumId", doc.getString("id")),
                        new Document("$inc", new Document("likes", -1))
                );
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String url = request.getPathInfo();
        String[] urlParts = url.split("/");

        if (urlParts.length != 3 || (urlParts[1] == "" || urlParts[2] == "")) {
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
            String likeJson = "{\"id\":\"" + id + "\",\"value\":1}";
            channel.basicPublish("", QUEUE_NAME, null, likeJson.getBytes());
            channel.basicPublish("", QUEUE_NAME, null, likeJson.getBytes());
        } else {
            String dislikeJson = "{\"id\":\"" + id + "\",\"value\":-1}";
            channel.basicPublish("", QUEUE_NAME, null, dislikeJson.getBytes());
        }

        response.setStatus(201);
        response.getWriter().write(
                gson.toJson(
                        new Response("Review Created", "Write successful")
                ));

//        try {
//            channel.close();
//        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
//        }
    }
}
