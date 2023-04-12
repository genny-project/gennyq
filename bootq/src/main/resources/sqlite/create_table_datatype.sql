CREATE TABLE IF NOT EXISTS datatype (
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    classname TEXT NOT NULL,
    component TEXT NOT NULL,
    validations TEXT NOT NULL,
    inputmask TEXT
);