CREATE INDEX name_idx       ON locationNames    (name);

CREATE INDEX occurs_idx     ON events           (occurs);

CREATE INDEX n_locid_idx    ON locationNames    (locationID);

CREATE INDEX el_locid_idx   ON eventLocations   (locationID);

CREATE INDEX el_evid_idx    ON eventLocations   (eventID);
