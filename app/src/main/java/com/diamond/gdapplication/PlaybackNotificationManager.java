package com.diamond.gdapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class PlaybackNotificationManager {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "music_playback";
    private static final String ACTION_PREVIOUS =
            "com.diamond.gdapplication.notification.PREVIOUS";
    private static final String ACTION_PLAY =
            "com.diamond.gdapplication.notification.PLAY";
    private static final String ACTION_PAUSE =
            "com.diamond.gdapplication.notification.PAUSE";
    private static final String ACTION_NEXT =
            "com.diamond.gdapplication.notification.NEXT";
    private static final String ACTION_PLAY_MODE =
            "com.diamond.gdapplication.notification.PLAY_MODE";

    private final MusicController musicController;
    private final Player player;
    private final MediaSession mediaSession;
    private final androidx.media3.ui.PlayerNotificationManager notificationManager;

    public PlaybackNotificationManager(
            Context context,
            MusicController musicController,
            Player player
    ) {
        Context appContext = context.getApplicationContext();
        this.musicController = musicController;
        this.player = player;
        this.mediaSession = new MediaSession.Builder(appContext, player).build();

        Intent openAppIntent = new Intent(appContext, ComposeMainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        notificationManager =
                new androidx.media3.ui.PlayerNotificationManager.Builder(
                        appContext,
                        NOTIFICATION_ID,
                        CHANNEL_ID
                )
                        .setChannelNameResourceId(R.string.playback_channel_name)
                        .setChannelDescriptionResourceId(
                                R.string.playback_channel_description
                        )
                        .setSmallIconResourceId(android.R.drawable.ic_media_play)
                        .setMediaDescriptionAdapter(
                                new androidx.media3.ui.PlayerNotificationManager
                                        .MediaDescriptionAdapter() {
                                    @Override
                                    public CharSequence getCurrentContentTitle(
                                            Player ignored
                                    ) {
                                        Track track =
                                                musicController.getCurrentTrack();
                                        return track == null
                                                ? "GD 音乐"
                                                : track.name;
                                    }

                                    @Nullable
                                    @Override
                                    public PendingIntent getCurrentContentIntent(
                                            Player ignored
                                    ) {
                                        return contentIntent;
                                    }

                                    @Nullable
                                    @Override
                                    public CharSequence getCurrentContentText(
                                            Player ignored
                                    ) {
                                        Track track =
                                                musicController.getCurrentTrack();
                                        if (track == null) {
                                            return null;
                                        }

                                        if (
                                                track.album != null &&
                                                !track.album.isEmpty()
                                        ) {
                                            return track.artist
                                                    + " · "
                                                    + track.album;
                                        }

                                        return track.artist;
                                    }

                                    @Nullable
                                    @Override
                                    public Bitmap getCurrentLargeIcon(
                                            Player ignored,
                                            androidx.media3.ui
                                                    .PlayerNotificationManager
                                                    .BitmapCallback callback
                                    ) {
                                        return null;
                                    }
                                }
                        )
                        .setCustomActionReceiver(
                                new NotificationActionReceiver(appContext)
                        )
                        .build();

        notificationManager.setMediaSessionToken(
                mediaSession.getPlatformToken()
        );
        notificationManager.setUsePlayPauseActions(false);
        notificationManager.setUsePreviousAction(false);
        notificationManager.setUseNextAction(false);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseStopAction(false);
        notificationManager.setPlayer(player);
    }

    public void invalidate() {
        notificationManager.invalidate();
    }

    public void release() {
        notificationManager.setPlayer(null);
        mediaSession.release();
    }

    private class NotificationActionReceiver
            implements androidx.media3.ui.PlayerNotificationManager
            .CustomActionReceiver {

        private final Context context;

        NotificationActionReceiver(Context context) {
            this.context = context;
        }

        @Override
        public Map<String, NotificationCompat.Action> createCustomActions(
                Context ignored,
                int instanceId
        ) {
            Map<String, NotificationCompat.Action> actions = new HashMap<>();
            actions.put(
                    ACTION_PREVIOUS,
                    action(
                            ACTION_PREVIOUS,
                            android.R.drawable.ic_media_previous,
                            "上一首",
                            instanceId,
                            instanceId + 1
                    )
            );
            actions.put(
                    ACTION_PLAY,
                    action(
                            ACTION_PLAY,
                            android.R.drawable.ic_media_play,
                            "播放",
                            instanceId,
                            instanceId + 2
                    )
            );
            actions.put(
                    ACTION_PAUSE,
                    action(
                            ACTION_PAUSE,
                            android.R.drawable.ic_media_pause,
                            "暂停",
                            instanceId,
                            instanceId + 3
                    )
            );
            actions.put(
                    ACTION_NEXT,
                    action(
                            ACTION_NEXT,
                            android.R.drawable.ic_media_next,
                            "下一首",
                            instanceId,
                            instanceId + 4
                    )
            );
            actions.put(
                    ACTION_PLAY_MODE,
                    action(
                            ACTION_PLAY_MODE,
                            android.R.drawable.ic_menu_rotate,
                            musicController.getPlayModeName(),
                            instanceId,
                            instanceId + 5
                    )
            );
            return actions;
        }

        @Override
        public List<String> getCustomActions(Player ignored) {
            return Arrays.asList(
                    ACTION_PREVIOUS,
                    player.isPlaying() ? ACTION_PAUSE : ACTION_PLAY,
                    ACTION_NEXT,
                    ACTION_PLAY_MODE
            );
        }

        @Override
        public void onCustomAction(
                Player ignored,
                String action,
                Intent intent
        ) {
            switch (action) {
                case ACTION_PREVIOUS:
                    musicController.playPrevious();
                    break;

                case ACTION_PLAY:
                    if (!player.isPlaying()) {
                        musicController.playOrPause();
                    }
                    break;

                case ACTION_PAUSE:
                    if (player.isPlaying()) {
                        musicController.playOrPause();
                    }
                    break;

                case ACTION_NEXT:
                    musicController.playNext();
                    break;

                case ACTION_PLAY_MODE:
                    musicController.switchPlayMode();
                    break;
            }

            invalidate();
        }

        private NotificationCompat.Action action(
                String action,
                int icon,
                String title,
                int instanceId,
                int requestCode
        ) {
            Intent intent = new Intent(action)
                    .setPackage(context.getPackageName());
            intent.putExtra(
                    androidx.media3.ui.PlayerNotificationManager
                            .EXTRA_INSTANCE_ID,
                    instanceId
            );
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
            );

            return new NotificationCompat.Action.Builder(
                    icon,
                    title,
                    pendingIntent
            ).build();
        }
    }
}
