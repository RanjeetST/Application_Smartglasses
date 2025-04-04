package com.st.smartglasses

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import org.andresoviedo.android_3d_model_engine.animation.Animator
import org.andresoviedo.android_3d_model_engine.drawer.DrawerFactory
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel
import org.andresoviedo.android_3d_model_engine.model.Camera
import org.andresoviedo.android_3d_model_engine.model.Object3D
import org.andresoviedo.android_3d_model_engine.model.Object3DData
import org.andresoviedo.android_3d_model_engine.services.Object3DBuilder
import org.andresoviedo.util.android.GLUtil
import org.andresoviedo.util.math.Quaternion
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(private val main: ModelSurfaceView) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ModelRenderer"
        private const val NEAR = 1f
        private const val FAR = 100f
        private const val EYE_DISTANCE = 0.64f
        private val COLOR_RED = floatArrayOf(1f, 0f, 0f, 1f)
        private val COLOR_BLUE = floatArrayOf(0f, 1f, 0f, 1f)
    }

    private var width: Int = 0
    private var height: Int = 0
    private val drawer = DrawerFactory(main.context)
    private val axis = Object3DBuilder.buildAxis().setId("axis")
    private val wireframes = mutableMapOf<Object3DData, Object3DData>()
    private val textures = mutableMapOf<Any, Int>()
    private val skeleton = mutableMapOf<Object3DData, Object3DData>()
    private val viewMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val lightPosInEyeSpace = FloatArray(4)
    private val viewMatrixLeft = FloatArray(16)
    private val projectionMatrixLeft = FloatArray(16)
    private val viewProjectionMatrixLeft = FloatArray(16)
    private val viewMatrixRight = FloatArray(16)
    private val projectionMatrixRight = FloatArray(16)
    private val viewProjectionMatrixRight = FloatArray(16)
    private val infoLogged = mutableMapOf<Object3DData, Boolean>()
    private var anaglyphSwitch = false
    private val animator = Animator()
    private var fatalException = false
    private var quaternion: Quaternion? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val backgroundColor = main.parent.getBackgroundColor()
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3])
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, NEAR, FAR)
        Matrix.frustumM(projectionMatrixRight, 0, -ratio, ratio, -1f, 1f, NEAR, FAR)
        Matrix.frustumM(projectionMatrixLeft, 0, -ratio, ratio, -1f, 1f, NEAR, FAR)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (fatalException) return

        try {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glScissor(0, 0, width, height)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val scene = main.parent.getScene() ?: return

            if (scene.isBlendingEnabled) {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            } else {
                GLES20.glDisable(GLES20.GL_BLEND)
            }

            scene.onDrawFrame()

            val camera = scene.camera
            if (camera.hasChanged()) {
                val ratio = width.toFloat() / height
                if (!scene.isStereoscopic) {
                    Matrix.setLookAtM(viewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView, camera.zView, camera.xUp, camera.yUp, camera.zUp)
                    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
                } else {
                    val (leftCamera, rightCamera) = camera.toStereo(EYE_DISTANCE)
                    Matrix.setLookAtM(viewMatrixLeft, 0, leftCamera.xPos, leftCamera.yPos, leftCamera.zPos, leftCamera.xView, leftCamera.yView, leftCamera.zView, leftCamera.xUp, leftCamera.yUp, leftCamera.zUp)
                    Matrix.setLookAtM(viewMatrixRight, 0, rightCamera.xPos, rightCamera.yPos, rightCamera.zPos, rightCamera.xView, rightCamera.yView, rightCamera.zView, rightCamera.xUp, rightCamera.yUp, rightCamera.zUp)

                    if (scene.isAnaglyph) {
                        Matrix.frustumM(projectionMatrixRight, 0, -ratio, ratio, -1f, 1f, NEAR, FAR)
                        Matrix.frustumM(projectionMatrixLeft, 0, -ratio, ratio, -1f, 1f, NEAR, FAR)
                    } else if (scene.isVRGlasses) {
                        val ratio2 = (width / 2f) / height
                        Matrix.frustumM(projectionMatrixRight, 0, -ratio2, ratio2, -1f, 1f, NEAR, FAR)
                        Matrix.frustumM(projectionMatrixLeft, 0, -ratio2, ratio2, -1f, 1f, NEAR, FAR)
                    }

                    Matrix.multiplyMM(viewProjectionMatrixLeft, 0, projectionMatrixLeft, 0, viewMatrixLeft, 0)
                    Matrix.multiplyMM(viewProjectionMatrixRight, 0, projectionMatrixRight, 0, viewMatrixRight, 0)
                }
                camera.setChanged(false)
            }

            if (!scene.isStereoscopic) {
                drawScene(viewMatrix, projectionMatrix, viewProjectionMatrix, lightPosInEyeSpace, null)
            } else if (scene.isAnaglyph) {
                if (anaglyphSwitch) {
                    drawScene(viewMatrixLeft, projectionMatrixLeft, viewProjectionMatrixLeft, lightPosInEyeSpace, COLOR_RED)
                } else {
                    drawScene(viewMatrixRight, projectionMatrixRight, viewProjectionMatrixRight, lightPosInEyeSpace, COLOR_BLUE)
                }
                anaglyphSwitch = !anaglyphSwitch
            } else if (scene.isVRGlasses) {
                GLES20.glViewport(0, 0, width / 2, height)
                GLES20.glScissor(0, 0, width / 2, height)
                drawScene(viewMatrixLeft, projectionMatrixLeft, viewProjectionMatrixLeft, lightPosInEyeSpace, null)
                GLES20.glViewport(width / 2, 0, width / 2, height)
                GLES20.glScissor(width / 2, 0, width / 2, height)
                drawScene(viewMatrixRight, projectionMatrixRight, viewProjectionMatrixRight, lightPosInEyeSpace, null)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Fatal exception: ${ex.message}", ex)
            fatalException = true
        }
    }

    private fun drawScene(viewMatrix: FloatArray, projectionMatrix: FloatArray, viewProjectionMatrix: FloatArray,
                          lightPosInEyeSpace: FloatArray, colorMask: FloatArray?) {
        val scene = main.parent.getScene()

        if (scene.isDrawLighting) {
            val lightDrawer = drawer.pointDrawer
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, scene.lightBulb.modelMatrix, 0)
            Matrix.multiplyMV(lightPosInEyeSpace, 0, modelViewMatrix, 0, scene.lightPosition, 0)
            lightDrawer.draw(scene.lightBulb, projectionMatrix, viewMatrix, -1, lightPosInEyeSpace, colorMask)
        }

        if (scene.isDrawAxis) {
            drawer.pointDrawer.draw(axis, projectionMatrix, viewMatrix, axis.drawMode, axis.drawSize, -1, lightPosInEyeSpace, colorMask)
        }

        for (objData in scene.objects) {
            try {
                val drawerObject = drawer.getDrawer(objData, scene.isDrawTextures, scene.isDrawLighting, scene.isDoAnimation, scene.isDrawColors) ?: continue

                if (!infoLogged.containsKey(objData)) {
                    Log.i(TAG, "Model '${objData.id}'. Drawer ${drawerObject.javaClass.name}")
                    infoLogged[objData] = true
                }

                val changed = objData.isChanged
                var textureId = textures[objData.textureData] ?: run {
                    objData.textureData?.let {
                        val textureStream = ByteArrayInputStream(it)
                        val emissiveStream = objData.emissiveTextureData?.let { data -> ByteArrayInputStream(data) }
                        val textureIds = GLUtil.loadTexture(textureStream, emissiveStream)
                        textureStream.close()
                        emissiveStream?.close()
                        objData.emissiveTextureHandle = textureIds[1]
                        textures[objData.textureData] = textureIds[0]
                        textures[objData.emissiveTextureData] = textureIds[1]
                        textureIds[0]
                    } ?: -1
                }

                if (objData.drawMode == GLES20.GL_POINTS) {
                    drawer.pointDrawer.draw(objData, projectionMatrix, viewMatrix, GLES20.GL_POINTS, lightPosInEyeSpace)
                } else if (scene.isDrawWireframe && objData.drawMode !in listOf(GLES20.GL_POINTS, GLES20.GL_LINES, GLES20.GL_LINE_STRIP, GLES20.GL_LINE_LOOP)) {
                    val wireframe = wireframes[objData] ?: Object3DBuilder.buildWireframe(objData).also { wireframes[objData] = it }
                    drawerObject.draw(wireframe, projectionMatrix, viewMatrix, wireframe.drawMode, wireframe.drawSize, textureId, lightPosInEyeSpace, colorMask)
                } else if (scene.isDrawPoints || objData.faces == null || !objData.faces.loaded()) {
                    drawerObject.draw(objData, projectionMatrix, viewMatrix, GLES20.GL_POINTS, objData.drawSize, textureId, lightPosInEyeSpace, colorMask)
                } else if (scene.isDrawSkeleton && objData is AnimatedModel && objData.animation != null) {
                    val skel = skeleton[objData] ?: Object3DBuilder.buildSkeleton(objData).also { skeleton[objData] = it }
                    animator.update(skel, scene.isShowBindPose)
                    drawer.getDrawer(skel, false, scene.isDrawLighting, scene.isDoAnimation, scene.isDrawColors)?.draw(skel, projectionMatrix, viewMatrix, -1, lightPosInEyeSpace, colorMask)
                } else {
                    drawerObject.setQuaternion(quaternion)
                    drawerObject.draw(objData, projectionMatrix, viewMatrix, textureId, lightPosInEyeSpace, colorMask)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error rendering object '${objData.id}': ${ex.message}", ex)
            }
        }
    }

    fun setQuaternion(q: Quaternion) {
        this.quaternion = q
    }

    fun getWidth() = width
    fun getHeight() = height
    fun getModelProjectionMatrix(): FloatArray = projectionMatrix
    fun getModelViewMatrix(): FloatArray = viewMatrix
}
