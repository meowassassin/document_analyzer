#!/usr/bin/env python3
"""
문서 타입 분류기 학습 스크립트
"""
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
import numpy as np
import json
from pathlib import Path
import argparse

# 모델 정의
class DocumentClassifierModel(nn.Module):
    def __init__(self, input_dim=6, hidden_dim1=64, hidden_dim2=32, hidden_dim3=16, num_classes=5):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(input_dim, hidden_dim1),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim1, hidden_dim2),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim2, hidden_dim3),
            nn.ReLU(),
            nn.Linear(hidden_dim3, num_classes),
            nn.Softmax(dim=1)
        )

    def forward(self, x):
        return self.network(x)

def load_data(csv_path):
    """학습 데이터 로드"""
    df = pd.read_csv(csv_path)
    print(f"✅ 데이터 로드 완료: {len(df)} samples")
    print(f"라벨 분포:\n{df['label'].value_counts()}")
    return df

def extract_features(df):
    """특징 추출"""
    feature_cols = [
        'headers_ratio',
        'lists_ratio',
        'paragraphs_ratio',
        'avg_length',
        'avg_importance',
        'log_cell_count'
    ]

    X = df[feature_cols].values
    y = df['label'].values

    return X, y, feature_cols

def train_model(X_train, y_train, X_val, y_val, num_classes, epochs=100):
    """모델 학습"""
    model = DocumentClassifierModel(input_dim=X_train.shape[1], num_classes=num_classes)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    # 텐서 변환
    X_train_tensor = torch.FloatTensor(X_train)
    y_train_tensor = torch.LongTensor(y_train)
    X_val_tensor = torch.FloatTensor(X_val)
    y_val_tensor = torch.LongTensor(y_val)

    best_val_acc = 0
    best_model_state = None

    print("\n🚀 학습 시작...")
    for epoch in range(epochs):
        # 학습
        model.train()
        optimizer.zero_grad()
        outputs = model(X_train_tensor)
        loss = criterion(outputs, y_train_tensor)
        loss.backward()
        optimizer.step()

        # 검증
        model.eval()
        with torch.no_grad():
            val_outputs = model(X_val_tensor)
            val_preds = val_outputs.argmax(dim=1)
            val_acc = (val_preds == y_val_tensor).float().mean().item()

        if val_acc > best_val_acc:
            best_val_acc = val_acc
            best_model_state = model.state_dict().copy()

        if (epoch + 1) % 10 == 0:
            print(f"Epoch {epoch+1}/{epochs} - Loss: {loss:.4f}, Val Acc: {val_acc:.4f}")

    # 최적 모델 복원
    model.load_state_dict(best_model_state)
    print(f"\n✅ 학습 완료 - 최고 검증 정확도: {best_val_acc:.4f}")

    return model, best_val_acc

def evaluate_model(model, X_test, y_test, label_encoder):
    """모델 평가"""
    model.eval()
    X_test_tensor = torch.FloatTensor(X_test)
    y_test_tensor = torch.LongTensor(y_test)

    with torch.no_grad():
        outputs = model(X_test_tensor)
        preds = outputs.argmax(dim=1)
        test_acc = (preds == y_test_tensor).float().mean().item()

    print(f"\n📊 테스트 정확도: {test_acc:.4f}")

    # 클래스별 정확도
    print("\n클래스별 정확도:")
    for i, class_name in enumerate(label_encoder.classes_):
        mask = y_test == i
        if mask.sum() > 0:
            class_acc = (preds[mask] == y_test_tensor[mask]).float().mean().item()
            print(f"  {class_name}: {class_acc:.4f} ({mask.sum()} samples)")

    return test_acc

def save_model(model, scaler, label_encoder, feature_names, test_acc, output_dir):
    """모델 저장"""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # TorchScript 변환
    model.eval()
    example_input = torch.randn(1, len(feature_names))
    traced_model = torch.jit.trace(model, example_input)

    model_path = output_dir / 'document_classifier.pt'
    traced_model.save(str(model_path))
    print(f"\n✅ 모델 저장: {model_path}")

    # 메타데이터 저장
    metadata = {
        'feature_names': feature_names,
        'label_mapping': {i: label for i, label in enumerate(label_encoder.classes_)},
        'scaler_mean': scaler.mean_.tolist(),
        'scaler_std': scaler.scale_.tolist(),
        'test_accuracy': float(test_acc),
        'num_classes': len(label_encoder.classes_),
        'input_dim': len(feature_names)
    }

    metadata_path = output_dir / 'model_metadata.json'
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"✅ 메타데이터 저장: {metadata_path}")

def main():
    parser = argparse.ArgumentParser(description='문서 타입 분류기 학습')
    parser.add_argument('--data', type=str, required=True, help='학습 데이터 CSV 파일')
    parser.add_argument('--output', type=str, default='../core/models', help='모델 출력 디렉토리')
    parser.add_argument('--epochs', type=int, default=100, help='학습 에폭 수')
    args = parser.parse_args()

    # 데이터 로드
    df = load_data(args.data)

    # 특징 추출
    X, y, feature_names = extract_features(df)

    # 라벨 인코딩
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)

    # 데이터 분할
    X_temp, X_test, y_temp, y_test = train_test_split(
        X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
    )
    X_train, X_val, y_train, y_val = train_test_split(
        X_temp, y_temp, test_size=0.2, random_state=42, stratify=y_temp
    )

    print(f"\n📊 데이터 분할:")
    print(f"  Train: {len(X_train)} samples")
    print(f"  Val: {len(X_val)} samples")
    print(f"  Test: {len(X_test)} samples")

    # 정규화
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_val = scaler.transform(X_val)
    X_test = scaler.transform(X_test)

    # 모델 학습
    model, val_acc = train_model(X_train, y_train, X_val, y_val,
                                   len(label_encoder.classes_), args.epochs)

    # 모델 평가
    test_acc = evaluate_model(model, X_test, y_test, label_encoder)

    # 모델 저장
    save_model(model, scaler, label_encoder, feature_names, test_acc, args.output)

    print("\n🎉 학습 완료!")

if __name__ == '__main__':
    main()
