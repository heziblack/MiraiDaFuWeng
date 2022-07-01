package com.example.Data

import com.beust.klaxon.*

/**
 * 只能转化基本的变量类型
 */
data class DataTest (
    @Json("姓名")
    var name: String,
    @Json("年龄")
    var age: Int,
    @Json(name = "数组")
    val array:Array<Int>,
    @Json(name = "类型")
    val aaa: type = type.T1
    ){
    /**
     * 也可以写成：
     *
     *      private val exNote:String
     *          get(){...}
     * 或者你仍希望解析器识别到此变量：
     *
     *      @Json(ignored = false)
     *      private val exNote:String
     *          get(){...}
     */
    @Json(ignored = true)
    val exNote:String
        get() {return ""}

    enum class type{
        T1,
        T2
    }

}