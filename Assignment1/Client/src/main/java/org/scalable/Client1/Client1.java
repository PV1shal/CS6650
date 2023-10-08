package org.scalable.Client1;

import org.scalable.StaticVariables.StaticVariables;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client1 {

    private static void client(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) throws InterruptedException, IOException {
        Long startTime;

        IPAddr = IPAddr.trim();
        String boundary = UUID.randomUUID().toString();
        byte[] imageBytes = Files.readAllBytes(StaticVariables.imageFile.toPath());

        String CRLF = "\r\n";
        String requestBody = "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"profile\"" + CRLF +
                "Content-Type: application/json" + CRLF +
                CRLF +
                "{\"artist\":\"hi\",\"title\":\"hello\",\"year\":\"bye\"}" + CRLF +
                "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF;

        requestBody += new String(imageBytes, StandardCharsets.UTF_8) + CRLF;
        requestBody += "--" + boundary + "--" + CRLF;

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(IPAddr + StaticVariables.postPath))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(IPAddr + StaticVariables.getPath))
                .GET()
                .build();

        startTime = System.currentTimeMillis();

        List<Thread> initialThreads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int retryCount = 0;
                    boolean success = false;
                    while (!success && retryCount < 5) {
                        try {
                            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

                            // Check the response codes for errors
                            if (getResponse.statusCode() >= 400 || postResponse.statusCode() >= 400) {
                                retryCount++;
                            } else {
                                success = true;
                            }
                        } catch (Exception e) {
                            System.out.println("Request failed. Retrying...");
                            retryCount++;
                        }
                    }

                    if (!success) {
                        System.out.println("Request failed after 5 retries.");
                    }
                }
            });
            initialThreads.add(thread);
            thread.start();
        }

// Wait for all initial threads to finish
        for (Thread thread : initialThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<Thread> threads = new ArrayList<>();

        for (int group = 0; group < numThreadGroups; group++) {
            for (int i = 0; i < threadGroupSize; i++) {
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) {
                        int retryCount = 0;
                        boolean success = false;
                        while (!success && retryCount < 5) {
                            try {
                                // Get Request
                                Long requestGetStartTime = System.currentTimeMillis();
                                HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                                Long requestReceivedTime = System.currentTimeMillis();

                                if (getResponse.statusCode() >= 400) {
                                    retryCount++;
                                } else {
                                    success = true;
                                    Long requestGetEndTime = System.currentTimeMillis();
                                    List<Object> requestData = new ArrayList<>();
                                    requestData.add(requestGetStartTime);
                                    requestData.add("GET");
                                    requestData.add(requestGetEndTime - requestGetStartTime);
                                    requestData.add(getResponse.statusCode());
                                }
                            } catch (Exception e) {
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
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // Statistics
        long endTime = System.currentTimeMillis();
        double wallTime = (endTime - startTime) / 1000.0;
        int totalRequests = (threadGroupSize * numThreadGroups * 1000) + 1000;  // 1000 requests from initial thread pool

        System.out.println("\n");
        System.out.println("Total Requests: " + totalRequests + " requests");
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + totalRequests / wallTime + "req/sec");
    }

    public static void main(String[] args) throws URISyntaxException {
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