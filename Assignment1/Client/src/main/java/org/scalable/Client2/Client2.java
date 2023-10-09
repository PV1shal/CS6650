package org.scalable.Client2;

import com.opencsv.CSVWriter;
import org.scalable.StaticVariables.StaticVariables;

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

public class Client2 {

    private static void client(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) throws InterruptedException, IOException {
        Long startTime;
        List<Long> latencyListGET = new ArrayList<>();
        List<Long> latencyListPOST = new ArrayList<>();
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
                                // POST Request
                                Long reqyestPostStart = System.currentTimeMillis();
                                HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                                Long reqyestPostEnd = System.currentTimeMillis();

                                // GET Request
                                Long requestGetStartTime = System.currentTimeMillis();
                                HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                                Long requestReceivedTime = System.currentTimeMillis();

//                                synchronized (latencyList) {
//                                    latencyList.add(requestReceivedTime - requestGetStartTime);
//                                }

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


                                    List<Object> requestData1 = new ArrayList<>();
                                    requestData1.add(reqyestPostStart);
                                    requestData1.add("POST");
                                    requestData1.add(reqyestPostEnd - reqyestPostStart);
                                    requestData1.add(postResponse.statusCode());
//                                    synchronized (requestDataList) {
                                    requestDataList.add(requestData);
                                    requestDataList.add(requestData1);
//                                    }
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
        double sumGET = 0;
        double sumPOST = 0;
        double meanLatency;
        double medianLatency = 0;
        double wallTime = (endTime - startTime) / 1000.0;
        int totalRequests = (threadGroupSize * numThreadGroups * 2000) + 2000;  // 1000 requests from initial thread pool

        // Writing to CSV File
        for (List<Object> data : requestDataList) {
            String[] dataArr = new String[data.size()];
            long latency = (long) data.get(2);
            if(data.get(1).equals("GET")) {
                sumGET += latency;
                latencyListGET.add(latency);
            } else{
                sumPOST += latency;
                latencyListPOST.add(latency);
            }
            for (int i = 0; i < data.size(); i++) {
                dataArr[i] = data.get(i).toString();
            }
            writer.writeNext(dataArr);
        }

        double ninetyNinePercentile = latencyListGET.get((int) (latencyListGET.size() * 0.99));
        double minResponseTime = Collections.min(latencyListGET);
        double maxResponseTime = Collections.max(latencyListGET);
        meanLatency = sumGET / latencyListGET.size();

        if (latencyListGET.size() % 2 == 0) {
            medianLatency = (latencyListGET.get(latencyListGET.size() / 2) + latencyListGET.get((latencyListGET.size() / 2) - 1)) / 2.0;
        } else {
            medianLatency = latencyListGET.get(latencyListGET.size() / 2);
        }

        double ninetyNinePercentilePOST = latencyListPOST.get((int) (latencyListPOST.size() * 0.99));
        double minResponseTimePOST = Collections.min(latencyListPOST);
        double maxResponseTimePOST = Collections.max(latencyListPOST);
        double meanPostLatency = sumPOST / latencyListPOST.size();
        double medianLatencyPOST;

        if (latencyListGET.size() % 2 == 0) {
            medianLatencyPOST = (latencyListGET.get(latencyListGET.size() / 2) + latencyListGET.get((latencyListGET.size() / 2) - 1)) / 2.0;
        } else {
            medianLatencyPOST = latencyListGET.get(latencyListGET.size() / 2);
        }

        double averageThroughput = totalRequests / wallTime;

        System.out.println("\n");
        System.out.println("Total Requests: " + totalRequests + " requests");
        System.out.println("Mean GET Latency: " + meanLatency + " milliseconds");
        System.out.println("Median GET Latency: " + medianLatency + " milliseconds");
        System.out.println("Mean POST Latency: " + meanPostLatency + " milliseconds");
        System.out.println("Median POST Latency: " + medianLatencyPOST + " milliseconds");
        System.out.println("99th Percentile Latency: " + ninetyNinePercentilePOST + " milliseconds");
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("avg. Throughput: " + averageThroughput + " requests/second");
        System.out.println("min. GET Response Time: " + minResponseTime + " milliseconds");
        System.out.println("max. GET Response Time: " + maxResponseTime + " milliseconds");
        System.out.println("min. POST Response Time: " + minResponseTimePOST + " milliseconds");
        System.out.println("max. POST Response Time: " + maxResponseTimePOST + " milliseconds");


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