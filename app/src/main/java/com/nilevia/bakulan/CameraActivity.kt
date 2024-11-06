package com.nilevia.bakulan

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.nilevia.bakulan.databinding.ActivityCameraBinding
import com.nilevia.bakulan.utils.Classifier
import com.nilevia.bakulan.utils.Constants.INTENT_MODE
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.NumberFormat
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private var binding: ActivityCameraBinding? = null
    private var mode = MODE_OBJECT_DETECTOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        mode = intent.getIntExtra(INTENT_MODE, MODE_OBJECT_DETECTOR)
        startCamera()

    }

    // Main function to start the camera, initializing the classifier, preview, and analysis components
    private fun startCamera() {
        binding?.let { bind ->
            // Set up the image classifier helper
            val classifier =  if (mode == MODE_OBJECT_DETECTOR) setupObjectDetector() else setupImageClassifier()
            // Get an instance of the CameraProvider asynchronously
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            // Use addListener to safely get the cameraProvider once it’s ready
            // not always ready at begining
            cameraProviderFuture.addListener({
                // Configure the camera preview and analysis settings
                // do here to make sure camera is ready before set
                val preview = setupPreview()
                val imageAnalysis = setupAnalysis(bind.preview, classifier)
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@CameraActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(this))
        }
    }

    // Sets up the ImageClassifierHelper, responsible for running image classification
    private fun setupImageClassifier(): ImageClassifier {
        return ImageClassifier(
            context = this,
            classifierListener = object : ImageClassifier.ImageClassifierListener {
                // Handle any classification errors and display them as a Toast message
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@CameraActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onSuccess(result: List<Classifications>?) {
                    runOnUiThread{
                        var displayText = ""
                        result?.let { list ->
                            if (list.isNotEmpty()) {
                                displayText = list[0].categories.joinToString("\n") {
                                    val score = NumberFormat.getPercentInstance().format(it.score)
                                    "${it.label} ${score}"
                                }
                            }
                        }
                        binding?.tvResult?.text = displayText
                    }
                }
            }
        )
    }

    private fun setupObjectDetector(): ObjectDetector{
        binding?.tvResult?.visibility = View.GONE
        return ObjectDetector(
            context = this,
            classifierListener = object : ObjectDetector.ObjectDetectorListener{
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@CameraActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onSuccess(
                    result: MutableList<Detection>?,
                    imageHeight: Int,
                    imageWidth: Int
                ) {
                    if (result?.isNotEmpty() == true && result[0].categories.isNotEmpty()) {
                        // show bounding box
                        binding?.objectView?.setResults(
                            result, imageHeight, imageWidth
                        )
                    } else {
                        binding?.objectView?.clear()
                    }
                }
            }
        )
    }

    // Configures the camera preview, setting up a surface provider to display the camera feed
    private fun setupPreview(): Preview {
        return Preview.Builder().build().also {
            it.setSurfaceProvider(binding?.preview?.surfaceProvider)
        }
    }

    // Configures image analysis, setting up resolution, rotation, and an analyzer for processing frames
    // ImageAnalysis allow us to analyze image taken by camera and do action
    private fun setupAnalysis(previewView: PreviewView, imageClassifier: Classifier): ImageAnalysis {
        // Define the desired resolution strategy
        val resolutionSelector = ResolutionSelector.Builder()
            // select ascpect ratio if available, if not find closest one
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        return ImageAnalysis.Builder()
            // Set the selected resolution and rotation for image analysis
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(previewView.display.rotation)
            // only get latest image even previous image are not finish analyze yet
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // make the output format same as we define in the image classifier
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also { analysis ->
                // Set an analyzer to classify each frame with the ImageClassifierHelper
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                   imageClassifier.detect(image)
                }
            }

    }

    companion object {

        const val MODE_IMAGE_DETECTOR = 0
        const val MODE_OBJECT_DETECTOR = 1
    }

}