package models;

public class AlbumInfo {
    private String artist;
    private String title;
    private int year;
    private int likes;

    public AlbumInfo(String artist, String title, int year, int likes) {
        this.artist = artist;
        this.title = title;
        this.year = year;
        this.likes = likes;
    }

    @Override
    public String toString() {
        return "models.AlbumInfo{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", likes='" + likes + '\'' +
                '}';
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
    public int getLikes() {
        return likes;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
