-- HU-004 · Ver planes cercanos
-- La búsqueda filtra por distancia en metros con ST_DWithin sobre geography (esfera terrestre). Un índice GiST
-- sobre la columna geometry no sirve para esa expresión, así que se sustituye por un índice de expresión.
DROP INDEX plan_location;
CREATE INDEX plan_location_geography ON plan USING GIST ((location::geography));
