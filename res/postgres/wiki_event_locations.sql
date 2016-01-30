SELECT
  E.occurs, E.description,
  (CASE
   WHEN L.gt_name IS NULL OR L.gt_name = ''
     THEN P.page_title
     ELSE L.gt_name
   END) AS name,
  L.gt_lat AS latitude, L.gt_lon AS longitude
FROM
  events E,
  geo_tags L,
  wikieventlocations EL,
  page P
WHERE
  E.occurs > ? AND E.occurs < ? AND
  EL.eventid = E.id AND
  EL.locationid = L.gt_id AND
  P.page_id = L.gt_page_id