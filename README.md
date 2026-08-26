# GD Application

一个使用 Kotlin、Java 与 Jetpack Compose 开发的 Android 在线音乐播放器。项目通过 **GD 音乐台 API** 搜索歌曲、获取播放地址、封面和歌词，当前主要用于学习 Android Compose、Media3 播放器及网络请求相关开发。

> 本项目仍在开发中，仅供个人学习与技术交流，请勿用于商业用途，也请勿下载、传播受版权保护的音乐资源。

简体中文 | [English](README_EN.md)

## 当前进度

目前正在把原有的 Java View 界面迁移到 Jetpack Compose，并保留已有的 Java 播放及网络请求逻辑。

已完成或正在接入的功能：

- 适配全面屏的 Compose + Material 3 界面
- 首页、收藏、网易歌单和搜索页面导航
- 单曲/歌手搜索与专辑搜索
- 搜索音乐源切换
- 最近搜索记录
- 搜索结果列表及歌曲操作菜单
- 在线获取歌曲播放地址和专辑封面
- Media3 ExoPlayer 音频播放
- 迷你播放器
    - 专辑封面、歌曲名和歌手
    - 播放/暂停
    - 播放进度条
    - 列表循环、单曲循环、随机播放模式切换
    - 打开当前播放列表
- 当前播放列表
    - 固定高度底部弹窗
    - 标记正在播放的歌曲
    - 点击列表歌曲切换播放
    - 添加到下一首播放
    - 删除单首歌曲
    - 清空播放列表
- 本地收藏歌曲（持续完善中）

仍待完善：

- 网易云歌单搜索与歌单详情
- 收藏页面和本地歌单的完整管理能力
- 歌词展示与滚动同步
- 播放错误提示、网络异常处理和重试
- 后台播放、通知栏控制及音频焦点处理
- 播放进度拖动
- 页面状态持久化和进程恢复
- UI 细节、动画与深色模式适配
- 单元测试及 UI 测试

## 技术栈

- Kotlin
- Java
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- OkHttp
- Gson
- Coil 3
- SharedPreferences（搜索历史与本地收藏）

## 项目结构

当前推荐的代码结构如下，实际目录会随着 Compose 迁移继续调整：

```text
app/src/main/java/com/diamond/gdapplication/
├── ComposeMainActivity.kt          # Compose 应用入口及播放器状态连接
├── GdMusicApi.java                 # GD 音乐台 API 请求
├── MusicController.java            # 播放列表与播放模式控制
├── PlayerManager.java              # Media3 ExoPlayer 封装
├── Track.java                      # 歌曲数据模型
├── LocalPlaylistStore.java         # 本地收藏存储
├── data/
│   └── SearchHistoryStore.kt       # 搜索历史
├── model/
│   └── SearchModels.kt             # 页面、搜索类型及音乐源模型
└── ui/
    ├── MusicApp.kt                 # 页面导航与整体 Scaffold
    ├── components/
    │   ├── MiniPlayer.kt
    │   └── QueueBottomSheet.kt
    └── screens/
        ├── HomeScreen.kt
        ├── SearchScreen.kt
        ├── SearchResultsScreen.kt
        ├── FavoriteScreen.kt
        └── NeteasePlaylistScreen.kt
```

## 运行要求

- Android Studio（建议使用当前稳定版本）
- JDK 17
- Android SDK
- 支持 Jetpack Compose 的 Android Gradle Plugin
- 能够访问 GD 音乐台 API 的网络环境

## 本地运行

1. 克隆项目并使用 Android Studio 打开：

   ```bash
   git clone <项目仓库地址>
   cd GD_Application
   ```

2. 等待 Gradle Sync 完成。

3. 确认 `AndroidManifest.xml` 包含网络权限：

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. 选择 Android 模拟器或实体设备。

5. 点击 Android Studio 顶部的 **Run** 按钮启动应用。

如果安装后找不到应用图标，请先确认已经执行 Run、设备安装成功，并检查启动 Activity 是否包含 `MAIN` 和 `LAUNCHER` intent-filter。

## GD 音乐台 API

项目使用以下服务：

- API 地址：`https://music-api.gdstudio.xyz/api.php`
- 项目出处：**GD 音乐台（music.gdstudio.xyz）**
- 文档标注的访问限制：5 分钟内不超过 50 次请求
- 文档标注的稳定音乐源：`netease`、`joox`、`bilibili`

应用中列出的音乐源：

| 音乐源 | 参数 | 当前建议 |
| --- | --- | --- |
| 网易云 | `netease` | 推荐 |
| JOOX | `joox` | 推荐 |
| 哔哩哔哩 | `bilibili` | 推荐 |
| 腾讯音乐 | `tencent` | 非稳定源 |
| 酷我 | `kuwo` | 非稳定源 |
| Tidal | `tidal` | 非稳定源 |
| Qobuz | `qobuz` | 非稳定源 |
| Apple Music | `apple` | 非稳定源 |
| YouTube Music | `ytmusic` | 非稳定源 |
| Spotify | `spotify` | 非稳定源 |

主要接口：

```text
# 搜索
GET /api.php?types=search&source={source}&name={keyword}&count={count}&pages={page}

# 获取播放地址
GET /api.php?types=url&source={source}&id={trackId}&br={quality}

# 获取专辑图
GET /api.php?types=pic&source={source}&id={picId}&size={size}

# 获取歌词
GET /api.php?types=lyric&source={source}&id={lyricId}
```

专辑搜索目前通过在音乐源后添加 `_album` 调用，例如 `netease_album`。

### API 使用注意事项

- 请在使用时注明出处“GD 音乐台（music.gdstudio.xyz）”。
- 不要并发或高频请求，建议在客户端增加请求节流与缓存。
- 部分音乐源可能临时不可用，返回的实际音质也可能低于请求音质。
- 播放链接可能具有时效性，不应长期保存。
- 当前 API 文档没有提供完整的网易云歌单搜索接口，因此歌单功能仍处于待实现状态。

## 已知问题

- 项目正处于 Java View 向 Compose 迁移阶段，部分旧界面或旧入口可能尚未清理。
- 非推荐音乐源不保证可用。
- 收藏数据目前主要保存在本地，清除应用数据后可能丢失。
- 播放列表、当前歌曲和播放进度在应用进程被终止后暂时不会完整恢复。
- 在线音乐内容及接口可用性由第三方服务决定。

## 开发约定

- Compose UI 放在 `ui` 目录，播放器和网络逻辑不要直接写进 Composable。
- `MusicController` 负责播放队列、当前索引和播放模式。
- `PlayerManager` 只负责与 Media3 Player 交互。
- UI 状态通过监听器回调到 `ComposeMainActivity`，再作为参数传给 `MusicApp`。
- 修改队列后应调用 `notifyQueueChanged()`，切换播放模式后应调用 `onPlayModeChanged()`。
- API 请求需遵守频率限制，并在失败时向用户展示可理解的提示。

## 声明

本项目为非商业学习项目。音乐、封面、歌词及其他内容来自第三方网络服务，其版权归原作者或权利人所有。本项目不存储、不分发音乐文件，也不对第三方接口的稳定性、合法性或内容承担保证责任。

如相关内容侵犯了您的合法权益，请联系对应服务提供者或项目维护者处理。

GD 音乐台 API 由 GD Studio 提供，其文档声明采用 **CC BY-NC 4.0**，并要求仅用于学习用途。

## License

项目自身的开源许可证尚未确定。在添加明确的 `LICENSE` 文件前，请勿将本项目用于商业用途或擅自再分发。

