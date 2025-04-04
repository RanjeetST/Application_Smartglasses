package com.st.clab.bleSensor.fwUpgradeChecker.repository

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import com.st.BlueSTSDK.gui.fwUpgrade.fwVersionConsole.FwVersionBoard

@Entity(primaryKeys = ["boardType","mcu", "name"], tableName = "DeviceFirmware")
internal data class DeviceFirmware(@ColumnInfo(name = "boardType")
                          @SerializedName("boardType")
                          val boardType: Byte,
                          @SerializedName("name")
                          @ColumnInfo(name = "name")
                          val name: String,
                          @SerializedName("mcu")
                          @ColumnInfo(name = "mcu")
                          val mcu: String,
                          @SerializedName("relativeFwPath")
                          @ColumnInfo(name = "relativeFwPath")
                          val relativeFwPath: String,
                          @SerializedName("version_major")
                          @ColumnInfo(name = "version_major")
                          val versionMajor: Int,
                          @SerializedName("version_minor")
                          @ColumnInfo(name = "version_minor")
                          val versionMinor: Int,
                          @SerializedName("version_patch")
                          @ColumnInfo(name = "version_patch")
                          val versionPatch: Int){


    val version:FwVersionBoard
            get() = FwVersionBoard(name,mcu,versionMajor,versionMinor,versionPatch)

}