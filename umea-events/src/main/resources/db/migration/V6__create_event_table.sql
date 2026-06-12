CREATE TABLE event (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    venue_id    UUID         NOT NULL REFERENCES venue(id),
    category_id UUID         NOT NULL REFERENCES category(id),
    owner_id    UUID         NOT NULL REFERENCES app_user(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_status   ON event(status);
CREATE INDEX idx_event_owner    ON event(owner_id);
CREATE INDEX idx_event_venue    ON event(venue_id);
CREATE INDEX idx_event_category ON event(category_id);
