package com.zeabay.common.tsid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves TSID node ID from environment.
 *
 * <p>Order of resolution:
 *
 * <ol>
 *   <li>Trailing ordinal from HOSTNAME (e.g. {@code auth-service-0} → 0, {@code pod-42} → 42)
 *   <li>Deterministic hash of hostname (0–1023)
 *   <li>Random (caller uses no-arg TsidGenerator) when HOSTNAME is empty
 * </ol>
 *
 * <p>K8s StatefulSet and Docker Compose typically set HOSTNAME with an ordinal suffix.
 */
public final class TsidNodeIdResolver {

  private static final int MAX_NODE = 1023;
  private static final Pattern TRAILING_ORDINAL = Pattern.compile("[-_](\\d+)$");

  private TsidNodeIdResolver() {}

  /**
   * Resolves node ID from HOSTNAME. Returns null if no deterministic value can be derived (caller
   * should use random).
   */
  public static Integer resolve() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null || hostname.isBlank()) {
      hostname = System.getenv("COMPUTERNAME");
    }
    if (hostname == null || hostname.isBlank()) {
      return null;
    }
    // Try trailing ordinal (K8s: auth-service-0, Docker: container_1)
    Matcher m = TRAILING_ORDINAL.matcher(hostname);
    if (m.find()) {
      try {
        int ordinal = Integer.parseInt(m.group(1));
        return ordinal % (MAX_NODE + 1);
      } catch (NumberFormatException ignored) {
        // fall through to hash
      }
    }
    // Deterministic hash from hostname
    return Math.abs(hostname.hashCode()) % (MAX_NODE + 1);
  }
}
