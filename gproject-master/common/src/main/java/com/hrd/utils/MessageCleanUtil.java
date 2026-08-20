package com.hrd.utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import com.hrd.entity.Message;

public class MessageCleanUtil {
    //模拟POI字典
    private static final Map<String, BigDecimal[]> LOCATION_POI = new HashMap<>();
    //文本清洗用的正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{18}|\\d{17}(\\d|X|x)");
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\ud800\\udc00-\\udbff\\udfff\\ud83c\\udf00-\\ud83f\\udfff\\ud83e\\udd00-\\ud83e\\udfff]");
    private static final Pattern USELESS_WORDS_PATTERN = Pattern.compile("啊啊啊|卧槽|尼玛|额|哦|哎{2,}");

    // 静态代码块初始化字典
    static {
        // ===== 初始化POI字典 =====
        LOCATION_POI.put("滨河小区3号楼2单元", new BigDecimal[]{new BigDecimal("39.908823"), new BigDecimal("116.397470")});
        LOCATION_POI.put("南环大桥下辅路", new BigDecimal[]{new BigDecimal("39.912345"), new BigDecimal("116.401234")});
        LOCATION_POI.put("滨河路西段积水点", new BigDecimal[]{new BigDecimal("39.915678"), new BigDecimal("116.405678")});
        LOCATION_POI.put("城东商业广场负一楼", new BigDecimal[]{new BigDecimal("39.920123"), new BigDecimal("116.410123")});
        LOCATION_POI.put("城南工业园区A区3号厂房", new BigDecimal[]{new BigDecimal("39.923456"), new BigDecimal("116.413456")});
        LOCATION_POI.put("物流园西门仓库", new BigDecimal[]{new BigDecimal("39.926789"), new BigDecimal("116.416789")});
        LOCATION_POI.put("绿城小区底商", new BigDecimal[]{new BigDecimal("39.930123"), new BigDecimal("116.420123")});
        LOCATION_POI.put("五金市场北门", new BigDecimal[]{new BigDecimal("39.933456"), new BigDecimal("116.423456")});
        LOCATION_POI.put("城郊村3组后山", new BigDecimal[]{new BigDecimal("39.936789"), new BigDecimal("116.426789")});
        LOCATION_POI.put("卧龙镇旅游路滑坡点", new BigDecimal[]{new BigDecimal("39.940123"), new BigDecimal("116.430123")});
        LOCATION_POI.put("青山镇村口大桥", new BigDecimal[]{new BigDecimal("39.943456"), new BigDecimal("116.433456")});
        LOCATION_POI.put("西河乡沿河村5组", new BigDecimal[]{new BigDecimal("39.946789"), new BigDecimal("116.436789")});
        LOCATION_POI.put("东风路与人民路交叉口", new BigDecimal[]{new BigDecimal("39.950123"), new BigDecimal("116.440123")});
        LOCATION_POI.put("绕城高速K12路段", new BigDecimal[]{new BigDecimal("39.953456"), new BigDecimal("116.443456")});
        LOCATION_POI.put("跨河大桥中段", new BigDecimal[]{new BigDecimal("39.956789"), new BigDecimal("116.446789")});
        LOCATION_POI.put("滨河路西段", new BigDecimal[]{new BigDecimal("39.960123"), new BigDecimal("116.450123")});
        LOCATION_POI.put("阳光花园5号楼", new BigDecimal[]{new BigDecimal("39.963456"), new BigDecimal("116.453456")});
        LOCATION_POI.put("水岸华庭西区10栋", new BigDecimal[]{new BigDecimal("39.966789"), new BigDecimal("116.456789")});
        LOCATION_POI.put("安居苑北区8号楼", new BigDecimal[]{new BigDecimal("39.970123"), new BigDecimal("116.460123")});
    }

    /**
     * 清洗单条消息
     * @param message 待清洗的消息
     * @return 清洗后的消息
     */
    public static Message cleanMessage(Message message) {
        if (message == null) {
            return null;
        }

        // 1. 文本清洗
        String cleanedText = cleanOriginalText(message.getOriginalText());
        message.setCleanedText(cleanedText);

        // 2. 地理解析（设置lat/lng，未匹配则为null）
        parseLocation(message);

        // 3. 更新清洗状态（0-待清洗，1-已清洗）
        message.setStatus(1);

        return message;
    }

    /**
     * 文本清洗：去噪、脱敏、格式统一
     */
    private static String cleanOriginalText(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return "";
        }
        String cleaned = originalText;

        // 1. 过滤emoji
        cleaned = EMOJI_PATTERN.matcher(cleaned).replaceAll("");
        // 2. 多个空格替换为单个，过滤换行/制表符
        cleaned = cleaned.replaceAll("\\s+", " ").replaceAll("[\n\t\r]", "");
        // 3. 手机号脱敏
        cleaned = PHONE_PATTERN.matcher(cleaned).replaceAll("*******");
        // 4. 身份证脱敏
        cleaned = ID_CARD_PATTERN.matcher(cleaned).replaceAll("******************");
        // 5. 去无意义语气词
        cleaned = USELESS_WORDS_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned;
    }

    /**
     * 地理解析：匹配POI字典，未匹配则lat/lng设为null
     * 如果消息原本就有经纬度，保留原有值
     */
    private static void parseLocation(Message message) {
        String locationText = message.getLocationText();

        if (message.getLat() != null && message.getLng() != null) {
            return;
        }

        if (locationText == null || locationText.isEmpty()) {
            message.setLat(null);
            message.setLng(null);
            return;
        }

        if (LOCATION_POI.containsKey(locationText)) {
            BigDecimal[] latLng = LOCATION_POI.get(locationText);
            message.setLat(latLng[0]);
            message.setLng(latLng[1]);
            return;
        }

        for (Map.Entry<String, BigDecimal[]> entry : LOCATION_POI.entrySet()) {
            if (locationText.contains(entry.getKey())) {
                BigDecimal[] latLng = entry.getValue();
                message.setLat(latLng[0]);
                message.setLng(latLng[1]);
                return;
            }
        }

        message.setLat(null);
        message.setLng(null);
    }

    private MessageCleanUtil() {}

}
