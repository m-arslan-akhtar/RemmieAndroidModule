package com.remmie.server

data class ResponseData(
    val success: Boolean,
    val message: String,
    val data: Data
)

data class Data(
    val url: String
)
