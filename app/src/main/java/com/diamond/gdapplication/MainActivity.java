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

public class MainActivity extends AppCompatActivity {

    private GdMusicApi api;
    private PlayerManager playerManager;
    private MusicController musicController;

    private TextView resultText;
    private EditText searchInput;
    private Button modeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = new GdMusicApi();
        playerManager = new PlayerManager(this);
        musicController = new MusicController(api, playerManager);

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
        searchInput.setHint("输入歌曲名，例如：稻香");
        searchInput.setText("稻香");

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

        musicController.setListener(new MusicController.Listener() {
            @Override
            public void onStatusChanged(String message) {
                resultText.setText(message);
            }

            @Override
            public void onStatusAppend(String message) {
                resultText.append(message);
            }

            @Override
            public void onModeChanged(String modeName) {
                modeButton.setText("播放模式：" + modeName);
            }
        });

        searchButton.setOnClickListener(v -> {
            String keyword = searchInput.getText().toString().trim();

            if (keyword.isEmpty()) {
                resultText.setText("请输入搜索关键词");
                return;
            }

            musicController.searchAndPlayFirst(keyword);
        });

        prevButton.setOnClickListener(v -> musicController.playPrevious());

        nextButton.setOnClickListener(v -> musicController.playNext());

        playPauseButton.setOnClickListener(v -> musicController.playOrPause());

        modeButton.setOnClickListener(v -> musicController.switchPlayMode());

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playerManager != null) {
            playerManager.release();
        }
    }
}