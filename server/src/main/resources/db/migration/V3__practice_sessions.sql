CREATE TABLE practice_sessions (
    owner_session_id UUID NOT NULL REFERENCES development_sessions(id) ON DELETE CASCADE,
    practice_session_id UUID NOT NULL,
    request_payload TEXT NOT NULL,
    plan_payload TEXT NOT NULL,
    focus_id VARCHAR(32) NOT NULL,
    plan_revision VARCHAR(64) NOT NULL,
    seed INTEGER NOT NULL CHECK (seed >= 0),
    session_expires_at BIGINT NOT NULL,
    cursor INTEGER NOT NULL DEFAULT 0 CHECK (cursor BETWEEN 0 AND 8),
    introduction_opened BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (owner_session_id, practice_session_id)
);

CREATE TABLE practice_items (
    owner_session_id UUID NOT NULL,
    practice_session_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 7),
    exercise_id UUID NOT NULL,
    lesson_opened BOOLEAN NOT NULL DEFAULT FALSE,
    hint_opened BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (owner_session_id, practice_session_id, position),
    UNIQUE (owner_session_id, exercise_id),
    FOREIGN KEY (owner_session_id, practice_session_id)
        REFERENCES practice_sessions(owner_session_id, practice_session_id) ON DELETE CASCADE,
    FOREIGN KEY (owner_session_id, exercise_id)
        REFERENCES exercises(session_id, id) ON DELETE CASCADE
);

CREATE TABLE practice_attempt_context (
    owner_session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    practice_session_id UUID NOT NULL,
    position INTEGER NOT NULL,
    context_payload TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (owner_session_id, attempt_id),
    FOREIGN KEY (owner_session_id, attempt_id)
        REFERENCES attempts(session_id, attempt_id) ON DELETE CASCADE,
    FOREIGN KEY (owner_session_id, practice_session_id, position)
        REFERENCES practice_items(owner_session_id, practice_session_id, position) ON DELETE CASCADE
);

CREATE INDEX practice_attempt_context_item_idx
    ON practice_attempt_context(owner_session_id, practice_session_id, position, created_at);
