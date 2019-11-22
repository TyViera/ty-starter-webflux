package pe.ty.webflux.error.handler;

import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import reactor.core.publisher.Mono;

public class GenericExceptionHandler implements CoreHandler<Throwable> {

  @Override
  public Mono<CoreException> handle(Throwable ex) {
    return Mono.fromCallable(() -> CoreException.builder()
        .status(CoreExceptionStatus.UNEXPECTED).build());
  }

}
