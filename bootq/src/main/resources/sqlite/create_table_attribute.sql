CREATE TABLE IF NOT EXISTS attribute (
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    datatype TEXT NOT NULL,
    hint TEXT,
    privacy TEXT,
    description TEXT,
    help TEXT,
    placeholder TEXT,
    defaultvalue TEXT,
    conditions TEXT,
    icon TEXT
);