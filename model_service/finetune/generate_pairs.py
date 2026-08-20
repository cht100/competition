# -*- coding: utf-8 -*-
"""
应急领域对比学习训练数据生成器
从模拟事件数据中构建 FlagEmbedding 标准格式的训练样本：
  {"query": str, "pos": [str, ...], "neg": [str, ...]}

正例对: 同一事件的不同消息表述
困难负例: 同类灾种但不同事件
简单负例: 不同灾种的事件
"""

import json
import random
import os
import sys

# 添加上级目录到path，以便导入config
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

random.seed(42)

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'data')
OUTPUT_FILE = os.path.join(OUTPUT_DIR, 'train_pairs.jsonl')

# =====================================================================
# 应急领域事件语料库（覆盖10类灾种，多样化表述）
# =====================================================================
EVENT_CORPUS = {
    "洪涝": [
        # 事件1: 城区内涝
        [
            "朝阳区望京花园地下车库严重积水，水位已达50cm",
            "望京小区地下停车场水位上涨，多辆车被泡",
            "望京花园B2层车库遭暴雨倒灌，积水持续加深",
            "望京附近小区地库进水严重，业主车辆受损",
            "暴雨导致望京花园地下两层车库全部被淹",
        ],
        # 事件2: 河道洪水
        [
            "丰台南苑路槐房路段暴雨内涝，道路水深及膝",
            "槐房路一带积水导致交通瘫痪，公交改线",
            "南苑路路面积水过深，多辆私家车抛锚",
            "丰台槐房路段突降暴雨，低洼地带严重积水",
            "南苑槐房路积水至腰部，消防已到场救援",
        ],
        # 事件3: 山洪
        [
            "门头沟山区突发山洪，河道水位暴涨",
            "门头沟妙峰山镇山洪暴发，多处道路被冲断",
            "门头沟山区暴雨引发泥水横流，村庄被围困",
            "妙峰山附近突发洪水下泄，沿河居民紧急转移",
            "门头沟山洪携带大量泥沙冲入河道，桥梁告急",
        ],
    ],
    "火灾": [
        # 事件1: 写字楼火灾
        [
            "海淀中关村创新大厦12层起火，浓烟弥漫",
            "中关村办公楼12楼冒出大量黑烟，有明火",
            "创新大厦中层突然着火，附近写字楼员工紧急疏散",
            "中关村大厦发生火灾，消防车已赶赴现场",
            "海淀中关村某写字楼12层窗口有火光和烟雾冒出",
        ],
        # 事件2: 老旧居民楼火灾
        [
            "东城和平里北街老楼厨房起火，浓烟窜出",
            "和平里一栋老旧居民楼发生厨房火灾",
            "东城区和平里北街一户人家做饭引发火灾",
            "和平里老楼3层冒出黑烟，疑似厨房着火",
            "东城和平里居民楼突发火灾，老人被困",
        ],
        # 事件3: 工厂火灾
        [
            "大兴工业区一化工仓库起火，火势凶猛",
            "大兴区某工厂仓库发生大火，爆炸声不断",
            "大兴亦庄附近工业园区仓库着火冒出浓烟",
            "大兴工业区化学品仓库起火，周边已拉起警戒线",
            "亦庄工厂区仓库火灾，消防出动十余辆消防车",
        ],
    ],
    "地震": [
        # 事件1
        [
            "昌平区回龙观有明显震感，持续约5秒",
            "回龙观小区居民反映房屋晃动，吊灯摇摆厉害",
            "昌平回龙观附近感受到地震，楼房明显震动",
            "回龙观一带地震很强，很多人跑到楼下避险",
            "昌平发生有感地震，回龙观居民夜间被震醒",
        ],
        # 事件2
        [
            "通州区发生轻微地震，部分居民有感",
            "通州副中心一带有轻微震感，持续两三秒",
            "通州新华大街附近居民感受到轻微晃动",
            "有人在通州感受到地面震动，但很快停了",
            "通州发生小规模地震，暂无人员伤亡报告",
        ],
    ],
    "交通事故": [
        # 事件1
        [
            "西城二环路德胜门段多车连环追尾",
            "德胜门附近二环路发生多车连撞事故",
            "二环路德胜门段3车追尾，道路严重拥堵",
            "西城区二环德胜门处发生交通事故，有人员受伤",
            "德胜门立交桥附近多车相撞，交警已到场",
        ],
        # 事件2
        [
            "朝阳国贸桥附近一辆货车侧翻",
            "国贸桥下货车翻车，货物散落一地",
            "CBD国贸桥东侧大货车侧翻堵住车道",
            "朝阳建外大街国贸桥处发生货车侧翻事故",
            "国贸附近有大车翻了，交通基本瘫痪",
        ],
    ],
    "燃气泄漏": [
        # 事件1
        [
            "五道口华联旁餐厅天然气泄漏，气味刺鼻",
            "五道口一家餐馆发生燃气泄漏，周围已疏散",
            "海淀五道口附近能闻到很浓的煤气味",
            "五道口华联商场旁边餐厅燃气管道破裂泄漏",
            "五道口有燃气泄漏，消防和燃气公司都来了",
        ],
        # 事件2
        [
            "学院路某小区地下管道疑似燃气泄漏",
            "海淀学院路居民区闻到天然气味道，疑似管道破损",
            "学院路附近地下有燃气味冒出，已拉起警戒带",
            "学院路一带疑似燃气管道泄漏，居民被要求撤离",
            "海淀学院路小区物业通知燃气泄漏，请居民远离",
        ],
    ],
    "暴雪": [
        # 事件1
        [
            "大兴黄村暴雪导致道路结冰，多处路段封闭",
            "大兴区暴雪来袭，积雪厚度超过20厘米",
            "黄村一带暴雪造成道路严重结冰，公交停运",
            "大兴黄村大雪后路面打滑，连发多起交通事故",
            "大兴区遭遇暴风雪，多条公路被迫封闭",
        ],
    ],
    "台风": [
        # 事件1
        [
            "通州新华大街台风过境，多处树木倒伏",
            "台风过境通州区，大风吹倒路边行道树",
            "通州新华大街因台风影响树倒路断",
            "台风导致通州多处大树连根拔起，车辆被砸",
            "通州区台风造成严重破坏，建筑外墙脱落",
        ],
    ],
    "泥石流": [
        [
            "房山区大石河上游突发泥石流，道路被堵",
            "房山山区暴雨后引发泥石流，冲毁公路",
            "大石河沿线发生泥石流灾害，泥浆涌入村庄",
            "房山山区泥石流导致多人被困，救援正在进行",
            "房山泥石流灾害导致道路中断，已启动应急预案",
        ],
    ],
    "山体滑坡": [
        [
            "密云区水库附近山体滑坡，大量碎石滚落",
            "密云山区暴雨后发生山体滑坡，道路被掩埋",
            "密云水库旁山坡出现崩塌，碎石散落路面",
            "密云某村附近出现山体滑坡险情，村民已转移",
            "密云区山体塌方导致公路中断，抢修正在进行",
        ],
    ],
    "其他": [
        # 事件1: 坍塌
        [
            "丰台花乡旧厂房部分坍塌，有人被困",
            "花乡一处老旧厂房突然发生垮塌事故",
            "丰台花乡工业区旧建筑墙体倒塌",
            "丰台一废弃厂房发生坍塌，现场有施工人员",
            "花乡旧厂房结构失稳坍塌，消防正在搜救",
        ],
        # 事件2: 停电
        [
            "朝阳CBD国贸大面积停电，电梯困人",
            "国贸写字楼群突然停电，大量人员被困电梯",
            "CBD一带大面积断电，办公楼空调电脑全停",
            "朝阳区国贸附近停电范围扩大，多栋楼受影响",
            "国贸地区突发停电故障，地铁站照明中断",
        ],
    ],
}


def generate_training_pairs(num_augment=3):
    """
    生成 FlagEmbedding 标准格式的对比学习训练对
    
    格式: {"query": str, "pos": [str], "neg": [str]}
    """
    pairs = []
    
    # 构建灾种→事件列表的映射
    all_events = []  # [(灾种, 事件消息列表)]
    for disaster_type, events in EVENT_CORPUS.items():
        for event_msgs in events:
            all_events.append((disaster_type, event_msgs))
    
    for evt_idx, (dtype, msgs) in enumerate(all_events):
        # 对该事件的每条消息作为 query
        for q_idx, query in enumerate(msgs):
            pos_list = [m for j, m in enumerate(msgs) if j != q_idx]
            
            # 困难负例: 同灾种但不同事件
            hard_negs = []
            for other_idx, (other_dtype, other_msgs) in enumerate(all_events):
                if other_dtype == dtype and other_idx != evt_idx:
                    hard_negs.extend(other_msgs)
            
            # 简单负例: 不同灾种
            easy_negs = []
            for other_idx, (other_dtype, other_msgs) in enumerate(all_events):
                if other_dtype != dtype:
                    easy_negs.extend(random.sample(other_msgs, min(2, len(other_msgs))))
            
            # 采样负例: 困难负例优先，补充简单负例
            neg_list = []
            if hard_negs:
                neg_list.extend(random.sample(hard_negs, min(5, len(hard_negs))))
            if easy_negs:
                neg_list.extend(random.sample(easy_negs, min(10, len(easy_negs))))
            
            if pos_list and neg_list:
                pairs.append({
                    "query": query,
                    "pos": pos_list,
                    "neg": neg_list
                })
    
    # 数据增强: 灾种级别的跨事件同义训练对
    for dtype, events in EVENT_CORPUS.items():
        if len(events) < 2:
            continue
        for i in range(len(events)):
            for j in range(i + 1, len(events)):
                # 同灾种不同事件间的对比（这些应该是负例）
                for q in events[i][:2]:
                    neg = events[j][:3]
                    pos = [m for m in events[i] if m != q][:3]
                    other_neg = []
                    for od, oe in EVENT_CORPUS.items():
                        if od != dtype:
                            for ev in oe:
                                other_neg.extend(ev[:1])
                    neg.extend(random.sample(other_neg, min(5, len(other_neg))))
                    pairs.append({
                        "query": q,
                        "pos": pos,
                        "neg": neg
                    })
    
    random.shuffle(pairs)
    return pairs


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    pairs = generate_training_pairs()
    
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        for p in pairs:
            f.write(json.dumps(p, ensure_ascii=False) + '\n')
    
    # 统计
    total_queries = len(pairs)
    total_pos = sum(len(p['pos']) for p in pairs)
    total_neg = sum(len(p['neg']) for p in pairs)
    
    print(f"训练数据已生成: {OUTPUT_FILE}")
    print(f"  总查询数: {total_queries}")
    print(f"  正例总数: {total_pos}")
    print(f"  负例总数: {total_neg}")
    print(f"  平均正例/查询: {total_pos / total_queries:.1f}")
    print(f"  平均负例/查询: {total_neg / total_queries:.1f}")
    
    # 同时生成验证集（取20%）
    val_size = max(1, len(pairs) // 5)
    val_pairs = pairs[:val_size]
    train_pairs = pairs[val_size:]
    
    train_file = os.path.join(OUTPUT_DIR, 'train.jsonl')
    val_file = os.path.join(OUTPUT_DIR, 'val.jsonl')
    
    with open(train_file, 'w', encoding='utf-8') as f:
        for p in train_pairs:
            f.write(json.dumps(p, ensure_ascii=False) + '\n')
    
    with open(val_file, 'w', encoding='utf-8') as f:
        for p in val_pairs:
            f.write(json.dumps(p, ensure_ascii=False) + '\n')
    
    print(f"\n  训练集: {len(train_pairs)} 条 → {train_file}")
    print(f"  验证集: {len(val_pairs)} 条 → {val_file}")


if __name__ == '__main__':
    main()
