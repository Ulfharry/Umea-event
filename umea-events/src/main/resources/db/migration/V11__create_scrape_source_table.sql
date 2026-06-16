-- Configured scraping sources. The weekly scheduled job iterates the enabled rows and runs
-- each through the sitemap scraper. Adding a new website = inserting one row (low-touch goal).
CREATE TABLE scrape_source (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name               TEXT        NOT NULL,          -- human label, e.g. 'O''Learys Umeå'
    sitemap_url        TEXT        NOT NULL,          -- XML sitemap URL
    url_pattern        TEXT        NOT NULL,          -- regex matched against <loc> URLs
    enabled            BOOLEAN     NOT NULL DEFAULT TRUE,
    last_run_at        TIMESTAMPTZ,                   -- when the job last ran this source
    last_run_new_count INTEGER,                       -- events staged on the last run
    last_run_error     TEXT,                          -- error from the last run, null on success
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- One source per sitemap + pattern combination.
    CONSTRAINT uq_scrape_source_sitemap_pattern UNIQUE (sitemap_url, url_pattern)
);

CREATE INDEX idx_scrape_source_enabled ON scrape_source(enabled);
