package pe.ty.webflux.error.handler;

import org.springframework.web.server.ServerWebExchange;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import reactor.core.publisher.Mono;

public class GenericExceptionHandler implements CoreHandler<Throwable> {

  @Override
  public Mono<CoreException> handle(ServerWebExchange exchange, Throwable ex) {
    return Mono.fromCallable(() -> CoreException.builder()
        .status(CoreExceptionStatus.UNEXPECTED).build());
  }

}
