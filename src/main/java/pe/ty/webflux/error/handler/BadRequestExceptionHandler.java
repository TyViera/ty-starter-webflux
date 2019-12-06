package pe.ty.webflux.error.handler;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import reactor.core.publisher.Mono;

public class BadRequestExceptionHandler implements CoreHandler<ServerWebInputException> {

  @Override
  public Mono<CoreException> handle(ServerWebExchange exchange, ServerWebInputException throwable) {
    return Mono.fromCallable(() -> CoreException.builder()
        .status(CoreExceptionStatus.BAD_REQUEST)
        .message(throwable.getReason())
        .resolved(true)
        .build());
  }
}
