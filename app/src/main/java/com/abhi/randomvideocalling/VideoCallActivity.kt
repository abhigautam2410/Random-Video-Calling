package com.abhi.randomvideocalling

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.contains
import io.agora.rtc.Constants

import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import java.util.*

class VideoCallActivity : AppCompatActivity() {

    lateinit var imgEndCall: ImageView
    lateinit var imgAudioControl: ImageView
    lateinit var imgSwitchCamera: ImageView
    var joinChanelSuccess = false
    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            joinChanelSuccess = true
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { onRemoteUserLeft() }
        }

    }
    var rtcEngine: RtcEngine? = null
    private fun initializeAgoraEngine() {
        try {
            rtcEngine =
                RtcEngine.create(baseContext, getString(R.string.agora_app_id), rtcEventHandler)
        } catch (e: Exception) {
            Toast.makeText(this, "rtcEngine not initialize", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO,
                PERMISSION_REQ_ID_RECORD_AUDIO
            ) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
        ) {
            initAgoraEngineAndJoinChannel()
        } else {
            Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show()
        }

        imgEndCall = findViewById(R.id.imgEndCall)
        imgEndCall.setOnClickListener { onBackPressed() }

        imgAudioControl = findViewById(R.id.imgAudioControl)
        imgAudioControl.setOnClickListener {
            val audioToggle = imgAudioControl.drawable.constantState
            val imgMute = ContextCompat.getDrawable(this, R.drawable.btn_mute)?.constantState
            if (audioToggle != imgMute) {
                rtcEngine!!.muteLocalAudioStream(true)
                imgAudioControl.setImageResource(R.drawable.btn_mute)
            } else {
                rtcEngine!!.muteLocalAudioStream(false)
                imgAudioControl.setImageResource(R.drawable.btn_voice)
            }
        }

        imgSwitchCamera = findViewById(R.id.imgSwitchCamera)
        imgSwitchCamera.setOnClickListener { rtcEngine!!.switchCamera() }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                requestCode
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
                } else {
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            PERMISSION_REQ_ID_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel()
                } else {
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun initAgoraEngineAndJoinChannel(uid: Int = 0) {
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo(uid)
        onRejoinChanel(uid)
    }

    private fun setupVideoProfile() {
        rtcEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine!!.enableVideo()
        rtcEngine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    private fun setupLocalVideo(uid: Int) {

        rtcEngine!!.enableVideo()

        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        val surfaceView = RtcEngine.CreateRendererView(this)

        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        rtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun joinChannel(uid: Int) {
        var token: String? = getString(R.string.agora_access_token)
        if (token!!.isEmpty()) {
            token = null
        }
        rtcEngine!!.joinChannel(
            token,
            "abhishekchannel", "optionalInfo", uid
        )
    }


    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        if (container.childCount >= 1) {
            return
        }
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)

        rtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))


    }

    private fun onRemoteUserLeft() {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()
        onBackPressed()
    }

    override fun onBackPressed() {
        rtcEngine!!.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        super.onBackPressed()
    }

    private fun onRejoinChanel(uid: Int) {
        joinChannel(uid)
        val handler = Handler()
        val runnable = {
            if (!joinChanelSuccess) {
                Toast.makeText(this, "Try Again After Some Time", Toast.LENGTH_LONG).show()
                onBackPressed()
            }
        }
        handler.postDelayed(runnable, 15000)
    }
}