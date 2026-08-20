# -*- coding: utf-8 -*-
"""
LoRA 微调 bge-large-zh-v1.5 嵌入模型
基于 sentence-transformers + PEFT 实现参数高效微调

用法:
    python train_lora.py                          # 使用默认参数
    python train_lora.py --epochs 5 --lr 2e-5     # 自定义参数
"""

import argparse
import json
import os
import sys
import logging
from datetime import datetime

import torch
from torch.utils.data import DataLoader, Dataset
from sentence_transformers import SentenceTransformer, InputExample, losses, evaluation
from peft import LoraConfig, get_peft_model, TaskType

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config import EMBEDDING_MODEL_PATH, EMBEDDING_MODEL_NAME, EMBEDDING_DEVICE

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
log = logging.getLogger(__name__)

# =====================================================================
# 默认超参数（与文档保持一致）
# =====================================================================
DEFAULT_LORA_R = 8
DEFAULT_LORA_ALPHA = 16
DEFAULT_LORA_DROPOUT = 0.05
DEFAULT_LR = 1e-5
DEFAULT_EPOCHS = 3
DEFAULT_BATCH_SIZE = 16
DEFAULT_WARMUP_RATIO = 0.1
DEFAULT_TEMPERATURE = 0.02


class ContrastivePairDataset(Dataset):
    """加载 FlagEmbedding 格式的对比学习数据"""
    
    def __init__(self, filepath):
        self.examples = []
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                item = json.loads(line.strip())
                query = item['query']
                # 每个正例生成一个 InputExample
                for pos in item.get('pos', []):
                    self.examples.append(InputExample(texts=[query, pos], label=1.0))
    
    def __len__(self):
        return len(self.examples)
    
    def __getitem__(self, idx):
        return self.examples[idx]


def apply_lora_to_model(model, r=DEFAULT_LORA_R, alpha=DEFAULT_LORA_ALPHA, 
                         dropout=DEFAULT_LORA_DROPOUT):
    """
    在 BERT-Large 的 Q, V 投影矩阵上注入 LoRA 适配器
    """
    # 获取底层 transformers 模型
    transformer = model[0].auto_model
    
    lora_config = LoraConfig(
        r=r,
        lora_alpha=alpha,
        lora_dropout=dropout,
        target_modules=["query", "value"],  # BERT attention Q, V
        bias="none",
        task_type=TaskType.FEATURE_EXTRACTION,
    )
    
    peft_model = get_peft_model(transformer, lora_config)
    
    # 统计参数
    trainable = sum(p.numel() for p in peft_model.parameters() if p.requires_grad)
    total = sum(p.numel() for p in peft_model.parameters())
    log.info(f"LoRA 注入完成: 可训练参数 {trainable:,} / 总参数 {total:,} ({100*trainable/total:.2f}%)")
    
    # 替换回 sentence-transformers 模型中
    model[0].auto_model = peft_model
    return model, peft_model


def train(args):
    """执行 LoRA 微调训练"""
    log.info("=" * 60)
    log.info("应急领域 LoRA 微调训练启动")
    log.info("=" * 60)
    
    # 1. 加载基座模型
    model_path = EMBEDDING_MODEL_PATH if os.path.isdir(EMBEDDING_MODEL_PATH) else EMBEDDING_MODEL_NAME
    log.info(f"加载基座模型: {model_path}")
    model = SentenceTransformer(model_path, device=EMBEDDING_DEVICE)
    log.info(f"模型维度: {model.get_sentence_embedding_dimension()}")
    
    # 2. 注入 LoRA 适配器
    model, peft_model = apply_lora_to_model(
        model, r=args.lora_r, alpha=args.lora_alpha, dropout=args.lora_dropout
    )
    
    # 3. 加载训练数据
    data_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'data')
    train_file = os.path.join(data_dir, 'train.jsonl')
    val_file = os.path.join(data_dir, 'val.jsonl')
    
    if not os.path.exists(train_file):
        log.error(f"训练数据不存在: {train_file}")
        log.error("请先运行 generate_pairs.py 生成训练数据")
        return
    
    train_dataset = ContrastivePairDataset(train_file)
    log.info(f"训练样本数: {len(train_dataset)}")
    
    train_dataloader = DataLoader(
        train_dataset, shuffle=True, batch_size=args.batch_size
    )
    
    # 4. 定义损失函数（InfoNCE / MultipleNegativesRankingLoss）
    train_loss = losses.MultipleNegativesRankingLoss(
        model=model,
        scale=1.0 / args.temperature,  # temperature -> scale
    )
    
    # 5. 评估器（可选）
    evaluator = None
    if os.path.exists(val_file):
        val_examples = []
        with open(val_file, 'r', encoding='utf-8') as f:
            for line in f:
                item = json.loads(line.strip())
                query = item['query']
                for pos in item.get('pos', []):
                    val_examples.append(InputExample(texts=[query, pos], label=1.0))
                for neg in item.get('neg', []):
                    val_examples.append(InputExample(texts=[query, neg], label=0.0))
        
        if val_examples:
            sentences1 = [e.texts[0] for e in val_examples]
            sentences2 = [e.texts[1] for e in val_examples]
            scores = [e.label for e in val_examples]
            evaluator = evaluation.EmbeddingSimilarityEvaluator(
                sentences1, sentences2, scores,
                name='emergency-val'
            )
            log.info(f"验证集样本数: {len(val_examples)}")
    
    # 6. 训练配置
    warmup_steps = int(len(train_dataloader) * args.epochs * args.warmup_ratio)
    output_dir = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        'models', 'bge-lora-emergency'
    )
    
    log.info(f"训练配置:")
    log.info(f"  LoRA rank: {args.lora_r}, alpha: {args.lora_alpha}")
    log.info(f"  学习率: {args.lr}")
    log.info(f"  Epochs: {args.epochs}")
    log.info(f"  Batch size: {args.batch_size}")
    log.info(f"  Temperature: {args.temperature}")
    log.info(f"  Warmup steps: {warmup_steps}")
    log.info(f"  输出目录: {output_dir}")
    
    # 7. 开始训练
    model.fit(
        train_objectives=[(train_dataloader, train_loss)],
        epochs=args.epochs,
        warmup_steps=warmup_steps,
        optimizer_params={'lr': args.lr},
        evaluator=evaluator,
        evaluation_steps=100,
        output_path=output_dir,
        show_progress_bar=True,
        use_amp=True,  # FP16 混合精度
        scheduler='WarmupCosine',
    )
    
    # 8. 单独保存 LoRA 适配器权重
    lora_save_path = os.path.join(output_dir, 'lora_adapter')
    os.makedirs(lora_save_path, exist_ok=True)
    peft_model.save_pretrained(lora_save_path)
    log.info(f"LoRA 适配器已保存至: {lora_save_path}")
    
    # 统计文件大小
    total_size = 0
    for f in os.listdir(lora_save_path):
        fp = os.path.join(lora_save_path, f)
        if os.path.isfile(fp):
            total_size += os.path.getsize(fp)
    log.info(f"LoRA 适配器大小: {total_size / 1024 / 1024:.1f} MB")
    
    log.info("=" * 60)
    log.info("训练完成！")
    log.info("=" * 60)


def parse_args():
    parser = argparse.ArgumentParser(description='LoRA 微调 bge-large-zh-v1.5')
    parser.add_argument('--lora_r', type=int, default=DEFAULT_LORA_R, help='LoRA 秩')
    parser.add_argument('--lora_alpha', type=int, default=DEFAULT_LORA_ALPHA, help='LoRA alpha')
    parser.add_argument('--lora_dropout', type=float, default=DEFAULT_LORA_DROPOUT, help='LoRA dropout')
    parser.add_argument('--lr', type=float, default=DEFAULT_LR, help='学习率')
    parser.add_argument('--epochs', type=int, default=DEFAULT_EPOCHS, help='训练轮次')
    parser.add_argument('--batch_size', type=int, default=DEFAULT_BATCH_SIZE, help='批大小')
    parser.add_argument('--warmup_ratio', type=float, default=DEFAULT_WARMUP_RATIO, help='Warmup 比例')
    parser.add_argument('--temperature', type=float, default=DEFAULT_TEMPERATURE, help='温度系数')
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    train(args)
