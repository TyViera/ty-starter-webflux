package pe.ty.webflux.error.handler;

import pe.ty.core.exception.CoreException;
import reactor.core.publisher.Mono;

public class CoreExceptionHandler implements CoreHandler<CoreException> {

  @Override
  public Mono<CoreException> handle(CoreException throwable) {
    return Mono.fromCallable(() -> throwable);
  }
}
