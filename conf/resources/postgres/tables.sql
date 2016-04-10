CREATE TABLE date_precision (
  id              SERIAL PRIMARY KEY,
  type            TEXT
);

INSERT INTO date_precision(type) VALUES ('PreciseToYear');
INSERT INTO date_precision(type) VALUES ('PreciseToMonth');
INSERT INTO date_precision(type) VALUES ('PreciseToDate');
INSERT INTO date_precision(type) VALUES ('NotPrecise');

CREATE TABLE events (
  id              SERIAL PRIMARY KEY,
  occurs          DATE,
  precision       INT REFERENCES date_precision,
  description     TEXT
);

CREATE TABLE locations (
  id              SERIAL PRIMARY KEY,
  name            TEXT,
  latitude        REAL,
  longitude       REAL
);

CREATE TABLE locationNames (
  id              SERIAL PRIMARY KEY,
  location_id     INT REFERENCES locations,
  name            TEXT
);

CREATE TABLE eventLocations (
  event_id        INT REFERENCES events,
  location_id     INT REFERENCES locations,
  PRIMARY KEY (event_id, location_id)
);
