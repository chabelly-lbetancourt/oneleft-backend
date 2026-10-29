"""Reproducible synthetic history of notices and whether each led to a spot (HU-012).

There is no real history before launch, so the model is trained on this one. The way people accept is written down
here, as a logistic function of the features plus a hidden propensity of each person that only pastRate shows
in part. The same function is the ground truth of the simulator (evaluate.py): the comparison shows that ranking
works when acceptance depends on more than distance, not how much it gains with real people. Real outcomes are stored
by notifications (accepted_at) to retrain later.
"""
import numpy as np

from .features import FEATURES, PRIOR_ACCEPTED, PRIOR_SENT


def sample_candidates(rng: np.random.Generator, n: int, minutes_to_start: float | None = None,
                      free_spots: int | None = None) -> tuple[np.ndarray, np.ndarray]:
    """n candidates: the feature matrix (columns as FEATURES) and the true probability that each accepts."""
    radius = rng.uniform(0.5, 10.0, n)
    distance = radius * rng.uniform(0, 1, n) ** 0.8
    minutes = np.full(n, minutes_to_start) if minutes_to_start is not None else rng.uniform(20, 360, n)
    hour = rng.integers(8, 23, n)
    weekday = rng.integers(0, 7, n)
    explicit = (rng.uniform(0, 1, n) < 0.6).astype(float)
    via_alert = (rng.uniform(0, 1, n) < 0.1).astype(float)
    propensity = rng.beta(2, 6, n)  # hidden: how often this person says yes
    sent = rng.poisson(6, n)
    accepted = rng.binomial(sent, propensity)
    past_rate = (accepted + PRIOR_ACCEPTED) / (sent + PRIOR_SENT)
    sent_today = np.minimum(rng.poisson(1.0, n), 5)
    spots = np.full(n, free_spots) if free_spots is not None else rng.integers(1, 6, n)
    x = np.column_stack([distance, distance / radius, minutes, hour, weekday, explicit, via_alert, past_rate, sent,
                         sent_today, spots]).astype(float)
    return x, acceptance_probability(x, propensity)


def acceptance_probability(x: np.ndarray, propensity: np.ndarray) -> np.ndarray:
    col = {name: x[:, i] for i, name in enumerate(FEATURES)}
    logit = (-1.3
             + 1.2 * col['explicitInterest'] + 1.5 * col['viaAlert']
             - 0.30 * col['distanceKm'] - 1.0 * col['distanceRatio']
             - 0.0045 * col['minutesToStart'] + 0.9 * (col['minutesToStart'] < 90)
             + 5.0 * (propensity - 0.25)
             + 0.45 * (col['weekday'] >= 5) + 0.5 * ((col['localHour'] >= 17) & (col['localHour'] <= 21))
             - 0.35 * col['sentToday'])
    return 1 / (1 + np.exp(-logit))


def history(rows: int, seed: int) -> tuple[np.ndarray, np.ndarray]:
    """rows notices and whether each was accepted (0/1)."""
    rng = np.random.default_rng(seed)
    x, p = sample_candidates(rng, rows)
    return x, (rng.uniform(0, 1, rows) < p).astype(int)
