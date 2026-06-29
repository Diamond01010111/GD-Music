package com.diamond.gdapplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GdMusicApi {

    private final OkHttpClient client = new OkHttpClient();

    public interface SearchCallback {
        void onSuccess(List<Track> tracks);
        void onError(Exception e);
    }

    public interface TrackCallback {
        void onSuccess(Track track);
        void onError(Exception e);
    }

    public void searchTracks(String keywordRaw, SearchCallback callback) {
        searchTracks(keywordRaw, 5, 1, callback);
    }

    public void searchTracks(String keywordRaw, int count, int page, SearchCallback callback) {
        searchTracks(keywordRaw, "netease", count, page, callback);
    }

    public void searchTracks(String keywordRaw, String source, int count, int page, SearchCallback callback) {
        try {
            String keyword = URLEncoder.encode(keywordRaw, "UTF-8");

            String url = "https://music-api.gdstudio.xyz/api.php"
                    + "?types=search"
                    + "&source=" + source
                    + "&name=" + keyword
                    + "&count=" + count
                    + "&pages=" + page;

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";

                        if (!response.isSuccessful()) {
                            callback.onError(new Exception(
                                    "HTTP 错误：" + response.code()
                                            + "\n\n返回内容：\n"
                                            + body.substring(0, Math.min(body.length(), 300))
                            ));
                            return;
                        }

                        String trimmed = body.trim();

                        if (!trimmed.startsWith("[")) {
                            callback.onError(new Exception(
                                    "搜索接口没有返回 JSON 数组，可能是 API 挂了：\n\n"
                                            + trimmed.substring(0, Math.min(trimmed.length(), 300))
                            ));
                            return;
                        }

                        JSONArray arr = new JSONArray(trimmed);
                        List<Track> tracks = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.getJSONObject(i);

                            Track track = new Track(
                                    item.getString("id"),
                                    item.optString("source", source),
                                    item.optString("name", "未知歌曲"),
                                    cleanArtist(item.opt("artist").toString()),
                                    item.optString("album", ""),
                                    item.optString("pic_id", ""),
                                    item.optString("lyric_id", "")
                            );

                            tracks.add(track);
                        }

                        callback.onSuccess(tracks);

                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private String cleanArtist(String artistRaw) {
        if (artistRaw == null) {
            return "";
        }

        return artistRaw
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");
    }

    public void getAudioUrl(Track track, TrackCallback callback) {
        getAudioUrl(track, 320, callback);
    }

    public void getAudioUrl(Track track, int br, TrackCallback callback) {
        try {
            String idEncoded = URLEncoder.encode(track.id, "UTF-8");

            String url = "https://music-api.gdstudio.xyz/api.php"
                    + "?types=url"
                    + "&source=" + track.source
                    + "&id=" + idEncoded
                    + "&br=" + br;

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";

                        if (!response.isSuccessful()) {
                            callback.onError(new Exception("HTTP 错误：" + response.code()));
                            return;
                        }

                        JSONObject obj = parseObject(body);

                        track.audioUrl = obj.optString("url", "");

                        if (track.audioUrl != null
                                && !track.audioUrl.isEmpty()
                                && !track.audioUrl.equals("null")) {
                            track.audioUrlCachedAt = System.currentTimeMillis();
                        }

                        callback.onSuccess(track);

                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void getPicUrl(Track track, TrackCallback callback) {
        if (track.picId == null || track.picId.isEmpty() || track.picId.equals("null")) {
            callback.onSuccess(track);
            return;
        }

        try {
            String picIdEncoded = URLEncoder.encode(track.picId, "UTF-8");

            String url = "https://music-api.gdstudio.xyz/api.php"
                    + "?types=pic"
                    + "&source=" + track.source
                    + "&id=" + picIdEncoded
                    + "&size=500";

            Request request = new Request.Builder().url(url).get().build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        JSONObject obj = parseObject(body);

                        track.picUrl = obj.optString("url", "");

                        callback.onSuccess(track);

                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void getLyric(Track track, TrackCallback callback) {
        if (track.lyricId == null || track.lyricId.isEmpty() || track.lyricId.equals("null")) {
            callback.onSuccess(track);
            return;
        }

        try {
            String lyricIdEncoded = URLEncoder.encode(track.lyricId, "UTF-8");

            String url = "https://music-api.gdstudio.xyz/api.php"
                    + "?types=lyric"
                    + "&source=" + track.source
                    + "&id=" + lyricIdEncoded;

            Request request = new Request.Builder().url(url).get().build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        JSONObject obj = parseObject(body);

                        track.lyric = obj.optString("lyric", "");
                        track.translatedLyric = obj.optString("tlyric", "");

                        callback.onSuccess(track);

                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private JSONObject parseObject(String body) throws Exception {
        String trimmed = body.trim();

        if (trimmed.startsWith("[")) {
            JSONArray arr = new JSONArray(trimmed);
            if (arr.length() == 0) {
                return new JSONObject();
            }
            return arr.getJSONObject(0);
        }

        return new JSONObject(trimmed);
    }
}