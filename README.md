# GD Application

一个使用 Kotlin、Java 和 Jetpack Compose 开发的 Android 在线音乐播放器。本项目用于学习 Android 应用开发、Media3 音频播放、Jetpack Compose 界面开发和网络请求处理。

> 本项目仅供学习与技术交流。请遵守当地法律、第三方服务条款及版权要求，请勿使用本项目下载、传播或商业利用未经授权的音乐内容。

简体中文 | [English](README_EN.md)

## 功能

### 音乐搜索与播放

- 支持单曲、歌手和专辑搜索
- 支持切换多个音乐源
- 保存最近搜索记录
- 在线获取播放地址和专辑封面
- 使用 AndroidX Media3 / ExoPlayer 播放音频
- 支持播放、暂停和播放进度显示
- 支持列表循环、单曲循环和随机播放
- 支持播放下一首、切换队列歌曲、删除单曲和清空队列

### 本地收藏

- 创建多个本地收藏歌单
- 将搜索结果或正在浏览的歌曲加入指定收藏
- 浏览收藏详情并播放全部歌曲
- 从收藏中移除歌曲
- 删除收藏歌单
- 使用歌单首曲封面作为收藏封面
- 本地数据通过 SharedPreferences 保存

### 网易云歌单

- 输入网易云音乐用户 ID
- 获取该用户公开创建和收藏的歌单
- 将创建歌单与收藏歌单分组显示
- 使用双列封面网格展示歌单
- 在本机记住用户 ID
- 进入页面时自动刷新
- 支持手动刷新和退出当前用户

目前仅展示公开可见的歌单信息，暂不支持查看歌单歌曲或直接播放网易云歌单。

### Android Auto

- 在车机端提供“收藏”和“网易云歌单”两个入口
- 支持浏览本地收藏歌单及其中的歌曲
- 支持从 Android Auto 播放收藏歌曲
- 网易云歌单入口目前提示先在手机端导入

## 技术栈

- Kotlin
- Java
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- OkHttp
- Coil 3
- SharedPreferences

## 运行要求

- Android Studio
- JDK 17
- Android SDK
- 最低 Android 版本：Android 6.0（API 23）
- 目标 Android 版本：API 36
- 可访问相关第三方音乐服务的网络环境

## 本地运行

1. 克隆仓库：

   ```bash
   git clone https://github.com/Diamond01010111/GD_Application.git
   cd GD_Application
   ```

2. 使用 Android Studio 打开项目。
3. 等待 Gradle Sync 完成。
4. 连接 Android 设备或启动模拟器。
5. 点击 **Run** 构建并安装应用。

若要测试 Android Auto，可使用 Android Studio 提供的 Desktop Head Unit，或连接支持 Android Auto 的设备和车辆。

## 第三方服务

项目使用 GD 音乐台 API 搜索音乐，并获取播放地址、专辑封面和歌词数据：

- API：`https://music-api.gdstudio.xyz/api.php`
- 来源：GD 音乐台（music.gdstudio.xyz）
- 文档标注的请求限制：5 分钟内不超过 50 次
- 文档标注的稳定音乐源：`netease`、`joox`、`bilibili`

网易云歌单页面通过网易云音乐的公开接口获取指定用户公开可见的歌单。接口、数据格式和可用性可能随时变化，私密歌单不会显示。

使用本项目时请自行确认并遵守各第三方服务的条款、许可证和请求频率限制。

## 待完善

- 网易云歌单详情和歌曲导入
- 歌词展示及滚动同步
- 可拖动的播放进度条
- 更完整的后台播放和通知栏控制
- 网络错误提示、重试和缓存策略
- 播放状态与页面状态恢复
- 深色模式、动画和界面细节
- 单元测试和 UI 测试

## 已知限制

- 第三方音乐源可能临时不可用
- 实际返回音质可能低于请求音质
- 播放链接可能具有时效性
- 清除应用数据会删除本地收藏及已保存的网易云用户 ID
- 应用进程被终止后，播放队列、当前歌曲和播放进度暂时无法完整恢复
- Android Auto 的网易云歌单浏览和播放尚未实现

## 免责声明

本项目不托管、不提供也不分发音乐文件。音乐、封面、歌词、歌单信息及其他第三方内容的权利归原作者、平台或相应权利人所有。

项目作者不保证第三方接口或内容的可用性、准确性、稳定性和合法性。使用者应自行承担使用第三方服务产生的责任。

GD 音乐台 API 由 GD Studio 提供，其文档声明采用 **CC BY-NC 4.0**，并要求仅用于学习用途。该授权适用于相应第三方服务，不代表项目作者拥有或能够再次授权其中的音乐及其他内容。

## License

本项目自行编写的代码采用 [MIT License](https://opensource.org/license/mit)。MIT License 不适用于第三方 API、依赖项、音乐、封面、歌词、商标或其他第三方内容；这些内容分别受其自身条款和许可证约束。
