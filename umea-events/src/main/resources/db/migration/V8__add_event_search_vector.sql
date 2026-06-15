ALTER TABLE event
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector('swedish', coalesce(title, '') || ' ' || coalesce(description, ''))
        ) STORED;

CREATE INDEX idx_event_search_vector ON event USING GIN(search_vector);
