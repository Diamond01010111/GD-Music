package com.diamond.gdapplication;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import androidx.media3.common.Player;



public class MusicController {

    public enum PlayMode {
        LIST_LOOP,
        SINGLE_LOOP,
        RANDOM
    }

    public interface Listener {
        void onStatusChanged(String message);
        void onStatusAppend(String message);
        void onModeChanged(String modeName);

        default void onTrackChanged(Track track) {
        }

        default void onPlayingChanged(boolean isPlaying) {
        }

        default void onQueueChanged(
                List<Track> tracks,
                int currentIndex
        ) {
        }

        default void onPlayModeChanged(PlayMode playMode) {
        }

        default void onTrackCleared() {
        }
    }

    private final GdMusicApi api;
    private final PlayerManager playerManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<Track> playlist = new ArrayList<>();
    private final Random random = new Random();

    private int currentIndex = -1;
    private PlayMode playMode = PlayMode.LIST_LOOP;

    private Listener listener;
    private int audioQuality = 999;
    private boolean showDetailedInfo = false;

    public MusicController(GdMusicApi api, PlayerManager playerManager) {
        this.api = api;
        this.playerManager = playerManager;

        this.playerManager.setPlaybackEventListener(() -> {
            mainHandler.post(this::handleTrackEnded);
        });

        playerManager.getPlayer().addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onPlayingChanged(isPlaying);
                    }
                });
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void searchAndPlayFirst(String keyword) {
        setStatus("正在搜索：" + keyword);

        api.searchTracks(keyword, new GdMusicApi.SearchCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                if (tracks == null || tracks.isEmpty()) {
                    setStatus("没有搜索结果");
                    return;
                }

                playlist.clear();
                playlist.addAll(tracks);
                currentIndex = 0;

                Track firstTrack = playlist.get(currentIndex);

                setStatus(
                        "搜索成功，共 " + playlist.size() + " 首\n\n"
                                + "准备播放第一首：\n"
                                + firstTrack.name + "\n"
                                + firstTrack.artist + "\n"
                                + firstTrack.album + "\n\n"
                                + "trackId = " + firstTrack.id + "\n"
                                + "picId = " + firstTrack.picId + "\n"
                                + "lyricId = " + firstTrack.lyricId + "\n\n"
                                + "正在获取播放 URL..."
                );

                playTrackAtCurrentIndex();
            }

            @Override
            public void onError(Exception e) {
                setStatus("搜索失败：\n" + e.getMessage());
            }
        });
    }

    public void setPlaylistAndPlay(List<Track> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty()) {
            setStatus("播放队列为空");
            return;
        }

        if (startIndex < 0 || startIndex >= tracks.size()) {
            setStatus("选择的歌曲序号不合法：" + startIndex);
            return;
        }

        playlist.clear();
        playlist.addAll(tracks);
        currentIndex = startIndex;

        playTrackAtCurrentIndex();
    }

    public void playNext() {
        if (playlist.isEmpty()) {
            setStatus("播放队列为空，请先搜索歌曲");
            return;
        }

        if (playMode == PlayMode.RANDOM) {
            playRandomTrack();
            return;
        }

        currentIndex++;

        if (currentIndex >= playlist.size()) {
            currentIndex = 0;
        }

        playTrackAtCurrentIndex();
    }

    public void addToPlayNext(Track track) {
        if (track == null) {
            setStatus("歌曲为空，无法加入下一首播放");
            return;
        }

        // 当前没有播放队列时，直接播放这首歌
        if (playlist.isEmpty() || currentIndex < 0) {
            playlist.clear();
            playlist.add(track);
            currentIndex = 0;

            // 通知 Compose 刷新播放列表
            notifyQueueChanged();

            setStatus(
                    "播放队列为空，直接播放：\n"
                            + track.name
                            + "\n"
                            + track.artist
            );

            playTrackAtCurrentIndex();
            return;
        }

        int insertIndex = currentIndex + 1;

        if (insertIndex > playlist.size()) {
            insertIndex = playlist.size();
        }

        playlist.add(insertIndex, track);

        // 就加在这里：歌曲成功插入之后
        notifyQueueChanged();

        appendStatus(
                "\n\n已加入下一首播放：\n"
                        + track.name
                        + "\n"
                        + track.artist
        );
    }

    public void playPrevious() {
        if (playlist.isEmpty()) {
            setStatus("播放队列为空，请先搜索歌曲");
            return;
        }

        currentIndex--;

        if (currentIndex < 0) {
            currentIndex = playlist.size() - 1;
        }

        playTrackAtCurrentIndex();
    }

    public void playOrPause() {
        playerManager.playOrPause();

        if (playerManager.isPlaying()) {
            appendStatus("\n\n继续播放");
        } else {
            appendStatus("\n\n已暂停");
        }
    }

    public void setPlayMode(PlayMode newMode) {
        if (newMode == null) {
            return;
        }

        playMode = newMode;

        // 保存快照，避免异步通知时值发生改变
        PlayMode modeSnapshot = playMode;
        String modeNameSnapshot = getPlayModeName();

        mainHandler.post(() -> {
            if (listener != null) {
                listener.onModeChanged(modeNameSnapshot);
                listener.onPlayModeChanged(modeSnapshot);
                listener.onStatusAppend(
                        "\n\n已切换播放模式："
                                + modeNameSnapshot
                );
            }
        });
    }

    public void switchPlayMode() {
        PlayMode nextMode;

        if (playMode == PlayMode.LIST_LOOP) {
            nextMode = PlayMode.SINGLE_LOOP;
        } else if (playMode == PlayMode.SINGLE_LOOP) {
            nextMode = PlayMode.RANDOM;
        } else {
            nextMode = PlayMode.LIST_LOOP;
        }

        // 必须通过 setPlayMode 修改
        setPlayMode(nextMode);
    }

    private void playTrackAtCurrentIndex() {
        if (playlist.isEmpty()) {
            setStatus("播放队列为空");
            return;
        }

        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            setStatus("currentIndex 不合法：" + currentIndex);
            return;
        }

        Track track = playlist.get(currentIndex);
        notifyTrackChanged(track);
        notifyQueueChanged();

        if (hasValidCachedAudioUrl(track)) {
            mainHandler.post(() -> {
                setStatusDirect(
                        "使用缓存播放：\n"
                                + track.name + "\n"
                                + track.artist + "\n"
                                + track.album + "\n\n"
                                + "当前序号：" + (currentIndex + 1) + " / " + playlist.size() + "\n"
                                + "播放模式：" + getPlayModeName()
                );

                playerManager.playTrack(track);
            });

            return;
        }

        setStatus(
                "当前播放队列：" + playlist.size() + " 首\n"
                        + "当前序号：" + (currentIndex + 1) + " / " + playlist.size() + "\n"
                        + "播放模式：" + getPlayModeName() + "\n\n"
                        + "正在获取播放 URL：\n"
                        + track.name + "\n"
                        + track.artist + "\n"
                        + track.album + "\n\n"
                        + "trackId = " + track.id + "\n"
                        + "picId = " + track.picId + "\n"
                        + "lyricId = " + track.lyricId
        );

        api.getAudioUrl(track, audioQuality, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                if (updatedTrack.audioUrl == null
                        || updatedTrack.audioUrl.isEmpty()
                        || updatedTrack.audioUrl.equals("null")) {
                    setStatus(
                            "没有拿到播放 URL：\n"
                                    + updatedTrack.name + "\n"
                                    + updatedTrack.artist
                    );
                    return;
                }

                mainHandler.post(() -> {
                    setStatusDirect(buildNowPlayingText(updatedTrack));
                    playerManager.playTrack(updatedTrack);
                });

                requestPicAndLyric(updatedTrack);
            }

            @Override
            public void onError(Exception e) {
                setStatus("获取播放 URL 失败：\n" + e.getMessage());
            }
        });
    }

    private String buildNowPlayingText(Track track) {
        String text =
                "正在播放：\n"
                        + track.name + "\n"
                        + track.artist + "\n"
                        + track.album + "\n\n"
                        + "当前序号：" + (currentIndex + 1) + " / " + playlist.size() + "\n"
                        + "播放模式：" + getPlayModeName() + "\n"
                        + "音质：" + audioQuality;

        if (showDetailedInfo) {
            text += "\n\n"
                    + "trackId = " + track.id + "\n"
                    + "picId = " + track.picId + "\n"
                    + "lyricId = " + track.lyricId + "\n\n"
                    + "audioUrl:\n"
                    + track.audioUrl;
        }

        return text;
    }

    private void requestPicAndLyric(Track track) {
        api.getPicUrl(track, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                notifyTrackChanged(updatedTrack);

                if (updatedTrack.picUrl != null
                        && !updatedTrack.picUrl.isEmpty()) {
                    appendStatus(
                            "\n\n专辑图 URL:\n" + updatedTrack.picUrl
                    );
                }
            }

            @Override
            public void onError(Exception e) {
                appendStatus("\n\n获取专辑图失败：" + e.getMessage());
            }
        });

        api.getLyric(track, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                int lyricLength = updatedTrack.lyric == null ? 0 : updatedTrack.lyric.length();
                int translatedLength = updatedTrack.translatedLyric == null ? 0 : updatedTrack.translatedLyric.length();

                appendStatus("\n\n歌词已获取");
                appendStatus("\n原文歌词长度：" + lyricLength);
                appendStatus("\n翻译歌词长度：" + translatedLength);
            }

            @Override
            public void onError(Exception e) {
                appendStatus("\n\n获取歌词失败：" + e.getMessage());
            }
        });
    }

    private void handleTrackEnded() {
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size()) {
            return;
        }

        if (playMode == PlayMode.SINGLE_LOOP) {
            playerManager.replayCurrentTrack();
            return;
        }

        if (playMode == PlayMode.RANDOM) {
            playRandomTrack();
            return;
        }

        currentIndex++;

        if (currentIndex >= playlist.size()) {
            currentIndex = 0;
        }

        playTrackAtCurrentIndex();
    }

    private void playRandomTrack() {
        if (playlist.isEmpty()) {
            setStatus("播放队列为空");
            return;
        }

        if (playlist.size() == 1) {
            currentIndex = 0;
            playTrackAtCurrentIndex();
            return;
        }

        int nextIndex;

        do {
            nextIndex = random.nextInt(playlist.size());
        } while (nextIndex == currentIndex);

        currentIndex = nextIndex;
        playTrackAtCurrentIndex();
    }

    private boolean hasValidCachedAudioUrl(Track track) {
        if (track.audioUrl == null || track.audioUrl.isEmpty() || track.audioUrl.equals("null")) {
            return false;
        }

        long now = System.currentTimeMillis();
        long maxAge = 30 * 60 * 1000; // 30 分钟

        return now - track.audioUrlCachedAt < maxAge;
    }


    public String getPlayModeName() {
        if (playMode == PlayMode.SINGLE_LOOP) {
            return "单曲循环";
        }

        if (playMode == PlayMode.RANDOM) {
            return "随机播放";
        }

        return "自动循环";
    }

    public Track getCurrentTrack() {
        if (
                currentIndex < 0 ||
                currentIndex >= playlist.size()
        ) {
            return null;
        }

        return playlist.get(currentIndex);
    }

    private void setStatus(String message) {
        mainHandler.post(() -> setStatusDirect(message));
    }

    private void setStatusDirect(String message) {
        if (listener != null) {
            listener.onStatusChanged(message);
        }
    }

    private void appendStatus(String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onStatusAppend(message));
        }
    }

    public List<Track> getPlaylistSnapshot() {
        return new ArrayList<>(playlist);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }


    public void playAtIndex(int index) {
        if (playlist.isEmpty()) {
            setStatus("播放队列为空");
            return;
        }

        if (index < 0 || index >= playlist.size()) {
            setStatus("歌曲序号不合法：" + index);
            return;
        }

        currentIndex = index;
        playTrackAtCurrentIndex();
    }

    public void addToPlaylist(Track track) {
        if (track == null) {
            setStatus("歌曲为空，无法加入播放列表");
            return;
        }

        playlist.add(track);

        notifyQueueChanged();

        appendStatus(
                "\n\n已加入播放列表：\n"
                        + track.name
                        + "\n"
                        + track.artist
        );
    }

    public void setAudioQuality(int audioQuality) {
        this.audioQuality = audioQuality;

        // 音质改了以后，已有 audioUrl 可能还是旧音质，简单做法：清掉缓存
        for (Track track : playlist) {
            track.audioUrl = "";
            track.audioUrlCachedAt = 0;
        }

        setStatus("已切换音质：" + audioQuality);
    }

    public int getAudioQuality() {
        return audioQuality;
    }

    public void setShowDetailedInfo(boolean showDetailedInfo) {
        this.showDetailedInfo = showDetailedInfo;
        setStatus("显示详细歌曲信息：" + (showDetailedInfo ? "开启" : "关闭"));
    }

    public boolean isShowDetailedInfo() {
        return showDetailedInfo;
    }

    private void notifyTrackChanged(Track track) {
        if (listener != null) {
            mainHandler.post(() -> {
                listener.onTrackChanged(track);
            });
        }
    }

    private void notifyQueueChanged() {
        if (listener != null) {
            List<Track> snapshot = new ArrayList<>(playlist);
            int indexSnapshot = currentIndex;

            mainHandler.post(() -> {
                listener.onQueueChanged(
                        snapshot,
                        indexSnapshot
                );
            });
        }
    }

    public void clearPlaylist() {
        playlist.clear();
        currentIndex = -1;

        playerManager.stopAndClear();

        notifyQueueChanged();
        notifyTrackCleared();

        setStatus("播放列表已清空");
    }

    public void removeFromPlaylist(int index) {
        if (index < 0 || index >= playlist.size()) {
            setStatus("无法删除歌曲，序号不合法：" + index);
            return;
        }

        boolean removingCurrentTrack =
                index == currentIndex;

        Track removedTrack = playlist.remove(index);

        // 删除后队列为空
        if (playlist.isEmpty()) {
            currentIndex = -1;

            playerManager.stopAndClear();

            notifyQueueChanged();
            notifyTrackCleared();

            setStatus(
                    "已删除："
                            + removedTrack.name
                            + "\n播放列表已为空"
            );

            return;
        }

        // 当前还没有开始播放歌曲
        if (currentIndex < 0) {
            notifyQueueChanged();
            return;
        }

        // 删除的是当前歌曲之前的歌曲
        if (index < currentIndex) {
            currentIndex--;

            notifyQueueChanged();
            return;
        }

        // 删除的就是当前正在播放的歌曲
        if (removingCurrentTrack) {
            /*
             * 删除中间歌曲后，原本的下一首会移动到相同位置。
             * 删除最后一首后，从列表第一首开始。
             */
            if (currentIndex >= playlist.size()) {
                currentIndex = 0;
            }

            playTrackAtCurrentIndex();
            return;
        }

        // 删除的是当前歌曲之后的歌曲
        notifyQueueChanged();
    }

    private void notifyTrackCleared() {
        if (listener != null) {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onTrackCleared();
                }
            });
        }
    }
}
