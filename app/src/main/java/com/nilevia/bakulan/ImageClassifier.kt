package com.nilevia.bakulan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.nilevia.bakulan.utils.Classifier
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

/**
 * 1 Create a ImageClassifierClass
 */
class ImageClassifier(
    val context: Context,
    val classifierListener: ImageClassifierListener?
): Classifier {
    private var imageClassifier: ImageClassifier? = null
    private val threshold = 0.1f
    private val maxResult = 3
    private val cpuThread = 4
    private var imageProcessor : ImageProcessor? = null
    private val ML_PATH = "efficientnet-tflite-lite4-int8-v2.tflite"

    init {
        setUp()
    }
    override fun setUp() {
        val optionBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResult)
        val baseOptionBuilder = BaseOptions.builder()
            .setNumThreads(cpuThread)
        optionBuilder.setBaseOptions(baseOptionBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                ML_PATH,
                optionBuilder.build()
            )
            imageProcessor =  ImageProcessor.Builder().build()
        } catch (e: IllegalStateException) {
            classifierListener?.onError(e.message.toString())
        }
    }

    fun staticImageDetection(imageUri: Uri){
        // convert imageUri to Bitmap
        if (Build.VERSION.SDK_INT >= 28){
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }.copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
            // bitmap to TensorImage
            // A mutable Bitmap allows you to modify its pixels, which is helpful for editing, drawing, or applying transformations.
            val tensorImage = imageProcessor?.process(TensorImage.fromBitmap(bitmap))

            val result = imageClassifier?.classify(tensorImage)
            classifierListener?.onSuccess(result)

        }

    }

    override fun detect(imageProxy: ImageProxy){
       val bitmapTmp = Bitmap.createBitmap(
           imageProxy.width,
           imageProxy.height,
           Bitmap.Config.ARGB_8888
       )

        imageProxy.use { bitmapTmp.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        // create tensor image
        val tensorImage = imageProcessor?.process(TensorImage.fromBitmap(bitmapTmp))

        // add processing option before classify
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(imageProxy.imageInfo.rotationDegrees))
            .build()

        val result = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        classifierListener?.onSuccess(result)

    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ImageClassifierListener{
        fun onError(e:String)
        fun onSuccess(
            result: List<Classifications>?
        )
    }

}
