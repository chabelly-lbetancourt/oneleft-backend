-- HU-002 · Perfil con aficiones y nivel
CREATE TABLE profile (
    user_id        UUID PRIMARY KEY,
    display_name   VARCHAR(50)  NOT NULL,
    zone_name      VARCHAR(60),
    -- Coordenadas redondeadas a 2 decimales (~1,1 km): nunca la ubicación exacta
    zone_latitude  NUMERIC(5, 2),
    zone_longitude NUMERIC(6, 2),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT zone_complete CHECK (
        (zone_name IS NULL AND zone_latitude IS NULL AND zone_longitude IS NULL)
        OR (zone_name IS NOT NULL AND zone_latitude IS NOT NULL AND zone_longitude IS NOT NULL))
);

CREATE TABLE profile_hobby (
    user_id  UUID        NOT NULL REFERENCES profile (user_id) ON DELETE CASCADE,
    activity VARCHAR(30) NOT NULL,
    level    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, activity)
);

CREATE INDEX profile_hobby_activity ON profile_hobby (activity);
