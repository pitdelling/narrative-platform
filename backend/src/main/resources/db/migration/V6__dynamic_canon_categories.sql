-- Replaces the closed 5-category enum model with narrator-managed, freely named
-- categories (name, description, hex color). See PartyAiTagSettingsService (pre-migration)
-- for the enum defaults/colors being backfilled below.

-- 1. New live category config table.
CREATE TABLE IF NOT EXISTS canon_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(7) NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_canon_categories_party ON canon_categories(party_id);

-- 2. Backfill: only categories that were enabled=true today become dynamic categories.
INSERT INTO canon_categories (id, party_id, name, description, color, display_order, created_at, updated_at)
SELECT gen_random_uuid(), party_id,
    CASE category
        WHEN 'PERSON' THEN 'Pessoas'
        WHEN 'PLACE' THEN 'Lugares'
        WHEN 'ITEM' THEN 'Itens'
        WHEN 'SPELL' THEN 'Magias'
        WHEN 'CREATURE' THEN 'Criaturas'
    END,
    CASE category
        WHEN 'PERSON' THEN 'Pessoas, personagens e outras entidades conscientes.'
        WHEN 'PLACE' THEN 'Lugares, regiões e construções relevantes para a história.'
        WHEN 'ITEM' THEN 'Objetos e artefatos relevantes para a história.'
        WHEN 'SPELL' THEN 'Magias, rituais e poderes usados na história.'
        WHEN 'CREATURE' THEN 'Criaturas e seres não pensantes da história.'
    END,
    CASE color
        WHEN 'GOLD' THEN '#c29042'
        WHEN 'COPPER' THEN '#a76d3d'
        WHEN 'VIOLET' THEN '#7665a7'
        WHEN 'AZURE' THEN '#3d6fa5'
        WHEN 'GREEN' THEN '#627a66'
        WHEN 'ROSE' THEN '#9a4f47'
        WHEN 'SLATE' THEN '#5f6a78'
    END,
    display_order, NOW(), NOW()
FROM party_ai_tag_settings
WHERE enabled = true;

-- 3. The old live-config table is fully superseded; drop it (its unique constraint goes with it).
DROP TABLE party_ai_tag_settings;

-- 4a. Widen the generation-time snapshot table. Old enum columns are kept a little longer
--     below, since canon_tags' backfill (step 5) still needs to join on them.
ALTER TABLE canon_map_generation_categories ADD COLUMN name VARCHAR(160);
ALTER TABLE canon_map_generation_categories ADD COLUMN description VARCHAR(500);
ALTER TABLE canon_map_generation_categories ADD COLUMN color_hex VARCHAR(7);

UPDATE canon_map_generation_categories SET
    name = CASE category
        WHEN 'PERSON' THEN 'Pessoas'
        WHEN 'PLACE' THEN 'Lugares'
        WHEN 'ITEM' THEN 'Itens'
        WHEN 'SPELL' THEN 'Magias'
        WHEN 'CREATURE' THEN 'Criaturas'
    END,
    description = CASE category
        WHEN 'PERSON' THEN 'Pessoas, personagens e outras entidades conscientes.'
        WHEN 'PLACE' THEN 'Lugares, regiões e construções relevantes para a história.'
        WHEN 'ITEM' THEN 'Objetos e artefatos relevantes para a história.'
        WHEN 'SPELL' THEN 'Magias, rituais e poderes usados na história.'
        WHEN 'CREATURE' THEN 'Criaturas e seres não pensantes da história.'
    END,
    color_hex = CASE color
        WHEN 'GOLD' THEN '#c29042'
        WHEN 'COPPER' THEN '#a76d3d'
        WHEN 'VIOLET' THEN '#7665a7'
        WHEN 'AZURE' THEN '#3d6fa5'
        WHEN 'GREEN' THEN '#627a66'
        WHEN 'ROSE' THEN '#9a4f47'
        WHEN 'SLATE' THEN '#5f6a78'
    END;

-- 5. canon_tags: point each tag at its generation's category snapshot row instead of
--    duplicating category/color. Backfill while canon_map_generation_categories.category
--    (the old enum column) still exists, since it is the join key here.
ALTER TABLE canon_tags ADD COLUMN category_snapshot_id UUID;
UPDATE canon_tags t SET category_snapshot_id = c.id
FROM canon_map_generation_categories c
WHERE c.generation_id = t.generation_id AND c.category = t.category;

-- 4b. Now that canon_tags no longer needs it, drop the old enum columns from the snapshot table.
ALTER TABLE canon_map_generation_categories ALTER COLUMN name SET NOT NULL;
ALTER TABLE canon_map_generation_categories ALTER COLUMN color_hex SET NOT NULL;
ALTER TABLE canon_map_generation_categories DROP CONSTRAINT uq_canon_map_generation_category;
ALTER TABLE canon_map_generation_categories DROP COLUMN category;
ALTER TABLE canon_map_generation_categories DROP COLUMN color;
ALTER TABLE canon_map_generation_categories DROP COLUMN enabled;
ALTER TABLE canon_map_generation_categories RENAME COLUMN color_hex TO color;

-- 5b. Finish canon_tags: enforce the FK, replace the old enum-based unique constraint.
ALTER TABLE canon_tags ALTER COLUMN category_snapshot_id SET NOT NULL;
ALTER TABLE canon_tags ADD CONSTRAINT fk_canon_tags_category_snapshot
    FOREIGN KEY (category_snapshot_id) REFERENCES canon_map_generation_categories(id);
ALTER TABLE canon_tags DROP CONSTRAINT uq_canon_tag_name;
ALTER TABLE canon_tags DROP COLUMN category;
ALTER TABLE canon_tags DROP COLUMN color;
ALTER TABLE canon_tags ADD CONSTRAINT uq_canon_tag_name UNIQUE(generation_id, category_snapshot_id, normalized_name);
CREATE INDEX IF NOT EXISTS idx_canon_tags_category_snapshot ON canon_tags(category_snapshot_id);
