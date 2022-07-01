package com.example.model

import com.example.Test
import com.example.Test.logger
import com.example.pluginModul.ExcelHelper
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.messageChainOf
import java.io.File

/**
 * 游戏状态机，存储着当前游戏状态
 */
class GameStateMachine(val group:Group) {
    /**
     * 某一工作群中游戏的状态
     */
    enum class GAMESTATUES(){
        LAUNCHING, // 招募阶段 此时 只接收指令 参加、确定（发起者）、取消（发起者）
        READYING,  // 此时 只接受指令 【数字】
        PLAYING, // 此时 接收指令 走你、背包、使用【数字】、【数字】
        VOTING, // 投票结束比赛
        CLOSED, // 此时 只接受指令 发起大富翁
    }

    /**
     * 游戏地图
     */
    var map:GameMap = GameMap(File(Test.dataFolder.toString(),"默认地图.xlsx"))
    /**
     * 玩家列表
     */
    var players:ArrayList<Player> = ArrayList<Player>()
    // 以后再看看能不能做电脑人
    /**
     * 状态机状态
     */
    var STATE = GAMESTATUES.CLOSED

    /**
     * 行动玩家下标
     */
    var indexOfPlayingPlayer = -1

    /**
     * 玩家数量
     */
    val playersNumber
        get() = players.size

    /**
     * 经过起点时的利润率
     */
    var depositRate:Double = 0.5

    /**
     * 机会池/抽卡池
     */
    val chancePool:ArrayList<Chance> = arrayListOf()

    /**
     * 初始化
     */
    init {
        // 从excel读取机会
        val chancel = ExcelHelper.readChances(File(Test.dataFolder.toString(),"默认地图.xlsx"))
        for (c in chancel){
            chancePool.add(c)
        }
    }

    /**
     * 起始金钱
     */
    var startMoney = 5000

    /**
     * 状态机文本输出
     */
    override fun toString():String{
        val strbder:StringBuilder = StringBuilder()
        strbder.append("****************\n")
        strbder.append("群：${this.group.name}\n")
        strbder.append("状态：${this.STATE}\n")
        strbder.append("玩家：\n")
        for (p in this.players){
            val nick = MyUntil.getNameCardOrNick(p.id,group)
            strbder.append(">${nick}(总资金:${p.getMoney()})\n")
        }
        if(this.STATE == GAMESTATUES.PLAYING){
            val nick = MyUntil.getNameCardOrNick(players[indexOfPlayingPlayer].id, group)
            strbder.append("行动玩家：${nick}\n")
        }
        strbder.append("经过起点返利:${depositRate*100}%\n")
        strbder.append("设置启动资金:￥${startMoney}\n")
        strbder.append("****************")
        return strbder.toString()
    }

    /**
     * 检查qq号是否在房间
     */
    fun isInGame(id:Long):Boolean{
        var inGame = false
        for (p in this.players){
            if (id == p.id){
                inGame = true
                break
            }
        }
        return inGame
    }

    /**
     * 检查玩家是否全部准备！
     */
    fun allReady():Boolean{
        var allOK = true
        for (p in this.players){
            if (!p.isReady()){
                allOK = false
                break
            }
        }
        return allOK
    }

    /**
     * 根据id查找玩家在列表中的位置，-1代表未找到
     */
    fun playerIndex(id:Long): Int {
        var idx:Int = -1
        for (p in this.players){
            if (id == p.id){
                idx = this.players.indexOf(p)
                break
            }
        }
        return idx
    }

    /**
     * 返回字符串显示玩家状态
     */
    fun getPlayerState(id:Long):String{
        return if(isInGame(id)){
            val idx = playerIndex(id)
            val player = players[idx]
            val strBuilder:StringBuilder = StringBuilder(player.toString())
            val pos = player.position
            val landName = map.getLand(pos).name
            val playerLandList = player.landList
            val mapLength = map.length
            if (playerLandList.isNotEmpty()){
                strBuilder.append("\n不动产：\n")
                for (i in playerLandList){
                    val land = map.getLand(i)
                    val name = land.name
                    val level = when(land.level){
                        Land.Levels.L1->{"空空如也"}
                        Land.Levels.L2->{"小楼遍地"}
                        Land.Levels.L3->{"高楼林立"}
                    }
                    strBuilder.append("\t[${i+1}/${mapLength}]${name}(${level})")
                    if (playerLandList.indexOf(i)<playerLandList.size-1){
                        strBuilder.append("\n")
                    }
                }
            }
            val lordMark = if (isRoomLord(id)){"[房主]"} else {""}
            strBuilder.toString()
                .replace("【位置】",landName+"[${pos+1}/${mapLength}]")
                .replace("【是否房主】",lordMark)
        }else{ "【未找到该玩家】" }
    }

    /**
     * 移除玩家(破产/被动)
     */
    private fun removePlayer(id: Long){
        if (isInGame(id)){
            val idx = playerIndex(id)
            val lands = this.players[idx].landList
            for(i in lands){
                map.getLand(i).owner = -1L
                map.getLand(i).level = Land.Levels.L1
            }
        }
    }

    /**
     * 除移玩家（主动）
     */
    suspend fun removePlayer(id: Long, isHostAct:Boolean):Int{
        return when(STATE){
            GAMESTATUES.PLAYING->{
                if (isInGame(id)){
                    val idx = playerIndex(id)
                    val lands = this.players[idx].landList
                    for(i in lands){
                        map.getLand(i).owner = -1L
                        map.getLand(i).level = Land.Levels.L1
                    }
                    players.removeAt(idx)
                    if (indexOfPlayingPlayer > players.lastIndex){
                        indexOfPlayingPlayer %= playersNumber
                    }
                    val player = players[indexOfPlayingPlayer]
                    player.wake()
                    if (!player.isOnterm()){
                        group.sendMessage("${MyUntil.getNameCardOrNick(player.id,group)}冻结中，剩余${player.freezeLeft}回合")
                        passToWhoCanMove()
                    }else{
                        val member = MyUntil.getMemberByID(player.id,group)
                        if (member!=null){
                            group.sendMessage(messageChainOf(At(member),PlainText("的回合")))
                        }else{
                            group.sendMessage(messageChainOf(PlainText("${MyUntil.getNameCardOrNick(player.id,group)}的回合")))
                        }
                    }
                }
                playersNumber
            }
            GAMESTATUES.LAUNCHING->{
                if (isInGame(id)){
                    val idx = playerIndex(id)
                    val lands = this.players[idx].landList
                    for(i in lands){
                        map.getLand(i).owner = -1L
                        map.getLand(i).level = Land.Levels.L1
                    }
                    players.removeAt(idx)
                }
                playersNumber
            }
            else->{-1}
        }

    }

    /**
     * 重载地图
     */
    private fun reloadMap(){
        val file = map.mapFile
        map = GameMap(file)
    }

    /**
     * 重载状态机
     */
    fun reSet(){
        reloadMap()
        players.clear()
        indexOfPlayingPlayer = -1
        STATE = GAMESTATUES.CLOSED
    }

    /**
     * 进入游戏状态
     */
    fun turnToPlayingMode(){
        indexOfPlayingPlayer = 0
        players[indexOfPlayingPlayer].state = Player.PlayerStatues.OnTerm
        for(p in players){
            p.addCash(startMoney)
        }
        STATE = GAMESTATUES.PLAYING
    }

    /**
     * 检查playing状态是否可以继续
     */
    fun canKeepPlaying(): Boolean {
        return playersNumber > 1
    }

    /**
     * 字符串显示玩家列表
     */
    fun strPlayerList():String{
        val sb = StringBuilder()
        for(member in players){
            val nick = MyUntil.getNameCardOrNick(member.id,group)
            sb.append(nick)
            if(players.indexOf(member) < players.lastIndex){
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    /**
     * 检查id是否为房主id
     */
    fun isRoomLord(id: Long):Boolean{
        return if (isInGame(id)) playerIndex(id) == 0 else false
    }

    /**
     * 为玩家准备
     */
    fun playerReady(id:Long){
        val idx = playerIndex(id)
        if (!players[idx].isReady()){
            players[idx].state = Player.PlayerStatues.Waiting
        }
    }

    /**
     * 取消玩家准备
     */
    fun playerUnReady(id:Long){
        val idx = playerIndex(id)
        if (players[idx].isReady()){
            players[idx].state = Player.PlayerStatues.Waiting
        }
    }

    /**
     * 获取已准备人数
     */
    fun beenReadyNumber():Int{
        var counter = 0
        for (p in players){
            if (p.isReady()){
                counter += 1
            }
        }
        return counter
    }

    /**
     * 玩家是否准备
     */
    fun isReady(id: Long):Boolean{
        return players[playerIndex(id)].isReady()
    }

    /**
     * 是否为玩家回合
     */
    fun isPlayerOnTerm(id: Long):Boolean{
        return players[playerIndex(id)].isOnterm()
    }

    /**
     * 将玩家状态置为移动后
     */
    fun playerChangeToMoved(id:Long){
        players[playerIndex(id)].toMovedState()
    }

    /**
     * 检查玩家是否可以购买当前地块
     */
    fun canBuy(id: Long,pos:Int):Boolean{
        val player =  players[playerIndex(id)]
        val land = map.getLand(pos)
        return if(!land.hasBeenOwned()){ player.BankSaving >= land.landPrice }else{ false }
    }

    /**
     * # 玩家移动行为
     */
    suspend fun playerMove(id: Long, step:Int){
        group.sendMessage("掷出${step}点！")
        delay(500)
        val player = players[playerIndex(id)]
        val beforePos = player.position
        val afterPos = (beforePos+step) % map.length
        val landedLand = map.getLand(afterPos)
        if (afterPos < beforePos){
            // 说明经过了起点
            val interest = (player.BankSaving * depositRate).toInt()
            player.addBankSaving(interest)
            group.sendMessage("经过起点，获得利息${interest}")
            delay(500)
        }
        when(landedLand.type){
            Land.Type.Chance->{
                group.sendMessage("到达【机会】地块！直面未知的命运！")
                delay(500)
                val chance = chancePool[(0 until chancePool.size).random()]
                group.sendMessage("${chance.tittle}:${chance.Condition}")
                dealWithChance(chance,getPlayer(id))
                // 玩家状态值得商榷
            }
            Land.Type.Prison->{
                group.sendMessage("到达【监狱】地块！主动自首，冻结1回合！")
                player.freezeLeft = 1
            }
            Land.Type.Land->{
                if(landedLand.hasBeenOwned()){
                    if (landedLand.isOwner(id)){
                        //在自己土地上
                        val levelString = levelToStr(landedLand.level)
                        val uc = landedLand.getUpgradeCost()
                        val playerMoney = player.getMoney()
                        if (uc != -1){
                            if (uc > playerMoney){
                                group.sendMessage("你来到了${landedLand.name}(${levelString})，这里属于你，但你的资金不足以升级\n" +
                                        "持有：${playerMoney}(总共)\n" +
                                        "需要：${uc}\n")
                            }else{
                                group.sendMessage("你来到了${landedLand.name}(${levelString})，这里属于你，是否升级建筑？\n" +
                                        "持有：${playerMoney}(总共)\n" +
                                        "需要：${uc}\n")
                                player.toMovedState()
                            }
                        }else{
                            group.sendMessage("你来到了${landedLand.name}(${levelString}),这里欣欣向荣，将为你带来无穷财富")
                        }
                    }else{
                        // 在别人土地上
                        val receiver = getPlayer(landedLand.owner)
                        val passCost = when(landedLand.level){
                            Land.Levels.L1 -> landedLand.passCost[0]
                            Land.Levels.L2 -> landedLand.passCost[1]
                            Land.Levels.L3 -> landedLand.passCost[2]
                        }
                        group.sendMessage("你来到了${landedLand.name}，" +
                                "这里是${MyUntil.getNameCardOrNick(receiver.id, group)}的地盘，" +
                                "被收取过路费￥${passCost}")
                        if(!player.payToBankSaving(passCost,receiver)) {
                            val c = group[id]
                            if (c != null){
                                group.sendMessage(At(c)+"破产了！")
                            }
                            player.state = Player.PlayerStatues.NotInGame
                        }
                    }
                }else{
                    // 在无主土地上
                    if (canBuy(id,afterPos)){
                        group.sendMessage("你来到了${landedLand.name}，这是一片未开发区，是否购买？（使用存款）")
                        player.toMovedState()
                    }else{
                        group.sendMessage("你来到了${landedLand.name}，这是一片未开发区，但你的存款不够~也许下一次吧（下次一定）")
                    }
                }
            }
            else->{
                group.sendMessage("到达${landedLand.name},平安喜乐")
            }
        }
        // afterPos 计算beforePos + step后的产物
        // position 玩家当前位置
        // before 玩家之前的位置
        if(player.position == beforePos){
            // 如果没有移动就做骰子移动
            player.moveTo(afterPos)
        }
        if(player.state != Player.PlayerStatues.Moved) {
            if(player.state == Player.PlayerStatues.NotInGame){
                // 执行删除玩家，并将行动权交给下一位
                removePlayer(player.id)
                indexOfPlayingPlayer %= playersNumber
                val playerCur = players[indexOfPlayingPlayer]
                playerCur.wake()
                if(playerCur.state == Player.PlayerStatues.Waiting){
                    val c = MyUntil.getMemberByID(playerCur.id,group)
                    if (c!=null){
                        group.sendMessage("${MyUntil.getNameCardOrNick(c)}正在坐牢，剩余${playerCur.freezeLeft}回合")
                    }
                    passToWhoCanMove()
                }else if (playerCur.state == Player.PlayerStatues.OnTerm){
                    val c = MyUntil.getMemberByID(playerCur.id,group)
                    if (c != null){
                        group.sendMessage("现在是${MyUntil.getNameCardOrNick(c)}的回合")
                    }
                }
            }else{
                player.sleep()
                passToWhoCanMove()
            }
        }
    }

    /**
     * 土地等级转字符串
     */
    private fun levelToStr(level:Land.Levels):String{
        return when(level){
            Land.Levels.L1->"空空如也"
            Land.Levels.L2->"小楼遍地"
            Land.Levels.L3->"高楼林立"
        }
    }

    /**
     * 行动权传递给下一个可以行动的
     */
    suspend fun passToWhoCanMove(){
        moveCur()
        val curPlayer = players[indexOfPlayingPlayer]
        curPlayer.wake()
        if (curPlayer.state == Player.PlayerStatues.Waiting){
            val c = group[curPlayer.id]
            if (c!=null){
                group.sendMessage("${MyUntil.getNameCardOrNick(c)}正在坐牢，剩余${curPlayer.freezeLeft}回合")
            }else{
                throw Exception("玩家不在群中")
            }
            passToWhoCanMove()
        }else{
            curPlayer.state = Player.PlayerStatues.OnTerm
            val c = group[curPlayer.id]
            if (c != null){
                group.sendMessage("现在是${MyUntil.getNameCardOrNick(c)}的回合")
            }
        }
    }

    /**
     * 下标循环移动
     */
    private fun moveCur(){
        indexOfPlayingPlayer = (indexOfPlayingPlayer + 1) % players.size
    }

    /**
     * 是否处于后移动状态
     */
    fun isPlayerMoved(id:Long):Boolean{
        return players[playerIndex(id)].isMoved()
    }

    /**
     * 购买或升级土地
     */
    fun buyLandOrUpdate(){
        val player = players[indexOfPlayingPlayer]
        val land = map.getLand(player.position)
        if (land.isOwner(player.id)){
            val cost = land.getUpgradeCost()
            player.payBankSavingFirst(cost)
            land.upgrade()
        }else{
            player.buyLand(map.landArray.indexOf(land),land.landPrice)
            land.owner = player.id
        }
    }

    /**
     * 使玩家进入休眠
     */
    fun sleepPlayer(id:Long){
        players[playerIndex(id)].sleep()
    }

    /**
     * 房间是否持续(能否维持房间)
     */
    fun canKeepRoom():Boolean{
        return playersNumber > 0
    }
    /**
     * 获得房主id
     */
    fun getLordId():Long{
        return players[0].id
    }

    /**
     * 根据ID返回一个GameMachine中的Player对象
     */
    fun getPlayer(id: Long):Player{
        return players[playerIndex(id)]
    }

    fun changeGameMap(mapFile:File){
        map = GameMap(mapFile)
    }

    /**
     * ## 执行chance中的行为
     */
    private suspend fun dealWithChance(chance: Chance, player: Player){
        val host = chance.host
        val guest = chance.guest
        val act = chance.action
        val arg = chance.argNum
        logger.info("行为主体:${host},行为客体:${guest},行为:${act},参数:${arg}")
        when(act){
            Chance.Action.Pay->{
                if (host==Chance.HostCondition.Self&&guest==Chance.GuestCondition.None){
                    val r = player.payCashFirst(arg.toInt())
                    if (!r){
                        val nick = MyUntil.getNameCardOrNick(player.id,group)
                        player.state = Player.PlayerStatues.NotInGame
                        group.sendMessage("${nick}破产！")
                    }
                }
                else if(host==Chance.HostCondition.Self&&guest==Chance.GuestCondition.Others){
                    for (o in players){
                        if (o.id != player.id){
                            val result = player.payToCashFirst(o,arg.toInt())
                            if(!result){
                                player.state = Player.PlayerStatues.NotInGame
                                break
                            }
                        }
                    }
                    if (player.state==Player.PlayerStatues.NotInGame){
                        val nick = MyUntil.getNameCardOrNick(player.id,group)
                        group.sendMessage("${nick}破产！")
                    }
                }
            }
            Chance.Action.Get->{
                if (host==Chance.HostCondition.Self&&guest==Chance.GuestCondition.None){
                    player.addCash(arg.toInt())
                }
            }
            Chance.Action.Move->{
                if(host==Chance.HostCondition.Self&&guest==Chance.GuestCondition.None){
                    val pos = arg.toInt()
                    if(player.position>pos){
                        val interest = (player.BankSaving * depositRate).toInt()
                        player.addBankSaving(interest)
                        group.sendMessage("中途经过起点，收取利息${interest}元")
                    }
                    logger.info("获得的参数：$pos")
                    val pl = getPlayer(player.id)
                    pl.moveTo(pos)
                    logger.info("玩家位置：${player.position}")
                }
            }
            Chance.Action.Upgrade->{
                if(host==Chance.HostCondition.Self && guest==Chance.GuestCondition.None){
                    val uga = arrayListOf<Int>()
                    if (player.landList.isNotEmpty()){
                        for(pos in player.landList){
                            if(map.getLand(pos).level!=Land.Levels.L3){
                                uga.add(pos)
                            }
                        }
                    }
                    if (uga.isNotEmpty()) {
                        val land = map.getLand(uga.random())
                        when(land.level){
                            Land.Levels.L1 -> land.level = Land.Levels.L2
                            Land.Levels.L2 -> land.level = Land.Levels.L3
                            else->{}
                        }
                        val nick = MyUntil.getNameCardOrNick(player.id,group)
                        group.sendMessage("${nick}的${land.name}升级到${land.level}级")
                    }else{
                        val nick = MyUntil.getNameCardOrNick(player.id,group)
                        group.sendMessage("${nick}没有可以升级的地产，真可惜")
                    }
                }
            }
            Chance.Action.Freeze->{
                if(host==Chance.HostCondition.Self&&guest==Chance.GuestCondition.None){
                    player.freezeLeft = arg.toInt()
                }
            }
        }
    }

    /**
     * 将随机事件输出为文本
     */
    fun showChancePool():String{
        val sb = StringBuilder("随机事件：\n")
        for(c in chancePool){
            val i = chancePool.indexOf(c)+1
            val tittle = c.tittle
            val condi = c.Condition
            val agr = c.argNum
            if (i==chancePool.size){
                sb.append("${i}.${tittle}(${condi})${agr}")
            }else{
                sb.append("${i}.${tittle}(${condi})${agr}\n")
            }
        }
        return sb.toString()
    }

    /**
     * 返回全局地图文本
     */
    fun getMapString():String{
        val sb = StringBuilder()
        for(land in map.landArray){
            val index = map.landArray.indexOf(land)+1
            val indexStr = if (index > 9){index.toString()}else{ "0$index" }
            sb.append("$indexStr|")
            val name = land.name
            val nameL = name.length
            var nameStr = name
            for(i in (0 until  7 - nameL)){
                nameStr = "$nameStr  "
            }
            sb.append(nameStr)
            val t = land.type
            if (t==Land.Type.Land)
            {
                val level = when(land.level){
                    Land.Levels.L1->{"空空如也"}
                    Land.Levels.L2->{"小楼遍地"}
                    Land.Levels.L3->{"高楼林立"}
                }
                val price = land.landPrice
                val cost = when(land.level){
                    Land.Levels.L1->{land.passCost[0]}
                    Land.Levels.L2->{land.passCost[1]}
                    Land.Levels.L3->{land.passCost[2]}
                }
                val owner = land.owner
                val isOwned = if (owner!=-1L){ "√" }else{ "o" }
                sb.append("[${isOwned}](${level})")
                if (owner==-1L){
                    sb.append(" 价格:$price")
                }else{
                    sb.append(" 路费:$cost")
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * 显示地块详情
     */
    fun landInfoStr(landID:Int):String{
        val sb = StringBuilder("")
        if (landID<1||landID>map.length){
            return ""
        }
        val indexStr = if (landID<10){ "0${landID}" }else{ landID.toString() }
        val land = map.getLand(landID-1)
        val name = land.name
        val price = land.landPrice
        val level = land.level
        val owner = land.owner
        val pCost = land.passCost
        val bCost = land.buildCost
        val levelStr = when(level){
            Land.Levels.L1->{"空空如也"}
            Land.Levels.L2->{"小楼遍地"}
            Land.Levels.L3->{"高楼林立"}
        }
        val ownerStr =
            if (owner != -1L){
                val ownerContact = group[owner]
                if (ownerContact!=null){
                    MyUntil.getNameCardOrNick(ownerContact)
                }else{
                    "无"
                }
            }else{
                "无"
            }
        if (land.type==Land.Type.Land){
            sb.append("""
            ${indexStr}|${name}(${levelStr})
            所有者：${ownerStr}
            价格：$price
            建设花费：
                    ${bCost[0]}(1->2)
                    ${bCost[1]}(2->3)
            过路费:  
                    ${pCost[0]}(空空如也)
                    ${pCost[1]}(小楼遍地)
                    ${pCost[2]}(高楼林立)
        """.trimIndent())
        }else{
            sb.append("""
            ${indexStr}|${name}
            """.trimIndent())
        }
        return sb.toString()
    }

    /**
     * 创建游戏房间
     */
    fun createGame(member:Member){
        if (players.size!=0){ players.clear() }
        val player = Player(member.id)
        player.state = Player.PlayerStatues.InRoom
        players.add(player)
        STATE = GAMESTATUES.LAUNCHING
    }

    /**
     * 添加新玩家
     */
    fun addPlayer(member:Member):Boolean{
        val playerID = member.id
        return if(isInGame(playerID)){
            false
        }else{
            val newPlayer = Player(playerID)
            newPlayer.state = Player.PlayerStatues.InRoom
            players.add(newPlayer)
            true
        }
    }

}