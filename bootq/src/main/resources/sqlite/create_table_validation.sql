CREATE TABLE IF NOT EXISTS validation (
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    regex TEXT NOT NULL,
    groupcodes TEXT,
    recursive TEXT,
    multiallowed TEXT,
    errormessage TEXT,
    options TEXT
);