"""HTTP API: POST /v1/score gives the probability that each candidate takes a spot (HU-012)."""
import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from pydantic import BaseModel, Field

from .features import FEATURES
from .model import load, score

MAX_CANDIDATES = 5000


class Candidate(BaseModel):
    id: str = Field(max_length=64)
    distanceKm: float = Field(ge=0, le=1000)
    distanceRatio: float = Field(ge=0, le=100)
    minutesToStart: float = Field(ge=0, le=100_000)
    localHour: int = Field(ge=0, le=23)
    weekday: int = Field(ge=0, le=6)
    explicitInterest: int = Field(ge=0, le=1)
    viaAlert: int = Field(ge=0, le=1)
    pastRate: float = Field(ge=0, le=1)
    pastSent: int = Field(ge=0, le=100_000)
    sentToday: int = Field(ge=0, le=1000)
    freeSpots: int = Field(ge=0, le=1000)


class ScoreRequest(BaseModel):
    candidates: list[Candidate] = Field(max_length=MAX_CANDIDATES)


class Score(BaseModel):
    id: str
    probability: float


class ScoreResponse(BaseModel):
    modelVersion: str
    scores: list[Score]


state: dict = {}


@asynccontextmanager
async def lifespan(_: FastAPI):
    state['model'], state['info'] = load(Path(os.environ.get('RANKER_MODEL_DIR', '.')))
    yield
    state.clear()


app = FastAPI(title='OneLeft ranker', version='1.0.0', lifespan=lifespan)


@app.get('/health')
def health() -> dict:
    return {'status': 'UP', 'modelVersion': state['info']['version']}


@app.post('/v1/score')
def score_candidates(request: ScoreRequest) -> ScoreResponse:
    rows = [[getattr(c, name) for name in FEATURES] for c in request.candidates]
    probabilities = score(state['model'], rows) if rows else []
    return ScoreResponse(modelVersion=state['info']['version'],
                         scores=[Score(id=c.id, probability=round(float(p), 6))
                                 for c, p in zip(request.candidates, probabilities)])
