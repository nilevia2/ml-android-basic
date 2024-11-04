package com.nilevia.bakulan.otherexample

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface PredictApi {
    @Multipart // Add annotation to indicate that this method accepts a MultipartBody
    @POST("nilevia/predict")
    suspend fun predictImage(
        @Part file: MultipartBody.Part, // Sends files in a multi-part way
        @Part("category") category: RequestBody, // Add another param to the request
        @Part("timestamp") timestamp: RequestBody, // another param
        @Part("user_id") userId: RequestBody, // another param
        // or use @PartMap
        // @PartMap descriptions: Map<String,RequestBody>
    ): PredictResult
}