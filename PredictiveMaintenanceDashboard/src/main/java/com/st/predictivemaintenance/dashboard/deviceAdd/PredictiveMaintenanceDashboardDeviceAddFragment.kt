package com.st.predictivemaintenance.dashboard.deviceAdd

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.st.predictivemaintenance.dashboard.PredictiveMaintenanceDashboardViewModel
import com.st.predictivemaintenance.dashboard.R
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice

class PredictiveMaintenanceDashboardDeviceAddFragment : Fragment() {

    private val viewModel: PredictiveMaintenanceDashboardViewModel by activityViewModels()

    private lateinit var deviceUIDTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var addDeviceToDashboardButton : Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_predictive_maintenance_dashboard_device_add,
            container,
            false
        )

        addDeviceToDashboardButton = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_add_add_device_to_dashboard_btn)
        addDeviceToDashboardButton.setOnClickListener {
            onAddDeviceToDashboardButton(it)
        }
        addDeviceToDashboardButton.isEnabled = false

        deviceUIDTextView = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_add_device_uid_textview)
        deviceUIDTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleAddDeviceToDashboardButtonAvailability()
            }
        })

        deviceNameTextView = rootView.findViewById(R.id.predictive_maintenance_dashboard_device_add_device_name_textview)
        deviceNameTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleAddDeviceToDashboardButtonAvailability()
            }
        })

        viewModel.currentDeviceUIDLiveData.observe(viewLifecycleOwner) {
            deviceUIDTextView.text = it
            deviceNameTextView.text = viewModel.getNodeName()
        }

        return rootView
    }

    private fun handleAddDeviceToDashboardButtonAvailability() {
        addDeviceToDashboardButton.isEnabled = inputDataIsSane()
    }

    private fun onAddDeviceToDashboardButton(view: View?) {

        val deviceId = deviceUIDTextView.text.toString()
        val deviceName = deviceNameTextView.text.toString()
        val device = PredictiveMaintenanceDevice(
            deviceId = deviceId,
            name = deviceName,
            deviceType = viewModel.getNodeType(),
            certificate = "",
            privateKey = ""
        )

        viewModel.setCurrentDeviceUID(deviceUIDTextView.text.toString())
        viewModel.onAddDeviceToDashboardButtonClick(device)

    }

    private fun inputDataIsSane() : Boolean {

        var inputDataIsSane = true
        if (deviceUIDTextView.text.isEmpty() or deviceNameTextView.text.isEmpty())
            inputDataIsSane = false

        return inputDataIsSane

    }

    private fun showInvalidConfigurationAlertDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder
            .setTitle("Error")
            .setIcon(R.drawable.ic_baseline_error_24)
            .setMessage("You need to specify a valid device name")
            .setPositiveButton("CANCEL"
            ) { dialog, id ->
                // user cancelled the dialog
            }
        builder.create().show()
    }

}