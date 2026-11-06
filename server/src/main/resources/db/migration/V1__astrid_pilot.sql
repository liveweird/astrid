CREATE TABLE development_sessions (
    id UUID PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE exercises (
    id UUID NOT NULL,
    session_id UUID NOT NULL REFERENCES development_sessions(id) ON DELETE CASCADE,
    format_id VARCHAR(32) NOT NULL,
    target_skill_id VARCHAR(32) NOT NULL,
    seed INTEGER NOT NULL,
    generation_index INTEGER NOT NULL,
    content_revision VARCHAR(64) NOT NULL,
    generator_revision VARCHAR(64) NOT NULL,
    presentation_payload TEXT NOT NULL,
    reproduction_fingerprint CHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (session_id, id)
);

CREATE TABLE attempts (
    session_id UUID NOT NULL REFERENCES development_sessions(id) ON DELETE CASCADE,
    attempt_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    request_payload TEXT NOT NULL,
    raw_answer TEXT NOT NULL,
    response_payload TEXT NOT NULL,
    content_revision VARCHAR(64) NOT NULL,
    generator_revision VARCHAR(64) NOT NULL,
    evaluation_revision VARCHAR(64) NOT NULL,
    retry_number INTEGER NOT NULL CHECK (retry_number > 0),
    created_at BIGINT NOT NULL,
    PRIMARY KEY (session_id, attempt_id),
    FOREIGN KEY (session_id, exercise_id) REFERENCES exercises(session_id, id) ON DELETE CASCADE
);

CREATE INDEX attempts_exercise_retry_idx ON attempts(session_id, exercise_id, retry_number);
