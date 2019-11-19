package pe.ty.webflux.error.handler;

import pe.ty.core.exception.CoreException;
import reactor.core.publisher.Mono;

public interface CoreHandler<T extends Throwable> {

  Mono<CoreException> handle(T throwable);

}
