package com.ct.ertclib.dc.core.ui.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R

class MainMiniAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val itemView: View = view
    var imageItem: ImageView = view.findViewById(R.id.miniapp_icon)
    var textItem: TextView = view.findViewById(R.id.miniapp_title)
    var disableItem: View = view.findViewById(R.id.disable_view)
}