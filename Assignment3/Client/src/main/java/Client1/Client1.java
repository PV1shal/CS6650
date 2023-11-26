package Client1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Client1 {

    private static void client(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) throws InterruptedException, IOException {
        Long startTime;
        IPAddr = IPAddr.trim();
        String boundary = UUID.randomUUID().toString();
        byte[] imageBytes = Files.readAllBytes(new File("./src/main/resources/nmtb.png").toPath());
//
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(IPAddr + "albums/"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"profile\"\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" +
                        "{\"artist\":\"hi\",\"title\":\"hello\",\"year\":1999,\"likes\":0}\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "\r\n" +
                        new String(imageBytes) + "\r\n" +
                        "--" + boundary + "--\r\n"))
                .build();

        List<Thread> threads = new ArrayList<>();

        //RabbitMQ

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    int retryCount = 0;
                    boolean success = false;
                    while (!success && retryCount < 5) {
                        try {
                            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                            if (postResponse.statusCode() == 201) {
                                success = true;
                            }
                        } catch (IOException | InterruptedException e) {
                            retryCount++;
                        }
                    }
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        startTime = System.currentTimeMillis();

        threads = new ArrayList<>();

        for (int group = 0; group < numThreadGroups; group++) {
            for (int i = 0; i < threadGroupSize; i++) {
                String finalIPAddr = IPAddr;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        int retryCount = 0;
                        boolean success = false;
                        while (!success && retryCount < 5) {
                            try {
                                HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                                JsonObject responseBody = JsonParser.parseString(postResponse.body()).getAsJsonObject();
                                String message = responseBody.get("message").getAsString();
                                String id = message.substring(message.indexOf("{value=") + 7, message.length() - 1);

                                // HTTP POST request to /reviews/{likeordislike}/{id}
                                HttpRequest likeRequest = HttpRequest.newBuilder()
                                        .uri(java.net.URI.create(finalIPAddr + "reviews/like/" + id))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(""))
                                        .build();
                                HttpRequest disLikeRequest = HttpRequest.newBuilder()
                                        .uri(java.net.URI.create(finalIPAddr + "reviews/like/" + id))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(""))
                                        .build();
                                HttpResponse<String> reviewPostResponse = httpClient.send(likeRequest, HttpResponse.BodyHandlers.ofString());
                                HttpResponse<String> reviewPostResponse1 = httpClient.send(likeRequest, HttpResponse.BodyHandlers.ofString());
                                HttpResponse<String> reviewPostResponse2 = httpClient.send(disLikeRequest, HttpResponse.BodyHandlers.ofString());

                                if (postResponse.statusCode() == 201
                                        && reviewPostResponse.statusCode() == 201
                                        && reviewPostResponse1.statusCode() == 201
                                        && reviewPostResponse2.statusCode() == 201) {
                                    success = true;
                                }
                            } catch (IOException | InterruptedException e) {
                                retryCount++;
                            }
                        }
                        if (!success) {
                            System.out.println("Request failed after 5 retries.");
                        }
                    }
                });
                threads.add(thread);
                thread.start();
            }
            Thread.sleep(delay * 1000);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Long endTime = System.currentTimeMillis();

        int totalRequests = threadGroupSize * numThreadGroups * 100 * 4 + 100;
        long wallTime = (endTime - startTime) / 1000;

        System.out.println("Total requests: " + totalRequests);
        System.out.println("Wall time: " + wallTime + " seconds");
        System.out.println("Throughput: " + (double) totalRequests / wallTime + " requests/second");
    }

    public static void main(String[] args) {
        int threadGroupSize, numThreadGroups, delay;
        String IPAddr;

        System.out.println("Please enter threadGroupSize = N, numThreadGroups = N, delay = N (seconds), and IPAddr = server URI");
        Scanner inputs = new Scanner(System.in);

        threadGroupSize = inputs.nextInt();
        numThreadGroups = inputs.nextInt();
        delay = inputs.nextInt();
        IPAddr = inputs.nextLine();

        try {
            client(threadGroupSize, numThreadGroups, delay, IPAddr);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
