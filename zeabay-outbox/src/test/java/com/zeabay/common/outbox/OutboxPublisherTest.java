package com.zeabay.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  @Mock OutboxEventRepository repository;
  @Mock KafkaTemplate<String, Object> kafkaTemplate;
  @Mock OutboxProperties properties;

  @InjectMocks OutboxPublisher publisher;

  private OutboxEvent pendingEvent() {
    return OutboxEvent.builder()
        .id(1L)
        .topic("test.topic")
        .aggregateId(42L)
        .payload("{\"key\":\"value\"}")
        .eventType("TestEvent")
        .traceId("trace-123")
        .status(OutboxEvent.Status.PENDING)
        .retryCount(0)
        .build();
  }

  @BeforeEach
  void setUp() {
    when(properties.getBatchSize()).thenReturn(10);
  }

  // ────────────────────────────────────────────────────────────────
  //  publishPendingEvents — happy path
  // ────────────────────────────────────────────────────────────────

  @Nested
  class HappyPath {

    @Test
    void publishesEventAndMarksPublished() throws InterruptedException {
      OutboxEvent event = pendingEvent();
      CountDownLatch latch = new CountDownLatch(1);

      when(repository.findPendingEvents(anyInt())).thenReturn(Flux.just(event));
      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                latch.countDown();
                return Mono.just(inv.getArgument(0));
              });

      publisher.publishPendingEvents();
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

      ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
      assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void sendsToCorrectTopicWithAggregateIdAsKey() throws InterruptedException {
      OutboxEvent event = pendingEvent();
      CountDownLatch latch = new CountDownLatch(1);

      when(repository.findPendingEvents(anyInt())).thenReturn(Flux.just(event));
      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                latch.countDown();
                return Mono.just(inv.getArgument(0));
              });

      publisher.publishPendingEvents();
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

      verify(kafkaTemplate).send("test.topic", "42", "{\"key\":\"value\"}");
    }

    @Test
    void skipsWhenNoPendingEvents() throws InterruptedException {
      CountDownLatch latch = new CountDownLatch(1);

      // Flux.empty() completes immediately — doFinally resets flag
      when(repository.findPendingEvents(anyInt()))
          .thenAnswer(
              inv -> {
                latch.countDown();
                return Flux.empty();
              });

      publisher.publishPendingEvents();
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

      verify(repository, never()).save(any());
    }
  }

  // ────────────────────────────────────────────────────────────────
  //  publishPendingEvents — Kafka failure / retry logic
  // ────────────────────────────────────────────────────────────────

  @Nested
  class RetryBehavior {

    @Test
    void kafkaFailure_incrementsRetryCount() throws InterruptedException {
      OutboxEvent event = pendingEvent();
      CountDownLatch latch = new CountDownLatch(1);
      when(properties.getMaxRetries()).thenReturn(3);

      when(repository.findPendingEvents(anyInt())).thenReturn(Flux.just(event));
      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenThrow(new RuntimeException("Kafka unavailable"));
      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                latch.countDown();
                return Mono.just(inv.getArgument(0));
              });

      publisher.publishPendingEvents();
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

      ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
      // Not yet FAILED — only 1 retry, max is 3
      assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
    }

    @Test
    void kafkaFailure_atMaxRetries_marksEventAsFailed() throws InterruptedException {
      OutboxEvent event =
          OutboxEvent.builder()
              .id(1L)
              .topic("test.topic")
              .aggregateId(42L)
              .payload("{\"key\":\"value\"}")
              .eventType("TestEvent")
              .traceId("trace-123")
              .status(OutboxEvent.Status.PENDING)
              .retryCount(2)
              .build(); // 1 more → reaches maxRetries=3
      CountDownLatch latch = new CountDownLatch(1);
      when(properties.getMaxRetries()).thenReturn(3);

      when(repository.findPendingEvents(anyInt())).thenReturn(Flux.just(event));
      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenThrow(new RuntimeException("Kafka down"));
      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                latch.countDown();
                return Mono.just(inv.getArgument(0));
              });

      publisher.publishPendingEvents();
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

      ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
      assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
    }
  }

  // ────────────────────────────────────────────────────────────────
  //  AtomicBoolean guard — concurrent invocation protection
  // ────────────────────────────────────────────────────────────────

  @Nested
  class ConcurrencyGuard {

    @Test
    void secondInvocationIsSkippedWhileFirstIsRunning() throws InterruptedException {
      CountDownLatch firstStarted = new CountDownLatch(1);
      CountDownLatch firstCanProceed = new CountDownLatch(1);
      CountDownLatch firstDone = new CountDownLatch(1);

      OutboxEvent event = pendingEvent();

      when(repository.findPendingEvents(anyInt()))
          .thenAnswer(
              inv -> {
                firstStarted.countDown(); // signal first call started
                return Flux.just(event);
              });

      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenAnswer(
              inv -> {
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                new Thread(
                        () -> {
                          try {
                            firstCanProceed.await(2, TimeUnit.SECONDS);
                            future.complete(mock(SendResult.class));
                          } catch (InterruptedException e) {
                            future.completeExceptionally(e);
                          }
                        })
                    .start();
                return future;
              });

      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                firstDone.countDown();
                return Mono.just(inv.getArgument(0));
              });

      // First invocation — starts and blocks inside kafkaTemplate.send()
      new Thread(publisher::publishPendingEvents).start();
      assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

      // Second invocation — should be skipped (AtomicBoolean is true)
      publisher.publishPendingEvents();

      // Verify second call did NOT call repository.findPendingEvents a second time
      verify(repository, times(1)).findPendingEvents(anyInt());

      // Unblock and wait for first to finish
      firstCanProceed.countDown();
      assertThat(firstDone.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void guardIsReleasedAfterCycleCompletes() throws InterruptedException {
      CountDownLatch firstDone = new CountDownLatch(1);
      CountDownLatch secondDone = new CountDownLatch(1);
      OutboxEvent event = pendingEvent();

      when(repository.findPendingEvents(anyInt())).thenReturn(Flux.just(event));
      when(kafkaTemplate.send(anyString(), anyString(), any()))
          .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
      when(repository.save(any()))
          .thenAnswer(
              inv -> {
                if (firstDone.getCount() > 0) {
                  firstDone.countDown();
                } else {
                  secondDone.countDown();
                }
                return Mono.just(inv.getArgument(0));
              });

      // First cycle
      publisher.publishPendingEvents();
      assertThat(firstDone.await(2, TimeUnit.SECONDS)).isTrue();

      // Second cycle should execute (flag released)
      publisher.publishPendingEvents();
      assertThat(secondDone.await(2, TimeUnit.SECONDS)).isTrue();

      verify(repository, times(2)).findPendingEvents(anyInt());
    }
  }
}
