-- Optional freshness filter per scrape source. When set, the sitemap scraper skips URLs whose
-- <lastmod> is older than this many days, so stale/old events never enter the review queue.
-- NULL = no filtering (previous behaviour).
ALTER TABLE scrape_source ADD COLUMN max_age_days INTEGER;
