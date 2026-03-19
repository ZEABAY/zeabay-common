package com.zeabay.common.constant;

import lombok.experimental.UtilityClass;

/** Global keys used for HTTP Headers, Reactor Context, and MDC logging. */
@UtilityClass
public class ZeabayConstants {
  public static final String TRACE_ID_CTX_KEY = "traceId";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  public static final String IP_CTX_KEY = "ip";
  public static final String USER_CTX_KEY = "user";
  public static final String METHOD_CTX_KEY = "method";
  public static final String PATH_CTX_KEY = "path";
}
