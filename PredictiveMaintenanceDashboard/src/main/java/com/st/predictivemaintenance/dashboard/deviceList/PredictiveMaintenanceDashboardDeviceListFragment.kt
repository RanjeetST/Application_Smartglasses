package com.st.predictivemaintenance.dashboard.deviceList

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.st.predictivemaintenance.dashboard.PredictiveMaintenanceDashboardViewModel
import com.st.predictivemaintenance.dashboard.R
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice

class PredictiveMaintenanceDashboardDeviceListFragment : Fragment() {

    private val viewModel: PredictiveMaintenanceDashboardViewModel by activityViewModels()

    private lateinit var emptyDeviceListTextView: TextView
    private lateinit var addDeviceBtn: View
    private lateinit var refreshDeviceListSwiper: SwipeRefreshLayout
    private lateinit var deviceListAdapter: PredictiveMaintenanceDashboardDeviceListAdapter
    private lateinit var rvDeviceList: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_predictive_maintenance_dashboard_device_list,
            container,
            false
        )

        emptyDeviceListTextView = rootView.findViewById(R.id.predictiveMaintenanceDashboardEmptyDeviceListTextView)

        addDeviceBtn = rootView.findViewById(R.id.predictiveMaintenanceDashboardDeviceAddFab)
        addDeviceBtn.setOnClickListener { view ->
            viewModel.onAddDeviceButtonClick()
        }

        refreshDeviceListSwiper = rootView.findViewById(R.id.predictiveMaintenanceDashboardRefreshDeviceListSwiper)
        refreshDeviceListSwiper.setOnRefreshListener {
            viewModel.getDeviceList()
        }

        deviceListAdapter = PredictiveMaintenanceDashboardDeviceListAdapter(viewModel)
        rvDeviceList = rootView.findViewById(R.id.rvPredictiveMaintenanceDashboardDeviceList) as RecyclerView
        rvDeviceList.adapter = deviceListAdapter

        viewModel.predictiveMaintenanceDevices.observe(viewLifecycleOwner, {
            handleDeviceListUpdate(it)
        })

        return rootView
    }

    private fun handleDeviceListUpdate(deviceList: List<PredictiveMaintenanceDevice>?) {
        deviceList?.let {
            if (it.isEmpty()) {
                emptyDeviceListTextView.visibility = View.VISIBLE
            }
            else {
                emptyDeviceListTextView.visibility = View.GONE
            }
            deviceListAdapter.submitList(it as MutableList<PredictiveMaintenanceDevice>)
            refreshDeviceListSwiper.isRefreshing = false
        }
    }

}