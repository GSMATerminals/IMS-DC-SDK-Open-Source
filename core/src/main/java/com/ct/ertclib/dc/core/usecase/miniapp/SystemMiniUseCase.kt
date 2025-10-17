package com.ct.ertclib.dc.core.usecase.miniapp

import android.content.Context
import com.blankj.utilcode.util.ZipUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.constants.MiniAppConstants.AVAILABLE_MEMORY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.BATTERY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.CPU_CORE_NUM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_GET_INFORMATION
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_INFORMATION_APPLICATION
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_INFORMATION_CAPABILITY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_INFORMATION_MODEL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_MESSAGE
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.miniapp.ModelInfo
import com.ct.ertclib.dc.core.data.miniapp.PluginMiniAppInfo
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.manager.IModelManager
import com.ct.ertclib.dc.core.port.usecase.mini.ISystemMiniUseCase
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wendu.dsbridge.CompletionHandler

class SystemMiniUseCase(
    private val miniToParentManager: IMiniToParentManager,
    private val modelManager: IModelManager
) : ISystemMiniUseCase {

    companion object {
        private const val TAG = "SystemMiniUseCase"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    override fun getInformationList(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        val informationType = params[PARAMS_GET_INFORMATION]
        when (informationType) {
            PARAMS_INFORMATION_MODEL -> {
                val modelList = modelManager.getAllModel()
                val responseModelList = mutableListOf<ModelInfo>()
                modelList.forEach {
                    responseModelList.add(ModelInfo(it.modelId, it.modelName, it.modelType, it.modelVersion, it.modelPath))
                }
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mapOf("modelList" to JsonUtil.toJson(responseModelList)))))
            }

            PARAMS_INFORMATION_CAPABILITY -> {

                scope.launch(Dispatchers.Default) {
                    val response = JSResponse(
                        RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mutableMapOf(
                            BATTERY to SystemUtils.getBatteryPercentage(context),
                            AVAILABLE_MEMORY to SystemUtils.getAvailableMemory(context) / 1000000000,
                            CPU_CORE_NUM to SystemUtils.getCpuCoreNum()
                        )
                    )
                    withContext(Dispatchers.Main) {
                        handler.complete(JsonUtil.toJson(response))
                    }
                }
            }

            PARAMS_INFORMATION_APPLICATION -> {
                miniToParentManager.getMiniAppList()?.let { appList ->
                    val pluginMiniAppList = mutableListOf<PluginMiniAppInfo>()
                    appList.applications?.forEach {
                        pluginMiniAppList.add(PluginMiniAppInfo(it.appId, it.appName, it.eTag))
                    }
                    val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mutableMapOf("miniAppList" to JsonUtil.toJson(pluginMiniAppList)))
                    handler.complete(JsonUtil.toJson(response))
                } ?: run {
                    handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "miniApp list is null"))))
                }
            }

            else -> {
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "invalid params"))))
            }
        }
    }
}