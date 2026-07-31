-- Widens party_invitation_links from "one link per party" to "one link per (party, target_role)"
-- so a party can have an independent invite link that registers new members directly as
-- SPECTATOR, alongside the existing reusable PLAYER link. No token is generated here: the
-- spectator link for each pre-existing party is lazily created by the application itself
-- (same find-or-create pattern already used for the player link), the first time a narrator
-- opens the invite panel, so this migration only needs to restructure the schema.

ALTER TABLE party_invitation_links ADD COLUMN id UUID DEFAULT gen_random_uuid();
UPDATE party_invitation_links SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE party_invitation_links ALTER COLUMN id SET NOT NULL;

ALTER TABLE party_invitation_links ADD COLUMN target_role VARCHAR(30) NOT NULL DEFAULT 'PLAYER';
ALTER TABLE party_invitation_links ALTER COLUMN target_role DROP DEFAULT;

ALTER TABLE party_invitation_links DROP CONSTRAINT party_invitation_links_pkey;
ALTER TABLE party_invitation_links ADD PRIMARY KEY (id);
ALTER TABLE party_invitation_links ADD CONSTRAINT uq_party_invitation_links_party_role UNIQUE (party_id, target_role);
CREATE INDEX IF NOT EXISTS idx_party_invitation_links_party ON party_invitation_links(party_id);
