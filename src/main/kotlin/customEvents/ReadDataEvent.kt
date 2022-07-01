package com.example.customEvents

import net.mamoe.mirai.event.AbstractEvent
import java.io.File

class ReadDataEvent(
    file: File,
    val keys:Array<String>,
    val dataType:String
):  DataEvent(file)  {
}