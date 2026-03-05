# ASR按住识别说话Android项目

这是一个基于Android的语音识别项目，实现了按住说话、松开识别的功能。

## 功能特性

- ✅ 按住按钮开始录音
- ✅ 松开按钮结束录音并显示识别结果
- ✅ 实时显示录音状态和录音时长
- ✅ 音频数据采集和处理
- ✅ 权限管理和申请

## 项目结构

```
app/src/main/
├── java/com/example/asrpressspeak/
│   └── MainActivity.kt          # 主Activity，实现按住录音逻辑
├── res/
│   ├── layout/
│   │   └── activity_main.xml   # 主界面布局
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

2. **按住识别交互**
   - 监听按钮的 `ACTION_DOWN` 和 `ACTION_UP` 事件
   - 按下时开始录音，松开时停止录音
   - 使用协程进行异步音频处理

3. **权限管理**
   - 运行时申请 `RECORD_AUDIO` 权限
   - 权限请求结果处理

## 使用说明

1. 首次启动时会请求录音权限，点击"允许"
2. 按住中间的蓝色按钮开始录音
3. 松开按钮结束录音
4. 识别结果会显示在下方区域

## 注意事项

⚠️ 当前项目使用模拟识别结果，实际使用时需要集成ASR服务API，如：

- 科大讯飞语音识别
- 百度智能云语音识别
- 阿里云语音识别
- Google Speech-to-Text

## 集成ASR服务示例

如需集成真实的ASR服务，可以在 `processRecognitionResult()` 方法中调用相应的SDK：

```kotlin
private fun processRecognitionResult(audioData: ByteArray) {
    // 调用ASR服务API
    val asrClient = ASRClient(apiKey, apiSecret)
    asrClient.recognize(audioData) { result ->
        runOnUiThread {
            tvResult.text = "识别结果：$result"
        }
    }
}
```

## 系统要求

- Android 7.0 (API 24) 或更高版本
- 录音权限
- 网络权限（用于调用ASR服务）

## 开发环境

- Android Studio Arctic Fox 或更高版本
- Kotlin 1.8.0
- Gradle 7.4.2
