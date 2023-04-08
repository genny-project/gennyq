CREATE TABLE validation (
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    regex TEXT NOT NULL,
    groupcodes TEXT,
    recursive TEXT,
    multiallowed TEXT,
    errormessage TEXT,
    options TEXT
);