package models;

public class Reviews {
    private String id;
    private int likes;

    public Reviews(String id, int likes) {
        this.id = id;
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public int getLikes() {
        return likes;
    }
}
