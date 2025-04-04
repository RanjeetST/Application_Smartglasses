package com.st.smartglasses

import android.content.Context
import android.opengl.GLSurfaceView
import org.andresoviedo.util.math.Quaternion
import java.io.IOException


class ModelSurfaceView(
        val parent: ModelActivity
) : GLSurfaceView(parent) {

    private val renderer: ModelRenderer

    init {
        // Set OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        // Initialize and set renderer
        renderer = ModelRenderer(this)
        setRenderer(renderer)

        // Optionally set to only render when data changes
        // renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun getModelActivity(): ModelActivity = parent

    fun getModelRenderer(): ModelRenderer = renderer

    fun setQuaternion(quaternion: Quaternion) {
        renderer.setQuaternion(quaternion)
    }
}
