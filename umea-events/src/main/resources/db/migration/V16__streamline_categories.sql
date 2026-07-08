-- Streamline the 8 specific categories into 6 broad "what are you in the mood for" buckets.
-- Renames preserve the row id, so existing events stay linked with no remap. Standup and
-- Temakvällar are folded into "Övriga event": their events are remapped, then the rows removed.
-- image_url is intentionally left untouched so any admin-set category image survives.

-- 1. Rename-in-place (events keep their category_id).
UPDATE category SET name = 'Quiz',       slug = 'quiz',       description = 'Quiz och frågesport'      WHERE slug = 'pubquiz';
UPDATE category SET name = 'Nattliv',    slug = 'nattliv',    description = 'Klubb, DJ och nattliv'     WHERE slug = 'dj-kvallar';
UPDATE category SET name = 'Sport',      slug = 'sport',      description = 'Sport på storbild'         WHERE slug = 'sportvisningar';
UPDATE category SET name = 'Gastronomi', slug = 'gastronomi', description = 'Mat, dryck och provningar'  WHERE slug = 'vinprovningar';
-- Livemusik and Övriga event keep their name/slug.

-- 2. Fold Standup + Temakvällar into Övriga event: move their events over first.
UPDATE event
   SET category_id = (SELECT id FROM category WHERE slug = 'ovriga-event')
 WHERE category_id IN (SELECT id FROM category WHERE slug IN ('standup', 'temakvallar'));

-- 3. Remove the now-empty categories.
DELETE FROM category WHERE slug IN ('standup', 'temakvallar');
