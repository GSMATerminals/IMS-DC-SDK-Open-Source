package com.ct.ertclib.dc.core.manager.screenshare

import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ScreenShareHelper: KoinComponent {
    private val screenShareUseCase : IScreenShareUseCase by inject()
    private val sketchBoardUseCase : ISketchBoardUseCase by inject()
    private var isInit = false
    fun init(){
        if (isInit){
            return
        }
        isInit = true
        screenShareUseCase.initManager()
        sketchBoardUseCase.initManager()
    }

    fun release(){
        if (!isInit){
            return
        }
        isInit = false
        screenShareUseCase.release()
        sketchBoardUseCase.release()
    }
}