package com.diamond.gdapplication;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private enum PlayMode {
        LIST_LOOP,
        SINGLE_LOOP,
        RANDOM
    }

    private GdMusicApi api;
    private PlayerManager playerManager;

    private TextView resultText;
    private EditText searchInput;
    private Button modeButton;

    private final List<Track> playlist = new ArrayList<>();
    private int currentIndex = -1;

    private PlayMode playMode = PlayMode.LIST_LOOP;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = new GdMusicApi();
        playerManager = new PlayerManager(this);

        playerManager.setPlaybackEventListener(new PlayerManager.PlaybackEventListener() {
            @Override
            public void onTrackEnded() {
                runOnUiThread(() -> handleTrackEnded());
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        PlayerView playerView = new PlayerView(this);
        playerView.setPlayer(playerManager.getPlayer());
        playerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
        ));

        searchInput = new EditText(this);
        searchInput.setHint("输入歌曲名");
        searchInput.setText("希望有羽毛和翅膀");  // sample 歌曲

        Button searchButton = new Button(this);
        searchButton.setText("搜索并播放前 5 首");

        Button prevButton = new Button(this);
        prevButton.setText("上一首");

        Button nextButton = new Button(this);
        nextButton.setText("下一首");

        Button playPauseButton = new Button(this);
        playPauseButton.setText("播放 / 暂停");

        modeButton = new Button(this);
        modeButton.setText("播放模式：自动循环");

        resultText = new TextView(this);
        resultText.setText("等待操作...");
        resultText.setTextSize(14);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(resultText);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        searchButton.setOnClickListener(v -> {
            String keyword = searchInput.getText().toString().trim();

            if (keyword.isEmpty()) {
                resultText.setText("请输入搜索关键词");
                return;
            }

            searchAndPlayFirst(keyword);
        });

        prevButton.setOnClickListener(v -> playPrevious());

        nextButton.setOnClickListener(v -> playNext());

        playPauseButton.setOnClickListener(v -> {
            playerManager.playOrPause();

            if (playerManager.isPlaying()) {
                resultText.append("\n\n继续播放");
            } else {
                resultText.append("\n\n已暂停");
            }
        });

        modeButton.setOnClickListener(v -> switchPlayMode());

        root.addView(playerView);
        root.addView(searchInput);
        root.addView(searchButton);
        root.addView(prevButton);
        root.addView(nextButton);
        root.addView(playPauseButton);
        root.addView(modeButton);
        root.addView(scrollView);

        setContentView(root);
    }

    private void searchAndPlayFirst(String keyword) {
        resultText.setText("正在搜索：" + keyword);

        api.searchTracks(keyword, new GdMusicApi.SearchCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                if (tracks == null || tracks.isEmpty()) {
                    runOnUiThread(() -> resultText.setText("没有搜索结果"));
                    return;
                }

                playlist.clear();
                playlist.addAll(tracks);
                currentIndex = 0;

                Track firstTrack = playlist.get(currentIndex);

                runOnUiThread(() -> {
                    resultText.setText(
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
                });

                playTrackAtCurrentIndex();
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> resultText.setText("搜索失败：\n" + e.getMessage()));
            }
        });
    }

    private void playTrackAtCurrentIndex() {
        if (playlist.isEmpty()) {
            runOnUiThread(() -> resultText.setText("播放队列为空"));
            return;
        }

        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            runOnUiThread(() -> resultText.setText("currentIndex 不合法：" + currentIndex));
            return;
        }

        Track track = playlist.get(currentIndex);

        runOnUiThread(() -> {
            resultText.setText(
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
        });

        api.getAudioUrl(track, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                if (updatedTrack.audioUrl == null || updatedTrack.audioUrl.isEmpty()) {
                    runOnUiThread(() -> resultText.setText(
                            "没有拿到播放 URL：\n"
                                    + updatedTrack.name + "\n"
                                    + updatedTrack.artist
                    ));
                    return;
                }

                runOnUiThread(() -> {
                    resultText.setText(
                            "正在播放：\n"
                                    + updatedTrack.name + "\n"
                                    + updatedTrack.artist + "\n"
                                    + updatedTrack.album + "\n\n"
                                    + "当前序号：" + (currentIndex + 1) + " / " + playlist.size() + "\n"
                                    + "播放模式：" + getPlayModeName() + "\n\n"
                                    + "trackId = " + updatedTrack.id + "\n"
                                    + "picId = " + updatedTrack.picId + "\n"
                                    + "lyricId = " + updatedTrack.lyricId + "\n\n"
                                    + "audioUrl:\n"
                                    + updatedTrack.audioUrl
                    );

                    playerManager.playTrack(updatedTrack);
                });

                // requestPicAndLyric(updatedTrack);
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> resultText.setText("获取播放 URL 失败：\n" + e.getMessage()));
            }
        });
    }

    private void requestPicAndLyric(Track track) {
        api.getPicUrl(track, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                runOnUiThread(() -> {
                    if (updatedTrack.picUrl != null && !updatedTrack.picUrl.isEmpty()) {
                        resultText.append("\n\n专辑图 URL:\n" + updatedTrack.picUrl);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> resultText.append("\n\n获取专辑图失败：" + e.getMessage()));
            }
        });

        api.getLyric(track, new GdMusicApi.TrackCallback() {
            @Override
            public void onSuccess(Track updatedTrack) {
                runOnUiThread(() -> {
                    int lyricLength = updatedTrack.lyric == null ? 0 : updatedTrack.lyric.length();
                    int translatedLength = updatedTrack.translatedLyric == null ? 0 : updatedTrack.translatedLyric.length();

                    resultText.append("\n\n歌词已获取");
                    resultText.append("\n原文歌词长度：" + lyricLength);
                    resultText.append("\n翻译歌词长度：" + translatedLength);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> resultText.append("\n\n获取歌词失败：" + e.getMessage()));
            }
        });
    }

    private void playNext() {
        if (playlist.isEmpty()) {
            resultText.setText("播放队列为空，请先搜索歌曲");
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

    private void playPrevious() {
        if (playlist.isEmpty()) {
            resultText.setText("播放队列为空，请先搜索歌曲");
            return;
        }

        currentIndex--;

        if (currentIndex < 0) {
            currentIndex = playlist.size() - 1;
        }

        playTrackAtCurrentIndex();
    }

    private void playRandomTrack() {
        if (playlist.isEmpty()) {
            resultText.setText("播放队列为空");
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

    private void switchPlayMode() {
        if (playMode == PlayMode.LIST_LOOP) {
            playMode = PlayMode.SINGLE_LOOP;
        } else if (playMode == PlayMode.SINGLE_LOOP) {
            playMode = PlayMode.RANDOM;
        } else {
            playMode = PlayMode.LIST_LOOP;
        }

        modeButton.setText("播放模式：" + getPlayModeName());
        resultText.append("\n\n已切换播放模式：" + getPlayModeName());
    }

    private String getPlayModeName() {
        if (playMode == PlayMode.SINGLE_LOOP) {
            return "单曲循环";
        }

        if (playMode == PlayMode.RANDOM) {
            return "随机播放";
        }

        return "自动循环";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerManager != null) {
            playerManager.release();
        }
    }
}