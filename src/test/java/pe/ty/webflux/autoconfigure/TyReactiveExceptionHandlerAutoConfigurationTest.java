package pe.ty.webflux.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import pe.ty.test.webflux.autoconfigure.WebFluxConfigurationTest;
import pe.ty.webflux.rest.RestTest;

@Slf4j
@WebFluxConfigurationTest
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = RestTest.class, properties = {
    "application.error.bad-request.code=ER0001",
    "application.error.not-authorized.code=ER0002",
    "application.error.forbidden.code=ER0003",
    "application.error.not-found.code=ER0004",
    "application.error.conflict.code=ER0005",
    "application.error.precondition-failed.code=ER0006",
    "application.error.timeout=ER0007",
    "application.error.external-error.code=ER0008",
    "application.error.invalid-external-data.code=ER0009",
    "application.error.unexpected.code=ER9999"
})
class TyReactiveExceptionHandlerAutoConfigurationTest {

  private final String URI_HELLO_TEST = RestTest.BASE_TEST_URI + "/" + RestTest.HELLO_TEST_URI;
  private final String URI_PARAMS_TEST =
      RestTest.BASE_TEST_URI + "/" + RestTest.HELLO_PARAMETERS_TEST_URI;

  @Autowired
  private WebTestClient webClient;

  @Captor
  private ArgumentCaptor<ClientRequest> argumentCaptor;

  private ExchangeFunction exchangeFunction;

  @Test
  void whenNoExceptions_thenReturnOk() {
    RestTest.errorResponse = null;
    this.webClient.get().uri(URI_HELLO_TEST)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.greeting").exists()
        .jsonPath("$.greeting").isEqualTo("Hello world!");
  }

  @Test
  void whenNotFoundCoreException_thenReturnNotFound() {
    RestTest.errorResponse = CoreException.builder().status(CoreExceptionStatus.NOT_FOUND).build();
    validateCoreExceptionTest(HttpStatus.NOT_FOUND);
  }

  @Test
  void whenBadRequestCoreException_thenReturnBadRequest() {
    RestTest.errorResponse = CoreException.builder().status(CoreExceptionStatus.BAD_REQUEST)
        .build();
    validateCoreExceptionTest(HttpStatus.BAD_REQUEST);
  }

  @Test
  void whenSendBadParameters_thenReturnBadRequest() {
    RestTest.errorResponse = null;
    MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
    result.add("param.string", "STRING_PARAM");
    result.add("param.integer", "INVALID_INTEGER_VALUE");
    this.webClient.get()
        .uri(uriBuilder -> uriBuilder.path(URI_PARAMS_TEST).queryParams(result).build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.code").isEqualTo("ER0001")
        .jsonPath("$.message").exists()
        .jsonPath("$.component").exists()
        .jsonPath("$.errorType").exists();

//    verifyCalledUrl(URI_PARAMS_TEST + "?param.integer=" + invalidIntegerValue);
  }

  @Test
  void whenNotSendRequiredParameters_thenReturnBadRequest() {
    RestTest.errorResponse = null;
    MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
    this.webClient.get()
        .uri(uriBuilder -> uriBuilder.path(URI_PARAMS_TEST).queryParams(result).build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.code").isEqualTo("ER0001")
        .jsonPath("$.message").exists()
        .jsonPath("$.component").exists()
        .jsonPath("$.errorType").exists();
  }

  @Test
  void whenConsumeNotMappingUri_thenReturnNotFound() {
    RestTest.errorResponse = null;
    this.webClient.get()
        .uri(uriBuilder -> uriBuilder.path("not/mapped/uri").build())
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.code").isEqualTo("ER0004")
        .jsonPath("$.message").exists()
        .jsonPath("$.component").exists()
        .jsonPath("$.errorType").exists();
  }

  @Test
  void whenMethodNotAllowed_thenReturnMethodNotAllowed() {
    RestTest.errorResponse = null;
    this.webClient.post()
        .uri(uriBuilder -> uriBuilder.path(URI_HELLO_TEST).build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        .expectBody()
        .jsonPath("$.code").isEqualTo("ER9999")
        .jsonPath("$.message").exists()
        .jsonPath("$.component").exists()
        .jsonPath("$.errorType").exists();
  }


  private void validateCoreExceptionTest(HttpStatus status) {
    this.webClient.get().uri(URI_HELLO_TEST)
        .exchange()
        .expectStatus().isEqualTo(status)
        .expectBody()
        .jsonPath("$.code").exists()
        .jsonPath("$.message").exists()
        .jsonPath("$.component").exists()
        .jsonPath("$.errorType").exists();
  }

  private void verifyCalledUrl(String relativeUrl) {
    ClientRequest request = this.argumentCaptor.getValue();
    Assert.assertEquals(String.format("%s", relativeUrl), request.url().toString());
    Mockito.verify(this.exchangeFunction).exchange(request);
    Mockito.verifyNoMoreInteractions(this.exchangeFunction);
  }

}