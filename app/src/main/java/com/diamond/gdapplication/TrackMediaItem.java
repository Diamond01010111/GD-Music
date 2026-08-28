package com.diamond.gdapplication;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;

/** Converts app {@link Track} objects to Media3 items that can cross process boundaries. */
@UnstableApi
public final class TrackMediaItem {

    private static final String PLAYBACK_SCHEME = "gdtrack";
    private static final String KEY_PREFIX = "com.diamond.gdapplication.track.";
    private static final String KEY_ID = KEY_PREFIX + "id";
    private static final String KEY_SOURCE = KEY_PREFIX + "source";
    private static final String KEY_NAME = KEY_PREFIX + "name";
    private static final String KEY_ARTIST = KEY_PREFIX + "artist";
    private static final String KEY_ALBUM = KEY_PREFIX + "album";
    private static final String KEY_PIC_ID = KEY_PREFIX + "pic_id";
    private static final String KEY_LYRIC_ID = KEY_PREFIX + "lyric_id";
    private static final String KEY_AUDIO_URL = KEY_PREFIX + "audio_url";
    private static final String KEY_AUDIO_CACHED_AT = KEY_PREFIX + "audio_cached_at";
    private static final String KEY_PIC_URL = KEY_PREFIX + "pic_url";
    private static final String KEY_EXTERNAL = KEY_PREFIX + "external";

    private TrackMediaItem() {
    }

    public static MediaItem create(String mediaId, Track track) {
        Bundle extras = new Bundle();
        extras.putString(KEY_ID, value(track.id));
        extras.putString(KEY_SOURCE, value(track.source));
        extras.putString(KEY_NAME, value(track.name));
        extras.putString(KEY_ARTIST, value(track.artist));
        extras.putString(KEY_ALBUM, value(track.album));
        extras.putString(KEY_PIC_ID, value(track.picId));
        extras.putString(KEY_LYRIC_ID, value(track.lyricId));
        extras.putString(KEY_AUDIO_URL, value(track.audioUrl));
        extras.putLong(KEY_AUDIO_CACHED_AT, track.audioUrlCachedAt);
        extras.putString(KEY_PIC_URL, value(track.picUrl));
        extras.putBoolean(KEY_EXTERNAL, track.externalMetadata);

        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(extras);
        if (present(track.picUrl)) {
            metadata.setArtworkUri(Uri.parse(track.picUrl));
        }

        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(playbackUri(mediaId))
                .setMediaMetadata(metadata.build())
                .build();
    }

    @Nullable
    public static Track toTrack(@Nullable MediaItem item) {
        if (item == null || item.mediaMetadata.extras == null) {
            return null;
        }
        Bundle extras = item.mediaMetadata.extras;
        String id = extras.getString(KEY_ID, "");
        String name = extras.getString(KEY_NAME, "");
        if (id.isEmpty() || name.isEmpty()) {
            return null;
        }

        Track track = new Track(
                id,
                extras.getString(KEY_SOURCE, ""),
                name,
                extras.getString(KEY_ARTIST, ""),
                extras.getString(KEY_ALBUM, ""),
                extras.getString(KEY_PIC_ID, ""),
                extras.getString(KEY_LYRIC_ID, "")
        );
        track.audioUrl = extras.getString(KEY_AUDIO_URL, "");
        track.audioUrlCachedAt = extras.getLong(KEY_AUDIO_CACHED_AT, 0L);
        track.picUrl = extras.getString(KEY_PIC_URL, "");
        track.externalMetadata = extras.getBoolean(KEY_EXTERNAL, false);
        return track;
    }

    public static boolean isPlaybackUri(Uri uri) {
        return PLAYBACK_SCHEME.equals(uri.getScheme());
    }

    @Nullable
    public static String mediaIdFromPlaybackUri(Uri uri) {
        return uri.getQueryParameter("mediaId");
    }

    private static Uri playbackUri(String mediaId) {
        return new Uri.Builder()
                .scheme(PLAYBACK_SCHEME)
                .authority("play")
                .appendQueryParameter("mediaId", mediaId)
                .build();
    }

    private static boolean present(@Nullable String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }

    private static String value(@Nullable String value) {
        return value == null ? "" : value;
    }
}
