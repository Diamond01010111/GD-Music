package com.diamond.gdapplication;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.media3.ui.PlayerView;

public class MainUi {

    private final Context context;

    public LinearLayout root;
    public LinearLayout contentContainer;

    public EditText searchInput;

    public Button searchButton;
    public Button prevButton;
    public Button nextButton;
    public Button playPauseButton;
    public Button modeButton;
    public Button playlistButton;

    public Button homeTabButton;
    public Button myPlaylistTabButton;
    public Button importTabButton;

    public TextView songTitleText;
    public TextView resultText;

    public EditText importUrlInput;
    public Button importPlaylistButton;

    public MainUi(Context context) {
        this.context = context;
    }

    public View createView(PlayerView playerView) {
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                dp(16),
                getStatusBarHeight() + dp(12),
                dp(16),
                dp(16)
        );
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout searchBar = createSearchBar();
        createContentContainer();
        LinearLayout miniPlayer = createMiniPlayer();
        LinearLayout bottomNav = createBottomNav();

        // 顶部搜索栏
        root.addView(searchBar);

        // 不再显示黑色播放器框
        // root.addView(playerView);

        // 中间内容区
        root.addView(contentContainer);

        // 底部迷你播放器
        root.addView(miniPlayer);

        // 最底部导航栏
        root.addView(bottomNav);

        showHomePage();

        return root;
    }

    private LinearLayout createSearchBar() {
        LinearLayout searchBar = new LinearLayout(context);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(context);
        searchInput.setHint("搜索歌曲");
        searchInput.setText("稻香");
        searchInput.setSingleLine(true);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        searchButton = new Button(context);
        searchButton.setText("搜索");

        searchBar.addView(searchInput);
        searchBar.addView(searchButton);

        return searchBar;
    }

    private void createContentContainer() {
        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        resultText = new TextView(context);
        resultText.setText("等待操作...");
        resultText.setTextSize(14);
    }

    private LinearLayout createMiniPlayer() {
        LinearLayout miniPlayer = new LinearLayout(context);
        miniPlayer.setOrientation(LinearLayout.HORIZONTAL);

        songTitleText = new TextView(context);
        songTitleText.setText("歌曲名称");
        songTitleText.setSingleLine(true);
        songTitleText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        prevButton = new Button(context);
        prevButton.setText("上一首");

        playPauseButton = new Button(context);
        playPauseButton.setText("▶");

        nextButton = new Button(context);
        nextButton.setText("下一首");

        modeButton = new Button(context);
        modeButton.setText("播放模式：自动循环");

        playlistButton = new Button(context);
        playlistButton.setText("播放列表");

        miniPlayer.addView(songTitleText);
        miniPlayer.addView(prevButton);
        miniPlayer.addView(playPauseButton);
        miniPlayer.addView(nextButton);
        miniPlayer.addView(modeButton);
        miniPlayer.addView(playlistButton);

        return miniPlayer;
    }

    private LinearLayout createBottomNav() {
        LinearLayout bottomNav = new LinearLayout(context);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);

        homeTabButton = new Button(context);
        homeTabButton.setText("主页");

        myPlaylistTabButton = new Button(context);
        myPlaylistTabButton.setText("我的歌单");

        importTabButton = new Button(context);
        importTabButton.setText("导入歌单");

        homeTabButton.setLayoutParams(navButtonParams());
        myPlaylistTabButton.setLayoutParams(navButtonParams());
        importTabButton.setLayoutParams(navButtonParams());

        bottomNav.addView(homeTabButton);
        bottomNav.addView(myPlaylistTabButton);
        bottomNav.addView(importTabButton);

        return bottomNav;
    }

    private LinearLayout.LayoutParams navButtonParams() {
        return new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
    }

    public void showHomePage() {
        contentContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setText("主页");
        title.setTextSize(22);

        TextView desc = new TextView(context);
        desc.setText("最近播放的歌曲 / 最近播放的歌单");

        detachFromParent(resultText);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(resultText);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        contentContainer.addView(title);
        contentContainer.addView(desc);
        contentContainer.addView(scrollView);
    }

    public void showMyPlaylistPage() {
        contentContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setText("我的歌单");
        title.setTextSize(22);

        TextView desc = new TextView(context);
        desc.setText("这里之后显示本地保存的歌单，比如：我喜欢的音乐、车载歌单、最近收藏。");

        contentContainer.addView(title);
        contentContainer.addView(desc);
    }

    public void showImportPlaylistPage() {
        contentContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setText("导入歌单");
        title.setTextSize(22);

        importUrlInput = new EditText(context);
        importUrlInput.setHint("粘贴网易云歌单链接");

        importPlaylistButton = new Button(context);
        importPlaylistButton.setText("导入网易歌单");

        TextView tip = new TextView(context);
        tip.setText("这里之后解析网易云歌单链接，获取歌曲名/歌手，再用 GD API 搜索匹配。");

        contentContainer.addView(title);
        contentContainer.addView(importUrlInput);
        contentContainer.addView(importPlaylistButton);
        contentContainer.addView(tip);
    }

    public void showQueuePage() {
        contentContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setText("播放列表");
        title.setTextSize(22);

        TextView desc = new TextView(context);
        desc.setText("这里之后显示当前临时播放队列。");

        detachFromParent(resultText);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(resultText);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        contentContainer.addView(title);
        contentContainer.addView(desc);
        contentContainer.addView(scrollView);
    }

    public void setStatus(String message) {
        resultText.setText(message);
    }

    public void appendStatus(String message) {
        resultText.append(message);
    }

    public void setSongTitle(String title) {
        songTitleText.setText(title);
    }

    public void setModeName(String modeName) {
        modeButton.setText("播放模式：" + modeName);
    }

    private void detachFromParent(View view) {
        ViewParent parent = view.getParent();

        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
        );

        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }

        return dp(24);
    }
}