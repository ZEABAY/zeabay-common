package com.zeabay.common.tsid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a deterministic TSID node ID (0-1023) from the environment hostname. Relies on trailing
 * ordinals (e.g., K8s 'auth-service-0') or hashes the hostname as a fallback.
 */
public final class TsidNodeIdResolver {

  private static final int MAX_NODE = 1023;
  private static final Pattern TRAILING_ORDINAL = Pattern.compile("[-_](\\d+)$");

  private TsidNodeIdResolver() {}

  /**
   * Resolves a node ID from the hostname environment variable.
   *
   * <p>Tries {@code HOSTNAME} first (Linux/K8s), then {@code COMPUTERNAME} (Windows). Extracts a
   * trailing ordinal if present, otherwise hashes the entire hostname.
   *
   * @return node ID between 0 and 1023, or {@code null} if no hostname is available
   */
  public static Integer resolve() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null || hostname.isBlank()) {
      hostname = System.getenv("COMPUTERNAME");
    }
    if (hostname == null || hostname.isBlank()) {
      return null; // Fallback to random node ID
    }

    Matcher m = TRAILING_ORDINAL.matcher(hostname);
    if (m.find()) {
      try {
        return Integer.parseInt(m.group(1)) % (MAX_NODE + 1);
      } catch (NumberFormatException ignored) {
        // Fall through to hash
      }
    }
    return Math.abs(hostname.hashCode()) % (MAX_NODE + 1);
  }
}
