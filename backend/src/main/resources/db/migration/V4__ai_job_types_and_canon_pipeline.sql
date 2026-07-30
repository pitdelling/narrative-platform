ALTER TABLE ai_jobs ADD COLUMN IF NOT EXISTS job_type VARCHAR(30) NOT NULL DEFAULT 'STORY_ADAPTATION_GENERATION';
ALTER TABLE ai_jobs ALTER COLUMN job_type DROP DEFAULT;
CREATE INDEX IF NOT EXISTS idx_ai_jobs_chronicle_type_status ON ai_jobs(chronicle_id, job_type, status);

ALTER TABLE chronicles ADD COLUMN IF NOT EXISTS synopsis VARCHAR(240);
ALTER TABLE chronicles ADD COLUMN IF NOT EXISTS current_synopsis_id UUID;

CREATE TABLE IF NOT EXISTS party_ai_tag_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    color VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_party_ai_tag_settings_category UNIQUE(party_id, category)
);
CREATE INDEX IF NOT EXISTS idx_party_ai_tag_settings_party ON party_ai_tag_settings(party_id);

CREATE TABLE IF NOT EXISTS chronicle_synopses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    content VARCHAR(240),
    model VARCHAR(100),
    input_tokens INTEGER,
    output_tokens INTEGER,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_chronicle_synopsis_version UNIQUE(chronicle_id, version_number)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chronicle_current_synopsis') THEN
        ALTER TABLE chronicles
            ADD CONSTRAINT fk_chronicle_current_synopsis
                FOREIGN KEY (current_synopsis_id)
                    REFERENCES chronicle_synopses(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS canon_map_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chronicle_id UUID NOT NULL REFERENCES chronicles(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_canon_map_generation_version UNIQUE(chronicle_id, version_number)
);

CREATE TABLE IF NOT EXISTS canon_map_generation_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_id UUID NOT NULL REFERENCES canon_map_generations(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    color VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT uq_canon_map_generation_category UNIQUE(generation_id, category)
);

CREATE TABLE IF NOT EXISTS canon_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_id UUID NOT NULL REFERENCES canon_map_generations(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    color VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    summary TEXT NOT NULL,
    visual_description TEXT NOT NULL,
    personality_description TEXT,
    visual_basis VARCHAR(20) NOT NULL,
    personality_basis VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_canon_tag_name UNIQUE(generation_id, category, normalized_name)
);
CREATE INDEX IF NOT EXISTS idx_canon_tags_generation ON canon_tags(generation_id, category);

CREATE TABLE IF NOT EXISTS canon_tag_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_id UUID NOT NULL REFERENCES canon_tags(id) ON DELETE CASCADE,
    segment_id UUID NOT NULL REFERENCES game_segments(id) ON DELETE CASCADE,
    CONSTRAINT uq_canon_tag_source UNIQUE(tag_id, segment_id)
);
