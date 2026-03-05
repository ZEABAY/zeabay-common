package com.zeabay.common.tsid;

import com.github.f4b6a3.tsid.TsidCreator;
import java.util.Locale;

public final class TsidIdGenerator {

  /** Returns a new TSID as a lowercase string (e.g. "054kg95e3i5"). */
  public String newId() {
    return TsidCreator.getTsid().toString().toLowerCase(Locale.ENGLISH);
  }

  /** Returns a new TSID as a 64-bit long — use this for BIGINT primary keys. */
  public long newLongId() {
    return TsidCreator.getTsid().toLong();
  }
}
