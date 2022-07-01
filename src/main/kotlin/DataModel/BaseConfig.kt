package com.example.DataModel

import com.beust.klaxon.*

class BaseConfig(
    @Json(name = "机器人QQ")
    var robotID:ArrayList<Long> = arrayListOf<Long>(),
    @Json(name = "工作群号")
    var serviceGroupID:ArrayList<Long> = arrayListOf<Long>()
)