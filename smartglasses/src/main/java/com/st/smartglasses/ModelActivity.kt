package com.st.smartglasses

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.FeatureAutoConfigurable
import com.st.BlueSTSDK.Features.FeatureMemsSensorFusion
import com.st.BlueSTSDK.Features.FeatureMemsSensorFusionCompact
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import org.andresoviedo.util.android.ContentUtils
import org.andresoviedo.util.math.Quaternion
import java.io.IOException

class ModelActivity : Activity(), View.OnClickListener {

    companion object {
        private const val REQUEST_CODE_LOAD_TEXTURE = 1000
        private const val FULLSCREEN_DELAY = 10000
    }

    private var immersiveMode = true
    private val backgroundColor = floatArrayOf(0f, 0f, 0f, 1f)
    private lateinit var gLView: ModelSurfaceView
    private lateinit var scene: SceneLoader
    private lateinit var handler: Handler
    private var paramUri: Uri? = null
    private var paramType = 0
    private var mNode: Node? = null
    private var mSensorFusion: FeatureAutoConfigurable? = null
    private var initQuaternion: Quaternion? = null
    private var isReset = false

    private val mSensorFusionListener = Feature.FeatureListener { _, sample ->
        if (isReset) {
            initQuaternion = Quaternion(
                    FeatureMemsSensorFusionCompact.getQj(sample),
                    FeatureMemsSensorFusionCompact.getQk(sample),
                    FeatureMemsSensorFusionCompact.getQi(sample),
                    FeatureMemsSensorFusionCompact.getQs(sample)
            ).conjugate().multiply(Quaternion(0f, 1f, 0f, 0f))
            isReset = false
        }
        val currentQ = Quaternion(
                FeatureMemsSensorFusionCompact.getQj(sample),
                FeatureMemsSensorFusionCompact.getQk(sample),
                FeatureMemsSensorFusionCompact.getQi(sample),
                FeatureMemsSensorFusionCompact.getQs(sample)
        )
        val finalQuaternion = initQuaternion?.let { currentQ.multiply(it) } ?: currentQ

        runOnUiThread {
            gLView.setQuaternion(
                    Quaternion(
                            -finalQuaternion.x,
                            -finalQuaternion.y,
                            -finalQuaternion.z,
                            finalQuaternion.w
                    )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nodeTag = intent.getStringExtra("tag") ?: ""
        mNode = Manager.getSharedInstance().getNodeWithTag(nodeTag)
        if (mNode == null) Log.d("ModelActivity", "Node is null")

        paramUri = Uri.parse("assets://$packageName/man.obj")

        handler = Handler(mainLooper)
        scene = SceneLoader(this).apply { init() }

        setContentView(R.layout.activity_model)
        findViewById<View>(R.id.reset).setOnClickListener(this)

        try {
            gLView = ModelSurfaceView(this)
            findViewById<LinearLayout>(R.id.mylinear).addView(gLView)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading OpenGL view:\n${e.message}", Toast.LENGTH_LONG).show()
        }

        ContentUtils.printTouchCapabilities(packageManager)
        setupOnSystemVisibilityChangeListener()
    }

    private fun setupOnSystemVisibilityChangeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            window.decorView.setOnSystemUiVisibilityChangeListener {
                if ((it and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    hideSystemUIDelayed()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUIDelayed()
    }

    private fun hideSystemUIDelayed() {
        if (!immersiveMode) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ hideSystemUI() }, FULLSCREEN_DELAY.toLong())
    }

    private fun hideSystemUI() {
        if (!immersiveMode) return
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE
                    )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LOW_PROFILE
                    )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.model, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.model_toggle_wireframe -> scene.toggleWireframe()
            R.id.model_toggle_animation -> scene.toggleAnimation()
            R.id.model_toggle_lights -> scene.toggleLighting()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mNode?.let { enableNotification(it) }
    }

    override fun onPause() {
        super.onPause()
        mNode?.let { disableNotification(it) }
    }

    private fun enableNotification(node: Node) {
        mSensorFusion = getSensorFusion(node)
        mSensorFusion?.let {
            it.addFeatureListener(mSensorFusionListener)
            node.enableNotification(it)
        }
    }

    private fun disableNotification(node: Node) {
        mSensorFusion?.let {
            it.removeFeatureListener(mSensorFusionListener)
            node.disableNotification(it)
            mSensorFusion = null
        }
    }

    private fun getSensorFusion(node: Node): FeatureAutoConfigurable? {
        return node.getFeature(FeatureMemsSensorFusionCompact::class.java)
                ?: node.getFeature(FeatureMemsSensorFusion::class.java)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.reset -> {
                isReset = true
                initQuaternion = null
            }
        }
    }

    fun getParamUri(): Uri? = paramUri
    fun getParamType(): Int = paramType
    fun getBackgroundColor(): FloatArray = backgroundColor
    fun getScene(): SceneLoader = scene
    fun getGLView(): ModelSurfaceView = gLView
}
