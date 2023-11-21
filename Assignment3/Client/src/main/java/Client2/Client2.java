package Client2;

import Response.Response;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client2 {

    private static void client(int threadGroupSize, int numThreadGroups, int delay, String IPAddr) throws InterruptedException, IOException {
        Long startTime;
        IPAddr = IPAddr.trim();
        String boundary = UUID.randomUUID().toString();
        byte[] imageBytes = Files.readAllBytes(new File("./src/main/resources/nmtb.png").toPath());
        ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(IPAddr + "albums/"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"profile\"\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" +
                        "{\"artist\":\"hi\",\"title\":\"hello\",\"year\":1999}\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "\r\n" +
                        new String(imageBytes) + "\r\n" +
                        "--" + boundary + "--\r\n"))
                .build();

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(IPAddr + "albums/6541b69154de2b1a3adc76fa"))
                .GET()
                .build();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int retryCount = 0;
                    boolean success = false;
                    while (!success && retryCount < 5) {
                        try {
                            long postStartTime = System.currentTimeMillis();
                            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                            long postEndTime = System.currentTimeMillis();
                            long getStartTime = System.currentTimeMillis();
                            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                            long getEndTime = System.currentTimeMillis();
                            if (getResponse.statusCode() == 200 && postResponse.statusCode() == 201) {
                                success = true;
                            }
                            responses.add(new Response(postStartTime, "POST", postEndTime - postStartTime, postResponse.statusCode()));
                            responses.add(new Response(getStartTime, "GET", getEndTime - getStartTime, getResponse.statusCode()));
                        } catch (IOException | InterruptedException e) {
                            retryCount++;
                        }
                    }
                }
            });
            threads.add(thread);
        }

        startTime = System.currentTimeMillis();

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        threads = new ArrayList<>();

        for (int group = 0; group < numThreadGroups; group++) {
            for (int i = 0; i < threadGroupSize; i++) {
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) {
                        int retryCount = 0;
                        boolean success = false;
                        while (!success && retryCount < 5) {
                            try {
                                long postStartTime = System.currentTimeMillis();
                                HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                                long postEndTime = System.currentTimeMillis();
                                long getStartTime = System.currentTimeMillis();
                                HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                                long getEndTime = System.currentTimeMillis();
                                if (getResponse.statusCode() == 200 && postResponse.statusCode() == 201) {
                                    success = true;
                                }
                                responses.add(new Response(postStartTime, "POST", postEndTime - postStartTime, postResponse.statusCode()));
                                responses.add(new Response(getStartTime, "GET", getEndTime - getStartTime, getResponse.statusCode()));
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

        long[] postLatencies = responses.stream()
                .filter(r -> r.getRequestType().equals("POST"))
                .mapToLong(Response::getLatency)
                .sorted().toArray();

        long[] getLatencies = responses.stream()
                .filter(r -> r.getRequestType().equals("GET"))
                .mapToLong(Response::getLatency)
                .sorted().toArray();

        int totalRequests = responses.size();
        long wallTime = (endTime - startTime) / 1000;
        double totalLatency = responses.stream().mapToLong(Response::getLatency).sum();
        double meanLatency = responses.stream().mapToLong(Response::getLatency).average().orElse(0.0);
        double medianLatency = responses.stream().sorted((a, b) -> (int) (a.getLatency() - b.getLatency())).skip(totalRequests / 2).findFirst().orElse(new Response(0, "", 0, 0)).getLatency();
        double p99Latency = responses.stream().sorted((a, b) -> (int) (a.getLatency() - b.getLatency())).skip((int) (totalRequests * 0.99)).findFirst().orElse(new Response(0, "", 0, 0)).getLatency();
        double minResponseTime = responses.stream().mapToLong(Response::getLatency).min().orElse(0);
        double maxResponseTime = responses.stream().mapToLong(Response::getLatency).max().orElse(0);
        double minResponseTimePOST = responses.stream().filter(r -> r.getRequestType().equals("POST")).mapToLong(Response::getLatency).min().orElse(0);
        double maxResponseTimePOST = responses.stream().filter(r -> r.getRequestType().equals("POST")).mapToLong(Response::getLatency).max().orElse(0);
        double minResponseTimeGET = responses.stream().filter(r -> r.getRequestType().equals("GET")).mapToLong(Response::getLatency).min().orElse(0);
        double maxResponseTimeGET = responses.stream().filter(r -> r.getRequestType().equals("GET")).mapToLong(Response::getLatency).max().orElse(0);
        double successfulRequests = responses.stream().filter(r -> r.getStatusCode() == 200 || r.getStatusCode() == 201).count();
        double meanGetLatency = getLatencies.length > 0 ? (double) Arrays.stream(getLatencies).sum() / getLatencies.length : 0;
        double meanPostLatency = postLatencies.length > 0 ? (double) Arrays.stream(postLatencies).sum() / postLatencies.length : 0;
        double medianGetLatency = getLatencies.length > 0 ? getLatencies[(int) (getLatencies.length / 2)] : 0;
        double medianPostLatency = postLatencies.length > 0 ? postLatencies[(int) (postLatencies.length / 2)] : 0;
        double p99GetLatency = getLatencies.length > 0 ? getLatencies[(int) (getLatencies.length * 0.99)] : 0;
        double p99PostLatency = postLatencies.length > 0 ? postLatencies[(int) (postLatencies.length * 0.99)] : 0;

        System.out.println("Config Used: "+ "\nGroup size: " + threadGroupSize + ", Number of groups: " + numThreadGroups + ", Delay: " + delay + ", IP Address: " + IPAddr);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successfulRequests);
        System.out.println("Unsuccessful requests: " + (totalRequests - successfulRequests));
        System.out.println("\n\nOverall statistics:\n");
        System.out.println("Wall time: " + wallTime + " seconds");
        System.out.println("avg. Throughput: " + (double) totalRequests / wallTime + " requests/second");
        System.out.println("Mean total latency Total: " + meanLatency + " ms");
        System.out.println("Median total latency Total: " + medianLatency + " ms");
        System.out.println("99th percentile latency: " + p99Latency + " ms");
        System.out.println("Min response time: " + minResponseTime + " ms");
        System.out.println("Max response time: " + maxResponseTime + " ms\n\n");
        System.out.println("POST request statistics:\n");
        System.out.println("Mean POST latency: " + meanPostLatency + " ms");
        System.out.println("Median POST latency: " + medianPostLatency + " ms");
        System.out.println("99th percentile POST latency: " + p99PostLatency + " ms");
        System.out.println("Min response time POST: " + minResponseTimePOST + " ms");
        System.out.println("Max response time POST: " + maxResponseTimePOST + " ms\n\n");
        System.out.println("GET request statistics:\n");
        System.out.println("Mean GET latency: " + meanGetLatency + " ms");
        System.out.println("Median GET latency: " + medianGetLatency + " ms");
        System.out.println("99th percentile GET latency: " + p99GetLatency + " ms");
        System.out.println("Min response time GET: " + minResponseTimeGET + " ms");
        System.out.println("Max response time GET: " + maxResponseTimeGET + " ms");

        FileWriter outputFile = new FileWriter("./src/main/resources/Client2Output.csv");
        CSVWriter writer = new CSVWriter(outputFile);
        writer.writeNext(new String[]{"Start Time", "Request Type", "Latency", "Status Code"});

        for (Response response : responses) {
            writer.writeNext(new String[]{String.valueOf(response.getStartTime()), response.getRequestType().toString(), String.valueOf(response.getLatency()), String.valueOf(response.getStatusCode())});
        }

        writer.close();
        outputFile.close();
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
