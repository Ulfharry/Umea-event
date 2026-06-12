CREATE TABLE venue (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL,
    address     VARCHAR(300),
    owner_id    UUID         NOT NULL REFERENCES app_user(id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venue_owner ON venue(owner_id);
CREATE INDEX idx_venue_active ON venue(active);
