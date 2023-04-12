CREATE TABLE IF NOT EXISTS baseentity (
    deploycode TEXT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    icon TEXT
);