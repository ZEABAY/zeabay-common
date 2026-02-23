package com.zeabay.common.tsid;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TsidIdGeneratorTest {

  private static final Pattern CANONICAL = Pattern.compile("^[0-9a-hjkmnp-tv-z]{13}$");

  @Test
  void format_should_be_canonical_lowercase_13() {
    TsidIdGenerator gen = new TsidIdGenerator();

    for (int i = 0; i < 10_000; i++) {
      String id = gen.newId();
      assertNotNull(id);
      assertTrue(CANONICAL.matcher(id).matches(), "invalid id: " + id);
    }
  }

  @Test
  void ids_should_be_unique_in_reasonable_sample() {
    TsidIdGenerator gen = new TsidIdGenerator();

    int n = 50_000;
    Set<String> seen = new HashSet<>(n * 2);

    for (int i = 0; i < n; i++) {
      String id = gen.newId();
      boolean added = seen.add(id);
      assertTrue(added, "duplicate id found: " + id);
    }
  }
}
