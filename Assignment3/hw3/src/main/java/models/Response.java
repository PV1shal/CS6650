package models;

public class Response {
    private String header;
    private String message;
    private String ImageSize;

    public Response(String header, String message) {
        this.header = header;
        this.message = message;
    }

    public Response(String header, String message, String optional) {
        this.header = header;
        this.message = message;
        this.ImageSize = optional;
    }
}
