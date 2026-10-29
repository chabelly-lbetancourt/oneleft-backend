import numpy as np
import pytest
from fastapi.testclient import TestClient

from ranker import main
from ranker.evaluate import run_plan, simulate
from ranker.features import FEATURES
from ranker.model import save, train
from ranker.synthetic import history, sample_candidates


@pytest.fixture(scope='module')
def trained():
    return train(rows=12_000, seed=1)


@pytest.fixture()
def client(trained, tmp_path, monkeypatch):
    save(*trained, directory=tmp_path)
    monkeypatch.setenv('RANKER_MODEL_DIR', str(tmp_path))
    with TestClient(main.app) as c:
        yield c


def candidate(identifier='a', **changes):
    base = dict(id=identifier, distanceKm=1.0, distanceRatio=0.3, minutesToStart=120, localHour=18, weekday=5,
                explicitInterest=1, viaAlert=0, pastRate=0.4, pastSent=8, sentToday=0, freeSpots=2)
    return {**base, **changes}


def test_history_is_reproducible():
    (x1, y1), (x2, y2) = history(500, 3), history(500, 3)
    assert np.array_equal(x1, x2) and np.array_equal(y1, y2)
    assert x1.shape == (500, len(FEATURES))


def test_candidates_of_a_plan_share_its_start_and_spots():
    x, p = sample_candidates(np.random.default_rng(0), 30, minutes_to_start=60, free_spots=3)
    assert set(x[:, FEATURES.index('minutesToStart')]) == {60.0}
    assert set(x[:, FEATURES.index('freeSpots')]) == {3.0}
    assert ((p > 0) & (p < 1)).all()


def test_model_ranks_better_than_the_distance_rule(trained):
    info = trained[1]
    assert info['auc'] > info['aucDistanceRule'] + 0.05
    assert info['brier'] < 0.15


def test_health_reports_the_model(client):
    body = client.get('/health').json()
    assert body['status'] == 'UP' and body['modelVersion'].startswith('gbm-')


def test_score_prefers_the_likelier_candidate(client):
    near_keen = candidate('keen', distanceKm=0.5, pastRate=0.7, explicitInterest=1, viaAlert=1)
    far_cold = candidate('cold', distanceKm=8, distanceRatio=0.95, pastRate=0.1, explicitInterest=0, sentToday=4)
    response = client.post('/v1/score', json={'candidates': [far_cold, near_keen]}).json()
    scores = {s['id']: s['probability'] for s in response['scores']}
    assert [s['id'] for s in response['scores']] == ['cold', 'keen']
    assert scores['keen'] > scores['cold'] and all(0 < v < 1 for v in scores.values())


def test_score_of_nobody_is_empty(client):
    assert client.post('/v1/score', json={'candidates': []}).json()['scores'] == []


@pytest.mark.parametrize('change', [{'localHour': 24}, {'pastRate': 1.5}, {'distanceKm': -1}, {'weekday': 7}])
def test_invalid_candidates_are_rejected(client, change):
    assert client.post('/v1/score', json={'candidates': [candidate(**change)]}).status_code == 422


def test_a_plan_is_completed_by_the_first_batch_that_fills_it():
    order = np.arange(10)
    accepts = np.array([0, 1, 0, 1, 0, 0, 1, 0, 0, 0], dtype=bool)
    result = run_plan(order, accepts, spots=2, batch=5, interval=3, deadline=60)
    assert result == {'completed': True, 'minutes': 0, 'notified': 4, 'accepted': 2}


def test_a_plan_that_is_not_filled_before_the_deadline_is_not_completed():
    result = run_plan(np.arange(10), np.zeros(10, dtype=bool), spots=1, batch=5, interval=3, deadline=60)
    assert result['completed'] is False and result['notified'] == 10


def test_the_model_beats_the_distance_rule_in_the_simulation(trained):
    result = simulate(trained[0], plans=400, seed=5)
    assert result['model']['acceptanceRate'] > result['distanceRule']['acceptanceRate']
    assert result['model']['completedPlans'] >= result['distanceRule']['completedPlans']
    assert result['model']['noticesPerPlan'] <= result['distanceRule']['noticesPerPlan']
