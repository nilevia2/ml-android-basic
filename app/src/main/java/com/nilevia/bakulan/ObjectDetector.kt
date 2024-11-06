package com.nilevia.bakulan

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.nilevia.bakulan.utils.Classifier
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetector(
    val context: Context,
    val classifierListener: ObjectDetectorListener?
): Classifier {

    private var objectDetector: ObjectDetector? = null
    private val threshold = 0.1f
    private val maxResult = 3
    private val cpuThread = 4
    private val ML_PATH = "efficientdet_lite0_v1.tflite"

    init {
        setUp()
    }

    override fun setUp() {
        val optionBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResult)
        val baseOptionBuilder = BaseOptions.builder()
            .setNumThreads(cpuThread)
        optionBuilder.setBaseOptions(baseOptionBuilder.build())

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                ML_PATH,
                optionBuilder.build()
            )
        } catch (e: Exception) {
            classifierListener?.onError(e.message.toString())
        }
    }

    override fun detect(imageProxy: ImageProxy){
        val bitmapTmp = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )

        val imageProcessor = ImageProcessor
            .Builder()
            .add(Rot90Op(-imageProxy.imageInfo.rotationDegrees / 90))
            .build()

        imageProxy.use { bitmapTmp.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmapTmp))
        val result = objectDetector?.detect(tensorImage)
        classifierListener?.onSuccess(
            result,
            tensorImage.height,
            tensorImage.width
        )

    }

    interface ObjectDetectorListener {
        fun onError(e: String)
        fun onSuccess(
            result: MutableList<Detection>?,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}