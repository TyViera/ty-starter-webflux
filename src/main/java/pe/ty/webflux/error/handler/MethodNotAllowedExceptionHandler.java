package pe.ty.webflux.error.handler;

import java.util.Collections;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import reactor.core.publisher.Mono;

public class MethodNotAllowedExceptionHandler implements CoreHandler<MethodNotAllowedException> {

  @Override
  public Mono<CoreException> handle(ServerWebExchange exchange,
      MethodNotAllowedException throwable) {
    return Mono.fromCallable(() -> {
      ServerHttpRequest request = exchange.getRequest();
      String rawQuery = request.getURI().getRawQuery();
      String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
      HttpMethod httpMethod = request.getMethod();
      String description =
          "Method " + httpMethod + " is not supported on the next URI: \"" + request.getPath()
              + query + "\" only: " + getSupportedMethods(throwable);
      return CoreException.builder()
          .httpStatusCode(throwable.getStatus().value())
          .status(CoreExceptionStatus.UNEXPECTED)
          .message(description)
          .resolved(true)
          .build();
    });
  }

  private String getSupportedMethods(MethodNotAllowedException throwable) {
    return Optional.ofNullable(throwable.getSupportedMethods()).orElse(Collections.emptySet())
        .stream()
        .map(Enum::toString)
        .reduce((x, y) -> x + ", " + y)
        .orElse("-");
  }

}
