package org.scalable.Client2;

import com.opencsv.CSVWriter;
import org.scalable.StaticVariables;

import java.io.FileWriter;
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

public class Client2 {

    private static void client(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) throws InterruptedException, IOException {
        Long startTime;
        List<Long> latencyList = new ArrayList<>();
        List<List<Object>> requestDataList = new ArrayList<>();

        // CSV Report file Creation/Generation.

        FileWriter outputFile = new FileWriter(StaticVariables.file);
        CSVWriter writer = new CSVWriter(outputFile);
        writer.writeNext(StaticVariables.header);

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

        ExecutorService initialThreadPool = Executors.newFixedThreadPool(10);
        CountDownLatch initialLatch = new CountDownLatch(10);
        startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            initialThreadPool.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    int retryCount = 0;
                    boolean success = false;
                    while (!success && retryCount < 5) {
                        try {
                            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
//                            System.out.println(postResponse.body().toString());

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
                initialLatch.countDown();
            });
        }
        initialThreadPool.shutdown();
        initialLatch.await();

        ExecutorService threadPool = Executors.newFixedThreadPool(threadGroupSize);

        for (int group = 0; group < numThreadGroups; group++) {
            for (int i = 0; i < threadGroupSize; i++) {
                threadPool.submit(() -> {
                    for (int j = 0; j < 1000; j++) {
                        int retryCount = 0;
                        boolean success = false;
                        while (!success && retryCount < 5) {
                            try {
                                // Get Request
                                Long requestGetStartTime = System.currentTimeMillis();
                                HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                                Long requestRecievedTime = System.currentTimeMillis();

                                synchronized (latencyList) {
                                    latencyList.add(requestRecievedTime - requestGetStartTime);
                                }

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
                                    synchronized (requestDataList) {
                                        requestDataList.add(requestData);
                                    }
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
            }
            Thread.sleep(delay * 1000);
        }
        threadPool.shutdown();
        threadPool.awaitTermination(Integer.MAX_VALUE, java.util.concurrent.TimeUnit.MINUTES);

        // Statistics
        long endTime = System.currentTimeMillis();
        double sum = 0;
        double meanLatency;
        double ninetyNinePercentile = latencyList.get((int) (latencyList.size() * 0.99));
        double medianLatency = 0;
        double wallTime = (endTime - startTime) / 1000.0;
        int totalRequests = (threadGroupSize * numThreadGroups * 1000) + 1000;  // 1000 requests from initial thread pool
        double minResponseTime = Collections.min(latencyList);
        double maxResponseTime = Collections.max(latencyList);

        for (Long latency : latencyList) {
            sum += latency;
        }
        meanLatency = sum / latencyList.size();
        if (latencyList.size() % 2 == 0) {
            medianLatency = (latencyList.get(latencyList.size() / 2) + latencyList.get((latencyList.size() / 2) - 1)) / 2.0;
        } else {
            medianLatency = latencyList.get(latencyList.size() / 2);
        }

        double littleLaw = (threadGroupSize / meanLatency) * 1000;
        double averageThroughput = totalRequests / wallTime;
        double percentageDifference = (Math.abs((littleLaw - averageThroughput)) / ((littleLaw + averageThroughput) / 2)) * 100;

        System.out.println("\n");
        System.out.println("Total Requests: " + totalRequests + " requests");
        System.out.println("Mean Latency: " + meanLatency + " milliseconds");
        System.out.println("Median Latency: " + medianLatency + " milliseconds");
        System.out.println("99th Percentile Latency: " + ninetyNinePercentile + " milliseconds");
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("avg. Throughput: " + averageThroughput + " requests/second");
        System.out.println("min. Response Time: " + minResponseTime + " milliseconds");
        System.out.println("max. Response Time: " + maxResponseTime + " milliseconds");
        System.out.println("Little's Law: " + littleLaw + " requests");
        System.out.println("Percentage Difference: " + percentageDifference + "%");

        // Writing to CSV File
        for (List<Object> data : requestDataList) {
            String[] dataArr = new String[data.size()];
            for (int i = 0; i < data.size(); i++) {
                dataArr[i] = data.get(i).toString();
            }
            writer.writeNext(dataArr);
        }
        //Closing Files
        writer.close();
        outputFile.close();
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