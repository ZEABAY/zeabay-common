package com.zeabay.common.web.filter;

import com.zeabay.common.constant.ZeabayConstants;
import java.net.InetSocketAddress;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Populates the reactive context with request metadata (IP, method, path) for structured logging
 * and tracing.
 *
 * <p><b>Security note:</b> User identity is intentionally <em>not</em> read from the {@code
 * X-User-Id} header here — that approach was vulnerable to log spoofing (any caller could forge the
 * header). Instead, the authenticated principal name is injected into the context by {@code
 * ZeabaySecurityContextFilter} in the {@code zeabay-security} module after the JWT has been
 * verified, keeping this filter free of a Spring Security dependency.
 */
public class ZeabayRequestContextWebFilter implements WebFilter {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  @Override
  @NonNull
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String ip = getClientIp(request);
    String method = request.getMethod().name();
    String path = request.getURI().getPath();

    return chain
        .filter(exchange)
        .contextWrite(
            ctx ->
                ctx.put(ZeabayConstants.IP_CTX_KEY, ip)
                    .put(
                        ZeabayConstants.USER_CTX_KEY, "anonymous") // overwritten by security filter
                    .put(ZeabayConstants.METHOD_CTX_KEY, method)
                    .put(ZeabayConstants.PATH_CTX_KEY, path));
  }

  private String getClientIp(ServerHttpRequest request) {
    String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    InetSocketAddress remoteAddress = request.getRemoteAddress();
    return remoteAddress != null && remoteAddress.getAddress() != null
        ? remoteAddress.getAddress().getHostAddress()
        : "unknown-ip";
  }
}
