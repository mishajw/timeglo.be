SELECT L.id, L.name, N.name AS matchedName, E.occurs, E.description, L.latitude, L.longitude, L.population
FROM
  locations L,
  events E,
  locationNames N,
  eventLocations EL
WHERE
  L.id = EL.locationID AND
  E.id = EL.eventID AND
  N.id = EL.nameID AND
  ? < E.occurs AND E.occurs < ?;