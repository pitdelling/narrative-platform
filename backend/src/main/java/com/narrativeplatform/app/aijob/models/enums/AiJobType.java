package com.narrativeplatform.app.aijob.models.enums;

public enum AiJobType {
    STORY_ADAPTATION_GENERATION, CANON_MAP_GENERATION, STORY_SYNOPSIS_GENERATION;

    public String getModelByType() {
        return switch (this) {
            case STORY_ADAPTATION_GENERATION -> "gpt-5.6-sol";
            case CANON_MAP_GENERATION -> "gpt-5.6-terra";
            case STORY_SYNOPSIS_GENERATION -> "gpt-5.6-luna";
        };
    }
}
