package com.example.asrpressspeak

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var btnPressToSpeak: Button
    private lateinit var tvResult: TextView
    private lateinit var tvStatus: TextView

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE_MULTIPLIER = 4

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupListeners()
    }

    private fun initViews() {
        btnPressToSpeak = findViewById(R.id.btnPressToSpeak)
        tvResult = findViewById(R.id.tvResult)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    private fun setupListeners() {
        btnPressToSpeak.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            updateUI(true)
            startRecordingLoop(bufferSize)
        }
    }

    private fun startRecordingLoop(bufferSize: Int) {
        recordingScope.launch {
            val buffer = ByteArray(bufferSize)
            val audioData = ByteArrayOutputStream()

            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    audioData.write(buffer, 0, readSize)

                    // 模拟语音识别（实际项目中需要调用ASR服务API）
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "正在录音... ${audioData.size() / 1024}KB"
                    }
                }
                delay(10)
            }

            // 录音结束，处理识别结果
            val audioBytes = audioData.toByteArray()
            processRecognitionResult(audioBytes)
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        updateUI(false)
    }

    private fun processRecognitionResult(audioData: ByteArray) {
        // 模拟语音识别结果
        // 实际项目中需要调用ASR服务API（如讯飞、百度、阿里等）
        val mockResults = listOf(
            "你好，欢迎使用语音识别",
            "今天天气真不错",
            "语音识别功能测试成功",
            "按住说话释放识别",
            "这是一个示例项目"
        )

        val randomResult = mockResults.random()

        runOnUiThread {
            tvResult.text = "识别结果：$randomResult"
            tvStatus.text = "识别完成，录音时长：${audioData.size / 16000}秒"
        }
    }

    private fun updateUI(recording: Boolean) {
        runOnUiThread {
            if (recording) {
                btnPressToSpeak.text = "松开识别"
                btnPressToSpeak.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        android.R.color.holo_red_light
                    )
                )
                tvStatus.text = "正在录音..."
                tvResult.text = ""
            } else {
                btnPressToSpeak.text = "按住说话"
                btnPressToSpeak.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        android.R.color.holo_blue_bright
                    )
                )
                tvStatus.text = "准备就绪"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.text = "录音权限已授权"
            } else {
                tvStatus.text = "需要录音权限才能使用语音识别功能"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        recordingScope.cancel()
    }
}
