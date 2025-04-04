package com.st.predictivemaintenance.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.Utils.ConnectionOption
import com.st.predictivemaintenance.dashboard.datasource.PredictiveMaintenanceDeviceRepository
import com.st.predictivemaintenance.dashboard.datasource.local.PredictiveMaintenanceDatabase
import com.st.predictivemaintenance.dashboard.datasource.remote.PredictiveMaintenanceService
import com.st.predictivemaintenance.dashboard.deviceAdd.PredictiveMaintenanceDashboardDeviceAddFragment
import com.st.predictivemaintenance.dashboard.deviceConfigureWifi.PredictiveMaintenanceDashboardDeviceWifiConfigureFragment
import com.st.predictivemaintenance.dashboard.deviceList.PredictiveMaintenanceDashboardDeviceListFragment


class PredictiveMaintenanceDashboardActivity : AppCompatActivity() {

    companion object {

        fun getStartIntent(context: Context, node: Node) : Intent {
            val intent = Intent(context, PredictiveMaintenanceDashboardActivity::class.java)
            intent.putExtra(NODE_TAG_ARG, node.tag)
            intent.putExtra(CONNECTION_OPT_ARG, ConnectionOption.buildDefault())
            return intent
        }

        private val NODE_TAG_ARG = PredictiveMaintenanceDashboardActivity::class.java.name +
                ".NODE_TAG"
        private val CONNECTION_OPT_ARG = PredictiveMaintenanceDashboardActivity::class.java.name +
                ".CONNECTION_OPT_ARG"

        private val TRANSACTION_NAME_NAVIGATE_TO_DEVICE_ADD_FRAGMENT = PredictiveMaintenanceDashboardActivity::class.java.name +
                ".NAVIGATE_TO_DEVICE_ADD_FRAGMENT"

        private val TRANSACTION_NAME_NAVIGATE_TO_DEVICE_WIFI_CONFIGURE_FRAGMENT = PredictiveMaintenanceDashboardActivity::class.java.name +
                ".NAVIGATE_TO_DEVICE_WIFI_CONFIGURE_FRAGMENT"

    }

    private lateinit var viewModel: PredictiveMaintenanceDashboardViewModel

    private lateinit var interactionProgressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = PredictiveMaintenanceDashboardFragmentFactory()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_predictive_maintenance_dashboard)

        interactionProgressBar = findViewById(R.id.predictive_maintenance_dashboard_interaction_progress_bar)

        val i = intent

        val mNode = Manager.getSharedInstance().getNodeWithTag(intent.getStringExtra(NODE_TAG_ARG)!!)!!

        val deviceRepository = PredictiveMaintenanceDeviceRepository(
            PredictiveMaintenanceDatabase.instance(applicationContext),
            PredictiveMaintenanceService(),
        )

        val viewModelInstance : PredictiveMaintenanceDashboardViewModel by viewModels {
            PredictiveMaintenanceDashboardViewModel.Factory(mNode, deviceRepository)
        }
        viewModel = viewModelInstance
        viewModel.currentView.observe(this, { destination ->
            handleDestination(destination)
        })
        viewModel.feedbackMessage.observe(this, { message ->
            handleFeedbackMessage(message)
        })
        viewModel.progressBarStatus.observe(this, { status ->
            handleProgressBarStatus(status)
        })

    }

    override fun onStart() {
        super.onStart()
        viewModel.enableNotificationsFromNode()
    }

    override fun onPause() {
        super.onPause()
        viewModel.disableNotificationsFromNode()
    }

    private fun handleDestination(destinationView: PredictiveMaintenanceDashboardViewModel.Destination) {
        when (destinationView) {
            is PredictiveMaintenanceDashboardViewModel.Destination.LoginPage -> {
                viewModel.loginToDashboard(activityResultRegistry, this, applicationContext)
            }
            is PredictiveMaintenanceDashboardViewModel.Destination.DashboardClose -> {
                finish()
            }
            is PredictiveMaintenanceDashboardViewModel.Destination.DashboardDeviceList -> {
                navigateToDeviceList()
            }
            is PredictiveMaintenanceDashboardViewModel.Destination.DashboardDeviceAdd -> {
                navigateToDeviceAdd()
            }
            is PredictiveMaintenanceDashboardViewModel.Destination.DashboardDeviceWifiConfigure -> {
                navigateToDeviceWifiConfigure()
            }
        }
    }

    private fun handleFeedbackMessage(message: String) {
        Snackbar.make(
            findViewById(R.id.predictive_maintenance_dashboard_root_view),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun handleProgressBarStatus(status: PredictiveMaintenanceDashboardViewModel.ProgressBarStatus) {
        when (status) {
            is PredictiveMaintenanceDashboardViewModel.ProgressBarStatus.ProgressBarGone -> {
                progressBarGone()
            }
            is PredictiveMaintenanceDashboardViewModel.ProgressBarStatus.ProgressBarVisible -> {
                progressBarVisible()
            }
        }
    }

    private fun navigateToDeviceList() {
        clearBackStack()

        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, PredictiveMaintenanceDashboardDeviceListFragment::class.java.name)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.predictive_maintenance_dashboard_root_view, fragment)
            .commit()
    }

    private fun navigateToDeviceAdd() {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, PredictiveMaintenanceDashboardDeviceAddFragment::class.java.name)
        supportFragmentManager.beginTransaction()
            .replace(R.id.predictive_maintenance_dashboard_root_view, fragment)
            .addToBackStack(TRANSACTION_NAME_NAVIGATE_TO_DEVICE_ADD_FRAGMENT)
            .commit()
    }

    private fun navigateToDeviceWifiConfigure() {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, PredictiveMaintenanceDashboardDeviceWifiConfigureFragment::class.java.name)
        supportFragmentManager.beginTransaction()
            .replace(R.id.predictive_maintenance_dashboard_root_view, fragment)
            .addToBackStack(TRANSACTION_NAME_NAVIGATE_TO_DEVICE_WIFI_CONFIGURE_FRAGMENT)
            .commit()
    }

    private fun clearBackStack() {
        supportFragmentManager
            .popBackStack(TRANSACTION_NAME_NAVIGATE_TO_DEVICE_ADD_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager
            .popBackStack(TRANSACTION_NAME_NAVIGATE_TO_DEVICE_WIFI_CONFIGURE_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun progressBarGone() {
        interactionProgressBar.visibility = View.GONE
    }

    private fun progressBarVisible() {
        interactionProgressBar.visibility = View.VISIBLE
    }

    internal class PredictiveMaintenanceDashboardFragmentFactory : FragmentFactory() {

        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                PredictiveMaintenanceDashboardDeviceListFragment::class.java.name -> {
                    PredictiveMaintenanceDashboardDeviceListFragment()
                }
                else -> super.instantiate(classLoader, className)
            }
        }
    }

}