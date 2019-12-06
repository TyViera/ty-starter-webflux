package pe.ty.webflux.error.handler;

import org.springframework.web.server.ServerWebExchange;
import pe.ty.core.exception.CoreException;
import reactor.core.publisher.Mono;

public interface CoreHandler<T extends Throwable> {

  Mono<CoreException> handle(ServerWebExchange exchange, T throwable);

}
