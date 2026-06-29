package com.diamond.gdapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import android.app.AlertDialog;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    private GdMusicApi api;
    private PlayerManager playerManager;
    private MusicController musicController;
    private MainUi ui;
    private static final int SEARCH_PAGE_SIZE = 30;

    private final List<Track> currentSearchResults = new ArrayList<>();

    private String currentSearchKeyword = "";
    private int currentSearchPage = 1;
    private boolean isLoadingSearch = false;
    private boolean hasMoreSearch = true;
    private LocalPlaylistStore localPlaylistStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        api = new GdMusicApi();
        playerManager = new PlayerManager(this);
        musicController = new MusicController(api, playerManager);
        localPlaylistStore = new LocalPlaylistStore(this);

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

            startSearch(keyword);
        });

        ui.prevButton.setOnClickListener(v -> musicController.playPrevious());

        ui.nextButton.setOnClickListener(v -> musicController.playNext());

        ui.playPauseButton.setOnClickListener(v -> musicController.playOrPause());

        ui.modeButton.setOnClickListener(v -> musicController.switchPlayMode());

        ui.playlistButton.setOnClickListener(v -> showCurrentQueuePage());

        ui.homeTabButton.setOnClickListener(v -> ui.showHomePage());

        ui.myPlaylistTabButton.setOnClickListener(v -> showMyLocalPlaylist());

        ui.importTabButton.setOnClickListener(v -> {
            ui.showImportPlaylistPage();

            if (ui.importPlaylistButton != null) {
                ui.importPlaylistButton.setOnClickListener(importView -> {
                    String playlistUrl = ui.importUrlInput.getText().toString().trim();

                    if (playlistUrl.isEmpty()) {
                        ui.setStatus("请先粘贴网易云歌单链接");
                        return;
                    }

                    ui.setStatus("导入功能还没接 API：\n" + playlistUrl);
                });
            }
        });

        ui.setOnSearchBottomReached(() -> loadNextSearchPage());
        ui.settingsButton.setOnClickListener(v -> showSettingsDialog());
    }

    private void startSearch(String keyword) {
        currentSearchKeyword = keyword;
        currentSearchPage = 1;
        isLoadingSearch = false;
        hasMoreSearch = true;
        currentSearchResults.clear();

        ui.showSearchResultsPage(keyword);
        ui.clearSearchResults();
        ui.setSearchFooter("正在加载第 1 页...");

        loadSearchPage(1);
    }

    private void loadNextSearchPage() {
        if (isLoadingSearch || !hasMoreSearch) {
            return;
        }

        loadSearchPage(currentSearchPage + 1);
    }

    private void loadSearchPage(int page) {
        if (currentSearchKeyword == null || currentSearchKeyword.isEmpty()) {
            return;
        }

        isLoadingSearch = true;
        ui.setSearchFooter("正在加载第 " + page + " 页...");

        api.searchTracks(currentSearchKeyword, SEARCH_PAGE_SIZE, page, new GdMusicApi.SearchCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                runOnUiThread(() -> {
                    isLoadingSearch = false;

                    if (tracks == null || tracks.isEmpty()) {
                        if (page == 1) {
                            ui.setSearchFooter("没有搜索结果");
                        } else {
                            ui.setSearchFooter("没有更多结果了");
                        }

                        hasMoreSearch = false;
                        return;
                    }

                    int startIndex = currentSearchResults.size();

                    currentSearchResults.addAll(tracks);

                    for (int i = 0; i < tracks.size(); i++) {
                        Track track = tracks.get(i);
                        int index = startIndex + i;

                        ui.addSearchResultItem(
                                track,
                                index + 1,
                                v -> {
                                    musicController.setPlaylistAndPlay(
                                            new ArrayList<>(currentSearchResults),
                                            index
                                    );
                                },
                                moreView -> showTrackMoreMenu(moreView, track)
                        );
                    }

                    currentSearchPage = page;

                    if (tracks.size() < SEARCH_PAGE_SIZE) {
                        hasMoreSearch = false;
                        ui.setSearchFooter("没有更多结果了");
                    } else {
                        ui.setSearchFooter("上滑到底加载更多");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    isLoadingSearch = false;
                    ui.setSearchFooter("搜索失败：\n" + e.getMessage());
                });
            }
        });
    }

    private void showTrackMoreMenu(View anchor, Track track) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add("下一首播放");
        popupMenu.getMenu().add("收藏到我的歌单");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("下一首播放")) {
                musicController.addToPlayNext(track);

                Toast.makeText(
                        this,
                        "已加入下一首播放：" + track.name,
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            if (title.equals("收藏到我的歌单")) {
                boolean added = localPlaylistStore.addToFavorite(track);

                if (added) {
                    Toast.makeText(
                            this,
                            "已收藏：" + track.name,
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            this,
                            "已经收藏过：" + track.name,
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void showMyLocalPlaylist() {
        ui.showMyPlaylistPage();

        List<Track> favorites = localPlaylistStore.getFavoriteTracks();

        ui.clearMyPlaylistItems();

        if (favorites.isEmpty()) {
            ui.setMyPlaylistEmpty("还没有收藏歌曲。可以在搜索结果里点“更多”收藏。");
            return;
        }

        for (int i = 0; i < favorites.size(); i++) {
            Track track = favorites.get(i);
            int index = i;

            ui.addMyPlaylistItem(track, index + 1, v -> {
                musicController.setPlaylistAndPlay(
                        new ArrayList<>(favorites),
                        index
                );
            });
        }
    }

    private void showCurrentQueueDialog() {
        List<Track> queue = musicController.getPlaylistSnapshot();
        int currentIndex = musicController.getCurrentIndex();

        if (queue.isEmpty()) {
            Toast.makeText(this, "当前播放队列为空", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(dp(12), dp(8), dp(12), dp(8));

        for (int i = 0; i < queue.size(); i++) {
            Track track = queue.get(i);
            int index = i;

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(dp(8), dp(10), dp(8), dp(10));
            itemLayout.setClickable(true);

            LinearLayout textColumn = new LinearLayout(this);
            textColumn.setOrientation(LinearLayout.VERTICAL);
            textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            TextView nameText = new TextView(this);

            String prefix = index == currentIndex ? "▶ " : "";
            nameText.setText(prefix + (index + 1) + ". " + track.name);
            nameText.setTextSize(16);

            TextView artistText = new TextView(this);
            artistText.setText("歌手：" + track.artist);
            artistText.setTextSize(13);

            TextView albumText = new TextView(this);
            albumText.setText("专辑：" + track.album);
            albumText.setTextSize(13);

            textColumn.addView(nameText);
            textColumn.addView(artistText);
            textColumn.addView(albumText);

            Button moreButton = new Button(this);
            moreButton.setText("更多");

            itemLayout.addView(textColumn);
            itemLayout.addView(moreButton);

            itemLayout.setOnClickListener(v -> {
                musicController.playAtIndex(index);
                Toast.makeText(this, "正在播放：" + track.name, Toast.LENGTH_SHORT).show();
            });

            moreButton.setOnClickListener(v -> {
                showTrackMoreMenu(v, track);
            });

            listLayout.addView(itemLayout);

            TextView divider = new TextView(this);
            divider.setText("────────────");
            listLayout.addView(divider);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(listLayout);

        new AlertDialog.Builder(this)
                .setTitle("当前播放列表")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showCurrentQueuePage() {
        ui.showQueuePage();

        java.util.List<Track> queue = musicController.getPlaylistSnapshot();
        int currentIndex = musicController.getCurrentIndex();

        ui.clearQueueItems();

        if (queue.isEmpty()) {
            ui.setQueueEmpty("当前播放队列为空。可以先搜索歌曲，然后点某首歌播放。");
            return;
        }

        for (int i = 0; i < queue.size(); i++) {
            Track track = queue.get(i);
            int index = i;
            boolean isCurrent = index == currentIndex;

            ui.addQueueItem(
                    track,
                    index + 1,
                    isCurrent,
                    v -> musicController.playAtIndex(index),
                    moreView -> showTrackMoreMenu(moreView, track)
            );
        }
    }


    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        TextView qualityLabel = new TextView(this);
        qualityLabel.setText("歌曲音质");

        Spinner qualitySpinner = new Spinner(this);

        Integer[] qualities = new Integer[]{128, 192, 320, 740, 999};

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                qualities
        );

        qualitySpinner.setAdapter(adapter);

        int currentQuality = musicController.getAudioQuality();
        int selectedIndex = 2;

        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i] == currentQuality) {
                selectedIndex = i;
                break;
            }
        }

        qualitySpinner.setSelection(selectedIndex);

        CheckBox detailCheckBox = new CheckBox(this);
        detailCheckBox.setText("显示详细歌曲信息");
        detailCheckBox.setChecked(musicController.isShowDetailedInfo());

        layout.addView(qualityLabel);
        layout.addView(qualitySpinner);
        layout.addView(detailCheckBox);

        new AlertDialog.Builder(this)
                .setTitle("设置")
                .setView(layout)
                .setPositiveButton("保存", (dialog, which) -> {
                    int selectedQuality = (Integer) qualitySpinner.getSelectedItem();
                    boolean showInfo = detailCheckBox.isChecked();

                    musicController.setAudioQuality(selectedQuality);
                    musicController.setShowDetailedInfo(showInfo);

                    Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerManager != null) {
            playerManager.release();
        }
    }
}