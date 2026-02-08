package com.zeabay.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zeabay.common.api.exception.BusinessException;
import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.autoconfigure.ZeabayCommonAutoConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ZeabayCommonStarterContractTest.TestApp.class)
class ZeabayCommonStarterContractTest {

  @LocalServerPort private int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    this.webTestClient =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Test
  void x_trace_id_is_sanitized_in_response_header() {
    String raw = "abc<>$%#@! ";

    webTestClient
        .get()
        .uri("/test/ok")
        .header(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER, raw)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER, "abc")
        .expectBody(String.class)
        .isEqualTo("ok");
  }

  @Test
  void validation_error_is_wrapped_with_standard_shape_and_trace_id() {
    var result =
        webTestClient
            .post()
            .uri("/test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"\"}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .exists(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER)
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.code")
            .isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.error.path")
            .isEqualTo("/test/validate")
            .jsonPath("$.error.validationErrors[0].field")
            .isEqualTo("name")
            .jsonPath("$.traceId")
            .exists()
            .returnResult();

    String traceHeader =
        result.getResponseHeaders().getFirst(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER);

    assertNotNull(traceHeader);
    assertFalse(traceHeader.isBlank());

    // traceId header ile body traceId eşleşmeli
    webTestClient
        .post()
        .uri("/test/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .header(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER, traceHeader)
        .bodyValue("{\"name\":\"\"}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.traceId")
        .isEqualTo(traceHeader);
  }

  @Test
  void business_error_is_wrapped_with_standard_shape_and_trace_id() {
    var result =
        webTestClient
            .get()
            .uri("/test/business")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .exists(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER)
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(false)
            .jsonPath("$.error.code")
            .isEqualTo("BUSINESS_ERROR")
            .jsonPath("$.error.message")
            .isEqualTo("boom")
            .jsonPath("$.error.path")
            .isEqualTo("/test/business")
            .jsonPath("$.traceId")
            .exists()
            .returnResult();

    String traceHeader =
        result.getResponseHeaders().getFirst(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER);

    assertNotNull(traceHeader);
    assertFalse(traceHeader.isBlank());

    webTestClient
        .get()
        .uri("/test/business")
        .header(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER, traceHeader)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.traceId")
        .isEqualTo(traceHeader);
  }

  @Test
  void openapi_docs_expose_bearer_auth_scheme() {
    webTestClient
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.components.securitySchemes.bearerAuth.type")
        .isEqualTo("http")
        .jsonPath("$.components.securitySchemes.bearerAuth.scheme")
        .isEqualTo("bearer")
        // bearerAuth genelde [] (empty scopes) olduğu için [0] yok -> sadece varlığını doğrula
        .jsonPath("$.security[0].bearerAuth")
        .exists();
  }

  @Test
  void swagger_ui_is_accessible() {
    webTestClient.get().uri("/swagger-ui/index.html").exchange().expectStatus().is2xxSuccessful();
  }

  @SpringBootApplication
  static class TestApp {

    @RestController
    @RequestMapping("/test")
    static class TestController {

      @GetMapping("/ok")
      Mono<String> ok() {
        return Mono.just("ok");
      }

      @PostMapping("/validate")
      Mono<String> validate(@Valid @RequestBody CreateReq req) {
        return Mono.just("ok");
      }

      @GetMapping("/business")
      Mono<String> business() {
        return Mono.error(new BusinessException(ErrorCode.BUSINESS_ERROR, "boom"));
      }
    }

    record CreateReq(@NotBlank String name) {}
  }
}
