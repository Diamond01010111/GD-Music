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
    public ScrollView searchResultScrollView;
    public LinearLayout searchResultList;
    public TextView searchResultFooter;

    private Runnable onSearchBottomReached;

    public LinearLayout myPlaylistList;
    public Button settingsButton;
    public LinearLayout queueList;

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

        settingsButton = new Button(context);
        settingsButton.setText("设置");
        settingsButton.setTextSize(12);
        settingsButton.setAllCaps(false);
        settingsButton.setMinWidth(0);
        settingsButton.setPadding(dp(4), 0, dp(4), 0);

        searchInput = new EditText(context);
        searchInput.setHint("搜索歌曲");
        searchInput.setText("音乐/作者/专辑");
        searchInput.setSingleLine(true);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        searchButton = new Button(context);
        searchButton.setText("搜索");

        searchBar.addView(settingsButton, new LinearLayout.LayoutParams(
                dp(64),
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
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
        miniPlayer.setOrientation(LinearLayout.VERTICAL);
        miniPlayer.setPadding(dp(4), dp(4), dp(4), dp(4));

        // 第一行：歌曲名
        songTitleText = new TextView(context);
        songTitleText.setText("歌曲名称");
        songTitleText.setSingleLine(true);
        songTitleText.setTextSize(14);
        songTitleText.setPadding(dp(4), dp(2), dp(4), dp(2));

        miniPlayer.addView(songTitleText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 第二行：控制按钮
        LinearLayout controlRow = new LinearLayout(context);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);

        prevButton = new Button(context);
        prevButton.setText("上一首");
        styleMiniButton(prevButton);

        playPauseButton = new Button(context);
        playPauseButton.setText("▶");
        styleMiniButton(playPauseButton);

        nextButton = new Button(context);
        nextButton.setText("下一首");
        styleMiniButton(nextButton);

        modeButton = new Button(context);
        modeButton.setText("循环");
        styleMiniButton(modeButton);

        playlistButton = new Button(context);
        playlistButton.setText("队列");
        styleMiniButton(playlistButton);

        controlRow.addView(prevButton, miniButtonParams());
        controlRow.addView(playPauseButton, miniButtonParams());
        controlRow.addView(nextButton, miniButtonParams());
        controlRow.addView(modeButton, miniButtonParams());
        controlRow.addView(playlistButton, miniButtonParams());

        miniPlayer.addView(controlRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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
        desc.setText("我的收藏");

        myPlaylistList = new LinearLayout(context);
        myPlaylistList.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(myPlaylistList);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        contentContainer.addView(title);
        contentContainer.addView(desc);
        contentContainer.addView(scrollView);
    }

    public void clearMyPlaylistItems() {
        if (myPlaylistList != null) {
            myPlaylistList.removeAllViews();
        }
    }

    public void setMyPlaylistEmpty(String message) {
        if (myPlaylistList == null) {
            return;
        }

        TextView emptyText = new TextView(context);
        emptyText.setText(message);
        emptyText.setPadding(dp(12), dp(20), dp(12), dp(20));

        myPlaylistList.addView(emptyText);
    }

    public void addMyPlaylistItem(
            Track track,
            int displayIndex,
            View.OnClickListener clickListener
    ) {
        if (myPlaylistList == null) {
            return;
        }

        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dp(12), dp(10), dp(12), dp(10));
        itemLayout.setClickable(true);
        itemLayout.setOnClickListener(clickListener);

        TextView nameText = new TextView(context);
        nameText.setText(displayIndex + ". " + track.name);
        nameText.setTextSize(17);

        TextView artistText = new TextView(context);
        artistText.setText("歌手：" + track.artist);
        artistText.setTextSize(14);

        TextView albumText = new TextView(context);
        albumText.setText("专辑：" + track.album);
        albumText.setTextSize(14);

        TextView divider = new TextView(context);
        divider.setText("────────────");

        itemLayout.addView(nameText);
        itemLayout.addView(artistText);
        itemLayout.addView(albumText);
        itemLayout.addView(divider);

        myPlaylistList.addView(itemLayout);
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

        queueList = new LinearLayout(context);
        queueList.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(queueList);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        contentContainer.addView(title);
        contentContainer.addView(scrollView);
    }

    public void clearQueueItems() {
        if (queueList != null) {
            queueList.removeAllViews();
        }
    }

    public void setQueueEmpty(String message) {
        if (queueList == null) {
            return;
        }

        TextView emptyText = new TextView(context);
        emptyText.setText(message);
        emptyText.setPadding(dp(12), dp(20), dp(12), dp(20));
        queueList.addView(emptyText);
    }

    public void addQueueItem(
            Track track,
            int displayIndex,
            boolean isCurrent,
            View.OnClickListener clickListener,
            View.OnClickListener moreClickListener
    ) {
        if (queueList == null) {
            return;
        }

        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dp(12), dp(10), dp(12), dp(10));
        itemLayout.setClickable(true);
        itemLayout.setOnClickListener(clickListener);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView nameText = new TextView(context);
        String prefix = isCurrent ? "▶ " : "";
        nameText.setText(prefix + displayIndex + ". " + track.name);
        nameText.setTextSize(17);

        TextView artistText = new TextView(context);
        artistText.setText("歌手：" + track.artist);
        artistText.setTextSize(14);

        TextView albumText = new TextView(context);
        albumText.setText("专辑：" + track.album);
        albumText.setTextSize(14);

        textColumn.addView(nameText);
        textColumn.addView(artistText);
        textColumn.addView(albumText);

        Button moreButton = new Button(context);
        moreButton.setText("更多");
        moreButton.setTextSize(12);
        moreButton.setAllCaps(false);
        moreButton.setOnClickListener(moreClickListener);

        row.addView(textColumn);
        row.addView(moreButton);

        TextView divider = new TextView(context);
        divider.setText("────────────");

        itemLayout.addView(row);
        itemLayout.addView(divider);

        queueList.addView(itemLayout);
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
        if (modeName.equals("自动循环")) {
            modeButton.setText("循环");
        } else if (modeName.equals("单曲循环")) {
            modeButton.setText("单曲");
        } else if (modeName.equals("随机播放")) {
            modeButton.setText("随机");
        } else {
            modeButton.setText(modeName);
        }
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

    private LinearLayout.LayoutParams miniButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(42),
                1
        );

        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private void styleMiniButton(Button button) {
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(2), 0, dp(2), 0);
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

    public void addSearchResultItem(
            Track track,
            int displayIndex,
            View.OnClickListener clickListener,
            View.OnClickListener moreClickListener
    ) {
        if (searchResultList == null) {
            return;
        }

        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dp(12), dp(10), dp(12), dp(10));
        itemLayout.setClickable(true);
        itemLayout.setOnClickListener(clickListener);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView nameText = new TextView(context);
        nameText.setText(displayIndex + ". " + track.name);
        nameText.setTextSize(17);

        TextView artistText = new TextView(context);
        artistText.setText("歌手：" + track.artist);
        artistText.setTextSize(14);

        TextView albumText = new TextView(context);
        albumText.setText("专辑：" + track.album);
        albumText.setTextSize(14);

        textColumn.addView(nameText);
        textColumn.addView(artistText);
        textColumn.addView(albumText);

        Button moreButton = new Button(context);
        moreButton.setText("更多");

        if (moreClickListener != null) {
            moreButton.setOnClickListener(moreClickListener);
        }

        row.addView(textColumn);
        row.addView(moreButton);

        TextView divider = new TextView(context);
        divider.setText("────────────");

        itemLayout.addView(row);
        itemLayout.addView(divider);

        searchResultList.addView(itemLayout);
    }

    public void setOnSearchBottomReached(Runnable runnable) {
        this.onSearchBottomReached = runnable;
    }

    public void showSearchResultsPage(String keyword) {
        contentContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setText("搜索结果：" + keyword);
        title.setTextSize(22);

        searchResultList = new LinearLayout(context);
        searchResultList.setOrientation(LinearLayout.VERTICAL);

        searchResultFooter = new TextView(context);
        searchResultFooter.setText("正在加载...");
        searchResultFooter.setPadding(dp(8), dp(16), dp(8), dp(16));

        LinearLayout pageContent = new LinearLayout(context);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.addView(searchResultList);
        pageContent.addView(searchResultFooter);

        searchResultScrollView = new ScrollView(context);
        searchResultScrollView.addView(pageContent);
        searchResultScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        searchResultScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (searchResultScrollView.getChildCount() == 0) {
                return;
            }

            View child = searchResultScrollView.getChildAt(0);
            int childHeight = child.getMeasuredHeight();
            int visibleHeight = searchResultScrollView.getHeight();

            boolean nearBottom = scrollY + visibleHeight >= childHeight - dp(80);

            if (nearBottom && onSearchBottomReached != null) {
                onSearchBottomReached.run();
            }
        });

        contentContainer.addView(title);
        contentContainer.addView(searchResultScrollView);
    }

    public void setSearchFooter(String message) {
        if (searchResultFooter != null) {
            searchResultFooter.setText(message);
        }
    }

    public void clearSearchResults() {
        if (searchResultList != null) {
            searchResultList.removeAllViews();
        }
    }
}