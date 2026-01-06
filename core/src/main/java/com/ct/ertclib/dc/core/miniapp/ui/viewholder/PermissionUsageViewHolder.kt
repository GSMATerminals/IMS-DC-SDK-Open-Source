package com.ct.ertclib.dc.core.miniapp.ui.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R

class PermissionUsageViewHolder(view: View) : BaseViewHolder(view) {
    val permissionTitle: TextView = view.findViewById(R.id.permission_usage_title)
    val permissionUsageTime: TextView = view.findViewById(R.id.permission_usage_time)
}