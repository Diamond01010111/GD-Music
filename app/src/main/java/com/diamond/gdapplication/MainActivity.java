package com.diamond.gdapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {

    private GdMusicApi api;
    private PlayerManager playerManager;
    private MusicController musicController;
    private MainUi ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        api = new GdMusicApi();
        playerManager = new PlayerManager(this);
        musicController = new MusicController(api, playerManager);

        PlayerView playerView = new PlayerView(this);
        playerView.setPlayer(playerManager.getPlayer());

        ui = new MainUi(this);
        setContentView(ui.createView(playerView));

        bindMusicControllerListener();
        bindUiEvents();
    }

    private void bindMusicControllerListener() {
        musicController.setListener(new MusicController.Listener() {
            @Override
            public void onStatusChanged(String message) {
                ui.setStatus(message);

                if (message != null && !message.isEmpty()) {
                    String firstLine = message.split("\n")[0];
                    ui.setSongTitle(firstLine);
                }
            }

            @Override
            public void onStatusAppend(String message) {
                ui.appendStatus(message);
            }

            @Override
            public void onModeChanged(String modeName) {
                ui.setModeName(modeName);
            }
        });
    }

    private void bindUiEvents() {
        ui.searchButton.setOnClickListener(v -> {
            String keyword = ui.searchInput.getText().toString().trim();

            if (keyword.isEmpty()) {
                ui.setStatus("请输入搜索关键词");
                return;
            }

            ui.showHomePage();
            musicController.searchAndPlayFirst(keyword);
        });

        ui.prevButton.setOnClickListener(v -> musicController.playPrevious());

        ui.nextButton.setOnClickListener(v -> musicController.playNext());

        ui.playPauseButton.setOnClickListener(v -> musicController.playOrPause());

        ui.modeButton.setOnClickListener(v -> musicController.switchPlayMode());

        ui.playlistButton.setOnClickListener(v -> ui.showQueuePage());

        ui.homeTabButton.setOnClickListener(v -> ui.showHomePage());

        ui.myPlaylistTabButton.setOnClickListener(v -> ui.showMyPlaylistPage());

        ui.importTabButton.setOnClickListener(v -> {
            ui.showImportPlaylistPage();

            if (ui.importPlaylistButton != null) {
                ui.importPlaylistButton.setOnClickListener(importView -> {
                    String playlistUrl = ui.importUrlInput.getText().toString().trim();

                    if (playlistUrl.isEmpty()) {
                        ui.setStatus("请先粘贴网易云歌单链接");
                        return;
                    }

                    ui.setStatus(
                            "导入功能还没接 API。\n\n"
                                    + "当前输入链接：\n"
                                    + playlistUrl + "\n\n"
                                    + "下一步可以做：解析 playlist id → 获取歌单歌曲 → 用 GD API 匹配播放。"
                    );
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerManager != null) {
            playerManager.release();
        }
    }
}