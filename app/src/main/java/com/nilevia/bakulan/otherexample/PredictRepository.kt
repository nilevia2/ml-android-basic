package com.nilevia.bakulan.otherexample

import com.nilevia.bakulan.otherexample.Constant.key_photo
import com.nilevia.bakulan.otherexample.Constant.type_image
import com.nilevia.bakulan.otherexample.Constant.type_text
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PredictRepository(
    val predictApi: PredictApi
) {

    suspend fun predictImage(
        file: File,
        category: String,
        userId: String,
        timestamp: String,
    ): PredictResult {

        val rbImageFile = file.asRequestBody(type_image.toMediaTypeOrNull())
        val imageMultipart: MultipartBody.Part =
            MultipartBody.Part.createFormData(
                key_photo, // key for server
                file.name, // filename as it appears on server
                rbImageFile)

        return predictApi.predictImage(
            imageMultipart,
            // RequestBody is the data sent by the client to your API.
            category.toRequestBody(type_text.toMediaType()),
            timestamp.toRequestBody(type_text.toMediaType()),
            userId.toRequestBody(type_text.toMediaType()),
        )

    }
}


object Constant {
    const val type_text = "text/plain"
    const val type_image = "image/jpeg"
    const val key_photo = "photo"
}
