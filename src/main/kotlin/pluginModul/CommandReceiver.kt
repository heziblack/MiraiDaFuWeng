package com.example.pluginModul

import com.example.Test
import com.example.Test.logger
import com.example.model.GameStateMachine
import com.example.model.ImageDrawer
import com.example.model.MyUntil
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import java.io.File

/**
 * 用于接收、识别、处理来自群内的指令
 */
class CommandReceiver: SimpleListenerHost() {
    /**
     * 无参数指令
     */
    private val allowedCMDWithoutArg:Array<String> = arrayOf(
        "测试",
        "来大富翁吧！",
        "加入！",
        "查看状态机！",
        "状态！",
        "退出！",
        "显示玩家！",
        "解散！",
        "准备！",
        "取消准备！",
        "掷！",
        "是！",
        "否！",
        "帮助！",
        "地图！",
        "version"
    )

    /**
     * 有参指令
     */
    private val allowedCMDWithArg:Array<String> = arrayOf(
        "含参测试",
        "存",
        "取",
        "地图详情",
        "设置"
    )
    private val gameSettingList:Array<String> = arrayOf(
        "利率",
        "初始金钱"
    )
    /**
     * 插件工作群对应的游戏状态机的列表
     */
    private var gameList:ArrayList<GameStateMachine> = arrayListOf()

    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val messageStr = e.message.contentToString()
        val message = e.message
        val group = e.group
        val sender = e.sender
//        logger.info("工作群（${group.name}）收到【${sender.nick}】的消息")

        // 返回该消息在状态机列表中的位置
        val indexOfList = createNewGameMachineIfNotInit(group)
        // 匹配无参数指令
        if (messageStr in allowedCMDWithoutArg){
            logger.info("发现指令:$messageStr")
            dealWithCMD(messageStr, indexOfList, sender,message.source)
            return
        }
        // 预处理有参指令
        val msgCmdList:List<String> = messageStr.split('！', limit = 2)
        val msgHead = msgCmdList[0]
        // 匹配有参数指令
        if (msgHead in allowedCMDWithArg){
            logger.info("发现参数指令:$msgHead")
            if (msgCmdList.size<=1){ return }
            val argString:String = msgCmdList[1]
            if (argString != ""){
                val args:List<String> = argString.split('-')
                val argsAL:ArrayList<String> = ArrayList()
                for (s in args){
                    if (s != "")
                    {
                        argsAL.add(s)
                    }
                }
                val formattedArg:List<String> = argsAL.toList()
                if (formattedArg.isNotEmpty()){
                    logger.info("处理含参指令：${msgHead},参数：${formattedArg}")
                    dealWithCMD(msgHead,indexOfList,formattedArg,sender,message.source)
                }
            }
            return
        }
    }

    /**
     * 创建新的群聊状态机如果没有创建的话
     *
     * 返回值是本次消息群在GameList中的下标
     */
    private fun createNewGameMachineIfNotInit(group:Group):Int{
        var isInit:Boolean = false
        var index:Int = -1
        for (g in gameList){
            if(group.id == g.group.id){
                index = gameList.indexOf(g)
                isInit = true
                break
            }
        }
        // 没有初始化就创建一个新的并加入List
        if(!isInit){
            logger.info("此群聊已开启功能，但未创建状态机")
            val newGameStateMachine:GameStateMachine = GameStateMachine(group)
            gameList.add(newGameStateMachine)
            logger.info("创建成功！")
            index = gameList.size -1
        }
//        logger.info("位置：${index+1}/${gameList.size}")
        return index
    }

    /**
     * ### 处理无参指令
     * * index:状态机下标
     * * sender:发送人
     */
    private suspend fun dealWithCMD(cmd: String, index:Int, sender:Member, messageSource: MessageSource){
        val gameMachine:GameStateMachine = gameList[index]
        when(cmd){
            // 测试
            allowedCMDWithoutArg[0] -> {
            }
            // 来大富翁吧！
            allowedCMDWithoutArg[1] -> {
                if(gameMachine.STATE == GameStateMachine.GAMESTATUES.CLOSED){
                    gameMachine.createGame(sender)
                    val newMessage = messageChainOf(At(sender),PlainText("发起了大富翁\n输入\'加入！\'参加游戏！"))
                    gameMachine.group.sendMessage(newMessage)
                }else{
                    gameMachine.group.sendMessage("已经开始了一场游戏了哦！")
                }
            }
            // 加入！
            allowedCMDWithoutArg[2] ->{
                if (gameMachine.STATE == GameStateMachine.GAMESTATUES.LAUNCHING){
                    if (!gameMachine.addPlayer(sender)){
                        gameMachine.group.sendMessage(messageChainOf(At(sender), PlainText("\n你已经在房间中！")))
                    }else{
                        val newMessageChain = messageChainOf(
                            PlainText("${MyUntil.getNameCardOrNick(sender)}已经加入，房间人数：${gameMachine.players.size}\n"),
                            PlainText("输入\'准备！\'以继续\n"),
                            PlainText("输入\'退出！\'退出房间")
                        )
                        gameMachine.group.sendMessage(newMessageChain)
                    }
                }
            }
            // 查看状态机！
            allowedCMDWithoutArg[3] -> {
                val state:String = gameMachine.toString()
                gameMachine.group.sendMessage(state)
            }
            // 状态！
            allowedCMDWithoutArg[4] -> {
                if(gameMachine.STATE != GameStateMachine.GAMESTATUES.CLOSED){
                    val alreadyIn:Boolean = gameMachine.isInGame(sender.id)
                    if (!alreadyIn){
                        gameMachine.group.sendMessage(At(sender) + "您未在游戏队列！")
                    }else{
                        gameMachine.group.sendMessage(At(sender) + "\n" + gameMachine.getPlayerState(sender.id))
                    }
                }
            }
            // 退出！
            allowedCMDWithoutArg[5] -> {
                if( (gameMachine.STATE == GameStateMachine.GAMESTATUES.LAUNCHING
                    || gameMachine.STATE == GameStateMachine.GAMESTATUES.PLAYING )
                    && gameMachine.isInGame(sender.id)){
                    gameMachine.group.sendMessage("${MyUntil.getNameCardOrNick(sender)}离开游戏！")
                    val r = gameMachine.removePlayer(sender.id,true)
                    delay(500)
                    when(gameMachine.STATE){
                        GameStateMachine.GAMESTATUES.LAUNCHING->{
                            if(gameMachine.canKeepRoom()){
                                gameMachine.group.sendMessage("剩余玩家数量：$r")
                            }else{
                                gameMachine.group.sendMessage("玩家全部退出，房间解散")
                                gameMachine.reSet()
                            }
                        }
                        GameStateMachine.GAMESTATUES.PLAYING->{
                            if (!gameMachine.canKeepPlaying()){
                                if(gameMachine.playersNumber <= 0){
                                    gameMachine.group.sendMessage("出现错误,房间解散")
                                }else{
                                    val winnerID = gameMachine.players[0].id
                                    val winnerContact = MyUntil.getMemberByID(winnerID, gameMachine.group)
                                    if (winnerContact!=null){
                                        gameMachine.group.sendMessage(
                                            messageChainOf(PlainText("恭喜"),
                                                At(winnerContact),
                                                PlainText("获得了最后胜利！")))
                                    }else{
                                        gameMachine.group.sendMessage(
                                            messageChainOf(PlainText("恭喜"),
                                                PlainText(MyUntil.getNameCardOrNick(winnerID,gameMachine.group)),
                                                PlainText("获得了最后胜利！")))
                                    }
                                }
                                gameMachine.reSet()
                            }
                        }
                        else->{}
                    }
                }
            }
            // 显示玩家！
            allowedCMDWithoutArg[6] -> {
                if(gameMachine.STATE != GameStateMachine.GAMESTATUES.CLOSED){
                    val msg = gameMachine.strPlayerList()
                    gameMachine.group.sendMessage("当前玩家：\n"+msg)
                }
            }
            // 解散！
            allowedCMDWithoutArg[7] -> {
                if ( gameMachine.STATE != GameStateMachine.GAMESTATUES.CLOSED ){
//                    logger.info("发送人${sender.id}/房主${gameMachine.players[0].id}")
                    if(gameMachine.isRoomLord(sender.id)){
                        gameMachine.reSet()
                        gameMachine.group.sendMessage("房间已被房主解散！")
                    }else{
                        gameMachine.group.sendMessage("只有房主(${MyUntil.getNameCardOrNick(sender)})才能随时解散房间")
                    }
                }
            }
            // 准备！
            allowedCMDWithoutArg[8] -> {
                if (gameMachine.STATE == GameStateMachine.GAMESTATUES.LAUNCHING
                    && gameMachine.isInGame(sender.id)){
                    gameMachine.playerReady(sender.id)
                    if (gameMachine.allReady()){
                        if(gameMachine.beenReadyNumber() > 1){
                            // 重要函数：启动游戏
                            gameMachine.turnToPlayingMode()
                            gameMachine.group.sendMessage("游戏正式开始！")
                            delay(1000)
                            if (gameMachine.playersNumber < 4){
                                gameMachine.group.sendMessage("提示：游戏人数少于4人")
                            }
                            delay(1000)
                            gameMachine.group.sendMessage("" +
                                    "接下来由[房主]${MyUntil.getNameCardOrNick(gameMachine.getLordId(),gameMachine.group)}开始游戏，\n" +
                                    "输入 ${allowedCMDWithoutArg[10]} 以行动\n" +
                                    "${allowedCMDWithoutArg[4]} 查看玩家状态\n" +
                                    "${allowedCMDWithArg[1]}！[数字] 将现金存入银行,例:存！200\n" +
                                    "${allowedCMDWithArg[2]}！[数字] 从银行取出现金,例:取！500\n" +
                                    "${allowedCMDWithoutArg[14]} 查看游戏地图\n"+
                                    "${allowedCMDWithArg[3]}！[数字] 查看地图详细信息,例: 地图详情！27\n"+
                                    "${allowedCMDWithoutArg[3]}检视游戏状态\n"+
                                    "(注意\'！\'是中文感叹号)")
                        }else{
                            gameMachine.group.sendMessage("${MyUntil.getNameCardOrNick(sender)}已准备，房间中只有Ta一个人，真的好可怜呢。")
                        }
                    }else{
                        gameMachine.group.sendMessage("${MyUntil.getNameCardOrNick(sender)}已准备\n" +
                                "进度：${gameMachine.beenReadyNumber()}/${gameMachine.playersNumber}")
                    }
                }
            }
            // 取消准备！
            allowedCMDWithoutArg[9] -> {
                if (gameMachine.STATE==GameStateMachine.GAMESTATUES.LAUNCHING
                    && gameMachine.isInGame(sender.id)
                    && gameMachine.isReady(sender.id)){
                    gameMachine.playerUnReady(sender.id)
                    gameMachine.group.sendMessage("${MyUntil.getNameCardOrNick(sender)}取消准备\n" +
                            "进度：${gameMachine.beenReadyNumber()}/${gameMachine.playersNumber}")
                }
            }
            // 掷！
            allowedCMDWithoutArg[10] -> {
                if (gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isPlayerOnTerm(sender.id)){
                    val dice = MyUntil.diceFrom6()
                    gameMachine.playerMove(sender.id, dice)
                    if(!gameMachine.canKeepPlaying()){
                        val winnerID = gameMachine.players[0].id
                        val winnerMember = gameMachine.group[winnerID]
                        val winnerName = MyUntil.getNameCardOrNick(winnerID,gameMachine.group)
                        if (winnerMember!=null){
                            val m = messageChainOf(PlainText("玩家"),At(winnerMember),PlainText("获得最后的胜利！"))
                            gameMachine.group.sendMessage(m)
                        }else{
                            gameMachine.group.sendMessage("玩家 $winnerName 获得最后的胜利！")
                        }
                        gameMachine.reSet()
                    }
                }
            }
            // 是！
            allowedCMDWithoutArg[11] -> {
                if (gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isPlayerMoved(sender.id)){
                    gameMachine.buyLandOrUpdate()
                    gameMachine.sleepPlayer(sender.id)
                    gameMachine.passToWhoCanMove()
                }
            }
            // 否！
            allowedCMDWithoutArg[12] -> {
                if (gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isPlayerMoved(sender.id))
                    gameMachine.sleepPlayer(sender.id)
                    gameMachine.passToWhoCanMove()
            }
            // 帮助！
            allowedCMDWithoutArg[13] -> {
                val help = helpStr(gameMachine,sender.id)
                if (help!=""){
                    gameMachine.group.sendMessage(help)
                }
            }
            // 地图！
            allowedCMDWithoutArg[14] -> {
                val s = gameMachine.getMapString()
                gameMachine.group.sendMessage(s)
            }
            // version
            allowedCMDWithoutArg[15] -> {
                val version = Test.description.version
                gameMachine.group.sendMessage("大富翁游戏插件：${version}\n")
            }
        }
    }

    /**
     * ### 处理有参指令
     *
     */
    private suspend fun dealWithCMD(cmd: String, index:Int, args:List<String>, sender: Member, messageSource: MessageSource){
        val gameMachine = gameList[index]
        when(cmd){
            // 含参测试
            allowedCMDWithArg[0]->{
            }
            // 存
            allowedCMDWithArg[1]->{
                if(gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isInGame(sender.id)
                    && gameMachine.isPlayerOnTerm(sender.id)){
                    val money:Int = try {
                        args[0].toInt()
                    }catch (e:Exception){
                        0
                    }
                    if(gameMachine.getPlayer(sender.id).save(money)){
                        gameMachine.group.sendMessage(At(sender) + "\n" + gameMachine.getPlayerState(sender.id))
                    }else{
                        gameMachine.group.sendMessage("没钱存可以不存")
                    }
                }
            }
            // 取
            allowedCMDWithArg[2]->{
                if(gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isInGame(sender.id)
                    && gameMachine.isPlayerOnTerm(sender.id)){
                    val money:Int = try {
                        args[0].toInt()
                    }catch (e:Exception){
                        0
                    }
                    if(gameMachine.getPlayer(sender.id).takeFromBank(money)){
                        gameMachine.group.sendMessage(At(sender) + "\n" + gameMachine.getPlayerState(sender.id))
                    }else{
                        gameMachine.group.sendMessage("您账户没有足够现金！")
                    }
                }
            }
            // 地图详情
            allowedCMDWithArg[3]->{
                if(gameMachine.STATE==GameStateMachine.GAMESTATUES.PLAYING
                    && gameMachine.isInGame(sender.id)){
                    val indexOfMap =
                    try{
                        args[0].toInt()
                    }catch (e:Exception){
                        -1
                    }
                    if (indexOfMap == -1){
                        return
                    }else{
                        val s = gameMachine.landInfoStr(indexOfMap)
                        gameMachine.group.sendMessage(s)
                    }
                }
            }
            // 设置
            allowedCMDWithArg[4]->{
                if (gameMachine.STATE==GameStateMachine.GAMESTATUES.LAUNCHING
                    && gameMachine.isRoomLord(sender.id)){
                    if (args.size<2){
                        return
                    }
                    val setCmd = args[0]
                    val setArg = args.subList(1,args.lastIndex+1)
                    forGameSetting(gameMachine,sender,setCmd,setArg)
                }
            }
        }
    }

    /**
     * 构造帮助文本
     */
    private fun helpStr(gameMachine:GameStateMachine,id:Long):String{
        val strBuilder:StringBuilder = StringBuilder("")
        when(gameMachine.STATE){
            GameStateMachine.GAMESTATUES.LAUNCHING->{
                strBuilder.append("""
                    当前处于准备阶段
                    玩家发送'准备！'进入准备状态
                    当所有人(不少于2人)都准备完成后，开始游戏
                """.trimIndent())
//                房主可在此阶段设置房间属性，发送'房间设置！'查看详情
            }
            GameStateMachine.GAMESTATUES.PLAYING->{
                strBuilder.append("""
                    游戏中，使用：
                    1 存/取！将现金存入或取出银行 例：‘存！200’
                    2 状态！显示玩家当前状态
                    3 地图！显示当前游戏地图
                    4 地图详情！【序号】查看指定地图详情 例'地图详情！06'
                """.trimIndent())
            }
            GameStateMachine.GAMESTATUES.CLOSED->{
                strBuilder.append("帮助：\n开启游戏：来大富翁吧！(注意感叹号是中文字符)\n\n")
                val strCYZL = """
                    游戏中常用指令：
                      1 存/取！将现金存入或取出银行 例：‘存！200’
                      2 状态！显示玩家当前状态
                      3 地图！显示当前地图
                      4 地图详情！[序号]查看指定地图详情 例'地图详情！06'
                """.trimIndent()
                strBuilder.append(strCYZL+"\n")
            }
            GameStateMachine.GAMESTATUES.READYING->{
            }
            GameStateMachine.GAMESTATUES.VOTING->{
            }
        }
        return strBuilder.toString()
    }

    /**
     * 处理状态机房间设置
     */
    private suspend fun forGameSetting(gameMachine: GameStateMachine, sender: Member, cmd: String, args: List<String>){
        logger.info("${args.size}:$args")
        when(cmd){
            // 利率
            gameSettingList[0]->{
                val a:Double = try {
                    args[0].toDouble()
                }catch (e:Exception){
                    gameMachine.group.sendMessage("${cmd}:参数错误！请输入正确的参数(数字)")
                    return
                }
                if (a < 0||a > 5){
                    gameMachine.group.sendMessage("${cmd}:参数不合法！请输入正确的参数值(利率在区间[0,5]之间)")
                    return
                }
                gameMachine.depositRate = a
                gameMachine.group.sendMessage("当前利率：${gameMachine.depositRate}")
            }
            // 初始金钱
            gameSettingList[1]->{
                val a:Int = try{
                    args[0].toInt()
                }catch (e:Exception){
                    gameMachine.group.sendMessage("${cmd}:参数错误！请输入正确的参数(数字)")
                    return
                }
                if (a<5000 || a>= Int.MAX_VALUE/2){
                    gameMachine.group.sendMessage("${cmd}:参数不合法！请输入正确的参数值(初始金钱在区间[5000,${Int.MAX_VALUE/2})中)")
                    return
                }
                gameMachine.startMoney = a
                gameMachine.group.sendMessage("当前初始金：${gameMachine.startMoney}")
            }
        }
    }
}

