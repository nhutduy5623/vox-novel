package com.voxnovel.media_tts_service.dto.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CambAiRequest(
        String text,
        String language,

        @JsonProperty("voice_id")
        int voiceId,

        @JsonProperty("speech_model")
        String speechModel,

        @JsonProperty("enhance_named_entities_pronunciation")
        boolean enhanceNamedEntitiesPronunciation,

        @JsonProperty("output_configuration")
        OutputConfig outputConfiguration,

        @JsonProperty("voice_settings")
        VoiceSettings voiceSettings
) {
    @Builder
    public record OutputConfig(String format) {}

    @Builder
    public record VoiceSettings(
            @JsonProperty("enhance_reference_audio_quality") boolean enhanceReferenceAudioQuality,
            @JsonProperty("maintain_source_accent") boolean maintainSourceAccent,
            @JsonProperty("speaking_rate") double speakingRate
    ) {}
}