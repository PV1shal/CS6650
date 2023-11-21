package Response;

public class Response {
    private long startTime;
    private String requestType;
    private long latency;
    private int statusCode;

    public Response(long startTime, String requestType, long latency, int statusCode) {
        this.startTime = startTime;
        this.requestType = requestType;
        this.latency = latency;
        this.statusCode = statusCode;
    }

    public long getLatency() {
        return latency;
    }

    public Object getRequestType() {
        return requestType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public char[] getStartTime() {
        return String.valueOf(startTime).toCharArray();
    }
}
