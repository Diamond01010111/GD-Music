package com.diamond.gdapplication;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class PlayerManager {

    public interface PlaybackEventListener {
        void onTrackEnded();
    }

    private final ExoPlayer player;
    private PlaybackEventListener listener;

    public PlayerManager(Context context) {
        player = new ExoPlayer.Builder(context).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    if (listener != null) {
                        listener.onTrackEnded();
                    }
                }
            }
        });
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void setPlaybackEventListener(PlaybackEventListener listener) {
        this.listener = listener;
    }

    public void playTrack(Track track) {
        if (track.audioUrl == null || track.audioUrl.isEmpty()) {
            return;
        }

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist(track.artist)
                .setAlbumTitle(track.album);

        if (track.picUrl != null && !track.picUrl.isEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(track.picUrl));
        }

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(track.audioUrl)
                .setMediaMetadata(metadataBuilder.build())
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    public void replayCurrentTrack() {
        player.seekTo(0);
        player.play();
    }

    public void playOrPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void stopAndClear() {
        player.stop();
        player.clearMediaItems();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void release() {
        player.release();
    }
}