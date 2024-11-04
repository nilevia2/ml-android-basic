package com.nilevia.bakulan.utils

import androidx.camera.core.ImageProxy

interface Classifier {

    fun setUp()
    fun detect(imageProxy: ImageProxy)
}