package com.zeabay.common.inbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Configuration properties for the Inbox pattern (prefix: {@code zeabay.inbox}).
 *
 * <p>Reserved for future consumer-side tunables such as event TTL or retention policy. Mirrors
 * {@code OutboxProperties} on the producer side.
 */
@Data
@ConfigurationProperties(prefix = "zeabay.inbox")
public class InboxProperties {}
