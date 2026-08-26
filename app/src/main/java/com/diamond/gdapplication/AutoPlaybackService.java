package com.diamond.gdapplication;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaLibraryService.LibraryParams;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Media browser and playback endpoint used by Android Auto.
 *
 * <p>Android Auto renders the media tree supplied here; it does not render the
 * application's Compose activities. The tree intentionally contains only the
 * two driving-safe top-level destinations requested by the app.</p>
 */
public final class AutoPlaybackService extends MediaLibraryService {

    private static final String ROOT_ID = "root";
    private static final String FAVORITES_ID = "favorites";
    private static final String NETEASE_ID = "netease_playlists";
    private static final String PLAYLIST_PREFIX = "playlist:";
    private static final String TRACK_PREFIX = "track:";

    private ExoPlayer player;
    private MediaLibrarySession mediaLibrarySession;
    private LocalPlaylistStore playlistStore;
    private GdMusicApi musicApi;

    @Override
    public void onCreate() {
        super.onCreate();
        playlistStore = new LocalPlaylistStore(getApplicationContext());
        musicApi = new GdMusicApi();
        player = new ExoPlayer.Builder(this).build();
        mediaLibrarySession = new MediaLibrarySession.Builder(
                this,
                player,
                new AutoLibraryCallback()
        ).setId("android_auto").build();
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaLibrarySession;
    }

    @Override
    public void onDestroy() {
        mediaLibrarySession.release();
        player.release();
        super.onDestroy();
    }

    private final class AutoLibraryCallback implements MediaLibrarySession.Callback {

        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                MediaLibrarySession session,
                MediaSession.ControllerInfo browser,
                @Nullable LibraryParams params
        ) {
            return Futures.immediateFuture(
                    LibraryResult.ofItem(
                            browsableItem(ROOT_ID, getString(R.string.app_name)),
                            params
                    )
            );
        }

        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                MediaLibrarySession session,
                MediaSession.ControllerInfo browser,
                String parentId,
                int page,
                int pageSize,
                @Nullable LibraryParams params
        ) {
            List<MediaItem> children = childrenFor(parentId);
            int fromIndex = Math.min(page * pageSize, children.size());
            int toIndex = Math.min(fromIndex + pageSize, children.size());

            return Futures.immediateFuture(
                    LibraryResult.ofItemList(children.subList(fromIndex, toIndex), params)
            );
        }

        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                MediaLibrarySession session,
                MediaSession.ControllerInfo browser,
                String mediaId
        ) {
            MediaItem item = itemFor(mediaId);
            if (item == null) {
                return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE));
            }
            return Futures.immediateFuture(LibraryResult.ofItem(item, null));
        }

        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(
                MediaSession mediaSession,
                MediaSession.ControllerInfo controller,
                List<MediaItem> mediaItems
        ) {
            if (mediaItems.isEmpty()) {
                return Futures.immediateFuture(ImmutableList.of());
            }

            SettableFuture<List<MediaItem>> result = SettableFuture.create();
            resolvePlayableItems(mediaItems, 0, new ArrayList<>(), result);
            return result;
        }
    }

    private List<MediaItem> childrenFor(String parentId) {
        List<MediaItem> items = new ArrayList<>();

        if (ROOT_ID.equals(parentId)) {
            items.add(browsableItem(FAVORITES_ID, getString(R.string.auto_favorites)));
            items.add(browsableItem(NETEASE_ID, getString(R.string.auto_netease_playlists)));
            return items;
        }

        if (FAVORITES_ID.equals(parentId)) {
            for (LocalPlaylistStore.LocalPlaylist playlist : playlistStore.getPlaylists()) {
                MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                        .setTitle(playlist.name)
                        .setSubtitle(playlist.tracks.size() + " 首歌曲")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST);

                Track cover = playlist.getCoverTrack();
                if (cover != null && isPresent(cover.picUrl)) {
                    metadata.setArtworkUri(Uri.parse(cover.picUrl));
                }

                items.add(new MediaItem.Builder()
                        .setMediaId(PLAYLIST_PREFIX + playlist.id)
                        .setMediaMetadata(metadata.build())
                        .build());
            }
            return items;
        }

        if (NETEASE_ID.equals(parentId)) {
            items.add(new MediaItem.Builder()
                    .setMediaId("netease_import_hint")
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(getString(R.string.auto_import_on_phone))
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .build())
                    .build());
            return items;
        }

        if (parentId.startsWith(PLAYLIST_PREFIX)) {
            String playlistId = parentId.substring(PLAYLIST_PREFIX.length());
            LocalPlaylistStore.LocalPlaylist playlist = findPlaylist(playlistId);
            if (playlist == null) {
                return items;
            }
            for (int index = 0; index < playlist.tracks.size(); index++) {
                items.add(trackItem(playlistId, index, playlist.tracks.get(index), false));
            }
        }

        return items;
    }

    @Nullable
    private MediaItem itemFor(String mediaId) {
        if (ROOT_ID.equals(mediaId)) {
            return browsableItem(ROOT_ID, getString(R.string.app_name));
        }
        if (FAVORITES_ID.equals(mediaId)) {
            return browsableItem(FAVORITES_ID, getString(R.string.auto_favorites));
        }
        if (NETEASE_ID.equals(mediaId)) {
            return browsableItem(NETEASE_ID, getString(R.string.auto_netease_playlists));
        }
        if (mediaId.startsWith(PLAYLIST_PREFIX)) {
            LocalPlaylistStore.LocalPlaylist playlist = findPlaylist(
                    mediaId.substring(PLAYLIST_PREFIX.length())
            );
            return playlist == null ? null : browsableItem(mediaId, playlist.name);
        }
        TrackLocation location = findTrack(mediaId);
        return location == null
                ? null
                : trackItem(location.playlistId, location.index, location.track, false);
    }

    private MediaItem browsableItem(String mediaId, String title) {
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                        .build())
                .build();
    }

    private MediaItem trackItem(String playlistId, int index, Track track, boolean includeUri) {
        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC);

        if (isPresent(track.picUrl)) {
            metadata.setArtworkUri(Uri.parse(track.picUrl));
        }

        MediaItem.Builder item = new MediaItem.Builder()
                .setMediaId(TRACK_PREFIX + playlistId + ":" + index)
                .setMediaMetadata(metadata.build());
        if (includeUri && isPresent(track.audioUrl)) {
            item.setUri(track.audioUrl);
        }
        return item.build();
    }

    private void resolvePlayableItems(
            List<MediaItem> requested,
            int index,
            List<MediaItem> resolved,
            SettableFuture<List<MediaItem>> future
    ) {
        if (index >= requested.size()) {
            future.set(resolved);
            return;
        }

        TrackLocation location = findTrack(requested.get(index).mediaId);
        if (location == null) {
            resolvePlayableItems(requested, index + 1, resolved, future);
            return;
        }

        if (isPresent(location.track.audioUrl)) {
            resolved.add(trackItem(location.playlistId, location.index, location.track, true));
            resolvePlayableItems(requested, index + 1, resolved, future);
            return;
        }

        musicApi.getAudioUrl(location.track, 999, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track track) {
                if (isPresent(track.audioUrl)) {
                    resolved.add(trackItem(location.playlistId, location.index, track, true));
                }
                resolvePlayableItems(requested, index + 1, resolved, future);
            }

            @Override
            public void onError(Exception error) {
                resolvePlayableItems(requested, index + 1, resolved, future);
            }
        });
    }

    @Nullable
    private LocalPlaylistStore.LocalPlaylist findPlaylist(String playlistId) {
        for (LocalPlaylistStore.LocalPlaylist playlist : playlistStore.getPlaylists()) {
            if (playlist.id.equals(playlistId)) {
                return playlist;
            }
        }
        return null;
    }

    @Nullable
    private TrackLocation findTrack(String mediaId) {
        if (mediaId == null || !mediaId.startsWith(TRACK_PREFIX)) {
            return null;
        }

        String value = mediaId.substring(TRACK_PREFIX.length());
        int separator = value.lastIndexOf(':');
        if (separator <= 0) {
            return null;
        }

        try {
            String playlistId = value.substring(0, separator);
            int index = Integer.parseInt(value.substring(separator + 1));
            LocalPlaylistStore.LocalPlaylist playlist = findPlaylist(playlistId);
            if (playlist == null || index < 0 || index >= playlist.tracks.size()) {
                return null;
            }
            return new TrackLocation(playlistId, index, playlist.tracks.get(index));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isPresent(@Nullable String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }

    private static final class TrackLocation {
        final String playlistId;
        final int index;
        final Track track;

        TrackLocation(String playlistId, int index, Track track) {
            this.playlistId = playlistId;
            this.index = index;
            this.track = track;
        }
    }
}
