package com.example.asrpressspeak

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

/**
 * ASR服务客户端 - WebSocket通信
 */
class ASRServiceClient private constructor() {

    private var webSocketClient: WebSocketClient? = null
    private val gson = Gson()
    private var isConnected = false

    companion object {
        private const val TAG = "ASRServiceClient"

        @Volatile
        private var instance: ASRServiceClient? = null

        fun getInstance(): ASRServiceClient {
            return instance ?: synchronized(this) {
                instance ?: ASRServiceClient().also { instance = it }
            }
        }
    }

    /**
     * 连接到ASR服务
     * @param serverUrl WebSocket服务器地址
     * @param onConnected 连接成功回调
     * @param onMessage 收到消息回调
     * @param onError 错误回调
     */
    fun connect(
        serverUrl: String,
        onConnected: () -> Unit = {},
        onMessage: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            disconnect()

            val uri = URI.parse(serverUrl)
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshake: ServerHandshake) {
                    Log.d(TAG, "WebSocket连接成功")
                    isConnected = true
                    onConnected()
                }

                override fun onMessage(message: String) {
                    Log.d(TAG, "收到消息: $message")
                    onMessage(message)
                }

                override fun onMessage(bytes: ByteBuffer) {
                    Log.d(TAG, "收到二进制消息")
                }

                override fun onClose(code: Int, reason: String, remote: Boolean) {
                    Log.d(TAG, "WebSocket连接关闭: code=$code, reason=$reason")
                    isConnected = false
                }

                override fun onError(ex: Exception) {
                    Log.e(TAG, "WebSocket错误: ${ex.message}", ex)
                    onError(ex.message ?: "未知错误")
                }
            }

            webSocketClient?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}", e)
            onError(e.message ?: "连接失败")
        }
    }

    /**
     * 发送开始识别指令
     * @param config 识别配置
     */
    fun sendStartRecognition(config: ASRConfig) {
        try {
            val message = createStartMessage(config)
            Log.d(TAG, "发送开始识别消息: $message")
            webSocketClient?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "发送开始识别失败: ${e.message}", e)
        }
    }

    /**
     * 发送音频数据
     * @param audioData 音频数据（PCM格式）
     */
    fun sendAudioData(audioData: ByteArray) {
        try {
            Log.d(TAG, "发送音频数据: ${audioData.size} bytes")
            webSocketClient?.send(audioData)
        } catch (e: Exception) {
            Log.e(TAG, "发送音频数据失败: ${e.message}", e)
        }
    }

    /**
     * 发送结束识别指令
     */
    fun sendEndRecognition() {
        try {
            val message = createEndMessage()
            Log.d(TAG, "发送结束识别消息: $message")
            webSocketClient?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "发送结束识别失败: ${e.message}", e)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            webSocketClient?.close()
            webSocketClient = null
            isConnected = false
            Log.d(TAG, "WebSocket已断开")
        } catch (e: Exception) {
            Log.e(TAG, "断开连接失败: ${e.message}", e)
        }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 创建开始识别消息
     */
    private fun createStartMessage(config: ASRConfig): String {
        val json = JsonObject().apply {
            addProperty("type", "start")
            addProperty("format", "pcm")
            addProperty("rate", config.sampleRate)
            addProperty("channel", config.channel)
            addProperty("encoding", "s16le")
            addProperty("language", config.language)
            addProperty("interim_results", config.interimResults)
            addProperty("punctuation", config.punctuation)
        }
        return gson.toJson(json)
    }

    /**
     * 创建结束识别消息
     */
    private fun createEndMessage(): String {
        val json = JsonObject().apply {
            addProperty("type", "end")
        }
        return gson.toJson(json)
    }

    /**
     * 解析识别结果
     */
    fun parseRecognitionResult(message: String): String? {
        return try {
            val json = gson.fromJson(message, JsonObject::class.java)
            when (json.get("type")?.asString) {
                "result" -> json.get("text")?.asString
                "interim" -> json.get("text")?.asString
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析识别结果失败: ${e.message}", e)
            null
        }
    }
}

/**
 * ASR识别配置
 */
data class ASRConfig(
    val sampleRate: Int = 16000,
    val channel: Int = 1,
    val language: String = "zh-cn",
    val interimResults: Boolean = true,
    val punctuation: Boolean = true
)
