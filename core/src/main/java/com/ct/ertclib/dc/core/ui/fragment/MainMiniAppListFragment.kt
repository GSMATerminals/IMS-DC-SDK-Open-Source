/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMiniAppListFragment : Fragment(){

    companion object {
        private const val TAG = "MainMiniAppListFragment"
    }

    private var emptyTv:View? = null
    private var recyclerView:RecyclerView? = null
    private var adapter: SomeRecyclerViewAdapter? = null
    private var isInCall:Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_mini_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyTv = view.findViewById(R.id.empty_tv)
        recyclerView = view.findViewById(R.id.recyclerview)
        recyclerView?.layoutManager = GridLayoutManager(this.context,4)
        adapter = this.context?.let { SomeRecyclerViewAdapter(it) }
        recyclerView?.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val telecomManager = this.context?.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        isInCall = if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else {
            // can be in dialing, ringing, active or holding states
            telecomManager.isInCall
        }
        loadData()
    }

    private fun loadData(){
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val list = NewCallAppSdkInterface.getMiniAppListFromRepo()
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "loadData listSize: ${list.size},list: $list")
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        refreshData(list)
                    }
                }
            }
        }
    }

    private fun refreshData(list: List<MiniAppInfo>?) {
        if (list != null) {
            emptyTv?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            adapter?.setData(list,isInCall)
        } else {
            emptyTv?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        }
    }

    class SomeRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<SomeRecyclerViewAdapter.SomeViewHolder>() {
        private var someData: List<MiniAppInfo> = mutableListOf()
        private var isInCall: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        fun setData(data: List<MiniAppInfo>, inCall:Boolean){
            someData = data
            isInCall = inCall
            notifyDataSetChanged()
        }
        class SomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            //获取子条目的布局控件ID
            val mItem: View = view
            var mImgItem: ImageView = view.findViewById(R.id.miniapp_icon)
            var mTxtItem: TextView = view.findViewById(R.id.miniapp_title)
            var mDisableItem: View = view.findViewById(R.id.disable_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SomeViewHolder {
            //设置RecyclerView 子条目的布局
            val someView = LayoutInflater.from(parent.context).inflate(R.layout.item_main_mini_app_view, null)
            return SomeViewHolder(someView)
        }

        override fun onBindViewHolder(holder: SomeViewHolder, position: Int) {
            if(isInCall){
                holder.mDisableItem.visibility = View.VISIBLE
            } else {
                holder.mDisableItem.visibility = View.GONE
            }
            //这里给子条目控件设置图片跟文字
            val miniAppInfo = someData[position]
            Glide.with(context)
                .load(miniAppInfo.appIcon)
                .placeholder(R.drawable.icon_miniapp)
                .into(holder.mImgItem)
            holder.mTxtItem.text = someData[position].appName
            holder.mItem.setOnClickListener {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "isInCall $isInCall")
                if(isInCall){
                    ToastUtils.showShortToast(context, context.getString(R.string.after_call_use))
                } else {
                    NewCallAppSdkInterface.startMiniAppOutOfCall(context, miniAppInfo)
                }
            }
        }
        override fun getItemCount(): Int {
            //这里控制条目要显示多少
            return someData.size
        }

    }
}

