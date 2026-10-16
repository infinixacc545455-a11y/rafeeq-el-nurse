package com.damanhour.nursing.rafeeq

data class Msg(
    val text: String,
    val user: Boolean,
    val loading: Boolean = false,
    val welcome: Boolean = false
)
