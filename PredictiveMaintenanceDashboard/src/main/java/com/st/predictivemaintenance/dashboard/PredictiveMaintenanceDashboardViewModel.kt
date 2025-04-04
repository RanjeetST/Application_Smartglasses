package com.st.predictivemaintenance.dashboard

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.*
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.ExtConfiguration.FeatureExtConfiguration
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.WifSettings
import com.st.BlueSTSDK.Node
import com.st.login.AuthData
import com.st.login.Configuration
import com.st.login.LoginManager
import com.st.login.loginprovider.LoginProviderFactory
import com.st.predictivemaintenance.dashboard.datasource.PredictiveMaintenanceDeviceRepository
import com.st.predictivemaintenance.dashboard.models.PredictiveMaintenanceDevice
import com.st.predictivemaintenance.dashboard.models.getBoardCertificateDTO
import com.st.predictivemaintenance.dashboard.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * view model to manage Predictive Maintenance Devices
 */
class PredictiveMaintenanceDashboardViewModel(
    private val mNode: Node,
    private val mDeviceRepository: PredictiveMaintenanceDeviceRepository) : ViewModel() {

    sealed class Destination {
        object LoginPage : Destination()
        object DashboardClose : Destination()
        object DashboardDeviceList : Destination()
        object DashboardDeviceAdd : Destination()
        object DashboardDeviceWifiConfigure : Destination()
    }

    private val mCurrentView = MutableLiveData<Destination>(Destination.LoginPage)
    val currentView: LiveData<Destination>
        get() = mCurrentView

    sealed class ProgressBarStatus {
        object ProgressBarGone : ProgressBarStatus()
        object ProgressBarVisible : ProgressBarStatus()
    }

    private val mProgressBarStatus = MutableLiveData<ProgressBarStatus>(ProgressBarStatus.ProgressBarGone)
    val progressBarStatus: LiveData<ProgressBarStatus>
        get() = mProgressBarStatus

    private val mFeedbackMessage = MutableLiveData<String>()
    val feedbackMessage: LiveData<String>
        get() = mFeedbackMessage

    private val mPredictiveMaintenanceDevices = MutableLiveData<List<PredictiveMaintenanceDevice>>()
    val predictiveMaintenanceDevices: LiveData<List<PredictiveMaintenanceDevice>>
        get() = mPredictiveMaintenanceDevices

    private val mCurrentDeviceUIDLiveData = MutableLiveData<String>()
    val currentDeviceUIDLiveData: LiveData<String>
        get() = mCurrentDeviceUIDLiveData

    private var mFeature : FeatureExtConfiguration? = null
    private val mFeatureListener = Feature.FeatureListener { _, sample ->

        if (sample is FeatureExtConfiguration.CommandSample) {
            val responseObj = sample.command
            if (responseObj != null) {

                var answer = FeatureExtConfiguration.resultCommandList(responseObj)
                if (answer != null) {
                    Log.d("TAG", "Got Supported Commands: $answer")
                    val deviceSupportsNeededCommands = checkDeviceSupportsNeededCommands(answer)
                    if (!deviceSupportsNeededCommands) {
                        handleNotSupportedDevice()
                    }
                    else {
                        extConfigurationReadUID()
                    }
                }

                answer = FeatureExtConfiguration.resultCommandSTM32UID(responseObj)
                if (answer != null) {
                    Log.d("TAG", "Got UID: $answer")
                    mCurrentDeviceUIDLiveData.postValue(answer!!)
                }

            }
        }
    }

    private var mAuthData : AuthData? = null

    init {
        mFeature = mNode.getFeature(FeatureExtConfiguration::class.java)
        getDeviceList()
    }

    fun getNodeType() : Node.Type {
        return mNode.type
    }

    fun getNodeName() : String {
        return mNode.name
    }

    fun enableNotificationsFromNode() {
        mFeature?.apply {
            addFeatureListener(mFeatureListener)
            enableNotification()
            writeCommandWithoutArgument(FeatureExtConfiguration.READ_COMMANDS)
        }
    }

    fun disableNotificationsFromNode() {
        mFeature?.apply {
            removeFeatureListener(mFeatureListener)
            disableNotification()
        }
    }

    fun loginToDashboard(resultRegistry: ActivityResultRegistry, activity: Activity, context: Context) {
        mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarVisible)
        viewModelScope.launch(Dispatchers.IO) {

            CoroutineScope(Dispatchers.Main).launch {
                val authData = LoginManager(
                    resultRegistry,
                    activity,
                    context,
                    LoginProviderFactory.LoginProviderType.PREDMNT,
                    Configuration.getInstance(
                        context,
                        R.raw.auth_config_predictive
                    )
                ).login()
                mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarGone)
                if (authData != null) {
                    mCurrentView.postValue(Destination.DashboardDeviceList)
                    mAuthData = authData
                }
                else {
                    mCurrentView.postValue(Destination.DashboardClose)
                }
            }
        }
    }

    fun setCurrentDeviceUID(deviceUID: String) {
        mCurrentDeviceUIDLiveData.postValue(deviceUID)
    }

    fun setCurrentDeviceCertificate() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = mDeviceRepository.getDevice(mCurrentDeviceUIDLiveData.value!!, mAuthData!!)
            when (result) {
                is Result.Error -> {
                    mFeedbackMessage.postValue(result.exception.message)
                }
                is Result.Success -> {
                    val device = result.data
                    extConfigurationSetCertificate(device.getBoardCertificateDTO())
                }
            }
        }
    }

    fun setCurrentDeviceWifiSettings(wifiSettings: WifSettings) {
        extConfigurationSetWiFi(wifiSettings)
    }

    fun getDeviceList() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = mDeviceRepository.getDevices()
            when (result) {
                is Result.Error -> {
                    mFeedbackMessage.postValue(result.exception.message)
                }
                is Result.Success -> {
                    mPredictiveMaintenanceDevices.postValue(result.data!!)
                }
            }
        }
    }

    fun addDevice(device: PredictiveMaintenanceDevice) {
        mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarVisible)
        viewModelScope.launch(Dispatchers.IO) {
            val result = mDeviceRepository.addDevice(device, mAuthData!!)
            when (result) {
                is Result.Error -> {
                    mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarGone)
                    mFeedbackMessage.postValue(result.exception.message)
                }
                is Result.Success -> {
                    getDeviceList()
                    mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarGone)
                    mCurrentView.postValue(Destination.DashboardDeviceList)
                    mFeedbackMessage.postValue("Device Added")
                    setCurrentDeviceCertificate()
                }
            }
        }
    }

    fun deleteDevice(device: PredictiveMaintenanceDevice) {
        mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarVisible)
        viewModelScope.launch(Dispatchers.IO) {
            val result = mDeviceRepository.deleteDevice(device, mAuthData!!)
            mProgressBarStatus.postValue(ProgressBarStatus.ProgressBarGone)
            when (result) {
                is Result.Error -> {
                    mFeedbackMessage.postValue(result.exception.message)
                }
                is Result.Success -> {
                    mFeedbackMessage.postValue("Device Removed")
                    getDeviceList()
                }
            }
        }
    }

    fun onAddDeviceButtonClick() {
        mCurrentView.postValue(Destination.DashboardDeviceAdd)
    }

    fun onAddDeviceToDashboardButtonClick(device: PredictiveMaintenanceDevice) {
        addDevice(device)
    }

    fun onDeviceItemWifiConfigureButtonClick() {
        mCurrentView.postValue(Destination.DashboardDeviceWifiConfigure)
    }

    fun onDeviceItemCertificatesConfigureButtonClick() {
        setCurrentDeviceCertificate()
        mFeedbackMessage.postValue("Certificates Sent to Device")
    }

    fun onDeviceItemWifiConfigureButtonClick(wifiSettings: WifSettings) {
        setCurrentDeviceWifiSettings(wifiSettings)
        mCurrentView.postValue(Destination.DashboardDeviceList)
        mFeedbackMessage.postValue("Wifi Configuration sent")
    }

    fun checkDeviceSupportsNeededCommands(commandList: String) : Boolean {
        val deviceSupportsReadUID = commandList.contains(FeatureExtConfiguration.READ_UID)
        val deviceSupportsSetWiFi = commandList.contains(FeatureExtConfiguration.SET_WIFI)
        if (!deviceSupportsReadUID and !deviceSupportsSetWiFi) {
            return false
        }

        return true
    }

    private fun extConfigurationReadUID() {
        mFeature?.writeCommandWithoutArgument(FeatureExtConfiguration.READ_UID)
    }

    private fun extConfigurationSetCertificate(certificate: String) {
        Log.d("TAG", "sending certificate: $certificate")
        mFeature?.writeCommandSetArgumentString(FeatureExtConfiguration.SET_CERTIFICATE, certificate)
    }

    private fun extConfigurationSetWiFi(wifiSettings: WifSettings) {
        mFeature?.writeCommandSetArgumentJSON(FeatureExtConfiguration.SET_WIFI, wifiSettings)
    }

    private fun handleNotSupportedDevice() {
        mFeedbackMessage.postValue("Device does not support UID and SetWiFi commands")
        viewModelScope.launch {
            delay(2000L)
            mCurrentView.postValue(Destination.DashboardClose)
        }
    }

    class Factory(
        private val node: Node,
        private val deviceRepository: PredictiveMaintenanceDeviceRepository) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PredictiveMaintenanceDashboardViewModel(node, deviceRepository) as T
        }
    }

}