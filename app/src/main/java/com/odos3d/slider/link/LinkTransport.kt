package com.odos3d.slider.link

interface LinkTransport {
    fun connect(address: String, onConnected: (Boolean, String?) -> Unit)
    fun isConnected(): Boolean
    fun write(bytes: ByteArray): Boolean
    fun setReader(onLine: (String) -> Unit)
    fun close()
}
