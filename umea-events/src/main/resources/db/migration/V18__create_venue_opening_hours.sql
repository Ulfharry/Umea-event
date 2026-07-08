-- Weekly opening hours per venue. One row per open interval; no row for a weekday = closed.
-- day_of_week is ISO (1 = Monday … 7 = Sunday). closes_at < opens_at means the interval runs
-- past midnight into the next day (e.g. 17:00–02:00). The schema allows several rows per weekday
-- (split lunch/evening hours), though the v1 editor writes a single interval per day.
CREATE TABLE venue_opening_hours (
    id          UUID PRIMARY KEY,
    venue_id    UUID    NOT NULL REFERENCES venue(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at    TIME    NOT NULL,
    closes_at   TIME    NOT NULL
);

CREATE INDEX idx_venue_opening_hours_venue ON venue_opening_hours(venue_id);
