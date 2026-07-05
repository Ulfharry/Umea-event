-- Allow deleting a published event that was promoted from a scraped one: null the back-link
-- instead of blocking the delete. event_occurrence / recurrence_rule / occurrence_override
-- already cascade on event delete.
ALTER TABLE raw_scraped_event DROP CONSTRAINT IF EXISTS raw_scraped_event_promoted_event_id_fkey;
ALTER TABLE raw_scraped_event
    ADD CONSTRAINT raw_scraped_event_promoted_event_id_fkey
    FOREIGN KEY (promoted_event_id) REFERENCES event(id) ON DELETE SET NULL;
