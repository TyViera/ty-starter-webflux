package pe.ty.webflux.error.handler;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import reactor.core.publisher.Mono;

public class ResponseStatusExceptionHandler implements CoreHandler<ResponseStatusException> {

  @Override
  public Mono<CoreException> handle(ServerWebExchange exchange, ResponseStatusException throwable) {
    return Mono.fromCallable(() -> {
      ServerHttpRequest request = exchange.getRequest();
      String rawQuery = request.getURI().getRawQuery();
      String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
      HttpMethod httpMethod = request.getMethod();
      String description =
          "Not found -> HTTP " + httpMethod + " \"" + request.getPath() + query + "\"";
      return CoreException.builder()
          .status(CoreExceptionStatus.NOT_FOUND)
          .message(description)
          .resolved(true)
          .build();
    });
  }
}
