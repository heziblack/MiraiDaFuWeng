package com.example.model

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageDrawer {
    /**
     * 测试函数
     * file 输出文件地址
     */
    fun testImage(file: File){
        val image = BufferedImage(100,100,BufferedImage.TYPE_3BYTE_BGR)
        val w = 0..99
        val h = 0..99
        image.apply {
            for (x in w){
                for (y in h){
                    image.setRGB(x,y,Color.red.rgb)
                }
            }
        }
        ImageIO.write(image,"jpg",file)
    }
}