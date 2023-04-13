CREATE TABLE IF NOT EXISTS question_question (
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
    hidden TEXT,
    capreqs TEXT,
    UNIQUE(parentcode, targetcode)
);