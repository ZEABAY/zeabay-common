package com.zeabay.common.tsid;

import com.github.f4b6a3.tsid.TsidCreator;
import java.util.Locale;

public final class TsidIdGenerator {

  public String newId() {
    return TsidCreator.getTsid().toString().toLowerCase(Locale.ROOT);
  }
}
