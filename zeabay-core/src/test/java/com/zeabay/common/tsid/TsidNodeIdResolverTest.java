package com.zeabay.common.tsid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TsidNodeIdResolverTest {

  @Test
  void resolve_returnsValueInRangeOrNull() {
    Integer result = TsidNodeIdResolver.resolve();
    // When HOSTNAME is set: 0-1023. When empty: null
    if (result != null) {
      assertThat(result).isBetween(0, 1023);
    }
  }
}
