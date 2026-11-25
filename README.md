# TV 手柄 · 手机变手柄，电视大屏玩

两件套，必须配合用：

| 端 | 平台 | 作用 |
|---|---|---|
| **TV 手柄（电视端）** | Android TV（海信等安卓电视） | 常驻「正在等待识别」，接收手机指令并注入到电视屏幕 |
| **TV 手柄（遥控端）** | iPhone / iPad | 识别电视、添加设备、显示虚拟手柄 |

## 使用流程

1. 电视安装 `TV手柄_电视端.apk`，打开，授权**无障碍服务**（不授权只能识别、不能操控）。
2. 手机装 IPA（未签名，用轻松签 / Sideloadly / TrollStore 签一下），打开进「正在识别设备」。
3. 电视和手机连**同一个 WiFi**，手机 15 秒内会列出电视。
4. 点电视名字 → 添加 → 左上角「主界面」→ 右上角「已添加设备」→ 点设备进手柄。
5. 按键不对？顶部「如果不适配，请选择」下拉换预设：通用 / 我的世界 Java / 我的世界 基岩版 / 传说之下 / 模拟器通用。

## 原理

- 识别：UDP 47810 收 discover，UDP 47812 每 2 秒主动广播；TCP 47811 传指令（一行一条 JSON）。
- 注入：电视端用无障碍服务 `dispatchGesture` 把按键坐标变成触摸。所以预设本质是「电视屏幕上的触屏点」，游戏自带触屏摇杆/按钮即可操控，**不需要 root**。
- 电视若已 root，额外支持真实按键注入（`input keyevent`），供模拟器 / Java 版启动器使用。

## 协议

电视 → 手机（TCP 建连即发一行）：
```json
{"t":"tvpad","name":"电视 · HZ55","ip":"192.168.1.20","port":47811,"sw":1920,"sh":1080,"a11y":true}
```

手机 → 电视：
```json
{"t":"tap","x":0.5,"y":0.5}
{"t":"press","id":3,"x":0.86,"y":0.58,"hold":true}
{"t":"release","id":3}
{"t":"drag","x1":0.15,"y1":0.62,"x2":0.2,"y2":0.55,"ms":140}
{"t":"key","n":51}
```

坐标全部是 0~1 归一化，跟电视分辨率无关。

## 构建

Android：`cd android && gradle assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
iOS：`cd ios && xcodegen generate && xcodebuild -project TVPad.xcodeproj -scheme TVPad -configuration Release -sdk iphoneos -derivedDataPath build CODE_SIGNING_ALLOWED=NO build`

GitHub Actions 已配置（`.github/workflows/build.yml`），推上去自动出 APK 和未签名 IPA。
