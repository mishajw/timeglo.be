SELECT E.occurs, E.description, N.allNames, L.latitude, L.longitude, L.population
FROM
  locations L,
  events E,
  eventLocations EL,
  (SELECT N2.locationID, string_agg(N2.name, ',') AS allNames
    FROM locationNames N2
    GROUP BY N2.locationID) as N
WHERE
  L.id = N.locationID AND
  L.id = EL.locationID AND
  E.id = EL.eventID
ORDER BY E.occurs
