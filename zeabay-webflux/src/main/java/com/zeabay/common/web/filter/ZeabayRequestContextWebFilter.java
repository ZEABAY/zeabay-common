package com.zeabay.common.web.filter;

import com.zeabay.common.constant.ZeabayConstants;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class ZeabayRequestContextWebFilter implements WebFilter {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String X_USER_ID = "X-User-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    String ip = getClientIp(request);
    String user = getUser(request);
    String method = request.getMethod().name();
    String path = request.getURI().getPath();

    return chain
        .filter(exchange)
        .contextWrite(
            ctx ->
                ctx.put(ZeabayConstants.IP_CTX_KEY, ip)
                    .put(ZeabayConstants.USER_CTX_KEY, user)
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

  private String getUser(ServerHttpRequest request) {
    return Optional.ofNullable(request.getHeaders().getFirst(X_USER_ID))
        .filter(u -> !u.isBlank())
        .orElse("anonymous");
  }
}
