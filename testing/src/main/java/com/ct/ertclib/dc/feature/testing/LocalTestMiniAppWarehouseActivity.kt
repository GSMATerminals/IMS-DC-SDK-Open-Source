package com.ct.ertclib.dc.feature.testing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ArrayUtils
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.utils.common.Base64Utils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestMiniAppWarehouseBinding

class LocalTestMiniAppWarehouseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LocalTestMiniAppWarehouseActivity"
    }
    private lateinit var binding: ActivityLocalTestMiniAppWarehouseBinding
    private var adapter: SomeRecyclerViewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalTestMiniAppWarehouseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT
        initView()
    }

    private fun initView() {
        binding.recyclerview.layoutManager = GridLayoutManager(this,4)
        adapter = SomeRecyclerViewAdapter(this)
        binding.recyclerview.adapter = adapter
        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, LocalTestMiniAppEditActivity::class.java)
            startActivity(intent)
        }
    }

    fun initData() {
        adapter?.setData()
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    class SomeRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<SomeRecyclerViewAdapter.SomeViewHolder>() {
        private var data: ArrayList<MiniAppInfo> = ArrayList()
        private val sLogger = Logger.getLogger(TAG)
        @SuppressLint("NotifyDataSetChanged")
        fun setData(){
            // 读取配置的小程序列表
            data.clear()
            SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                var strs = apps.split(",")
                strs.forEach { item ->
                    try {
                        val split = item.split("&zipPath=");
                        val appInfoJsonStr = split[0]
                        JsonUtil.fromJson(Base64Utils.decodeFromBase64(appInfoJsonStr), MiniAppInfo::class.java)?.let {
                            data.add(it)
                            sLogger.info("setData: ${it.toString()}")
                        }
                    } catch (e : Exception){
                        e.printStackTrace();
                    }
                }
            }
            notifyDataSetChanged()
        }
        class SomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            //获取子条目的布局控件ID
            val mItem: View = view
            var mImgItem: ImageView = view.findViewById(R.id.miniapp_icon)
            var mTxtItem: TextView = view.findViewById(R.id.miniapp_title)
            var mDeleteContainer: View = view.findViewById(R.id.delete_container)
            var mBtnDelete: View = view.findViewById(R.id.btn_delete)
            var mBtnCancel: View = view.findViewById(R.id.btn_cancel)
            var mTvPreCall: TextView = view.findViewById(R.id.tv_precall)
            var mTvInCall: TextView = view.findViewById(R.id.tv_incall)
            var mTvAutoload: TextView = view.findViewById(R.id.tv_autoload)
        }

        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SomeViewHolder {
            //设置RecyclerView 子条目的布局
            val someView = LayoutInflater.from(parent.context).inflate(R.layout.item_mini_app_warehouse, null)
            return SomeViewHolder(someView)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: SomeViewHolder, position: Int) {
            //这里给子条目控件设置图片跟文字
            val miniAppInfo = data[position]
            holder.mTxtItem.text = data[position].appName
            if (miniAppInfo.autoLoad){
                holder.mTvAutoload.visibility = View.VISIBLE
            } else {
                holder.mTvAutoload.visibility = View.GONE
            }
            if (miniAppInfo.isPhasePreCall()){
                holder.mTvPreCall.visibility = View.VISIBLE
                holder.mTvInCall.visibility = View.GONE
            } else {
                holder.mTvPreCall.visibility = View.GONE
                holder.mTvInCall.visibility = View.VISIBLE
            }

            holder.mItem.setOnClickListener {
                val intent = Intent(context, LocalTestMiniAppEditActivity::class.java)
                intent.putExtra("appId", miniAppInfo.appId)
                context.startActivity(intent)
            }
            holder.itemView.setOnLongClickListener {
                holder.mDeleteContainer.visibility = View.VISIBLE
                return@setOnLongClickListener true
            }
            holder.mBtnDelete.setOnClickListener {
                holder.mDeleteContainer.visibility = View.GONE

                SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                    var strs = apps.split(",")
                    var builder = StringBuilder()
                    strs.forEach{ item ->
                        try {
                            val split = item.split("&zipPath=");
                            val appInfoJsonStr = split[0]
                            val temp = JsonUtil.fromJson(Base64Utils.decodeFromBase64(appInfoJsonStr), MiniAppInfo::class.java)
                            if (temp?.appId != miniAppInfo.appId){
                                if (builder.isNotEmpty()){
                                    builder.append(",")
                                }
                                builder.append(item)
                            }
                        } catch (e : Exception){
                            e.printStackTrace()
                        }
                    }
                    SPUtils.getInstance().put("TestMiniAppList", builder.toString())
                    setData()
                }
            }
            holder.mBtnCancel.setOnClickListener {
                holder.mDeleteContainer.visibility = View.GONE
                notifyDataSetChanged()
            }
        }
        override fun getItemCount(): Int {
            //这里控制条目要显示多少
            return data.size
        }
    }
}