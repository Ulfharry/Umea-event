-- A venue can carry a default image (e.g. its logo). Events without their own image fall back to
-- their venue's image, then to the category stock image. NULL = no venue image.
ALTER TABLE venue ADD COLUMN image_url TEXT;
