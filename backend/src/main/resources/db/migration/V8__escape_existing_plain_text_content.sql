-- One-time backfill: pre-existing plain-text content in the 3 user-authored story-content
-- tables is HTML-escaped so it renders identically (and safely) once the frontend switches
-- from plain-text rendering to HTML rendering via the new rich-text editor. Order matters:
-- '&' must be escaped first, otherwise the '&' introduced by escaping '<'/'>' would itself
-- get re-escaped into '&amp;'.
--
-- generated_stories and chronicle_synopses are intentionally excluded: they hold
-- AI-generated prose, rendered as plain text today and unaffected by this change.

UPDATE game_segments
SET content = replace(replace(replace(content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;');

UPDATE game_drafts
SET content = replace(replace(replace(content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;');

UPDATE written_story_documents
SET content = replace(replace(replace(content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;');
