# ASR按住识别说话Android项目

这是一个基于Android的语音识别项目，实现了按住说话、松开识别的功能，并集成了ASR服务协议。

## 功能特性

- ✅ 按住按钮开始录音
- ✅ 松开按钮结束录音并显示识别结果
- ✅ 实时显示录音状态和录音时长
- ✅ 支持自定义ASR服务地址
- ✅ WebSocket实时语音识别
- ✅ 音频数据实时传输到服务器
- ✅ 预设服务器地址快速切换
- ✅ 支持ws://和wss://协议

## 项目结构

```
app/src/main/
├── java/com/example/asrpressspeak/
│   ├── MainActivity.kt          # 主Activity，实现按住录音逻辑
│   └── ASRServiceClient.kt      # ASR服务客户端，WebSocket通信
├── res/
│   ├── layout/
│   │   ├── activity_main.xml   # 主界面布局
│   │   └── dialog_config.xml   # 配置对话框布局
│   ├── values/
│   │   ├── strings.xml         # 字符串资源
│   │   ├── themes.xml          # 主题样式
│   │   └── dimens.xml          # 尺寸资源
│   └── drawable/               # 图片资源
└── AndroidManifest.xml         # 应用配置文件
```

## 技术实现

### 核心功能

1. **音频采集**
   - 使用 `AudioRecord` 进行音频采集
   - 采样率：16000Hz
   - 采样精度：16bit PCM
   - 单声道录音

2. **WebSocket通信**
   - 使用 `Java-WebSocket` 库实现WebSocket连接
   - 实时传输音频数据到ASR服务
   - 接收识别结果并显示

3. **ASR服务协议**
   - 支持配置自定义服务器地址
   - 发送开始识别指令
   - 实时传输PCM音频数据
   - 发送结束识别指令
   - 解析识别结果

4. **按住识别交互**
   - 监听按钮的 `ACTION_DOWN` 和 `ACTION_UP` 事件
   - 按下时开始录音和识别
   - 松开时停止录音

## ASR服务协议

### 连接格式
```
ws://your-server.com/ws
wss://your-server.com/ws
```

### 开始识别消息
```json
{
  "type": "start",
  "format": "pcm",
  "rate": 16000,
  "channel": 1,
  "encoding": "s16le",
  "language": "zh-cn",
  "interim_results": true,
  "punctuation": true
}
```

### 结束识别消息
```json
{
  "type": "end"
}
```

### 识别结果消息
```json
{
  "type": "result",
  "text": "识别到的文本内容"
}
```

## 使用说明

1. 首次启动时会请求录音权限，点击"允许"
2. 点击"⚙️ 配置服务"按钮，设置ASR服务地址
   - 可以从下拉列表中选择预设地址
   - 也可以手动输入自定义地址
3. 应用会自动连接到配置的ASR服务
4. 按住中间的蓝色按钮开始录音
5. 松开按钮结束录音
6. 识别结果会显示在下方区域

## 配置说明

### 预设服务器地址

- `ws://localhost:8080/ws` - 本地开发服务器
- `wss://your-server.com/ws` - 生产服务器示例
- `ws://192.168.1.100:8080/ws` - 局域网服务器

### 自定义配置

在代码中修改 `ASRConfig` 可以调整识别参数：

```kotlin
val asrConfig = ASRConfig(
    sampleRate = 16000,      // 采样率
    channel = 1,              // 声道数
    language = "zh-cn",       // 语言
    interimResults = true,    // 是否返回中间结果
    punctuation = true        // 是否添加标点
)
```

## 系统要求

- Android 7.0 (API 24) 或更高版本
- 录音权限
- 网络权限
- ASR服务服务器

## 开发环境

- Android Studio Arctic Fox 或更高版本
- Kotlin 1.8.0
- Gradle 7.4.2

## 依赖库

- Java-WebSocket: WebSocket通信
- Gson: JSON解析
- Kotlin Coroutines: 协程支持

## ASR服务部署

参考项目：https://git.woa.com/shaodiwei/allai/tree/master/asr-web-service/web

部署ASR服务后，将WebSocket地址配置到应用中即可使用。

## 注意事项

⚠️ 确保ASR服务地址正确且可访问
⚠️ 使用wss://协议时需要配置HTTPS证书
⚠️ 网络延迟可能会影响识别的实时性
⚠️ 音频数据传输需要稳定的网络连接
