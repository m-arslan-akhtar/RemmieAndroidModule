package org.freedesktop.gstreamer.tutorials.tutorial_2

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.google.gson.Gson
import com.remmie.server.R
import com.remmie.server.ResponseData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.freedesktop.gstreamer.GStreamer
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.IOException
import kotlin.coroutines.CoroutineContext


class Tutorial2 : AppCompatActivity(), CoroutineScope {
    private external fun setupGPrintHandler() // Initialize native code, build pipeline, etc
    private external fun startRtspServer() // Initialize native code, build pipeline, etc
    private external fun gPrintLog(e: String)

    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    // Initialize native code, build pipeline, etc
    private external fun nativeInit() // Initialize native code, build pipeline, etc
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativePlay() // Set pipeline to PLAYING
    private external fun nativePause() // Set pipeline to PAUSED
    private val native_custom_data // Native code will use this to keep private data
            : Long = 0
    private var is_playing_desired // Whether the user asked to go to PLAYING
            = false
    private var mediaPlayer: MediaPlayer? = null
    private var libVLC: LibVLC? = null
    private var publish : Button? = null
    private var stop_publish : Button? = null
    private val client = OkHttpClient()
    private var session : FFmpegSession? = null
    val JSON: MediaType? = MediaType.parse("application/json")


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    // Called when the activity is first created.
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(R.layout.main)
        publish = findViewById(R.id.button_play)
        stop_publish = findViewById(R.id.button_stop)
        publish?.isEnabled = true
        stop_publish?.isEnabled = false
        publish?.setOnClickListener {
            is_playing_desired = true
            run("https://dev.service.arcadia.theremmie.com/api/v1/create-token")
            it.isEnabled = false
            stop_publish?.isEnabled = true
            //nativePlay()
        }
        stop_publish?.setOnClickListener {
            Toast.makeText(this, "Stop Publishing", Toast.LENGTH_SHORT).show()
            it.isEnabled = false
            publish?.isEnabled = true
            session?.cancel()
            is_playing_desired = false
           // nativePause()
        }
        if (savedInstanceState != null) {
            is_playing_desired = savedInstanceState.getBoolean("playing")
            Log.i("GStreamer", "Activity created. Saved state is playing:$is_playing_desired")
        } else {
            is_playing_desired = false
            Log.i("GStreamer", "Activity created. There is no saved state, playing: false")
        }

        // Start with disabled buttons, until native code is initialized
        //publish?.isEnabled = false
        //stop_publish?.isEnabled = false
        nativeInit()
        setupGPrintHandler()
        gPrintLog("hello gprint")
        //        startRtspServer();
        val videoLayout = findViewById<VLCVideoLayout>(R.id.videoplayer)
        val options = ArrayList<String>()
        options.add("--no-drop-late-frames")
        options.add("--no-skip-frames")
        options.add("--rtsp-tcp")
        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer!!.attachViews(videoLayout, null, false, true)
        playStream("rtsp://127.0.0.1:8554/test")
        //        playStream("rtsp://127.0.0.1:8554/test");
    }

    private fun playStream(url: String) {
        val media = Media(libVLC, Uri.parse(url))
        mediaPlayer!!.media = media
        mediaPlayer!!.play()
        media.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("GStreamer", "Saving state, playing:$is_playing_desired")
        outState.putBoolean("playing", is_playing_desired)
    }

    override fun onDestroy() {
        nativeFinalize()
        mediaPlayer!!.detachViews()
        mediaPlayer!!.release()
        libVLC!!.release()
        super.onDestroy()
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private fun setMessage(message: String) {
        //final TextView tv = (TextView) this.findViewById(R.id.textview_message);
        runOnUiThread {
            //            tv.setText(message);
        }
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private fun onGStreamerInitialized() {
        Log.i("GStreamer", "Gst initialized. Restoring state, playing:$is_playing_desired")
        // Restore previous playing state
        if (is_playing_desired) {
            nativePlay()

//            startRtspServer();
//            setupGPrintHandler();
//            gPrintLog("hello gstreamer gprint");
        } else {
            nativePause()
        }

        // Re-enable buttons, now that GStreamer is initialized
        val activity: Activity = this
        runOnUiThread {
            publish?.isEnabled = true
            stop_publish?.isEnabled = true
        }
    }


    fun run(url: String) {
       // val json = "{'uuid':'c2dd8a36-d219-11ed-afa1-0242ac120002'}"            //"uuid": "c2dd8a36-d219-11ed-afa1-0242ac120002"
        val json = "{\"uuid\":\"c2dd8a36-d219-11ed-afa1-0242ac120002\"}"

        val body: RequestBody = RequestBody.create( JSON, json)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val gson = Gson()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body()?.string()
                    if (responseBody != null) {
                        val responseData = gson.fromJson(responseBody, ResponseData::class.java)
                        println("URL: ${responseData.data.url}")

                        responseData.data.url.let { createFolder(it) }
                        runOnUiThread{

                            Toast.makeText(this@Tutorial2, "Start Publishing", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@Tutorial2, "URL Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        })
    }

    private fun createFolder(rtspUrlPublish: String) {
//        val rtspUrl = "rtsp://3.217.96.78:8554/live"
        val rtspUrl = "rtsp://127.0.0.1:8554/test"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("OHELLO", "onCreate: Process Start")
            var rslt = -1
            launch {
                rslt = downloadrtsp(rtspUrl, rtspUrlPublish)
                Log.i("OHELLO", "onCreate:  Process end  : $rslt")
            }
        } else {
            Log.e("OHELLO", "onCreate: Version Code Below Error")
        }

    }

    fun downloadrtsp(rtspUrl: String, rtspUrlPublish: String): Int {

//        val output = "rtsp://192.168.102.5:8554/output"
        val output = rtspUrlPublish
//            "rtsp://stream.arcadia.theremmie.com:554/c2dd8a36-d219-11ed-afa1-0242ac120002?env=development_service&access_token=1R4zOriGNniTjSGmyjJkcL0orU0p8aS3ZHsnh8bAqf7O5BXFwtBhsxALAW2C2O58&refresh_token=pMi2n51NXM6i3hkwqujlHlQ33Tt18u2BPgx2uP8skPUvs2qPwI3SNaNz1Avh1D2D"

        val command = "-f rtsp -i $rtspUrl -c:v copy -f rtsp -rtsp_transport tcp $output"
//        val command = " -rtsp_transport tcp -i $rtspLink -c copy -t 10 $outputFile"
//        val command = "-i $rtspLink -c:v copy -timeout 5 -rtsp_transport tcp $outputFile"


         session = FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                // Command execution was successful
                runOnUiThread{
                    Toast.makeText(this@Tutorial2, "Publishing Success", Toast.LENGTH_SHORT).show()
                    publish?.isEnabled = false
                    stop_publish?.isEnabled = true
                }
                Log.i("FFmpegKit", "FFmpeg command executed successfully")
            } else if (ReturnCode.isCancel(returnCode)) {
                // Command execution was cancelled
                runOnUiThread{
                    Toast.makeText(this@Tutorial2, "Publishing Cancelled", Toast.LENGTH_SHORT).show()
                    publish?.isEnabled = true
                    stop_publish?.isEnabled = false
                }
                Log.i("FFmpegKit", "FFmpeg command execution cancelled")
            } else {
                // Command execution failed
                runOnUiThread{
                    Toast.makeText(this@Tutorial2, "Publishing Failed", Toast.LENGTH_SHORT).show()
                    publish?.isEnabled = true
                    stop_publish?.isEnabled = false
                }

                Log.e("FFmpegKit", "FFmpeg command execution failed with returnCode=$returnCode")
                Log.e("FFmpegKit", "Error output: ${session.allLogs}")
            }
        }


        return 0;
    }

    companion object {
        @JvmStatic
        external fun nativeClassInit(): Boolean
//        private external fun nativeClassInit(): Boolean // Initialize native class: cache Method IDs for callbacks

        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("tutorial-2")
            nativeClassInit()
        }
    }
}