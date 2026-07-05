-- Stock image per category so an event is never without a picture (event image falls back to
-- its category's image). Seeded with a deterministic placeholder per category; admins can
-- change these in the dashboard.
ALTER TABLE category ADD COLUMN image_url TEXT;

UPDATE category SET image_url = 'https://picsum.photos/seed/uven-' || slug || '/800/450';
