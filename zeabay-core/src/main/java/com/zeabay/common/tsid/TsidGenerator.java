package com.zeabay.common.tsid;

import com.github.f4b6a3.tsid.TsidFactory;
import java.util.Locale;

public final class TsidGenerator {

  private final TsidFactory factory;

  public TsidGenerator() {
    Integer nodeId = TsidNodeIdResolver.resolve();
    this.factory =
        nodeId != null
            ? TsidFactory.builder().withNode(nodeId).build()
            : TsidFactory.builder().build();
  }

  /** Returns a new TSID as a lowercase string (e.g. "054kg95e3i5"). */
  public String newId() {
    return factory.create().toString().toLowerCase(Locale.ROOT);
  }

  /** Returns a new TSID as a 64-bit long — use this for BIGINT primary keys. */
  public long newLongId() {
    return factory.create().toLong();
  }
}
