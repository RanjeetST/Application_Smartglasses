package com.st.smartglasses

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.youth.banner.adapter.BannerAdapter

class ImageAdapter(data: List<DataBean>) : BannerAdapter<DataBean, ImageAdapter.ImageHolder>(data) {

    fun updateData(data: List<DataBean>) {
        mDatas.clear()
        mDatas.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImageHolder(imageView)
    }

    override fun onBindView(holder: ImageHolder, data: DataBean, position: Int, size: Int) {
        holder.imageView.setImageResource(data.imageRes)
    }

    class ImageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view as ImageView
    }
}
