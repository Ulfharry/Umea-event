-- Raw event data from external sources. NEVER auto-published.
-- Admin must review and explicitly promote to a real Event+Occurrence.
CREATE TABLE raw_scraped_event (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source            VARCHAR(50) NOT NULL,           -- e.g. 'MANUAL_IMPORT', 'WEB_SCRAPER'
    external_id       VARCHAR(255),                   -- source-specific dedup key (nullable for manual)
    raw_title         TEXT        NOT NULL,
    raw_description   TEXT,
    raw_venue_name    TEXT,
    raw_starts_at     TEXT,                           -- original text as scraped, may be unparsed
    raw_ends_at       TEXT,
    parsed_starts_at  TIMESTAMPTZ,                   -- set when text was successfully parsed
    parsed_ends_at    TIMESTAMPTZ,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    admin_notes       TEXT,
    reviewed_at       TIMESTAMPTZ,
    reviewed_by       UUID REFERENCES app_user(id),
    promoted_event_id UUID REFERENCES event(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Prevent the same external source record from being imported twice.
    -- NULLs are excluded from uniqueness (each manual import is distinct).
    CONSTRAINT uq_scraped_source_external UNIQUE (source, external_id)
);

CREATE INDEX idx_raw_scraped_event_status ON raw_scraped_event(status);
CREATE INDEX idx_raw_scraped_event_source ON raw_scraped_event(source);
