package com.example.customEvents

import net.mamoe.mirai.event.AbstractEvent
import java.io.File

open class DataEvent(
    val file: File
):  AbstractEvent() {
}