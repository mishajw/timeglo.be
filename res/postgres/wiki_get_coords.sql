SELECT g.gt_name AS geo_name, p.page_title AS page_name, g.gt_lat, g.gt_lon
FROM page p, geo_tags g
WHERE
  p.page_title = ? AND
  g.gt_page_id = p.page_id;