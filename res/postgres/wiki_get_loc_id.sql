SELECT g.gt_id
FROM page p, geo_tags g
WHERE
  p.page_title = ? AND
  g.gt_page_id = p.page_id;