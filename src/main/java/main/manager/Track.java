package main.manager;

public class Track {
    private final String title;
    private final String duration;

    public Track(String title, String duration) {
        this.title = title;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }
}
