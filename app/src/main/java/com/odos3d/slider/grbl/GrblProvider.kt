package com.odos3d.slider.grbl

object GrblProvider {
    @Volatile
    var client: GrblClient? = null
}
