CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(40) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username_lower UNIQUE (username)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_case_insensitive ON users (LOWER(username));

CREATE TABLE IF NOT EXISTS parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    description VARCHAR(1000),
    image_url VARCHAR(1000),
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS party_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_party_members UNIQUE (party_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_party_members_user ON party_members(user_id, status);

CREATE TABLE IF NOT EXISTS party_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    created_by UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    recipient_contact VARCHAR(320),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    consumed_by UUID REFERENCES users(id),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_invites_party ON party_invites(party_id, created_at DESC);

CREATE TABLE IF NOT EXISTS chronicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    creator_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    generated_preview VARCHAR(600),
    current_generated_story_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_chronicles_party ON chronicles(party_id, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS written_story_documents (
    chronicle_id UUID PRIMARY KEY REFERENCES chronicles(id) ON DELETE CASCADE,
    content TEXT NOT NULL DEFAULT '',
    content_version BIGINT NOT NULL DEFAULT 0,
    locked_by UUID REFERENCES users(id),
    lock_token_hash VARCHAR(64),
    locked_at TIMESTAMPTZ,
    lock_expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS written_story_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    granted_by UUID NOT NULL REFERENCES users(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_written_permissions UNIQUE(chronicle_id, user_id)
);

CREATE TABLE IF NOT EXISTS game_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL UNIQUE REFERENCES chronicles(id) ON DELETE CASCADE,
    cycle_count SMALLINT NOT NULL CHECK (cycle_count BETWEEN 1 AND 3),
    participant_count INTEGER NOT NULL,
    current_sequence INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS game_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES game_runs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    cycle_number SMALLINT NOT NULL,
    position_in_cycle INTEGER NOT NULL,
    sequence_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    skipped_by UUID REFERENCES users(id),
    CONSTRAINT uq_game_turn_sequence UNIQUE(run_id, sequence_number)
);
CREATE INDEX IF NOT EXISTS idx_game_turn_current ON game_turns(run_id, status, sequence_number);

CREATE TABLE IF NOT EXISTS game_drafts (
    turn_id UUID PRIMARY KEY REFERENCES game_turns(id) ON DELETE CASCADE,
    content TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS game_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    turn_id UUID NOT NULL UNIQUE REFERENCES game_turns(id) ON DELETE CASCADE,
    run_id UUID NOT NULL REFERENCES game_runs(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    sequence_number INTEGER NOT NULL,
    cycle_number SMALLINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    disabled_reason VARCHAR(600),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_game_segments_sequence UNIQUE(run_id, sequence_number)
);
CREATE INDEX IF NOT EXISTS idx_game_segments_run ON game_segments(run_id, sequence_number);

CREATE TABLE IF NOT EXISTS segment_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id UUID NOT NULL REFERENCES game_segments(id) ON DELETE CASCADE,
    changed_by UUID NOT NULL REFERENCES users(id),
    previous_content TEXT NOT NULL,
    new_content TEXT,
    previous_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(600),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS generated_stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_generated_story_version UNIQUE(chronicle_id, version_number)
);
-- ALTER TABLE chronicles ADD CONSTRAINT fk_chronicle_generated_story
--     FOREIGN KEY (current_generated_story_id) REFERENCES generated_stories(id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_chronicle_generated_story'
    ) THEN
ALTER TABLE chronicles
    ADD CONSTRAINT fk_chronicle_generated_story
        FOREIGN KEY (current_generated_story_id)
            REFERENCES generated_stories(id);
END IF;
END $$;

CREATE TABLE IF NOT EXISTS ai_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_pending ON ai_jobs(status, created_at);

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id),
    party_id UUID REFERENCES parties(id),
    chronicle_id UUID REFERENCES chronicles(id),
    event_type VARCHAR(80) NOT NULL,
    payload_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
