package com.st.smartglasses

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node

class NodeArrayAdapter(private val activity: Activity) : ArrayAdapter<Node>(activity, R.layout.scanitem), Manager.ManagerListener {

    override fun onDiscoveryChange(m: Manager, enabled: Boolean) {}

    override fun onNodeDiscovered(m: Manager, node: Node) {
        activity.runOnUiThread {
            add(node)
        }
    }

    fun disconnectAllNodes() {
        for (i in 0 until count) {
            val node = getItem(i)
            if (node != null && node.isConnected) {
                node.disconnect()
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolderItem
        val view: View

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.scanitem, parent, false)
            viewHolder = ViewHolderItem(
                    view.findViewById(R.id.name),
                    view.findViewById(R.id.mac),
                    view.findViewById(R.id.type)
            )
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolderItem
        }

        val node = getItem(position)
        viewHolder.sensorName.text = node?.name
        viewHolder.sensorTag.text = node?.tag

        return view
    }

    private data class ViewHolderItem(
            val sensorName: TextView,
            val sensorTag: TextView,
            val boardType: ImageView
    )
}