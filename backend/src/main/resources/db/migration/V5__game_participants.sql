CREATE TABLE IF NOT EXISTS game_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES game_runs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    removed_by_type VARCHAR(20),
    removed_by_user_id UUID REFERENCES users(id),
    left_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_game_participant UNIQUE(run_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_game_participants_run ON game_participants(run_id);

-- Backfill: every existing run gets one ACTIVE row per user who already has at least one turn.
INSERT INTO game_participants (run_id, user_id, status, created_at)
SELECT DISTINCT t.run_id, t.user_id, 'ACTIVE', NOW()
FROM game_turns t
ON CONFLICT (run_id, user_id) DO NOTHING;
