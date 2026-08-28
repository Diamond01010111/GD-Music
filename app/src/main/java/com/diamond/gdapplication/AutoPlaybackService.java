package com.diamond.gdapplication;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaLibraryService.LibraryParams;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionError;

import com.diamond.gdapplication.data.NeteasePlaylist;
import com.diamond.gdapplication.data.NeteasePlaylistCache;
import com.diamond.gdapplication.data.NeteasePlaylistRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Media browser and playback endpoint used by Android Auto.
 *
 * <p>Android Auto renders the media tree supplied here; it does not render the
 * application's Compose activities. The tree intentionally contains only the
 * two driving-safe top-level destinations requested by the app.</p>
 */
@UnstableApi
public final class AutoPlaybackService extends MediaLibraryService {

    private static final String ROOT_ID = "root";
    private static final String FAVORITES_ID = "favorites";
    private static final String NETEASE_ID = "netease_playlists";
    private static final String NETEASE_CREATED_ID = "netease_created";
    private static final String NETEASE_SUBSCRIBED_ID = "netease_subscribed";
    private static final String NETEASE_PLAYLIST_PREFIX = "netease_playlist:";
    private static final String NETEASE_TRACK_PREFIX = "netease_track:";
    private static final String PLAYLIST_PREFIX = "playlist:";
    private static final String TRACK_PREFIX = "track:";

    private ExoPlayer player;
    private MediaLibrarySession mediaLibrarySession;
    private LocalPlaylistStore playlistStore;
    private GdMusicApi musicApi;
    private NeteasePlaylistRepository neteaseRepository;
    private final Map<String, NeteasePlaylist> neteasePlaylists = new LinkedHashMap<>();
    private final Map<String, List<Track>> neteaseTracks = new LinkedHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        playlistStore = new LocalPlaylistStore(getApplicationContext());
        musicApi = new GdMusicApi();
        neteaseRepository = new NeteasePlaylistRepository();
        String neteaseUserId = NeteasePlaylistCache.savedUserId(this);
        rememberNeteasePlaylists(NeteasePlaylistCache.read(this, neteaseUserId));
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                true
        );
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
            if (NETEASE_CREATED_ID.equals(parentId)
                    || NETEASE_SUBSCRIBED_ID.equals(parentId)) {
                return loadNeteasePlaylistChildren(parentId, page, pageSize, params);
            }
            if (parentId.startsWith(NETEASE_PLAYLIST_PREFIX)) {
                return loadNeteaseTrackChildren(parentId, page, pageSize, params);
            }

            List<MediaItem> children = childrenFor(parentId);
            return immediatePagedResult(children, page, pageSize, params);
        }

        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                MediaLibrarySession session,
                MediaSession.ControllerInfo browser,
                String mediaId
        ) {
            MediaItem item = itemFor(mediaId);
            if (item == null) {
                return Futures.immediateFuture(
                        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                );
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
            if (NeteasePlaylistCache.savedUserId(this).isEmpty()) {
                items.add(messageItem(
                        "netease_user_hint",
                        getString(R.string.auto_netease_user_hint)
                ));
            } else {
                items.add(browsableItem(
                        NETEASE_CREATED_ID,
                        getString(R.string.auto_netease_created)
                ));
                items.add(browsableItem(
                        NETEASE_SUBSCRIBED_ID,
                        getString(R.string.auto_netease_subscribed)
                ));
            }
            return items;
        }

        if (parentId.startsWith(PLAYLIST_PREFIX)) {
            String playlistId = parentId.substring(PLAYLIST_PREFIX.length());
            LocalPlaylistStore.LocalPlaylist playlist = findPlaylist(playlistId);
            if (playlist == null) {
                return items;
            }
            for (int index = 0; index < playlist.tracks.size(); index++) {
                items.add(trackItem(
                        localTrackId(playlistId, index),
                        playlist.tracks.get(index),
                        false
                ));
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
        if (NETEASE_CREATED_ID.equals(mediaId)) {
            return browsableItem(NETEASE_CREATED_ID, getString(R.string.auto_netease_created));
        }
        if (NETEASE_SUBSCRIBED_ID.equals(mediaId)) {
            return browsableItem(
                    NETEASE_SUBSCRIBED_ID,
                    getString(R.string.auto_netease_subscribed)
            );
        }
        if (mediaId.startsWith(NETEASE_PLAYLIST_PREFIX)) {
            NeteasePlaylist playlist = neteasePlaylists.get(
                    mediaId.substring(NETEASE_PLAYLIST_PREFIX.length())
            );
            return playlist == null ? null : neteasePlaylistItem(playlist);
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
                : trackItem(location.mediaId, location.track, false);
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

    private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>
    loadNeteasePlaylistChildren(
            String sectionId,
            int page,
            int pageSize,
            @Nullable LibraryParams params
    ) {
        String userId = NeteasePlaylistCache.savedUserId(this);
        if (userId.isEmpty()) {
            return immediatePagedResult(
                    Collections.singletonList(messageItem(
                            "netease_user_hint",
                            getString(R.string.auto_netease_user_hint)
                    )),
                    page,
                    pageSize,
                    params
            );
        }

        // 手机端手动刷新后，车机下次打开板块即可读取最新缓存。
        rememberNeteasePlaylists(NeteasePlaylistCache.read(this, userId));

        if (!neteasePlaylists.isEmpty() && !NeteasePlaylistCache.shouldRefresh(this)) {
            return immediatePagedResult(
                    neteasePlaylistItems(sectionId, userId),
                    page,
                    pageSize,
                    params
            );
        }

        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future =
                SettableFuture.create();
        NeteasePlaylistCache.markRefreshAttempt(this);
        neteaseRepository.loadPublicPlaylistsForJava(
                userId,
                new NeteasePlaylistRepository.PlaylistsCallback() {
                    @Override
                    public void onSuccess(List<? extends NeteasePlaylist> playlists) {
                        List<NeteasePlaylist> playlistList = new ArrayList<>(playlists);
                        rememberNeteasePlaylists(playlistList);
                        NeteasePlaylistCache.save(
                                AutoPlaybackService.this,
                                userId,
                                playlistList
                        );
                        future.set(pagedResult(
                                neteasePlaylistItems(sectionId, userId),
                                page,
                                pageSize,
                                params
                        ));
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (!neteasePlaylists.isEmpty()) {
                            future.set(pagedResult(
                                    neteasePlaylistItems(sectionId, userId),
                                    page,
                                    pageSize,
                                    params
                            ));
                            return;
                        }
                        future.set(pagedResult(
                                Collections.singletonList(messageItem(
                                        "netease_load_error",
                                        getString(R.string.auto_netease_load_error)
                                )),
                                page,
                                pageSize,
                                params
                        ));
                    }
                }
        );
        return future;
    }

    private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>
    loadNeteaseTrackChildren(
            String parentId,
            int page,
            int pageSize,
            @Nullable LibraryParams params
    ) {
        String playlistId = parentId.substring(NETEASE_PLAYLIST_PREFIX.length());
        List<Track> cachedTracks = neteaseTracks.get(playlistId);
        if (cachedTracks != null) {
            return immediatePagedResult(
                    neteaseTrackItems(playlistId, cachedTracks),
                    page,
                    pageSize,
                    params
            );
        }

        SettableFuture<LibraryResult<ImmutableList<MediaItem>>> future =
                SettableFuture.create();
        neteaseRepository.loadPlaylistTracksForJava(
                playlistId,
                new NeteasePlaylistRepository.TracksCallback() {
                    @Override
                    public void onSuccess(List<? extends Track> tracks) {
                        List<Track> trackList = new ArrayList<>(tracks);
                        neteaseTracks.put(playlistId, trackList);
                        future.set(pagedResult(
                                neteaseTrackItems(playlistId, trackList),
                                page,
                                pageSize,
                                params
                        ));
                    }

                    @Override
                    public void onError(Throwable error) {
                        future.set(pagedResult(
                                Collections.singletonList(messageItem(
                                        "netease_tracks_error:" + playlistId,
                                        getString(R.string.auto_netease_tracks_error)
                                )),
                                page,
                                pageSize,
                                params
                        ));
                    }
                }
        );
        return future;
    }

    private List<MediaItem> neteasePlaylistItems(String sectionId, String userId) {
        boolean created = NETEASE_CREATED_ID.equals(sectionId);
        List<MediaItem> items = new ArrayList<>();
        for (NeteasePlaylist playlist : neteasePlaylists.values()) {
            if (playlist.isCreatedBy(userId) == created) {
                items.add(neteasePlaylistItem(playlist));
            }
        }
        return items;
    }

    private MediaItem neteasePlaylistItem(NeteasePlaylist playlist) {
        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .setTitle(playlist.getName())
                .setSubtitle(playlist.getTrackCount() + " 首歌曲")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST);
        if (isPresent(playlist.getCoverUrl())) {
            metadata.setArtworkUri(Uri.parse(playlist.getCoverUrl()));
        }
        return new MediaItem.Builder()
                .setMediaId(NETEASE_PLAYLIST_PREFIX + playlist.getId())
                .setMediaMetadata(metadata.build())
                .build();
    }

    private List<MediaItem> neteaseTrackItems(String playlistId, List<Track> tracks) {
        List<MediaItem> items = new ArrayList<>();
        for (int index = 0; index < tracks.size(); index++) {
            items.add(trackItem(
                    neteaseTrackId(playlistId, index),
                    tracks.get(index),
                    false
            ));
        }
        return items;
    }

    private MediaItem messageItem(String mediaId, String title) {
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        .build())
                .build();
    }

    private void rememberNeteasePlaylists(List<NeteasePlaylist> playlists) {
        neteasePlaylists.clear();
        for (NeteasePlaylist playlist : playlists) {
            neteasePlaylists.put(playlist.getId(), playlist);
        }
    }

    private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> immediatePagedResult(
            List<MediaItem> items,
            int page,
            int pageSize,
            @Nullable LibraryParams params
    ) {
        return Futures.immediateFuture(pagedResult(items, page, pageSize, params));
    }

    private LibraryResult<ImmutableList<MediaItem>> pagedResult(
            List<MediaItem> items,
            int page,
            int pageSize,
            @Nullable LibraryParams params
    ) {
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min(Math.max(page, 0) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());
        return LibraryResult.ofItemList(items.subList(fromIndex, toIndex), params);
    }

    private String localTrackId(String playlistId, int index) {
        return TRACK_PREFIX + playlistId + ":" + index;
    }

    private String neteaseTrackId(String playlistId, int index) {
        return NETEASE_TRACK_PREFIX + playlistId + ":" + index;
    }

    private MediaItem trackItem(String mediaId, Track track, boolean includeUri) {
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
                .setMediaId(mediaId)
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
            resolved.add(trackItem(location.mediaId, location.track, true));
            resolvePlayableItems(requested, index + 1, resolved, future);
            return;
        }

        GdMusicApi.TrackCallback callback = new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track track) {
                if (isPresent(track.audioUrl)) {
                    resolved.add(trackItem(location.mediaId, track, true));
                }
                resolvePlayableItems(requested, index + 1, resolved, future);
            }

            @Override
            public void onError(Exception error) {
                resolvePlayableItems(requested, index + 1, resolved, future);
            }
        };

        if (location.track.externalMetadata) {
            musicApi.resolveExternalTrack(location.track, 999, callback);
        } else {
            musicApi.getAudioUrl(location.track, 999, callback);
        }
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
        if (mediaId == null) {
            return null;
        }

        boolean netease = mediaId.startsWith(NETEASE_TRACK_PREFIX);
        String value;
        if (netease) {
            value = mediaId.substring(NETEASE_TRACK_PREFIX.length());
        } else if (mediaId.startsWith(TRACK_PREFIX)) {
            value = mediaId.substring(TRACK_PREFIX.length());
        } else {
            return null;
        }
        int separator = value.lastIndexOf(':');
        if (separator <= 0) {
            return null;
        }

        try {
            String playlistId = value.substring(0, separator);
            int index = Integer.parseInt(value.substring(separator + 1));
            if (netease) {
                List<Track> tracks = neteaseTracks.get(playlistId);
                if (tracks == null || index < 0 || index >= tracks.size()) {
                    return null;
                }
                return new TrackLocation(mediaId, tracks.get(index));
            }
            LocalPlaylistStore.LocalPlaylist playlist = findPlaylist(playlistId);
            if (playlist == null || index < 0 || index >= playlist.tracks.size()) {
                return null;
            }
            return new TrackLocation(mediaId, playlist.tracks.get(index));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isPresent(@Nullable String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }

    private static final class TrackLocation {
        final String mediaId;
        final Track track;

        TrackLocation(String mediaId, Track track) {
            this.mediaId = mediaId;
            this.track = track;
        }
    }
}
