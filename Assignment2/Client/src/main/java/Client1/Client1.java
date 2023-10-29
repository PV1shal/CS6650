package Client1;

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
                .uri(java.net.URI.create(IPAddr + "albums/653d7f118b16dc446d86e911"))
                .GET()
                .build();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int retryCount = 0;
                    boolean success = false;
                    while (!success && retryCount < 5) {
                        try {
                            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                            if (getResponse.statusCode() == 200 && postResponse.statusCode() == 201) {
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

        startTime = System.currentTimeMillis();

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        threads = new ArrayList<>();

        for (int group = 0; group < numThreadGroups; group++) {
            for (int i = 0; i < threadGroupSize; i ++) {
                Thread thread = new Thread(() -> {
                   for (int j = 0; j < 1000; j++) {
                       int retryCount = 0;
                       boolean success = false;
                       while (!success && retryCount < 5) {
                            try {
                                 HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                                 HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                                 if (getResponse.statusCode() == 200
                                         && postResponse.statusCode() == 201
                                 ) {
                                      success = true;
                                 }
                            } catch (IOException | InterruptedException e) {
                                 retryCount++;
                            }
                       }
                       if (!success) {
                            System.out.println("Request failed after 5 retries.");
                            // Unsuccessful request counter here for client 2.
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

        int totalRequests = threadGroupSize * numThreadGroups * 1000 * 2 + 2000;
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
