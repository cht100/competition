package com.hrd.config;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 文本模拟数据配置类
 */
@Component
public class SimulateDataConfiguration {
    // 全局随机数工具
    public static final Random RANDOM = new Random();

    // 1. 消息来源平台（群聊核心+微博，贴合多源可插拔接入）
    public static final List<String> SOURCE_PLATFORM = Arrays.asList("微信群聊", "QQ群聊", "微博");

    // 2. 灾害种类（主打洪涝/火灾，补充地质/城市事故，覆盖应急核心场景）
    public static final List<String> DISASTER_TYPE = Arrays.asList(
            "洪涝", "火灾", "滑坡", "地震", "交通事故", "燃气泄漏", "电梯困人", "道路塌陷"
    );

    // 3. 模拟地点列表（按灾种高发场景分类，强匹配避免AI误判）
    // 洪涝高发
    private static final List<String> FLOOD_LOC = Arrays.asList("滨河小区3号楼2单元", "南环大桥下辅路", "滨河路西段积水点", "城东商业广场负一楼");
    // 火灾/燃气泄漏高发
    private static final List<String> FIRE_GAS_LOC = Arrays.asList("城南工业园区A区3号厂房", "物流园西门仓库", "绿城小区底商", "五金市场北门");
    // 滑坡/地震高发
    private static final List<String> LANDSLIDE_EARTHQUAKE_LOC = Arrays.asList("城郊村3组后山", "卧龙镇旅游路滑坡点", "青山镇村口大桥", "西河乡沿河村5组");
    // 交通事故/道路塌陷高发
    private static final List<String> TRAFFIC_COLLAPSE_LOC = Arrays.asList("东风路与人民路交叉口", "绕城高速K12路段", "跨河大桥中段", "滨河路西段");
    // 电梯困人/小区内事故高发
    private static final List<String> COMMUNITY_LOC = Arrays.asList("阳光花园5号楼", "水岸华庭西区10栋", "安居苑北区8号楼", "滨河小区3号楼2单元");

    // ==========================================
    // 核心改造：按【灾种】分类的模板池（群聊+微博）
    // 每个灾种单独维护模板，模板仅含该灾种话术，实现灾种-模板强绑定
    // ==========================================
    // 4. 群聊模板：Map<灾种, 模板列表> （7:3真实信息+隐性谣言，无显性标注）
    private static final Map<String, List<String>> CHAT_TPL_MAP = new HashMap<>();
    // 5. 微博模板：Map<灾种, 模板列表> （6:4真实信息+隐性谣言，带话题，无显性标注）
    private static final Map<String, List<String>> WEIBO_TPL_MAP = new HashMap<>();

    // 静态初始化：灾种-模板绑定（核心！一次性初始化所有灾种的模板）
    static {
        // ---------------------- 洪涝模板 ----------------------
        CHAT_TPL_MAP.put("洪涝", Arrays.asList(
                "[紧急求助]洪涝了！%s一楼全进水，老人小孩被困楼道，水漫到腰了，快来人救援！",
                "[求助]洪涝太严重了，%s地下车库全被淹，十几辆车泡水里，有人挪车被困了！",
                "[目击]洪涝了，%s路面积水快到膝盖，好多车抛锚，大家赶紧绕路！",
                "听说%s洪涝淹死人了，官方都不敢报，小区里都在偷偷传！", // 谣言
                "网传%s这次洪涝是排水系统故意堵的，根本没人来修！" // 谣言
        ));
        WEIBO_TPL_MAP.put("洪涝", Arrays.asList(
                "#洪涝求助# %s突发洪涝，多名居民被困楼道，求@本地消防 @应急管理局 支援！",
                "#现场目击# %s洪涝严重，路面积水齐腰，多车抛锚，附现场实拍图！",
                "#洪涝预警# %s周边河道水位暴涨，低洼地段居民尽快撤离！",
                "#本地洪涝# 听说%s淹得特别严重，死了好多人，官方都压着消息不报！", // 谣言
                "网传%s全城因洪涝停水停电，超市物资被抢空了！" // 谣言
        ));

        // ---------------------- 火灾模板 ----------------------
        CHAT_TPL_MAP.put("火灾", Arrays.asList(
                "救命！火灾了，%s厂房明火直窜二楼，还有工人没出来，求消防赶紧来！",
                "[紧急求助]%s底商着火了，浓烟往楼上窜，家里有老人小孩，快救命！",
                "刚看到火灾了，%s冒好大的黑烟，能看到明火，目前还没看到救援人员！",
                "有人说%s火灾是人为的，救援人员来了也不管，现场乱得很！", // 谣言
                "听说%s火灾烧死了好几个人，消防队故意拖延不出警！" // 谣言
        ));
        WEIBO_TPL_MAP.put("火灾", Arrays.asList(
                "#火灾救援# %s厂房突发火灾，明火蔓延迅速，现场有工人被困，急需消防力量！",
                "#火灾预警# %s小区底商突发火灾，周边居民请紧急撤离，注意安全！",
                "#现场实拍# %s仓库火灾现场黑烟冲天，消防车辆正在赶往现场！",
                "#火灾吐槽# %s发生重大火灾，救援人员不作为，见死不救，现场一片混乱！", // 谣言
                "网传%s火灾因消防设施过期引发，官方刻意隐瞒真相！" // 谣言
        ));

        // ---------------------- 滑坡模板 ----------------------
        CHAT_TPL_MAP.put("滑坡", Arrays.asList(
                "求助！滑坡了，%s后山滑坡把路堵死了，村里老人看病出不去，求路政来清理！",
                "[紧急求助]%s旅游路滑坡，有车辆被埋，赶紧来救援！",
                "目击%s青山镇村口大桥旁滑坡，泥土把路面封了，过往车辆别来！",
                "网传%s滑坡是开山挖矿导致的，政府早就知道却不制止！", // 谣言
                "听说%s滑坡埋了十几辆车，官方根本不敢公布伤亡数！" // 谣言
        ));
        WEIBO_TPL_MAP.put("滑坡", Arrays.asList(
                "#滑坡救援# %s后山滑坡阻断道路，村民被困，求@路政 @应急管理局 支援！",
                "#路况提醒# %s旅游路突发滑坡，道路完全中断，过往车辆请绕行！",
                "#现场目击# %s沿河村旁山体滑坡，部分房屋被冲毁，居民紧急撤离！",
                "网传%s多处山体滑坡，救援队伍根本不够，现场无人管！", // 谣言
                "听说%s滑坡是工程偷工减料导致的，相关部门正在压下消息！" // 谣言
        ));

        // ---------------------- 地震模板 ----------------------
        CHAT_TPL_MAP.put("地震", Arrays.asList(
                "地震了！%s小区楼体有明显摇晃，居民都跑下楼了，有人摔倒受伤！",
                "[目击]%s后山有轻微地震，窗户玻璃震碎了，暂无房屋倒塌！",
                "地震了！%s镇上的老房子有裂缝，大家赶紧到空旷地方！",
                "网传%s今晚还有强余震，好多人都收拾东西去广场了，别在家待着！", // 谣言
                "听说%s这次地震震级被官方调低了，实际有7级以上！" // 谣言
        ));
        WEIBO_TPL_MAP.put("地震", Arrays.asList(
                "#地震预警# %s发生轻微地震，楼体摇晃明显，居民注意防范余震！",
                "#现场目击# %s小区地震后部分墙体开裂，居民已全部撤离到安全区域！",
                "#地震求助# %s老城区有老人被困，因地震房屋轻微坍塌，求救援！",
                "#地震谣言# 网传%s今晚有强余震，让居民紧急撤离？纯假消息！", // 谣言
                "听说%s地震造成多人伤亡，官方刻意隐瞒伤亡数据！" // 谣言
        ));

        // ---------------------- 交通事故模板 ----------------------
        CHAT_TPL_MAP.put("交通事故", Arrays.asList(
                "有没有人管！交通事故了，%s路口货车翻车，司机卡驾驶室，急需救护车！",
                "[求助]%s高速口两车相撞，有人受伤昏迷，快叫120和交警！",
                "目击%s交叉口多车连撞，路面堵死了，救护车进不来！",
                "听说%s路口交通事故死了人，交警故意包庇肇事司机！", // 谣言
                "网传%s高速因交通事故封路，几十辆车连环撞，现场惨不忍睹！" // 谣言
        ));
        WEIBO_TPL_MAP.put("交通事故", Arrays.asList(
                "#交通事故求助# %s路口货车与轿车追尾，司机被困驾驶室，求@本地交警 @120急救中心 ！",
                "#路况提醒# %s高速路段突发交通事故，道路双向拥堵，过往车辆请绕行！",
                "#现场实拍# %s交叉口多车连撞，救护车已赶到现场，正在救援！",
                "网传%s路口交通事故因交警指挥失误导致，多人重伤！", // 谣言
                "听说%s高速交通事故有人员死亡，官方封锁现场不让拍照！" // 谣言
        ));

        // ---------------------- 燃气泄漏模板 ----------------------
        CHAT_TPL_MAP.put("燃气泄漏", Arrays.asList(
                "燃气泄漏！%s底商有刺鼻的燃气味，大家快撤离，求燃气公司抢修！",
                "[紧急求助]%s小区家里燃气泄漏，阀门关不上，快来人帮忙！",
                "目击%s绿城小区底商燃气泄漏，工作人员正在紧急排查！",
                "网传%s小区燃气泄漏要爆炸，附近居民都在连夜撤离，太可怕了！", // 谣言
                "有人说%s燃气泄漏是燃气公司偷工减料，根本没人来抢修！" // 谣言
        ));
        WEIBO_TPL_MAP.put("燃气泄漏", Arrays.asList(
                "#燃气泄漏预警# %s小区底商疑似燃气泄漏，居民紧急撤离，求燃气公司抢修！",
                "#燃气安全# %s小区发生燃气泄漏，提醒大家关闭阀门，开窗通风！",
                "#抢修现场# %s五金市场旁燃气泄漏，燃气公司正在紧急抢修中！",
                "#燃气泄漏谣言# 网传%s小区燃气泄漏爆炸，致多人受伤？无实据，系谣言！", // 谣言
                "听说%s燃气泄漏已造成人员中毒，官方刻意隐瞒！" // 谣言
        ));

        // ---------------------- 电梯困人模板 ----------------------
        CHAT_TPL_MAP.put("电梯困人", Arrays.asList(
                "急！电梯困人了，%s小区电梯困3人含1小孩，物业电话打不通，谁能帮忙？",
                "[求助]%s小区电梯坏了，困了一老人一小孩，快叫物业或消防！",
                "目击%s小区电梯困人，物业半个多小时还没来，太不负责任了！",
                "听说%s小区电梯困人把小孩吓晕了，物业根本不管，还删监控！", // 谣言
                "网传%s小区电梯经常困人，开发商和物业互相推诿，没人修！" // 谣言
        ));
        WEIBO_TPL_MAP.put("电梯困人", Arrays.asList(
                "#电梯困人# %s小区电梯困人超1小时，物业无人处理，求@本地住建 介入！",
                "#物业投诉# %s小区电梯频繁困人，物业未及时检修，存在安全隐患！",
                "#救援现场# %s小区电梯困人，消防人员已赶到，正在紧急救援！",
                "网传%s小区电梯困人导致人员受伤，物业拒绝赔偿还威胁业主！", // 谣言
                "听说%s小区电梯因年久失修困人，住建部门刻意包庇！" // 谣言
        ));

        // ---------------------- 道路塌陷模板 ----------------------
        CHAT_TPL_MAP.put("道路塌陷", Arrays.asList(
                "道路塌陷了！%s路段路面塌了个大坑，有车陷进去了，过往车辆快绕行！",
                "[紧急求助]%s跨河大桥旁路面塌陷，电动车掉进去了，有人受伤！",
                "目击%s滨河路西段路面塌陷，路政正在设置警示标志，别往这走！",
                "网传%s多条路段同时塌陷，全城交通瘫痪，根本没法出门！", // 谣言
                "有人说%s道路塌陷是工程偷工减料，政府早就知道却不处理！" // 谣言
        ));
        WEIBO_TPL_MAP.put("道路塌陷", Arrays.asList(
                "#道路塌陷# %s路段突发道路塌陷，有车辆陷坑，过往车辆请紧急绕行！",
                "#路况提醒# %s跨河大桥中段路面塌陷，道路临时封闭，请注意绕行！",
                "#抢修通知# %s东风路路面塌陷，路政部门正在紧急抢修，预计3小时恢复！",
                "#道路塌陷谣言# 网传%s多条路段同时塌陷，系人为破坏？官方辟谣：仅单路段！", // 谣言
                "听说%s道路塌陷导致人员伤亡，官方封锁消息不让传播！" // 谣言
        ));
    }

    /**
     * 随机获取集合中的元素
     */
    public static <T> T getRandomElement(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * 核心方法：按灾种匹配对应高发地点
     * @param disaster 灾种
     * @return 该灾种的高发地点
     */
    public String getMatchedLocation(String disaster) {
        return switch (disaster) {
            case "洪涝" -> getRandomElement(FLOOD_LOC);
            case "火灾", "燃气泄漏" -> getRandomElement(FIRE_GAS_LOC);
            case "滑坡", "地震" -> getRandomElement(LANDSLIDE_EARTHQUAKE_LOC);
            case "交通事故", "道路塌陷" -> getRandomElement(TRAFFIC_COLLAPSE_LOC);
            case "电梯困人" -> getRandomElement(COMMUNITY_LOC);
            default -> getRandomElement(FLOOD_LOC);
        };
    }

    /**
     * 生成绑定【固定地点+固定灾种】的文本
     * 自动匹配平台模板，群聊/微博均含隐性谣言，供AI正常检测研判
     */
    public String generateChatText(String fixedLocation, String fixedDisaster) {
        // 兼容原有单入口调用，随机匹配平台（群聊概率70%，微博30%）
        String platform = RANDOM.nextInt(10) < 7
                ? getRandomElement(Arrays.asList("微信群聊", "QQ群聊"))
                : "微博";
        return generateChatText(platform, fixedLocation, fixedDisaster);
    }

    /**
     * 重载方法：按【平台+地点+灾种】生成文本
     * @param fixedPlatform 来源平台
     * @param fixedLocation 匹配的地点
     * @param fixedDisaster 灾种
     * @return 灾种-平台-地点匹配的原始文本
     */
    public String generateChatText(String fixedPlatform, String fixedLocation, String fixedDisaster) {
        // 1. 按平台选择模板Map（群聊/微博）
        Map<String, List<String>> tplMap = "微博".equals(fixedPlatform) ? WEIBO_TPL_MAP : CHAT_TPL_MAP;
        // 2. 按灾种获取对应模板列表（核心！保证模板和灾种匹配）
        List<String> tplList = tplMap.getOrDefault(fixedDisaster, CHAT_TPL_MAP.get("洪涝"));
        // 3. 随机选模板，拼接地点生成文本
        String tpl = getRandomElement(tplList);
        return String.format(tpl, fixedLocation);
    }
}
