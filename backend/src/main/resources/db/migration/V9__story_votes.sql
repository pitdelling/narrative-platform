-- Positive-vote system for completed stories: each active party membership gets a
-- 2-unit daily budget (UTC day), spendable across one or two completed chronicles
-- (GAME or WRITTEN, status = PUBLISHED). Two tables mirror the domain model:
--   * story_vote_daily_budgets: source of truth for "how many units a membership has
--     used today" in a given party (lazily created on the first vote of the day).
--   * story_vote_allocations: units a membership currently has assigned to one
--     specific chronicle today; a row is deleted (never stored with units = 0) when
--     the member withdraws their vote from that story.
-- Only "today" (UTC) rows are ever mutated by the application; once the UTC day
-- rolls over, existing rows become historical and immutable. See docs/PROJECT_AUDIT.md
-- ("Votos em histórias — backend") for the full design rationale.

CREATE TABLE IF NOT EXISTS story_vote_daily_budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    membership_id UUID NOT NULL REFERENCES party_members(id),
    vote_date DATE NOT NULL,
    used_units SMALLINT NOT NULL DEFAULT 0 CHECK (used_units BETWEEN 0 AND 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_story_vote_daily_budgets UNIQUE (party_id, membership_id, vote_date)
);
CREATE INDEX IF NOT EXISTS idx_story_vote_daily_budgets_party_date ON story_vote_daily_budgets(party_id, vote_date);
CREATE INDEX IF NOT EXISTS idx_story_vote_daily_budgets_membership_date ON story_vote_daily_budgets(membership_id, vote_date);

CREATE TABLE IF NOT EXISTS story_vote_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    membership_id UUID NOT NULL REFERENCES party_members(id),
    vote_date DATE NOT NULL,
    units SMALLINT NOT NULL CHECK (units BETWEEN 1 AND 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_story_vote_allocations UNIQUE (chronicle_id, membership_id, vote_date)
);
CREATE INDEX IF NOT EXISTS idx_story_vote_allocations_chronicle ON story_vote_allocations(chronicle_id);
CREATE INDEX IF NOT EXISTS idx_story_vote_allocations_party_date ON story_vote_allocations(party_id, vote_date);
CREATE INDEX IF NOT EXISTS idx_story_vote_allocations_membership_date ON story_vote_allocations(membership_id, vote_date);
