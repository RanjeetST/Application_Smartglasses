package com.st.predictivemaintenance.dashboard.deviceList

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.st.BlueSTSDK.gui.NodeGui
import com.st.predictivemaintenance.dashboard.PredictiveMaintenanceDashboardViewModel
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice
import com.st.ui.R

class PredictiveMaintenanceDashboardDeviceListAdapter(
    private val viewModel: PredictiveMaintenanceDashboardViewModel,
) : ListAdapter<PredictiveMaintenanceDevice, PredictiveMaintenanceDashboardDeviceListAdapter.ViewHolder>(PredictiveMaintenanceDeviceDiffCallback) {

    inner class ViewHolder(
        private val itemView: View,
        private val viewModel: PredictiveMaintenanceDashboardViewModel,
    ) : RecyclerView.ViewHolder(itemView) {

        private val deviceIdTextView: TextView = itemView.findViewById(R.id.node_id)
        private val deviceNameTextView: TextView = itemView.findViewById(R.id.nodeName)
        private val deviceConnectivityImageView: ImageView = itemView.findViewById(R.id.iv_connectivity)
        private val deviceIconImageView: ImageView = itemView.findViewById(R.id.nodeBoardIcon)
        private val wifiConfigureButtonImageView: ImageView = itemView.findViewById(com.st.predictivemaintenance.dashboard.R.id.predictive_maintenance_dashboard_device_list_item_actions_configure_wifi)
        private val certificatesConfigureButtonImageView: ImageView = itemView.findViewById(com.st.predictivemaintenance.dashboard.R.id.predictive_maintenance_dashboard_device_list_item_actions_configure_certificates)

        private var itemDevice: PredictiveMaintenanceDevice? = null

        fun bind(device: PredictiveMaintenanceDevice) {
            itemDevice = device
            deviceIdTextView.text = device.deviceId
            deviceNameTextView.text = device.name

            deviceConnectivityImageView.setImageDrawable(
                ContextCompat.getDrawable(
                    itemView.context,
                    com.st.BlueSTSDK.gui.R.drawable.connectivity_ble
                )
            )
            ImageViewCompat.setImageTintList(
                deviceConnectivityImageView,
                ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.colorConnectivityOffline)
                )
            )

            @DrawableRes val boardImageRes = NodeGui.getRealBoardTypeImage(device.deviceType)
            Glide.with(itemView.context)
                .load(boardImageRes)
                .fitCenter()
                .into(deviceIconImageView)

            itemView.setOnLongClickListener {
                itemDevice?.let {
                    onItemLongClick(it)
                }
                true
            }

            viewModel.currentDeviceUIDLiveData.observeForever {
                if (it == device.deviceId) {
                    bindConnectedDevice()
                }
            }

        }

        private fun bindConnectedDevice() {
            ImageViewCompat.setImageTintList(
                deviceConnectivityImageView,
                ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.colorConnectivityOnline)
                )
            )
            ImageViewCompat.setImageTintList(
                wifiConfigureButtonImageView,
                ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.colorConnectivityOnline)
                )
            )
            ImageViewCompat.setImageTintList(
                certificatesConfigureButtonImageView,
                ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color.colorConnectivityOnline)
                )
            )
            wifiConfigureButtonImageView.setOnClickListener {
                itemDevice?.let {
                    onItemWifiConfigureClick(it)
                }
            }
            certificatesConfigureButtonImageView.setOnClickListener {
                itemDevice?.let {
                    onItemCertificatesConfigureClick(it)
                }
            }
        }

        private fun onItemClick(device: PredictiveMaintenanceDevice) {

        }

        private fun onItemLongClick(device: PredictiveMaintenanceDevice) {

            val builder = AlertDialog.Builder(itemView.context)
            builder
                .setTitle("Delete Device")
                .setMessage("Are you sure you want to delete this device?")
                .setPositiveButton("YES"
                ) { dialog, id ->
                    viewModel.deleteDevice(device)
                }
                .setNegativeButton("CANCEL"
                ) { dialog, id ->
                    // user cancelled the dialog
                }
            builder.create().show()
        }

        private fun onItemWifiConfigureClick(device: PredictiveMaintenanceDevice) {
            viewModel.onDeviceItemWifiConfigureButtonClick()
        }

        private fun onItemCertificatesConfigureClick(device: PredictiveMaintenanceDevice) {

            val builder = AlertDialog.Builder(itemView.context)
            builder
                .setTitle("Reload Certificates")
                .setMessage("Do you want to reload certificates on current device?")
                .setPositiveButton("YES"
                ) { dialog, id ->
                    viewModel.onDeviceItemCertificatesConfigureButtonClick()
                }
                .setNegativeButton("CANCEL"
                ) { dialog, id ->
                    // user cancelled the dialog
                }
            builder.create().show()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val deviceListItemView = inflater.inflate(R.layout.node_list_item, parent, false)

        val deviceListItemActionsView = inflater.inflate(com.st.predictivemaintenance.dashboard.R.layout.fragment_predictive_maintenance_dashboard_device_list_item_actions, parent, false)
        deviceListItemView.findViewById<LinearLayout>(R.id.placeholder_for_custom_view).addView(deviceListItemActionsView)

        return ViewHolder(
            deviceListItemView,
            viewModel
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

}

object PredictiveMaintenanceDeviceDiffCallback : DiffUtil.ItemCallback<PredictiveMaintenanceDevice>() {
    override fun areItemsTheSame(oldItem: PredictiveMaintenanceDevice, newItem: PredictiveMaintenanceDevice): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: PredictiveMaintenanceDevice, newItem: PredictiveMaintenanceDevice): Boolean {
        return oldItem.deviceId == newItem.deviceId
    }
}
