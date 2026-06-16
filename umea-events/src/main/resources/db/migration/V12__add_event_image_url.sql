-- Events are visual; the frontend needs an image per event. Optional URL to a hosted image.
ALTER TABLE event ADD COLUMN image_url TEXT;
