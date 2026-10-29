"""python -m ranker.evaluate [--plans N] [--seed S] [--batch B] [--interval T]: the model against the distance rule.

Each simulated plan has a pool of candidates and a few free spots. Both strategies notify in batches of B people every
T minutes until the plan is full or the pool is empty; they differ only in who goes first (nearest first, or the
likeliest first according to the model). The same random draw decides whether each candidate accepts, so the two are
compared on the same people. Acceptance follows synthetic.py, which the model has only seen as history.
"""
import argparse
import json
from pathlib import Path

import numpy as np

from .features import FEATURES
from .model import load, score, train
from .synthetic import sample_candidates


def run_plan(order: np.ndarray, accepts: np.ndarray, spots: int, batch: int, interval: int, deadline: float) -> dict:
    notified = accepted = 0
    for number, start in enumerate(range(0, len(order), batch)):
        minute = number * interval
        if minute >= deadline:
            break
        group = order[start:start + batch]
        take = min(int(accepts[group].sum()), spots - accepted)
        notified += len(group) if take < spots - accepted else int(np.searchsorted(np.cumsum(accepts[group]),
                                                                                  spots - accepted) + 1)
        accepted += take
        if accepted == spots:
            return {'completed': True, 'minutes': minute, 'notified': notified, 'accepted': accepted}
    return {'completed': False, 'minutes': deadline, 'notified': notified, 'accepted': accepted}


def summarise(results: list[dict]) -> dict:
    done = [r for r in results if r['completed']]
    notified = sum(r['notified'] for r in results)
    return {
        'completedPlans': round(len(done) / len(results), 4),
        'minutesToComplete': round(float(np.mean([r['minutes'] for r in done])), 2) if done else None,
        'acceptanceRate': round(sum(r['accepted'] for r in results) / notified, 4),
        'noticesPerPlan': round(notified / len(results), 2),
    }


def simulate(model, plans: int = 2000, seed: int = 7, batch: int = 5, interval: int = 3) -> dict:
    rng = np.random.default_rng(seed)
    results = {'distanceRule': [], 'model': []}
    for _ in range(plans):
        pool = int(rng.integers(20, 80))
        minutes, spots = float(rng.uniform(30, 240)), int(rng.integers(1, 5))
        x, p = sample_candidates(rng, pool, minutes_to_start=minutes, free_spots=spots)
        accepts = rng.uniform(0, 1, pool) < p
        by_distance = np.argsort(x[:, FEATURES.index('distanceKm')], kind='stable')
        by_model = np.argsort(-score(model, x.tolist()), kind='stable')
        deadline = minutes - 15  # nobody joins in the last quarter of an hour
        for name, order in (('distanceRule', by_distance), ('model', by_model)):
            results[name].append(run_plan(order, accepts, spots, batch, interval, deadline))
    return {'plans': plans, 'seed': seed, 'batchSize': batch, 'intervalMinutes': interval,
            **{name: summarise(rows) for name, rows in results.items()}}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--plans', type=int, default=2000)
    parser.add_argument('--seed', type=int, default=7)
    parser.add_argument('--batch', type=int, default=5)
    parser.add_argument('--interval', type=int, default=3)
    parser.add_argument('--model-dir', type=Path, default=None, help='uses a trained model; trains one if missing')
    args = parser.parse_args()
    trained = load(args.model_dir)[0] if args.model_dir else train()[0]
    print(json.dumps(simulate(trained, args.plans, args.seed, args.batch, args.interval), indent=2))
