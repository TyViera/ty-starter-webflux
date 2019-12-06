package pe.ty.webflux.error.handler;

import org.springframework.web.server.ServerWebExchange;
import pe.ty.core.exception.CoreException;
import reactor.core.publisher.Mono;

public class CoreExceptionHandler implements CoreHandler<CoreException> {

  @Override
  public Mono<CoreException> handle(ServerWebExchange exchange, CoreException throwable) {
    return Mono.fromCallable(() -> throwable);
  }
}
