package com.example.model

import com.beust.klaxon.Klaxon
import com.example.Test
import com.example.Test.logger
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.utils.info
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.random.Random

object MyUntil {
    /**
     * 输出一个测试版的地图
     */
    fun buildMapDemo():String{
        val text:StringBuilder = StringBuilder()
        val pPrison:List<Int> = listOf(10,25)
        val pChance:List<Int> = listOf(3,8,14,19,22,29)
        val landNameList:List<String> = listOf(
            "中山东路","淮海西路","延安西路","衡山路","复兴路","浦东大道","世纪大道",
            "滨江大道","外白渡桥","陆家嘴路","徐家汇路","外滩","沪宁高速公路","南京路"
        )
        var idxLandName:Int = 0

        for(i in (1..30)){
            val newLand = Land()
            if (i==1) {
                newLand.name = "起点"
                newLand.type = Land.Type.StartPoint
            }else if (i in pPrison){
                newLand.name = "监狱"
                newLand.type = Land.Type.Prison
            }else if (i in pChance){
                newLand.name = "机会"
                newLand.type = Land.Type.Chance
            }else{
                newLand.name = landNameList[idxLandName]
                idxLandName = if (idxLandName < landNameList.size-1) idxLandName + 1 else landNameList.size-1
            }
            val landString = Klaxon().toJsonString(newLand)
            text.append(landString)
            if(i!=30){
                text.append("\n")
            }
        }
        return text.toString()
    }

    /**
     * 获得一个6边形筛子结果
     */
    fun diceFrom6():Int{
        return (1..6).random()
    }

    /**
     * 获得玩家昵称或名片
     */
    fun getNameCardOrNick(member:Member):String{
        val nick = member.nick
        val nameCard = member.nameCard
        return if (nameCard != "") nameCard else nick
    }

    /**
     * 获得玩家昵称或名片，未找到返回id
     */
    fun getNameCardOrNick(id:Long,group:Group):String{
        val member = group[id]
        return if(member != null){
            val nick = member.nick
            val nameCard = member.nameCard
            if (nameCard != "") nameCard else nick
        }else{
            id.toString()
        }
    }

    fun getMemberByID(id: Long,group: Group):Member?{
        return group[id]
    }

    fun tranJsonToExcel(){
        val jsonFile= File(Test.dataFolder.toString()+"\\map\\mapShangHaiTan")
        val excelFile = File(Test.dataFolder.toString()+"\\测试工作表.xlsx")
        logger.info("json文件是否存在：${jsonFile.exists()},Excel文件是否存在：${excelFile.exists()}")
        if (jsonFile.exists() && excelFile.exists()){
            val landsString = jsonFile.readLines()
            logger.info("地图长度：${landsString.size}")
            val lands:ArrayList<Land> = arrayListOf<Land>()
            for (s in landsString){
                val obj: Land? = try{
                    Klaxon().parse<Land>(s)
                }catch (e:Exception){
                    logger.info("解析出错")
                    null
                }
                if (obj==null){
                    logger.info("解析结果为空")
                    return
                }else{
                    lands.add(obj)
                }
            }
            val workBook = XSSFWorkbook()
            val sheet = workBook.createSheet("sheet1")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("地名")
            header.createCell(1).setCellValue("类型")
            header.createCell(2).setCellValue("地价")
            header.createCell(3).setCellValue("1->2")
            header.createCell(4).setCellValue("2->3")
            header.createCell(5).setCellValue("过路费1")
            header.createCell(6).setCellValue("过路费2")
            header.createCell(7).setCellValue("过路费3")
            header.createCell(8).setCellValue("描述文本")
            for (i in (1..lands.size)){
                try {
                    val row = sheet.createRow(i)
                    row.createCell(0).setCellValue(lands[i-1].name)
                    row.createCell(1).setCellValue(lands[i-1].type.toString())
                    row.createCell(2).setCellValue(lands[i-1].landPrice.toDouble())
                    row.createCell(3).setCellValue(lands[i-1].buildCost[0].toDouble())
                    row.createCell(4).setCellValue(lands[i-1].buildCost[1].toDouble())
                    row.createCell(5).setCellValue(lands[i-1].passCost[0].toDouble())
                    row.createCell(6).setCellValue(lands[i-1].passCost[1].toDouble())
                    row.createCell(7).setCellValue(lands[i-1].passCost[2].toDouble())
//                    row.getCell(6).setCellValue()
                }catch (e:Exception){
                    logger.error(e)
                    break
                }
            }
            workBook.write(FileOutputStream(excelFile))
        }
    }
}