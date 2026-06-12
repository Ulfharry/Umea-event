-- V2: Seedar de åtta kategorierna från specen.
-- ON CONFLICT gör migrationen säker även om en rad redan finns.

INSERT INTO category (name, slug, description) VALUES
    ('Pubquiz',        'pubquiz',        'Frågesport på pub eller bar'),
    ('Livemusik',      'livemusik',      'Liveframträdanden med musik'),
    ('DJ-kvällar',     'dj-kvallar',     'DJ-set och klubbkvällar'),
    ('Standup',        'standup',        'Standup-comedy och humor'),
    ('Vinprovningar',  'vinprovningar',  'Provning av vin och dryck'),
    ('Temakvällar',    'temakvallar',    'Tematiska kvällar och specialevent'),
    ('Sportvisningar', 'sportvisningar', 'Visning av sportevenemang'),
    ('Övriga event',   'ovriga-event',   'Event som inte passar i övriga kategorier')
ON CONFLICT (slug) DO NOTHING;
