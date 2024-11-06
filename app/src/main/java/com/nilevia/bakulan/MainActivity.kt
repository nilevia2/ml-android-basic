package com.nilevia.bakulan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nilevia.bakulan.CameraActivity.Companion.MODE_IMAGE_DETECTOR
import com.nilevia.bakulan.CameraActivity.Companion.MODE_OBJECT_DETECTOR
import com.nilevia.bakulan.databinding.ActivityMainBinding
import com.nilevia.bakulan.utils.Constants.INTENT_MODE
import com.nilevia.bakulan.utils.PERMISSION_CAMERA
import com.nilevia.bakulan.utils.getImageUri
import com.nilevia.bakulan.utils.getPermission
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var currentImageUri: Uri? = null
    private var imageClassifier: ImageClassifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        getPermission(PERMISSION_CAMERA) { isGranted ->
            if (isGranted) {
                initAction()
                setImageClassifier()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initAction() {
        binding?.apply {
            btnGallery.setOnClickListener { startGallery() }
            btnCamera.setOnClickListener { startCamera() }
            btnLive.setOnClickListener{ startLiveDetector()}
            btnAnalyze.setOnClickListener {
                currentImageUri?.let {
                    imageClassifier?.staticImageDetection(it)
                }?: Toast.makeText(this@MainActivity, R.string.error_no_image, Toast.LENGTH_SHORT).show()

            }

            btnLiveObject.setOnClickListener {
                startObjectDetector()
            }
        }
    }

    private fun startGallery() {
        launcherGallery.launch(
            PickVisualMediaRequest(
                ActivityResultContracts
                    .PickVisualMedia
                    .ImageOnly
            )
        )
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Toast.makeText(this, R.string.error_no_image, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this).also {
            launcherIntentCamera.launch(it)
        }

    }


    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        } else {
            currentImageUri = null
        }
    }


    private fun startLiveDetector(){
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(INTENT_MODE, MODE_IMAGE_DETECTOR)
        }
        startActivity(intent)
    }

    private fun startObjectDetector(){
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(INTENT_MODE, MODE_OBJECT_DETECTOR)
        }
        startActivity(intent)
    }


    private fun showImage() {
        currentImageUri?.let {
            binding?.ivPreview?.setImageURI(it)
        }
    }

    private fun setImageClassifier(){
        imageClassifier = ImageClassifier(this, object : ImageClassifier.ImageClassifierListener{
            override fun onError(e: String) {
                binding?.tvResult?.text = getString(R.string.error_detection,e)
            }

            override fun onSuccess(result: List<Classifications>?) {
                // default sort by score
                var displayText = ""
               result?.let { list ->
                   if(list.isNotEmpty()){
                       displayText =  list[0].categories.joinToString("\n") {
                           val score = NumberFormat.getPercentInstance().format(it.score)
                           "${it.label} ${score}"
                       }
                   }
               }
                binding?.tvResult?.text = displayText
            }

        })
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}