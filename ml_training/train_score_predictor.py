#!/usr/bin/env python3
"""
구조적 점수 예측 모델 학습 스크립트
"""
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import numpy as np
import json
from pathlib import Path
import argparse

# 모델 정의
class ScorePredictorModel(nn.Module):
    def __init__(self, input_dim=7):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(input_dim, 32),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(32, 16),
            nn.ReLU(),
            nn.Linear(16, 1),
            nn.Sigmoid()  # 0~1 범위 출력
        )

    def forward(self, x):
        return self.network(x).squeeze()

def load_data(csv_path):
    """학습 데이터 로드"""
    df = pd.read_csv(csv_path)
    print(f"✅ 데이터 로드 완료: {len(df)} samples")
    print(f"점수 분포:\n{df['score'].describe()}")
    return df

def extract_features(df):
    """특징 추출"""
    feature_cols = [
        'importance',
        'resonance',
        'is_header',
        'length',
        'relative_font_size',
        'indent_level',
        'word_count'
    ]

    X = df[feature_cols].values
    y = df['score'].values

    return X, y, feature_cols

def train_model(X_train, y_train, X_val, y_val, epochs=100):
    """모델 학습"""
    model = ScorePredictorModel(input_dim=X_train.shape[1])
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    # 텐서 변환
    X_train_tensor = torch.FloatTensor(X_train)
    y_train_tensor = torch.FloatTensor(y_train)
    X_val_tensor = torch.FloatTensor(X_val)
    y_val_tensor = torch.FloatTensor(y_val)

    best_val_loss = float('inf')
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
            val_loss = criterion(val_outputs, y_val_tensor).item()
            val_mae = torch.abs(val_outputs - y_val_tensor).mean().item()

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_model_state = model.state_dict().copy()

        if (epoch + 1) % 10 == 0:
            print(f"Epoch {epoch+1}/{epochs} - Loss: {loss:.4f}, Val Loss: {val_loss:.4f}, Val MAE: {val_mae:.4f}")

    # 최적 모델 복원
    model.load_state_dict(best_model_state)
    print(f"\n✅ 학습 완료 - 최저 검증 Loss: {best_val_loss:.4f}")

    return model, best_val_loss

def evaluate_model(model, X_test, y_test):
    """모델 평가"""
    model.eval()
    X_test_tensor = torch.FloatTensor(X_test)
    y_test_tensor = torch.FloatTensor(y_test)

    with torch.no_grad():
        outputs = model(X_test_tensor)
        mse = nn.MSELoss()(outputs, y_test_tensor).item()
        mae = torch.abs(outputs - y_test_tensor).mean().item()
        rmse = np.sqrt(mse)

    print(f"\n📊 테스트 결과:")
    print(f"  MSE: {mse:.4f}")
    print(f"  MAE: {mae:.4f}")
    print(f"  RMSE: {rmse:.4f}")

    return mae

def save_model(model, scaler, feature_names, test_mae, output_dir):
    """모델 저장"""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # TorchScript 변환
    model.eval()
    example_input = torch.randn(1, len(feature_names))
    traced_model = torch.jit.trace(model, example_input)

    model_path = output_dir / 'score_predictor.pt'
    traced_model.save(str(model_path))
    print(f"\n✅ 모델 저장: {model_path}")

    # 메타데이터 저장
    metadata = {
        'feature_names': feature_names,
        'scaler_mean': scaler.mean_.tolist(),
        'scaler_std': scaler.scale_.tolist(),
        'test_mae': float(test_mae),
        'input_dim': len(feature_names)
    }

    metadata_path = output_dir / 'score_predictor_metadata.json'
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"✅ 메타데이터 저장: {metadata_path}")

def main():
    parser = argparse.ArgumentParser(description='구조적 점수 예측 모델 학습')
    parser.add_argument('--data', type=str, required=True, help='학습 데이터 CSV 파일')
    parser.add_argument('--output', type=str, default='../core/models', help='모델 출력 디렉토리')
    parser.add_argument('--epochs', type=int, default=100, help='학습 에폭 수')
    args = parser.parse_args()

    # 데이터 로드
    df = load_data(args.data)

    # 특징 추출
    X, y, feature_names = extract_features(df)

    # 데이터 분할
    X_temp, X_test, y_temp, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    X_train, X_val, y_train, y_val = train_test_split(
        X_temp, y_temp, test_size=0.2, random_state=42
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
    model, val_loss = train_model(X_train, y_train, X_val, y_val, args.epochs)

    # 모델 평가
    test_mae = evaluate_model(model, X_test, y_test)

    # 모델 저장
    save_model(model, scaler, feature_names, test_mae, args.output)

    print("\n🎉 학습 완료!")

if __name__ == '__main__':
    main()
