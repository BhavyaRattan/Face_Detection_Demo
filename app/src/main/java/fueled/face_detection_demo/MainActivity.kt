package fueled.face_detection_demo

import ai.deepar.ar.ARErrorType
import ai.deepar.ar.AREventListener
import ai.deepar.ar.CameraResolutionPreset.P1280x720
import ai.deepar.ar.DeepAR
import ai.deepar.ar.DeepAR.FaceData
import ai.deepar.ar.DeepAR.FaceTrackedCallback
import android.Manifest.permission
import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera.CameraInfo
import android.media.Image
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView

import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fueled.face_detection_demo.CameraGrabber.cameraSurfaceView
import fueled.face_detection_demo.R.layout

import kotlin.math.min
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), Callback, AREventListener {

    companion object {
        const val EYE_AR_THRESH = 0.2
        const val MOUTH_RATIO = 0.5
    }

    private var cameraGrabber: CameraGrabber? = null
    private var deepAR: DeepAR? = null
    private var faceData: FaceData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE, permission.RECORD_AUDIO),
                1)
        } else {
            // Permission has already been granted
            initialize()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return  // no permission
                }
                initialize()
            }
        }
    }

    private fun initialize() {
        setContentView(layout.activity_main)
        initializeViews()
        initializeDeepAR()
    }

    private fun initializeViews() {
        val arView = findViewById<SurfaceView>(R.id.surface)
        val startBtn = findViewById<Button>(R.id.start)
        arView.holder.addCallback(this)

//        // Surface might already be initialized, so we force the call to onSurfaceChanged
//        arView.visibility = View.GONE
//        arView.visibility = View.VISIBLE

        startBtn.setOnClickListener {
            deepAR!!.setVisionOnly()
        }
    }

    // if the device's natural orientation is portrait:

    /*
              get interface orientation from
              https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164
           */
    private val screenOrientation: Int
        private get() {
            val rotation = windowManager.defaultDisplay.rotation
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(dm)
            val width = dm.widthPixels
            val height = dm.heightPixels
            val orientation: Int
            // if the device's natural orientation is portrait:
            orientation = if ((rotation == Surface.ROTATION_0
                    || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                    || rotation == Surface.ROTATION_270) && width > height) {
                when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            } else {
                when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
            return orientation
        }

    private fun initializeDeepAR() {
        deepAR = DeepAR(this)
        deepAR!!.setLicenseKey("02b3c2a499a5a773f59b4b7a0eb2f4e03ba90eadf905e1eb94ca42e681e75ef3d8de176ba1e311dd")
        deepAR!!.initialize(this, this)

        deepAR!!.faceTrackedCallback = FaceTrackedCallback { faces ->
            faceData = faces[0]
            updateFaceData()
        }
    }

    private fun distance(points: Array<Float>): Float {
        return sqrt((points[2] - points[0]) * (points[2] - points[0]) + (points[3] - points[1]) * (points[3] - points[1]))
    }

    private fun updateFaceData() {
        faceData?.run {
            val leftEyeVertical1 = distance(arrayOf(
                landmarks2d[((38 * 2) - 2)],
                landmarks2d[((38 * 2) - 1)],
                landmarks2d[((42 * 2) - 2)],
                landmarks2d[((42 * 2) - 1)]))

            val leftEyeVertical2 = distance(arrayOf(
                landmarks2d[((39 * 2) - 2)],
                landmarks2d[((39 * 2) - 1)],
                landmarks2d[((41 * 2) - 2)],
                landmarks2d[((41 * 2) - 1)]))

            val leftEyeHorizontal = distance(arrayOf(
                landmarks2d[((37 * 2) - 2)],
                landmarks2d[((37 * 2) - 1)],
                landmarks2d[((40 * 2) - 2)],
                landmarks2d[((40 * 2) - 1)]))

            val leftEyeAspectRatio = (leftEyeVertical1 + leftEyeVertical2) / (2.0 * leftEyeHorizontal)

            val rightEyeVertical1 = distance(arrayOf(
                landmarks2d[((44 * 2) - 2)],
                landmarks2d[((44 * 2) - 1)],
                landmarks2d[((48 * 2) - 2)],
                landmarks2d[((48 * 2) - 1)]))

            val rightEyeVertical2 = distance(arrayOf(
                landmarks2d[((45 * 2) - 2)],
                landmarks2d[((45 * 2) - 1)],
                landmarks2d[((47 * 2) - 2)],
                landmarks2d[((47 * 2) - 1)]))

            val rightEyeHorizontal = distance(arrayOf(
                landmarks2d[((43 * 2) - 2)],
                landmarks2d[((43 * 2) - 1)],
                landmarks2d[((46 * 2) - 2)],
                landmarks2d[((46 * 2) - 1)]))

            val rightEyeAspectRatio = (rightEyeVertical1 + rightEyeVertical2) / (2.0 * rightEyeHorizontal)

            val topLip1 = distance(arrayOf(
                landmarks2d[((51 * 2) - 2)],
                landmarks2d[((51 * 2) - 1)],
                landmarks2d[((62 * 2) - 2)],
                landmarks2d[((62 * 2) - 1)]))

            val topLip2 = distance(arrayOf(
                landmarks2d[((52 * 2) - 2)],
                landmarks2d[((52 * 2) - 1)],
                landmarks2d[((63 * 2) - 2)],
                landmarks2d[((63 * 2) - 1)]))

            val topLip3 = distance(arrayOf(
                landmarks2d[((53 * 2) - 2)],
                landmarks2d[((53 * 2) - 1)],
                landmarks2d[((64 * 2) - 2)],
                landmarks2d[((64 * 2) - 1)]))

            val topLipHeight = (topLip1 + topLip2 + topLip3) / 3

            val bottomLip1 = distance(arrayOf(
                landmarks2d[((59 * 2) - 2)],
                landmarks2d[((59 * 2) - 1)],
                landmarks2d[((68 * 2) - 2)],
                landmarks2d[((68 * 2) - 1)]))

            val bottomLip2 = distance(arrayOf(
                landmarks2d[((58 * 2) - 2)],
                landmarks2d[((58 * 2) - 1)],
                landmarks2d[((67 * 2) - 2)],
                landmarks2d[((67 * 2) - 1)]))

            val bottomLip3 = distance(arrayOf(
                landmarks2d[((57 * 2) - 2)],
                landmarks2d[((57 * 2) - 1)],
                landmarks2d[((66 * 2) - 2)],
                landmarks2d[((66 * 2) - 1)]))

            val bottomLipHeight = (bottomLip1 + bottomLip2 + bottomLip3) / 3


            val mouth1 = distance(arrayOf(
                landmarks2d[((62 * 2) - 2)],
                landmarks2d[((62 * 2) - 1)],
                landmarks2d[((68 * 2) - 2)],
                landmarks2d[((68 * 2) - 1)]))

            val mouth2 = distance(arrayOf(
                landmarks2d[((63 * 2) - 2)],
                landmarks2d[((63 * 2) - 1)],
                landmarks2d[((67 * 2) - 2)],
                landmarks2d[((67 * 2) - 1)]))

            val mouth3 = distance(arrayOf(
                landmarks2d[((64 * 2) - 2)],
                landmarks2d[((64 * 2) - 1)],
                landmarks2d[((66 * 2) - 2)],
                landmarks2d[((66 * 2) - 1)]))

            val mouthHeight = (mouth1 + mouth2 + mouth3) / 3

            Log.e("mouthHeight", "$mouthHeight")
            // subject.onNext(emotions[FaceData.EMOTION_IDX_NEUTRAL])

            runOnUiThread {
                "Left Eye: ${if (leftEyeAspectRatio > EYE_AR_THRESH) "open" else "closed"}"
                    .also { findViewById<TextView>(R.id.leftEyeTextView).text = it }
                "Right Eye: ${if (rightEyeAspectRatio > EYE_AR_THRESH) "open" else "closed"}"
                    .also { findViewById<TextView>(R.id.rightEyeTextView).text = it }
                "Mouth: ${if (mouthHeight > min(topLipHeight, bottomLipHeight) * MOUTH_RATIO) "open" else "closed"}"
                    .also { findViewById<TextView>(R.id.mouthTextView).text = it }

                "Neutral: ${emotions[FaceData.EMOTION_IDX_NEUTRAL]}"
                    .also { findViewById<TextView>(R.id.neutralTextView).text = it }

                "Happiness: ${emotions[FaceData.EMOTION_IDX_HAPPINESS]}"
                    .also { findViewById<TextView>(R.id.happinessTextView).text = it }

                "Sadness: ${emotions[FaceData.EMOTION_IDX_SADNESS]}"
                    .also { findViewById<TextView>(R.id.sadnessTextView).text = it }

                "Surprise: ${emotions[FaceData.EMOTION_IDX_SURPRISE]}"
                    .also { findViewById<TextView>(R.id.surpriseTextView).text = it }

                "Anger: ${emotions[FaceData.EMOTION_IDX_ANGER]}"
                    .also { findViewById<TextView>(R.id.angerTextView).text = it }
            }
        }
    }

    private fun setupCamera(surface: SurfaceHolder) {
        val defaultCameraDevice = CameraInfo.CAMERA_FACING_FRONT
        cameraGrabber = CameraGrabber(defaultCameraDevice, surface)
        val screenOrientation = screenOrientation
        when (screenOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> cameraGrabber!!.screenOrientation = 90
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> cameraGrabber!!.screenOrientation = 270
            else -> cameraGrabber!!.screenOrientation = 0
        }

        // Available 1080p, 720p and 480p resolutions
        cameraGrabber!!.resolutionPreset = P1280x720
        val context: Activity = this
        cameraGrabber!!.initCamera(object : CameraGrabberListener {
            override fun onCameraInitialized() {
                cameraGrabber!!.setFrameReceiver(deepAR)
                cameraGrabber!!.startPreview()
            }

            override fun onCameraError(errorMsg: String) {
                val builder = Builder(context)
                builder.setTitle("Camera error")
                builder.setMessage(errorMsg)
                builder.setCancelable(true)
                builder.setPositiveButton("Ok") { dialogInterface, i -> dialogInterface.cancel() }
                val dialog = builder.create()
                dialog.show()
            }
        })
    }

    override fun onStop() {
        super.onStop()
        if (cameraGrabber == null) {
            return
        }
        cameraGrabber!!.setFrameReceiver(null)
        cameraGrabber!!.stopPreview()
        cameraGrabber!!.releaseCamera()
        cameraGrabber = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (deepAR == null) {
            return
        }
        deepAR!!.setAREventListener(null)
        deepAR!!.release()
        deepAR = null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.e("surfaceCreated", "surfaceCreated")
        if (holder != null)
            setupCamera(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        cameraSurfaceView = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        cameraSurfaceView = null
    }

    override fun screenshotTaken(bitmap: Bitmap) {}
    override fun videoRecordingStarted() {}
    override fun videoRecordingFinished() {}
    override fun videoRecordingFailed() {}
    override fun videoRecordingPrepared() {}
    override fun shutdownFinished() {}
    override fun initialized() {}
    override fun faceVisibilityChanged(b: Boolean) {}
    override fun imageVisibilityChanged(s: String, b: Boolean) {}
    override fun frameAvailable(image: Image) {}
    override fun error(arErrorType: ARErrorType, s: String) {}
    override fun effectSwitched(s: String) {}
}