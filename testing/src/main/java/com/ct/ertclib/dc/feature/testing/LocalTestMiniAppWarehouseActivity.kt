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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.utils.common.Base64Utils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestMiniAppWarehouseBinding
import com.ct.ertclib.dc.feature.testing.socket.DCSocketManager
import com.ct.ertclib.dc.feature.testing.socket.HotspotIpHelper

class LocalTestMiniAppWarehouseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LocalTestMiniAppWarehouseActivity"
    }
    private lateinit var spUtils: SPUtils
    private lateinit var hotspotIpHelper: HotspotIpHelper

    private lateinit var binding: ActivityLocalTestMiniAppWarehouseBinding
    private var adapter: SomeRecyclerViewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalTestMiniAppWarehouseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT
        spUtils = SPUtils.getInstance()
        hotspotIpHelper = HotspotIpHelper(this)

        initView()
    }

    private fun initView() {
        binding.recyclerview.layoutManager = GridLayoutManager(this,4)
        adapter = SomeRecyclerViewAdapter(this)
        binding.recyclerview.adapter = adapter
        initRole()
        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, LocalTestMiniAppEditActivity::class.java)
            startActivity(intent)
        }
        binding.tvClient.setOnClickListener {
            binding.tvClient.isSelected = true
            binding.tvServer.isSelected = false
            binding.etIp.isEnabled = true
            binding.etIp.hint = getString(R.string.client_hint)
            binding.etIp.setText("")

        }
        binding.tvServer.setOnClickListener {
            binding.tvClient.isSelected = false
            binding.tvServer.isSelected = true
            val host = hotspotIpHelper.getHotspotIpAddress()
            binding.etIp.isEnabled = false
            binding.etIp.hint = getString(R.string.server_hint)
            binding.etIp.setText(host)
        }
        binding.btnTcpToggle.setOnClickListener {
            if (binding.tvClient.isSelected){
                val host = binding.etIp.text.toString()
                if (host.isEmpty()){
                    ToastUtils.showShortToast(this@LocalTestMiniAppWarehouseActivity,R.string.client_hint)
                    return@setOnClickListener
                } else {
                    spUtils.put("host",host)
                    spUtils.put("tcpRole","client")
                    ToastUtils.showShortToast(this@LocalTestMiniAppWarehouseActivity,getString(R.string.saved_ok))
                    DCSocketManager.initSocket()
                }
            } else if (binding.tvServer.isSelected){
                val host = hotspotIpHelper.getHotspotIpAddress()
                binding.etIp.setText(host)
                if (host.isNullOrEmpty()){
                    ToastUtils.showShortToast(this@LocalTestMiniAppWarehouseActivity,R.string.server_hint)
                    return@setOnClickListener
                } else {
                    spUtils.put("tcpRole","server")
                    ToastUtils.showShortToast(this@LocalTestMiniAppWarehouseActivity,getString(R.string.saved_ok))
                    DCSocketManager.initSocket()
                }
            }
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
            val item: View = view
            var imgItem: ImageView = view.findViewById(R.id.miniapp_icon)
            var txtItem: TextView = view.findViewById(R.id.miniapp_title)
            var tvSupportScene: TextView = view.findViewById(R.id.tv_support_scene)
            var tvAutoload: TextView = view.findViewById(R.id.tv_autoload)
        }

        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SomeViewHolder {
            //设置RecyclerView 子条目的布局
            val someView = LayoutInflater.from(parent.context).inflate(R.layout.item_mini_app_warehouse, null)
            return SomeViewHolder(someView)
        }

        @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
        override fun onBindViewHolder(holder: SomeViewHolder, position: Int) {
            //这里给子条目控件设置图片跟文字
            val miniAppInfo = data[position]
            holder.txtItem.text = data[position].appName
            if (miniAppInfo.autoLoad){
                holder.tvAutoload.visibility = View.VISIBLE
            } else {
                holder.tvAutoload.visibility = View.GONE
            }
            if (miniAppInfo.isPhasePreCall()){
                holder.tvSupportScene.text = "Pre"
            } else {
                holder.tvSupportScene.text = "In"
            }

            holder.item.setOnClickListener {
                val intent = Intent(context, LocalTestMiniAppEditActivity::class.java)
                intent.putExtra("appId", miniAppInfo.appId)
                context.startActivity(intent)
            }
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(context)
                    .setTitle("确认删除")
                    .setMessage("你确定要删除该小程序吗？")
                    .setPositiveButton("删除") { dialog, which ->
                        // 执行删除操作
                        SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                            val strs = apps.split(",")
                            val builder = StringBuilder()
                            strs.forEach { item ->
                                try {
                                    val split = item.split("&zipPath=");
                                    val appInfoJsonStr = split[0]
                                    val temp = JsonUtil.fromJson(
                                        Base64Utils.decodeFromBase64(appInfoJsonStr),
                                        MiniAppInfo::class.java
                                    )
                                    if (temp?.appId != miniAppInfo.appId) {
                                        if (builder.isNotEmpty()) {
                                            builder.append(",")
                                        }
                                        builder.append(item)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            SPUtils.getInstance().put("TestMiniAppList", builder.toString())
                            setData()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return@setOnLongClickListener true
            }
        }
        override fun getItemCount(): Int {
            //这里控制条目要显示多少
            return data.size
        }
    }

    private fun initRole(){
        val role = spUtils.getString("tcpRole")
        if (role == "client"){
            binding.tvClient.isSelected = true
            binding.tvServer.isSelected = false
            val host = spUtils.getString("host")
            binding.etIp.isEnabled = true
            binding.etIp.setText(host)
        } else if (role == "server"){
            binding.tvClient.isSelected = false
            binding.tvServer.isSelected = true
            val host = hotspotIpHelper.getHotspotIpAddress()
            binding.etIp.isEnabled = false
            binding.etIp.setText(host)
        }
    }
}