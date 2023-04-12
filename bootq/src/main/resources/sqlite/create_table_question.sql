CREATE TABLE IF NOT EXISTS question (
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    attributecode TEXT NOT NULL,
    html TEXT,
    placeholder TEXT,
    oneshot TEXT,
    readonly TEXT,
    helper TEXT,
    icon TEXT
);