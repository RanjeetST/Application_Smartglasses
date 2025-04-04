package com.st.clab.bleSensor.fwUpgradeChecker.repository

import android.content.SharedPreferences
import java.util.Date

internal class RemoteSyncDao(private val storage: SharedPreferences){

    var lastSync:Date
        get(){
            return Date(storage.getLong(LAST_SYNC_KEY,0))
        }
        set(value) {
            storage.edit()
                    .putLong(LAST_SYNC_KEY,value.time)
                    .apply()
        }


    companion object{
        private val LAST_SYNC_KEY = RemoteSyncDao::class.java.name+".LAST_SYNC_KEY"
    }

}