public class PostResponse {
    private String albumID;
    private String imageSize;

    public PostResponse(String newId, String imagesize) {
        this.albumID = newId;
        this.imageSize = imagesize;
    }
}
