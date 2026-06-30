package com.cardnova.giftchat.model;

public record RealtimeFanoutEvent(
    String nodeId,
    String channelKey,
    String senderUserId,
    Object payload,
    boolean authorAware,
    String selfAuthor,
    String otherAuthor
) {
}
