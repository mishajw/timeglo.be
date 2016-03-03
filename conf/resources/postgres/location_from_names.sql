SELECT    N.id as nameID, L.id, L.name, L.latitude, L.longitude, L.population, N.name AS foundName
FROM      locations L, locationNames N
WHERE
  L.id = N.locationID AND
  N.name = ANY(?)
ORDER BY L.population DESC
LIMIT 1;
