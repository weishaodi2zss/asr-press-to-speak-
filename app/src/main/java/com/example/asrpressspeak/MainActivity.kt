package com.example.asrpressspeak

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var btnPressToSpeak: Button
    private lateinit var tvResult: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnConfig: Button

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE_MULTIPLIER = 4

    // ASR服务配置
    private val asrService = ASRServiceClient.getInstance()
    private var serverUrl = "ws://localhost:8080/ws" // 默认地址
    private val asrConfig = ASRConfig()

    // 预设的服务地址
    private val serverPresets = listOf(
        "ws://localhost:8080/ws",
        "wss://your-server.com/ws",
        "ws://192.168.1.100:8080/ws"
    )

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupListeners()

        // 尝试连接到ASR服务
        connectToASRService()
    }

    private fun initViews() {
        btnPressToSpeak = findViewById(R.id.btnPressToSpeak)
        tvResult = findViewById(R.id.tvResult)
        tvStatus = findViewById(R.id.tvStatus)
        btnConfig = findViewById(R.id.btnConfig)
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

        btnConfig.setOnClickListener {
            showConfigDialog()
        }
    }

    /**
     * 显示配置对话框
     */
    private fun showConfigDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_config, null)
        val etServerUrl = dialogView.findViewById<EditText>(R.id.etServerUrl)
        val spinnerPresets = dialogView.findViewById<Spinner>(R.id.spinnerPresets)

        // 设置当前服务器地址
        etServerUrl.setText(serverUrl)

        // 设置预设服务器地址
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            serverPresets
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPresets.adapter = adapter

        // 当选择预设地址时，自动填充到输入框
        spinnerPresets.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.view.View?, view: android.view.View?, position: Int, id: Long) {
                etServerUrl.setText(serverPresets[position])
            }

            override fun onNothingSelected(parent: android.view.View?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("配置ASR服务")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newUrl = etServerUrl.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    serverUrl = newUrl
                    // 重新连接到新地址
                    connectToASRService()
                    showToast("配置已保存，正在重新连接...")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 连接到ASR服务
     */
    private fun connectToASRService() {
        updateStatus("正在连接到ASR服务...")

        asrService.connect(
            serverUrl = serverUrl,
            onConnected = {
                updateStatus("已连接到ASR服务: $serverUrl")
            },
            onMessage = { message ->
                handleASRMessage(message)
            },
            onError = { error ->
                updateStatus("连接失败: $error")
            }
        )
    }

    /**
     * 处理ASR服务返回的消息
     */
    private fun handleASRMessage(message: String) {
        val result = asrService.parseRecognitionResult(message)
        if (result != null) {
            runOnUiThread {
                tvResult.text = "识别结果：$result"
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return

        if (!asrService.isConnected()) {
            showToast("未连接到ASR服务")
            return
        }

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

            // 发送开始识别指令
            asrService.sendStartRecognition(asrConfig)

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

                    // 实时发送音频数据到ASR服务
                    asrService.sendAudioData(buffer.copyOfRange(0, readSize))

                    withContext(Dispatchers.Main) {
                        tvStatus.text = "正在录音... ${audioData.size() / 1024}KB"
                    }
                }
                delay(10)
            }

            // 录音结束，发送结束指令
            asrService.sendEndRecognition()
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

    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
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
        asrService.disconnect()
        recordingScope.cancel()
    }
}
