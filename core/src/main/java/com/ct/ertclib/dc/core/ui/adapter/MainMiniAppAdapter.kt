package com.ct.ertclib.dc.core.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.ui.viewholder.MainMiniAppViewHolder
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils

class MainMiniAppAdapter(private val context: Context) : RecyclerView.Adapter<MainMiniAppViewHolder>() {

    companion object {
        private const val TAG = "MainMiniAppAdapter"
    }

    private var miniAppList: MutableList<MiniAppInfo> = mutableListOf()

    var isInCall: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value != field) {
                notifyDataSetChanged()
                field = value
            }
        }


    fun submitList(list: List<MiniAppInfo>) {
        LogUtils.debug(TAG, "submitList list: ${list.joinToString { it.appName }}, miniAppList: ${miniAppList.joinToString { it.appName }}")
        val diffCallback =  MiniAppDiffCallback(miniAppList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        miniAppList.clear()
        miniAppList.addAll(list)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainMiniAppViewHolder {
        val someView = LayoutInflater.from(parent.context).inflate(R.layout.item_main_mini_app_view, null)
        return MainMiniAppViewHolder(someView)
    }

    override fun getItemCount(): Int {
        return miniAppList.size
    }

    override fun onBindViewHolder(holder: MainMiniAppViewHolder, position: Int) {
        if(isInCall){
            holder.disableItem.visibility = View.VISIBLE
        } else {
            holder.disableItem.visibility = View.GONE
        }
        //这里给子条目控件设置图片跟文字
        val miniAppInfo = miniAppList[position]
        Glide.with(context)
            .load(miniAppInfo.appIcon)
            .placeholder(R.drawable.icon_miniapp)
            .into(holder.imageItem)
        holder.textItem.text = miniAppList[position].appName
        holder.itemView.setOnClickListener {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "isInCall $isInCall")
            if(isInCall){
                ToastUtils.showShortToast(context, context.getString(R.string.after_call_use))
            } else {
                NewCallAppSdkInterface.startMiniAppOutOfCall(context, miniAppInfo)
            }
        }
    }
}