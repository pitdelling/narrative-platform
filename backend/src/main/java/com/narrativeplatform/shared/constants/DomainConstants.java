package com.narrativeplatform.shared.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainConstants {
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_USERNAME_LENGTH = 40;
    public static final int MAX_GAME_CYCLES = 3;
    public static final int MAX_SEGMENT_LENGTH = 10_000;
    public static final int MAX_WRITTEN_STORY_LENGTH = 100_000;
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    public static final int STORY_VOTE_DAILY_LIMIT = 2;
}
