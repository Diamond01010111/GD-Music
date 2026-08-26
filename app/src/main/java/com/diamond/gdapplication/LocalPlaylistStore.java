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

    private final SharedPreferences preferences;

    public LocalPlaylistStore(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public boolean addToFavorite(Track track) {
        if (track == null
                || track.id == null
                || track.id.isEmpty()) {
            return false;
        }

        List<Track> tracks = getFavoriteTracks();

        for (Track existing : tracks) {
            if (existing.id.equals(track.id)
                    && existing.source.equals(track.source)) {
                return false;
            }
        }

        tracks.add(track);
        saveFavoriteTracks(tracks);

        return true;
    }

    public List<Track> getFavoriteTracks() {
        List<Track> tracks = new ArrayList<>();

        try {
            String raw = preferences.getString(
                    KEY_FAVORITES,
                    "[]"
            );

            JSONArray array = new JSONArray(raw);

            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.getJSONObject(index);

                Track track = new Track(
                        object.optString("id", ""),
                        object.optString("source", "netease"),
                        object.optString("name", ""),
                        object.optString("artist", ""),
                        object.optString("album", ""),
                        object.optString("picId", ""),
                        object.optString("lyricId", "")
                );

                track.picUrl =
                        object.optString("picUrl", "");

                tracks.add(track);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return tracks;
    }

    private void saveFavoriteTracks(List<Track> tracks) {
        try {
            JSONArray array = new JSONArray();

            for (Track track : tracks) {
                JSONObject object = new JSONObject();

                object.put("id", track.id);
                object.put("source", track.source);
                object.put("name", track.name);
                object.put("artist", track.artist);
                object.put("album", track.album);
                object.put("picId", track.picId);
                object.put("lyricId", track.lyricId);
                object.put(
                        "picUrl",
                        track.picUrl == null ? "" : track.picUrl
                );

                array.put(object);
            }

            preferences
                    .edit()
                    .putString(
                            KEY_FAVORITES,
                            array.toString()
                    )
                    .apply();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}