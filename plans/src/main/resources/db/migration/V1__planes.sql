-- HU-003 · Publicar un plan con plazas libres
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE plan (
    id                UUID PRIMARY KEY,
    organizer_id      UUID         NOT NULL,
    organizer_name    VARCHAR(50)  NOT NULL,
    activity          VARCHAR(30)  NOT NULL,
    title             VARCHAR(80)  NOT NULL,
    description       VARCHAR(280),
    meeting_point     VARCHAR(100) NOT NULL,
    -- Punto de encuentro en WGS84 (SRID 4326), la referencia del GPS
    location          GEOMETRY(Point, 4326) NOT NULL,
    starts_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    spots             INTEGER      NOT NULL CHECK (spots BETWEEN 1 AND 20),
    occupied          INTEGER      NOT NULL DEFAULT 0,
    level             VARCHAR(20),
    status            VARCHAR(20)  NOT NULL,
    published_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Bloqueo optimista: se usará al ocupar plazas (HU-005)
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT occupied_within_spots CHECK (occupied BETWEEN 0 AND spots)
);

-- Índice espacial para buscar planes cercanos (HU-004)
CREATE INDEX plan_location ON plan USING GIST (location);
CREATE INDEX plan_open_by_start ON plan (starts_at) WHERE status = 'ABIERTO';
CREATE INDEX plan_organizer ON plan (organizer_id, starts_at);
