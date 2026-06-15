-- Recurring rule for an event (RFC 5545 RRULE stored as text).
-- Wall-clock start time and IANA timezone are stored separately so that
-- occurrences are always expanded in local time (DST-correct).
CREATE TABLE recurrence_rule (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id         UUID        NOT NULL UNIQUE REFERENCES event(id) ON DELETE CASCADE,
    rrule            TEXT        NOT NULL,
    start_time       TIME        NOT NULL,
    duration_minutes INTEGER,
    timezone         VARCHAR(50) NOT NULL,
    -- Up to which instant occurrences have been materialized (NULL = never run)
    horizon          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recurrence_rule_event ON recurrence_rule(event_id);

-- Marks a specific date in a recurring series as cancelled or moved.
-- original_date is the LOCAL date (in the rule's timezone) of the occurrence.
CREATE TABLE occurrence_override (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id      UUID        NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    original_date DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'CANCELLED',
    new_starts_at TIMESTAMPTZ,
    new_ends_at   TIMESTAMPTZ,
    reason        TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, original_date)
);

CREATE INDEX idx_occurrence_override_event ON occurrence_override(event_id);

-- Track which recurrence date each occurrence belongs to (NULL for one-off events).
ALTER TABLE event_occurrence
    ADD COLUMN recurrence_date DATE;

CREATE UNIQUE INDEX idx_occurrence_recurrence_date
    ON event_occurrence(event_id, recurrence_date)
    WHERE recurrence_date IS NOT NULL;
