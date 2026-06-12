-- V1: Skapar category-tabellen.
-- gen_random_uuid() finns inbyggt i PostgreSQL 13+ (kärnan), ingen extension krävs.

CREATE TABLE category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(80) NOT NULL UNIQUE,
    slug        VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);
