package com.example

import com.beust.klaxon.Klaxon
import com.example.DataModel.BaseConfig
import com.example.pluginModul.CommandReceiver
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess


object Test : KotlinPlugin(
        JvmPluginDescription(
                id = "org.hezistudio.dafuweng",
                name = "大富翁",
                version = "0.1.5",
        ) {
            author("HeziBlack")
        }
)
{
    var workGroup:ArrayList<Long> = arrayListOf<Long>()
    var robotID:ArrayList<Long> = arrayListOf<Long>()
    // 插件体入口/主函数/启动函数
    override fun onEnable() {
        logger.info("插件启动中，正在加载")
        val configFile:File = File(this.configFolderPath.toString(),"config.json")
        if(!configFile.exists()){
            logger.info("配置文件不存在")
            pluginFilesInit()
        }

        // 读取配置文件信息
        try {
            logger.info("配置文件路径：${configFile.toPath().toString()}")
            val jStr:String = configFile.readText()
            val ob = Klaxon().parse<BaseConfig>(jStr)
            logger.info("解析得到的Json对象是否为空：${ob == null}")
            if (ob != null){
                workGroup = ob.serviceGroupID
                robotID = ob.robotID
                logger.info("群：${ob.serviceGroupID}，机器人qq：${ob.robotID}")


            }else{
                logger.warning("解析配置文件失败(返回空值)，请删除插件配置文件后重启Mirai客户端")
            }
        }catch (e:Exception){
            logger.error(e)
            logger.warning("解析配置文件失败（mismatch），请删除插件配置文件后重启Mirai客户端")
            exitProcess(0)
        }

        if (robotID.size == 0|| workGroup.size == 0 || robotID[0] == -1L || workGroup[0] == -1L ){
            val sb = StringBuilder()
            sb.append("\n机器人：\n")
            for (rid in robotID){
                sb.append(rid.toString()+"\n")
            }
            sb.append("群聊：\n")
            for (gid in workGroup){
                sb.append(gid.toString()+"\n")
            }
            logger.warning("未配置基本信息，插件可能无法正常运行,已为您结束Mirai进程")
            exitProcess(0)
        }
        // 根据群号注册事件
        val messageChannel = globalEventChannel().filter {
                    it is GroupMessageEvent
                    && it.group.id in workGroup
                    && it.bot.id in robotID
        }.registerListenerHost(CommandReceiver())

        logger.info("加载完成")
    }

    /**
     * 将预制文件复制到插件文件夹中
     */
    private fun pluginFilesInit(){
        val configFile = File(this.configFolderPath.toString(),"config.json")
        if (!configFile.exists()){
            if (!this.configFolder.exists()){ this.configFolder.mkdirs() }
            configFile.createNewFile()
        }

        val configData:String? = this.getResource("data/config.json")
        if (configData != null){
            configFile.writeText(configData)
        }else{
            logger.warning("未找到资源文件！")
        }

        val mapFile = File(this.dataFolderPath.toString(),"默认地图.xlsx")
        if (!mapFile.exists()){
            if (!mapFile.parentFile.exists()){ mapFile.parentFile.mkdirs() }
            mapFile.createNewFile()
        }
        val mapData = this.getResourceAsStream("data/默认地图上海滩.xlsx")
        if (mapData != null){
            val data = mapData.readBytes()
            mapFile.writeBytes(data)
        }else{
            logger.warning("未找到资源文件！")
        }
    }

}