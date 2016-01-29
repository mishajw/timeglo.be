SELECT
  earliest.occurs AS earliest_date,
  latest.occurs AS latest_date
FROM
  (
    SELECT e1.occurs
    FROM events e1
    ORDER BY e1.occurs ASC
    LIMIT 1
  ) AS earliest,
  (
    SELECT e2.occurs
    FROM events e2
    ORDER BY e2.occurs DESC
    LIMIT 1
  ) AS latest;