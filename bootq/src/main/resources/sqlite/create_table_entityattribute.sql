CREATE TABLE IF NOT EXISTS entityattribute (
    x TEXT,
    baseentitycode TEXT NOT NULL,
    attributecode TEXT NOT NULL,
    weight TEXT NOT NULL,
    valuestring TEXT,
    valuedatetime TEXT,
    valuelong TEXT,
    valueinteger TEXT,
    valuedouble TEXT,
    valuebaseentitycodelist TEXT,
    privacy TEXT,
    capreqs TEXT,
    UNIQUE(baseentitycode, attributecode)
);