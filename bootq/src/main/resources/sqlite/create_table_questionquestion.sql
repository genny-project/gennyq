CREATE TABLE question_question (
    parentcode TEXT NOT NULL,
    targetcode TEXT NOT NULL,
    weight TEXT NOT NULL,
    mandatory TEXT,
    oneshot TEXT,
    component TEXT,
    conditions TEXT,
    readonly TEXT,
    formtrigger TEXT,
    createontrigger TEXT,
    dependency TEXT,
    icon TEXT,
    disabled TEXT,
    hidden TEXT
);