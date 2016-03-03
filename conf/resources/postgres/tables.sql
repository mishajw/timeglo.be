CREATE TABLE events (
  id              SERIAL PRIMARY KEY,
  occurs          DATE,
  description     TEXT
);

CREATE TABLE locations (
  id              SERIAL PRIMARY KEY,
  name            TEXT,
  latitude        REAL,
  longitude       REAL,
  population      BIGINT
);

CREATE TABLE locationNames (
  id              SERIAL PRIMARY KEY,
  locationID      INT REFERENCES locations,
  name            TEXT
);

CREATE TABLE eventLocations (
  id              SERIAL PRIMARY KEY,
  eventID         INT REFERENCES events,
  locationID      INT REFERENCES locations,
  nameID          INT REFERENCES locationNames
);

CREATE TABLE wikiEventLocations (
  id              SERIAL PRIMARY KEY,
  eventID         INT REFERENCES events,
  locationID      INT REFERENCES geo_tags
);
