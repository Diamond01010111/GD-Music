package com.diamond.gdapplication;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalPlaylistStore {

    private static final String PREF_NAME = "local_playlists";
    private static final String KEY_FAVORITES = "favorites";

    private final SharedPreferences prefs;

    public LocalPlaylistStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean addToFavorite(Track track) {
        if (track == null || track.id == null || track.id.isEmpty()) {
            return false;
        }

        List<Track> tracks = getFavoriteTracks();

        for (Track existing : tracks) {
            if (existing.id.equals(track.id) && existing.source.equals(track.source)) {
                return false; // 已经收藏过
            }
        }

        tracks.add(track);
        saveFavoriteTracks(tracks);
        return true;
    }

    public List<Track> getFavoriteTracks() {
        List<Track> tracks = new ArrayList<>();

        try {
            String raw = prefs.getString(KEY_FAVORITES, "[]");
            JSONArray arr = new JSONArray(raw);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                Track track = new Track(
                        obj.optString("id", ""),
                        obj.optString("source", "netease"),
                        obj.optString("name", ""),
                        obj.optString("artist", ""),
                        obj.optString("album", ""),
                        obj.optString("picId", ""),
                        obj.optString("lyricId", "")
                );

                track.picUrl = obj.optString("picUrl", "");

                tracks.add(track);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tracks;
    }

    private void saveFavoriteTracks(List<Track> tracks) {
        try {
            JSONArray arr = new JSONArray();

            for (Track track : tracks) {
                JSONObject obj = new JSONObject();

                obj.put("id", track.id);
                obj.put("source", track.source);
                obj.put("name", track.name);
                obj.put("artist", track.artist);
                obj.put("album", track.album);
                obj.put("picId", track.picId);
                obj.put("lyricId", track.lyricId);
                obj.put("picUrl", track.picUrl == null ? "" : track.picUrl);

                arr.put(obj);
            }

            prefs.edit()
                    .putString(KEY_FAVORITES, arr.toString())
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}