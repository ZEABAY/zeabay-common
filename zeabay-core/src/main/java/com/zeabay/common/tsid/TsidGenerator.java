package com.zeabay.common.tsid;

import java.util.Locale;

import com.github.f4b6a3.tsid.TsidFactory;

/**
 * Global ID generator producing time-sorted, collision-resistant unique identifiers using the TSID
 * (Time-Sorted ID) algorithm.
 *
 * <p>Uses a node ID derived from {@link TsidNodeIdResolver} when available, which prevents
 * collisions across multiple service instances running concurrently.
 */
public final class TsidGenerator {

  private final TsidFactory factory;

  public TsidGenerator() {
    Integer nodeId = TsidNodeIdResolver.resolve();
    this.factory =
        nodeId != null
            ? TsidFactory.builder().withNode(nodeId).build()
            : TsidFactory.builder().build();
  }

  /**
   * Generates a URL-safe, lowercase TSID string (e.g., {@code "054kg95e3i5"}).
   *
   * <p>Use for string/JSON identifiers where human-readability and lexicographic ordering matter.
   *
   * @return a new unique TSID string
   */
  public String newId() {
    return factory.create().toString().toLowerCase(Locale.ROOT);
  }

  /**
   * Generates a 64-bit TSID as a {@code long}.
   *
   * <p>Use as a {@code BIGINT} primary key in R2DBC entities — time-ordered and safe for
   * distributed generation.
   *
   * @return a new unique TSID as a {@code long}
   */
  public long newLongId() {
    return factory.create().toLong();
  }
}
