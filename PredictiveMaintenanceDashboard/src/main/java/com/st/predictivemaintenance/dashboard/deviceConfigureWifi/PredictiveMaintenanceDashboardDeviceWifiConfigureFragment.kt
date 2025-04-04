package com.st.predictivemaintenance.dashboard.deviceConfigureWifi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.WifSettings
import com.st.predictivemaintenance.dashboard.PredictiveMaintenanceDashboardViewModel
import com.st.predictivemaintenance.dashboard.R

class PredictiveMaintenanceDashboardDeviceWifiConfigureFragment : Fragment() {

    private val viewModel: PredictiveMaintenanceDashboardViewModel by activityViewModels()

    private val wifiSecurityTypeList = listOf("OPEN", "WEP", "WPA", "WPA2", "WPA/WPA2")

    private lateinit var ssidTextView : TextView
    private lateinit var passwdTextView : TextView
    private lateinit var wifiConfigureButton: Button

    private lateinit var wifiSecurityTypeDataAdapter : ArrayAdapter<String>
    private var wifiSecurityTypeAdapterPosition = 0;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_predictive_maintenance_device_wifi_configure,
            container,
            false
        )

        wifiSecurityTypeDataAdapter = ArrayAdapter(requireActivity(),
            android.R.layout.simple_spinner_item, wifiSecurityTypeList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val wifiSecurityTypeSpinner = rootView.findViewById<Spinner>(R.id.predictive_maintenance_dashboard_device_wifi_security)
        wifiSecurityTypeSpinner.adapter = wifiSecurityTypeDataAdapter
        wifiSecurityTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                wifiSecurityTypeAdapterPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                wifiSecurityTypeAdapterPosition = 0
            }
        }

        wifiConfigureButton = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_wifi_configure_button)
        wifiConfigureButton.setOnClickListener {
            onWifiConfigureButtonClick(it)
        }
        wifiConfigureButton.isEnabled = false

        ssidTextView = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_wifi_ssid)
        ssidTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleWifiConfigureButtonAvailability()
            }
        })

        passwdTextView = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_wifi_password)
        passwdTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleWifiConfigureButtonAvailability()
            }
        })

        return rootView
    }

    private fun handleWifiConfigureButtonAvailability() {
        wifiConfigureButton.isEnabled = inputDataIsSane()
    }

    private fun inputDataIsSane() : Boolean {

        var inputDataIsSane = true
        if (ssidTextView.text.isEmpty() or passwdTextView.text.isEmpty())
            inputDataIsSane = false

        return inputDataIsSane

    }

    private fun onWifiConfigureButtonClick(view: View?) {
        val ssid = ssidTextView.text.toString()
        val password = passwdTextView.text.toString()
        val securityType = wifiSecurityTypeDataAdapter.getItem(wifiSecurityTypeAdapterPosition).toString()

        val wifiSettings = WifSettings(
            enable = true,
            ssid = ssid,
            password = password,
            securityType = securityType
        )

        viewModel.onDeviceItemWifiConfigureButtonClick(wifiSettings)
    }

}