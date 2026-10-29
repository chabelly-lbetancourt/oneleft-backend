"""Training, saving and loading of the acceptance model."""
import json
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.metrics import brier_score_loss, roc_auc_score

from .features import FEATURES
from .synthetic import history

MODEL_FILE = Path('model.joblib')
INFO_FILE = Path('model.json')


def train(rows: int = 40_000, seed: int = 42) -> tuple[GradientBoostingClassifier, dict]:
    x, y = history(rows, seed)
    cut = int(rows * 0.8)
    model = GradientBoostingClassifier(n_estimators=150, max_depth=3, learning_rate=0.08, random_state=seed)
    model.fit(x[:cut], y[:cut])
    probability = model.predict_proba(x[cut:])[:, 1]
    nearer_is_better = -x[cut:, FEATURES.index('distanceKm')]
    info = {
        'version': f'gbm-{rows}-{seed}',
        'rows': rows,
        'seed': seed,
        'acceptanceRate': round(float(y.mean()), 4),
        'auc': round(float(roc_auc_score(y[cut:], probability)), 4),
        'aucDistanceRule': round(float(roc_auc_score(y[cut:], nearer_is_better)), 4),
        'brier': round(float(brier_score_loss(y[cut:], probability)), 4),
        'features': FEATURES,
    }
    return model, info


def save(model, info: dict, directory: Path = Path('.')) -> None:
    joblib.dump(model, directory / MODEL_FILE, compress=3)
    (directory / INFO_FILE).write_text(json.dumps(info, indent=2))


def load(directory: Path = Path('.')) -> tuple[GradientBoostingClassifier, dict]:
    return joblib.load(directory / MODEL_FILE), json.loads((directory / INFO_FILE).read_text())


def score(model, rows: list[list[float]]) -> np.ndarray:
    return model.predict_proba(np.asarray(rows, dtype=float))[:, 1]
