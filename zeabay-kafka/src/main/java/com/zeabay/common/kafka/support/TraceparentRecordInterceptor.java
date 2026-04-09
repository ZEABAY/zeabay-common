package com.zeabay.common.kafka.support;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import com.zeabay.common.constant.ZeabayConstants;

/**
 * Extracts W3C traceparent from Kafka headers and injects it into MDC. Ensures Jaeger/Zipkin
 * displays HTTP -> Kafka -> Consumer as a single unified trace.
 */
public class TraceparentRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

  private static final Pattern TRACEPARENT =
      Pattern.compile("^[\\da-fA-F]{2}-([\\da-fA-F]{32})-[\\da-fA-F]{16}-[\\da-fA-F]{2}$");

  /**
   * Extracts the trace ID from the {@code traceparent} Kafka header and puts it into MDC before the
   * record is processed.
   */
  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    String traceId = extractTraceId(record);
    if (traceId != null) {
      MDC.put(ZeabayConstants.TRACE_ID_CTX_KEY, traceId);
    }
    return record;
  }

  /** Cleans up the MDC trace ID after the record has been processed. */
  @Override
  public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    MDC.remove(ZeabayConstants.TRACE_ID_CTX_KEY);
  }

  private String extractTraceId(ConsumerRecord<?, ?> record) {
    Iterator<org.apache.kafka.common.header.Header> it =
        record.headers().headers("traceparent").iterator();
    if (!it.hasNext()) return null;

    byte[] value = it.next().value();
    if (value == null || value.length == 0) return null;

    String traceparent = new String(value, StandardCharsets.UTF_8).trim();
    Matcher m = TRACEPARENT.matcher(traceparent);
    return m.matches() ? m.group(1).toLowerCase() : null;
  }
}
