package com.example.pluginModul
import com.example.Test.logger
import com.example.model.Chance
import com.example.model.Land
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import kotlin.system.exitProcess

object ExcelHelper {
    /**
     * 映射地图文件header与列序号
     *
     * map of header index-> mohi
     */
    private val MOHI:Map<String,Int> = mapOf(
        Pair("地名",0),
        Pair("类型",1),
        Pair("地价",2),
        Pair("1升2",3),
        Pair("2升3",4),
        Pair("过路1",5),
        Pair("过路2",6),
        Pair("过路3",7),
        Pair("描述文本",8)
    )

    private fun getIndexOfMapHeader(name:String):Int{
        val keys = MOHI.keys
        val result = MOHI[name]
        if(name !in keys) logger.warning("列名($name)不在地图文件中！")
        return result ?: 0
    }

    /**
     * 读取地图文件并转换为Map数组
     */
    fun readMap(mapFile: File):ArrayList<Land>{
        val map: ArrayList<Land> = arrayListOf()
        val fileName:String = mapFile.name
        if (!mapFile.exists()){
            logger.error("地图文件\"${fileName}\"不存在！")
            exitProcess(-1)
        } // 检查地图文件存在性，不存在就退出程序
        val workBook = XSSFWorkbook(FileInputStream(mapFile))
        val mapSheetIndex = workBook.getSheetIndex("地图")
        if (mapSheetIndex == -1){
            logger.error("未在文件中找到地图！")
            exitProcess(-1)
        } // 获取地图工作表的序号，获取失败退出
        val mapSheet = workBook.getSheetAt(mapSheetIndex)
        val mapSize = mapSheet.lastRowNum
        // 解析
        try {
            for(l in (1..mapSize)){
                val landRow = mapSheet.getRow(l)
                val landObj:Land
                val name = landRow.getCell(getIndexOfMapHeader("地名")).stringCellValue
                val type = landRow.getCell(getIndexOfMapHeader("类型")).stringCellValue
                val price = landRow.getCell(getIndexOfMapHeader("地价")).numericCellValue.toInt()
                val ugb1 = landRow.getCell(getIndexOfMapHeader("1升2")).numericCellValue.toInt()
                val ugb2 = landRow.getCell(getIndexOfMapHeader("2升3")).numericCellValue.toInt()
                val psc1 = landRow.getCell(getIndexOfMapHeader("过路1")).numericCellValue.toInt()
                val psc2 = landRow.getCell(getIndexOfMapHeader("过路2")).numericCellValue.toInt()
                val psc3 = landRow.getCell(getIndexOfMapHeader("过路3")).numericCellValue.toInt()
                val descriptionCell = landRow.getCell(getIndexOfMapHeader("描述文本"))
                val descriptionStr =
                    if (descriptionCell==null){ "" }
                    else{ descriptionCell.stringCellValue }
                val typeObj:Land.Type = when(type){
                    Land.Type.StartPoint.toString()-> Land.Type.StartPoint
                    Land.Type.Land.toString()-> Land.Type.Land
                    Land.Type.Chance.toString()-> Land.Type.Chance
                    Land.Type.Prison.toString()-> Land.Type.Prison
                    else -> Land.Type.StartPoint }
                landObj =
                    if (typeObj!=Land.Type.Land){ Land(name,typeObj,description=descriptionStr) }
                    else{ Land(name,typeObj,price, arrayOf(ugb1,ugb2), arrayOf(psc1,psc2,psc3),descriptionStr) }
                map.add(landObj)
            }
        }catch (e:Exception){
            logger.error(e)
            logger.error("解析地图失败，请检查地图文件\'${mapFile.toPath()}\'是否规范填写")
            exitProcess(-1)
        } // 解析失败就退出
        logger.info("解析成功，地图长度：${map.size}")
        return map
    }

    private val COHI:Map<String,Int> = mapOf(
        Pair("标题",0),
        Pair("描述",1),
        Pair("主体",2),
        Pair("客体",3),
        Pair("行为",4),
        Pair("参数",5)
    )

    private fun getIndexOfChanceHeader(headerStr:String):Int{
        val keys = COHI.keys
        val result = COHI[headerStr]
        if(headerStr !in keys) logger.warning("列名($headerStr)不在机会文件中！")
        return result ?: 0
    }

    /**
     * 从文件读取机会列表
     */
    fun readChances(file:File):ArrayList<Chance>{
        val chances:ArrayList<Chance> = arrayListOf()
        val fileName:String = file.name
        if (!file.exists()){
            logger.error("地图文件\"${fileName}\"不存在！")
            exitProcess(-1)
        } // 检查地图文件存在性，不存在就退出程序
        val workBook = XSSFWorkbook(FileInputStream(file))
        val sheetIndex = workBook.getSheetIndex("机会")
        if (sheetIndex==-1){
            logger.error("未在文件中找到地图！")
            exitProcess(-1)
        }
        // 获取地图工作表的序号，获取失败退出
        val sheet = workBook.getSheetAt(sheetIndex)
        val length = sheet.lastRowNum
        try{
            for(c in (1..length)){
                val chanceRow = sheet.getRow(c)
                val chanceObj:Chance
                val tittle = chanceRow.getCell(getIndexOfChanceHeader("标题")).stringCellValue
                val condition = chanceRow.getCell(getIndexOfChanceHeader("描述")).stringCellValue
                val host = when(chanceRow.getCell(getIndexOfChanceHeader("主体")).stringCellValue){
                    Chance.HostCondition.MostBuilding.toString()->Chance.HostCondition.MostBuilding
                    Chance.HostCondition.MostCash.toString()->Chance.HostCondition.MostCash
                    Chance.HostCondition.LeastCash.toString()->Chance.HostCondition.LeastCash
                    Chance.HostCondition.LeastBuilding.toString()->Chance.HostCondition.LeastBuilding
                    Chance.HostCondition.MostSaving.toString()->Chance.HostCondition.MostSaving
                    Chance.HostCondition.LeastSaving.toString()->Chance.HostCondition.LeastSaving
                    Chance.HostCondition.Others.toString()->Chance.HostCondition.Others
                    Chance.HostCondition.All.toString()->Chance.HostCondition.All
                    else->Chance.HostCondition.Self }
                val guest = when(chanceRow.getCell(getIndexOfChanceHeader("客体")).stringCellValue){
                    Chance.GuestCondition.MostCash.toString()->Chance.GuestCondition.MostCash
                    Chance.GuestCondition.LeastCash.toString()->Chance.GuestCondition.LeastCash
                    Chance.GuestCondition.MostBuilding.toString()->Chance.GuestCondition.MostBuilding
                    Chance.GuestCondition.LeastBuilding.toString()->Chance.GuestCondition.LeastBuilding
                    Chance.GuestCondition.MostSaving.toString()->Chance.GuestCondition.MostSaving
                    Chance.GuestCondition.LeastSaving.toString()->Chance.GuestCondition.LeastSaving
                    Chance.GuestCondition.Others.toString()->Chance.GuestCondition.Others
                    Chance.GuestCondition.All.toString()->Chance.GuestCondition.All
                    else->Chance.GuestCondition.None }
                val action = when(chanceRow.getCell(getIndexOfChanceHeader("行为")).stringCellValue){
                    Chance.Action.Get.toString()->Chance.Action.Get
                    Chance.Action.Move.toString()->Chance.Action.Move
                    Chance.Action.Upgrade.toString()->Chance.Action.Upgrade
                    Chance.Action.Freeze.toString()->Chance.Action.Freeze
                    else->Chance.Action.Pay
                }
                val arg = chanceRow.getCell(getIndexOfChanceHeader("参数")).numericCellValue
                chanceObj = Chance(tittle,condition,host,guest,action,arg)
                chances.add(chanceObj)
            }
        }catch (e:Exception){
            logger.error(e)
            logger.error("解析地图失败，请检查地图文件\'${file.toPath()}\'中机会是否规范填写")
            exitProcess(-1)
        }
        logger.info("解析成功，机会池长度：${chances.size}")
        return chances
    }

}