package com.diamond.gdapplication;

public class Track {
    public String id;
    public String source;
    public String name;
    public String artist;
    public String album;
    public String picId;
    public String lyricId;

    public String audioUrl;
    public long audioUrlCachedAt = 0;
    public String picUrl;
    public String lyric;
    public String translatedLyric;

    public Track(String id, String source, String name, String artist, String album, String picId, String lyricId) {
        this.id = id;
        this.source = source;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.picId = picId;
        this.lyricId = lyricId;
    }
}