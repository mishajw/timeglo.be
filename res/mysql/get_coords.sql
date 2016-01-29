SELECT p.gt_name, p.gt_lat, p.gt_long
FROM page p, geo_tags g
WHERE
  p.id = ? AND
  g.id = p.id;