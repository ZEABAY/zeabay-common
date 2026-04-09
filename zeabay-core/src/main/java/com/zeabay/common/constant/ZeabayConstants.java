package com.zeabay.common.constant;

import lombok.experimental.UtilityClass;

/** Global keys used for HTTP Headers, Reactor Context, and MDC logging. */
@UtilityClass
public class ZeabayConstants {
  /** Key for the trace ID in Reactor context and SLF4J MDC. */
  public static final String TRACE_ID_CTX_KEY = "traceId";

  /** HTTP header name used to propagate the trace ID between services. */
  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  /** Key for the client IP address in Reactor context and SLF4J MDC. */
  public static final String IP_CTX_KEY = "ip";

  /** Key for the authenticated user identity in Reactor context and SLF4J MDC. */
  public static final String USER_CTX_KEY = "user";

  /** Key for the HTTP method (GET, POST, etc.) in Reactor context and SLF4J MDC. */
  public static final String METHOD_CTX_KEY = "method";

  /** Key for the request URI path in Reactor context and SLF4J MDC. */
  public static final String PATH_CTX_KEY = "path";
}
