CREATE TABLE event_occurrence (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_occurrence_event     ON event_occurrence(event_id);
CREATE INDEX idx_event_occurrence_starts_at ON event_occurrence(starts_at);
