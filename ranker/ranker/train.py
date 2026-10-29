"""python -m ranker.train [--rows N] [--seed S] [--out DIR]: trains the model and saves model.joblib and model.json."""
import argparse
import json
from pathlib import Path

from .model import save, train

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--rows', type=int, default=40_000)
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--out', type=Path, default=Path('.'))
    args = parser.parse_args()
    trained, info = train(args.rows, args.seed)
    save(trained, info, args.out)
    print(json.dumps(info, indent=2))
