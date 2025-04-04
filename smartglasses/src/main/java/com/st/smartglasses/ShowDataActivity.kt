package com.st.smartglasses

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.FeatureWLC
import com.st.BlueSTSDK.Features.*
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node

class ShowDataActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "ShowDataActivity"
        private const val NODE_TAG_KEY = "tag"
    }
    private lateinit var temperatureText: TextView
    private lateinit var temperatureImage: ImageView
    private lateinit var temperatureCard: CardView
    private lateinit var temperatureSwitch: SwitchCompat
    private lateinit var temperatureTextOffset: TextView

    private lateinit var accelerText: TextView
    private lateinit var accelerCard: CardView

    private lateinit var gyroText: TextView
    private lateinit var gyroCard: CardView

    private lateinit var pressureText: TextView
    private lateinit var pressureCard: CardView

    private lateinit var sensorFusionText: TextView
    private lateinit var sensorFusionCard: CardView

    private lateinit var compassText: TextView
    private lateinit var compassCard: CardView

    private lateinit var batteryText: TextView
    private lateinit var batteryCard: CardView
    private lateinit var batteryImage: ImageView

    private var node: Node? = null
    private var temperatureOffset = 0f
    private var temperatureCelsius = true
    private var chargingLastState = FeatureWLC.AudioClass.UNKNOWN

    private lateinit var temperatureFeatures: List<FeatureTemperature>
    private lateinit var accelerFeatures: List<FeatureAcceleration>
    private lateinit var gyroFeatures: List<FeatureGyroscope>
    private lateinit var batteryFeatures: List<FeatureBattery>
    private lateinit var pressureFeatures: List<FeaturePressure>
    private lateinit var wlcFeatures: List<FeatureWLC>
    private var sensorFusion: FeatureAutoConfigurable? = null
    private var compassFeature: FeatureCompass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showdata)
        window.statusBarColor = getColor(R.color.blue)

        val nodeTag = intent.getStringExtra(NODE_TAG_KEY) ?: ""
        node = Manager.getSharedInstance().getNodeWithTag(nodeTag)

        if (node == null) {
            Log.e(TAG, "Node is null")
            return
        }

        initUI()
    }

    override fun onResume() {
        super.onResume()
        node?.let { enableNotifications(it) }
    }

    override fun onPause() {
        super.onPause()
        node?.let { disableNotifications(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initUI() {
        temperatureText = findViewById(R.id.thermometerText)
        temperatureImage = findViewById(R.id.thermometerImage)
        temperatureCard = findViewById(R.id.thermometerCard)
        temperatureTextOffset = findViewById(R.id.thermometerTextOffset)
        temperatureSwitch = findViewById(R.id.thermometerSwitch)

        accelerText = findViewById(R.id.accelerText)
        accelerCard = findViewById(R.id.accelerCard)

        gyroText = findViewById(R.id.gyroText)
        gyroCard = findViewById(R.id.gyroCard)

        pressureText = findViewById(R.id.pressureText)
        pressureCard = findViewById(R.id.pressureCard)

        sensorFusionText = findViewById(R.id.sensorfusionText)
        sensorFusionCard = findViewById(R.id.sensorfusionCard)
        sensorFusionCard.setOnClickListener(this)

        compassText = findViewById(R.id.compassText)
        compassCard = findViewById(R.id.compassCard)

        batteryText = findViewById(R.id.batteryText)
        batteryCard = findViewById(R.id.batteryCard)
        batteryImage = findViewById(R.id.batteryImage)

        temperatureTextOffset.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                temperatureOffset = s?.toString()?.toFloatOrNull() ?: 0f
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        temperatureSwitch.setOnClickListener {
            temperatureCelsius = !temperatureCelsius
        }
    }

    private fun enableNotifications(node: Node) {
        temperatureFeatures = node.getFeatures(FeatureTemperature::class.java)
        if (temperatureFeatures.isNotEmpty()) {
            temperatureCard.visibility = View.VISIBLE
            temperatureFeatures.forEach {
                it.addFeatureListener(temperatureListener)
                node.enableNotification(it)
            }
        } else {
            temperatureCard.visibility = View.GONE
        }

        accelerFeatures = node.getFeatures(FeatureAcceleration::class.java)
        accelerFeatures.forEach {
            it.addFeatureListener(accelerListener)
            node.enableNotification(it)
        }
        accelerCard.visibility = if (accelerFeatures.isNotEmpty()) View.VISIBLE else View.GONE

        gyroFeatures = node.getFeatures(FeatureGyroscope::class.java)
        gyroFeatures.forEach {
            it.addFeatureListener(gyroListener)
            node.enableNotification(it)
        }
        gyroCard.visibility = if (gyroFeatures.isNotEmpty()) View.VISIBLE else View.GONE

        batteryFeatures = node.getFeatures(FeatureBattery::class.java)
        batteryFeatures.forEach {
            it.addFeatureListener(batteryListener)
            node.enableNotification(it)
        }
        batteryCard.visibility = if (batteryFeatures.isNotEmpty()) View.VISIBLE else View.GONE

        wlcFeatures = node.getFeatures(FeatureWLC::class.java)
        wlcFeatures.forEach {
            it.addFeatureListener(wlcListener)
            node.enableNotification(it)
        }

        compassFeature = node.getFeature(FeatureCompass::class.java)
        compassFeature?.let {
            it.addFeatureListener(compassListener)
            it.enableNotification()
            compassCard.visibility = View.VISIBLE
        } ?: run {
            compassCard.visibility = View.GONE
        }

        sensorFusion = node.getFeature(FeatureMemsSensorFusionCompact::class.java)
                ?: node.getFeature(FeatureMemsSensorFusion::class.java)

        sensorFusion?.let {
            it.addFeatureListener(sensorFusionListener)
            node.enableNotification(it)
            sensorFusionCard.visibility = View.VISIBLE
        } ?: run {
            sensorFusionCard.visibility = View.GONE
        }

        pressureFeatures = node.getFeatures(FeaturePressure::class.java)
        pressureFeatures.forEach {
            it.addFeatureListener(pressureListener)
            node.enableNotification(it)
        }
        pressureCard.visibility = if (pressureFeatures.isNotEmpty()) View.VISIBLE else View.GONE

    }

    private fun disableNotifications(node: Node) {
        temperatureFeatures.forEach {
            it.removeFeatureListener(temperatureListener)
            node.disableNotification(it)
        }
        accelerFeatures.forEach {
            it.removeFeatureListener(accelerListener)
            node.disableNotification(it)
        }
        gyroFeatures.forEach {
            it.removeFeatureListener(gyroListener)
            node.disableNotification(it)
        }
        batteryFeatures.forEach {
            it.removeFeatureListener(batteryListener)
            node.disableNotification(it)
        }
        wlcFeatures.forEach {
            it.removeFeatureListener(wlcListener)
            node.disableNotification(it)
        }
        pressureFeatures.forEach {
            it.removeFeatureListener(pressureListener)
            node.disableNotification(it)
        }
        compassFeature?.let {
            it.removeFeatureListener(compassListener)
            node.disableNotification(it)
        }
        sensorFusion?.let {
            it.removeFeatureListener(sensorFusionListener)
            node.disableNotification(it)
        }
    }

    private val temperatureListener = Feature.FeatureListener { _, sample ->
        val data = temperatureFeatures.map { FeatureTemperature.getTemperature(it.sample) }
                .map { temp -> if (temperatureCelsius) temp else temp * 9 / 5 + 32 }
                .toFloatArray()
        val unit = if (temperatureCelsius) "°C" else "°F"
        val displayText = getDisplayString("%.1f [%s]", unit, data, temperatureOffset)
        runOnUiThread { temperatureText.text = displayText }
    }

    private val accelerListener = Feature.FeatureListener { _, sample ->
        val text = "Accelerate X: ${FeatureAcceleration.getAccX(sample)}\n" +
                "Accelerate Y: ${FeatureAcceleration.getAccY(sample)}\n" +
                "Accelerate Z: ${FeatureAcceleration.getAccZ(sample)}"
        runOnUiThread { accelerText.text = text }
    }

    private val gyroListener = Feature.FeatureListener { _, sample ->
        val text = "Gyroscope X: ${FeatureGyroscope.getGyroX(sample)}\n" +
                "Gyroscope Y: ${FeatureGyroscope.getGyroY(sample)}\n" +
                "Gyroscope Z: ${FeatureGyroscope.getGyroZ(sample)}"
        runOnUiThread { gyroText.text = text }
    }

    private val batteryListener = Feature.FeatureListener { _, sample ->
        val batteryLevel = FeatureBattery.getBatteryLevel(sample)
        runOnUiThread { batteryText.text = "$batteryLevel %" }
    }

    private val wlcListener = Feature.FeatureListener { _, sample ->
        val newState = FeatureWLC.getAudioClass(sample)
        if (chargingLastState != newState) {
            chargingLastState = newState
            runOnUiThread {
                when (newState) {
                    FeatureWLC.AudioClass.CHARGING -> {
                        batteryImage.setImageResource(R.drawable.charging)
                        ToastView(this, "Charging")
                    }
                    FeatureWLC.AudioClass.CHARGED -> {
                        batteryImage.setImageResource(R.drawable.batterys)
                    }
                    else -> {}
                }
            }
        }
    }

    private val pressureListener = Feature.FeatureListener { _, sample ->
        val pressure = FeaturePressure.getPressure(sample)
        runOnUiThread { pressureText.text = "$pressure hPa" }
    }

    private val compassListener = Feature.FeatureListener { _, sample ->
        val angle = FeatureCompass.getCompass(sample)
        runOnUiThread { compassText.text = "Angle: $angle°" }
    }

    private val sensorFusionListener = Feature.FeatureListener { _, sample ->
        val text = "Qi: ${FeatureMemsSensorFusionCompact.getQi(sample)}\n" +
                "Qj: ${FeatureMemsSensorFusionCompact.getQj(sample)}\n" +
                "Qk: ${FeatureMemsSensorFusionCompact.getQk(sample)}"
        runOnUiThread { sensorFusionText.text = text }
    }

    private fun getDisplayString(format: String, unit: String, values: FloatArray, offset: Float): String {
        return values.joinToString("\n") {
            String.format(format, it + offset, unit)
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.sensorfusionCard) {
            val tag = intent.getStringExtra(NODE_TAG_KEY)  // 取当前 Activity 的传入参数
            val nextIntent = Intent(this, ModelActivity::class.java)
            nextIntent.putExtra(NODE_TAG_KEY, tag)
            startActivity(nextIntent)
        }
    }

}
