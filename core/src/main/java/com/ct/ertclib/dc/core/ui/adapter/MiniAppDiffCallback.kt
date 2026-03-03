package com.ct.ertclib.dc.core.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.ct.ertclib.dc.core.data.model.MiniAppInfo

class MiniAppDiffCallback(
    private val oldList: List<MiniAppInfo>,
    private val newList: List<MiniAppInfo>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.appId == newItem.appId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.appName == newItem.appName
    }
}