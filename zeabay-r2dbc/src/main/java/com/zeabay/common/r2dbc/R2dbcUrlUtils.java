package com.zeabay.common.r2dbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility for parsing R2DBC connection URL parameters.
 *
 * <p>Extracts the {@code schema} query parameter from URLs like {@code
 * r2dbc:postgresql://host/db?schema=auth}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class R2dbcUrlUtils {

  /**
   * Extracts the {@code schema} query parameter from an R2DBC URL.
   *
   * @param url the R2DBC connection URL (e.g. {@code r2dbc:postgresql://host/db?schema=auth})
   * @return the schema name, or {@code null} if not present
   */
  public static String parseSchema(String url) {
    if (url == null || url.isBlank()) return null;
    int q = url.indexOf('?');
    if (q < 0) return null;
    for (String param : url.substring(q + 1).split("&")) {
      int eq = param.indexOf('=');
      if (eq > 0 && "schema".equals(param.substring(0, eq).trim())) {
        String value = param.substring(eq + 1).trim();
        return value.isEmpty() ? null : value;
      }
    }
    return null;
  }
}
