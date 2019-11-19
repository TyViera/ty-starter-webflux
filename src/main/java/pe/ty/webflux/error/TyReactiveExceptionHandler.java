package pe.ty.webflux.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreException.CoreExceptionBuilder;
import pe.ty.core.exception.CoreExceptionStatus;
import pe.ty.core.exception.CoreExceptionType;
import pe.ty.webflux.error.handler.CoreExceptionHandler;
import pe.ty.webflux.error.handler.CoreHandler;
import pe.ty.webflux.error.handler.GenericExceptionHandler;
import reactor.core.publisher.Mono;

@Slf4j
public class TyReactiveExceptionHandler implements WebExceptionHandler {

  private final Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> handlers;
  private final CoreHandler genericExceptionHandler;

  public TyReactiveExceptionHandler() {
    this.genericExceptionHandler = new GenericExceptionHandler();
    this.handlers = Collections.unmodifiableMap(registerHandlers());
  }

  @SuppressWarnings("unchecked")
  private Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> registerHandlers() {
    Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> handlers = new HashMap<>();
    handlers.put(CoreException.class, new CoreExceptionHandler());
    handlers.put(Throwable.class, genericExceptionHandler);
    return handlers;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    log.info("Handling exception: {}", ex);
    return Mono.fromCallable(() -> findExceptionHandler(ex.getClass()))
        .flatMap(handler -> handler.handle(ex))
        .flatMap(this::resolveFields)
        .flatMap(this::completeValues)
        .flatMap(this::createResponse)
        .flatMap(serverResponse -> this.writeResponse(exchange, serverResponse));
  }

  @SuppressWarnings("unchecked")
  private <E extends Throwable> CoreHandler<E> findExceptionHandler(Class<? extends Throwable> e) {
    return (CoreHandler<E>) this.handlers.entrySet().stream()
        .filter(x -> e.equals(x.getKey()))
        .findFirst()
        .map(Entry::getValue)
        .orElse(genericExceptionHandler);
  }

  private Mono<CoreException> resolveFields(CoreException ex) {
    return Mono.fromCallable(() -> {
      CoreExceptionBuilder builder = ex.toBuilder();
      if (ex.getStatus() == null) {
        builder.status(CoreExceptionStatus.UNEXPECTED);
      }

      if (CoreExceptionStatus.UNEXPECTED.equals(ex.getStatus())) {
        builder.code("ER9999").message("Unexpected").component("Generic")
            .errorType(CoreExceptionType.TECHNICAL);
      }
      if (CoreExceptionStatus.NOT_FOUND.equals(ex.getStatus())) {
        builder.code("ER0003").message("Not found").component("Generic")
            .errorType(CoreExceptionType.FUNCTIONAL);
      }

      return builder.build();
    });
  }

  private Mono<CoreException> completeValues(CoreException ex) {
    return Mono.fromCallable(() -> {
      CoreExceptionBuilder builder = ex.toBuilder();
      if (ex.getStatus() == null) {
        builder.status(CoreExceptionStatus.UNEXPECTED);
      }
      return builder.build();
    });
  }

  private Mono<ServerResponse> createResponse(CoreException coreException) {
    int httpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    if (coreException.getStatus() != null) {
      httpCode = coreException.getStatus().getHttpStatus();
    }
    BodyBuilder builder = ServerResponse.status(httpCode);
    if (!CollectionUtils.isEmpty(coreException.getHeaders())) {
      coreException.getHeaders().forEach(builder::header);
    }
    return builder.bodyValue(coreException);
  }

  private Mono<Void> writeResponse(ServerWebExchange exchange, ServerResponse serverResponse) {
    return serverResponse.writeTo(exchange, HandlerStrategiesResponseContext.NEW_INSTANCE);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class HandlerStrategiesResponseContext implements ServerResponse.Context {

    private static HandlerStrategiesResponseContext NEW_INSTANCE = new HandlerStrategiesResponseContext(
        HandlerStrategies.withDefaults());

    private HandlerStrategies strategies;

    public List<HttpMessageWriter<?>> messageWriters() {
      return this.strategies.messageWriters();
    }

    public List<ViewResolver> viewResolvers() {
      return this.strategies.viewResolvers();
    }
  }

}