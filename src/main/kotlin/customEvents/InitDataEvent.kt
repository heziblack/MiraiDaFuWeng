package com.example.customEvents

import net.mamoe.mirai.event.AbstractEvent
import java.io.File

/**
 * 自定义事件
 *
 * * 用于读取配置文件中的qq号群号等数据
 *
 * * 此事件在 onload 中最多只能触发一次
 * */
class InitDataEvent(
    file: File,
    val jsonString: String,
    val out:File
):  DataEvent(file) {

}