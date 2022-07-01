package com.example.model

import com.example.Test
import com.example.Test.logger
import com.example.pluginModul.ExcelHelper
import java.io.File

class GameMap(val mapFile:File) {
    val landArray:ArrayList<Land> = ArrayList<Land>()
    init {
        logger.info("地图文件是否存在：${mapFile.exists()}")
        val la =
        if (!mapFile.exists()){
            logger.warning("提供的地图文件不存在，使用默认地图")
            val defaultMapFile = File(Test.dataFolder.toString(),"默认地图.xlsx")
            ExcelHelper.readMap(defaultMapFile)
        }else{
            ExcelHelper.readMap(mapFile)
        }
        for (l in la){
            landArray.add(l)
        }
    }

    val length:Int
        get() = landArray.size

    fun getLand(i:Int):Land{
        return landArray[i]
    }
}