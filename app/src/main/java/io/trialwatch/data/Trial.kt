package io.trialwatch.data

import java.util.UUID

data class Trial(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String = "",
    val price: Double = 0.0,
    val end: Long,
    val done: Boolean = false,
    val created: Long = System.currentTimeMillis()
) {
    fun remaining(): Long = end - System.currentTimeMillis()
    fun isLive(): Boolean = !done && remaining() > 0
}
