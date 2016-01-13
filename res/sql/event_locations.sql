SELECT
  Ev.occurs, Ev.description, Ev.latitude, Ev.longitude, Ev.population, string_agg(LN.name, ',') AS allNames
FROM
  locationnames LN,
  (
    SELECT L.id, E.occurs, E.description, L.latitude, L.longitude, L.population
    FROM
      locations L,
      events E,
      eventLocations EL
    WHERE
      L.id = EL.locationID AND
      E.id = EL.eventID AND
      ? < E.occurs AND E.occurs < ?
  ) AS Ev
WHERE
  Ev.id = LN.locationID
GROUP BY
  Ev.id, Ev.occurs, Ev.description, Ev.latitude, Ev.longitude, Ev.population;