package com.cardnova.giftchat.model;

public record TranslationResult(
    String originalText,
    String translatedText,
    String source
) {
}
