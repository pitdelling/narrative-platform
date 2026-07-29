ALTER TABLE game_turns DROP CONSTRAINT uq_game_turn_sequence;
ALTER TABLE game_turns ADD CONSTRAINT uq_game_turn_sequence
    UNIQUE (run_id, sequence_number) DEFERRABLE INITIALLY DEFERRED;
