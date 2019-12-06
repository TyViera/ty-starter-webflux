package pe.ty.webflux.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreException.CoreExceptionBuilder;
import pe.ty.core.exception.CoreExceptionStatus;
import pe.ty.core.exception.CoreExceptionType;
import pe.ty.webflux.error.handler.BadRequestExceptionHandler;
import pe.ty.webflux.error.handler.CoreExceptionHandler;
import pe.ty.webflux.error.handler.CoreHandler;
import pe.ty.webflux.error.handler.GenericExceptionHandler;
import pe.ty.webflux.error.handler.MethodNotAllowedExceptionHandler;
import pe.ty.webflux.error.handler.ResponseStatusExceptionHandler;
import reactor.core.publisher.Mono;

@Slf4j
public class TyReactiveExceptionHandler implements WebExceptionHandler {

  private final static String DEFAULT_CORE_ERROR_CODE = "ER9999";
  private final static String DEFAULT_CORE_ERROR_MESSAGE = "No description configured.";
  private final static String DEFAULT_CORE_ERROR_COMPONENT = "default-component";
  private final static String PROPERTY_APPLICATION_NAME = "spring.application.name";
  private final static String BASE_PROPERTY_ERROR = "application.error";
  private final static String CODE_PROPERTY_ERROR = ".code";
  private final static String MESSAGE_PROPERTY_ERROR = ".message";

  private final Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> handlers;
  private final CoreHandler genericExceptionHandler;
  private final Environment environment;
  private final BuildProperties buildProperties;

  public TyReactiveExceptionHandler(Environment environment, BuildProperties buildProperties) {
    this.genericExceptionHandler = new GenericExceptionHandler();
    this.handlers = Collections.unmodifiableMap(registerHandlers());
    this.environment = environment;
    this.buildProperties = buildProperties;
  }

  @SuppressWarnings("unchecked")
  private Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> registerHandlers() {
    Map<Class<? extends Throwable>, CoreHandler<? extends Throwable>> handlers = new HashMap<>();
    handlers.put(CoreException.class, new CoreExceptionHandler());
    handlers.put(ServerWebInputException.class, new BadRequestExceptionHandler());
    handlers.put(MethodNotAllowedException.class, new MethodNotAllowedExceptionHandler());
    handlers.put(ResponseStatusException.class, new ResponseStatusExceptionHandler());
    handlers.put(Throwable.class, genericExceptionHandler);
    return handlers;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    log.info("Handling exception: {}", ex);
    return Mono.fromCallable(() -> findExceptionHandler(ex.getClass()))
        .flatMap(handler -> handler.handle(exchange, ex))
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
      String base = BASE_PROPERTY_ERROR + ex.getStatus().getPropertyName();
      if (StringUtils.isEmpty(ex.getCode()) || !ex.isResolved()) {
        builder.code(environment.getProperty(base + CODE_PROPERTY_ERROR));
      }
      if (StringUtils.isEmpty(ex.getMessage()) || !ex.isResolved()) {
        builder.message(environment.getProperty(base + MESSAGE_PROPERTY_ERROR));
      }
      return builder.build().markAsResolved();
    });
  }

  private Mono<CoreException> completeValues(CoreException ex) {
    return Mono.fromCallable(() -> {
      CoreExceptionBuilder builder = ex.toBuilder();
      if (ex.getStatus() == null) {
        builder.status(CoreExceptionStatus.UNEXPECTED);
      }
      if (StringUtils.isEmpty(ex.getComponent())) {
        builder.component(getComponentName());
      }
      if (StringUtils.isEmpty(ex.getCode())) {
        builder.code(DEFAULT_CORE_ERROR_CODE);
      }
      if (StringUtils.isEmpty(ex.getMessage())) {
        builder.message(DEFAULT_CORE_ERROR_MESSAGE);
      }
      if (ex.getErrorType() == null) {
        builder.errorType(CoreExceptionType.TECHNICAL);
      }
      return builder.build();
    });
  }

  private String getComponentName() {
    String component = environment.getProperty(PROPERTY_APPLICATION_NAME);
    if (StringUtils.isEmpty(component)) {
      component = buildProperties.getName();
      if (StringUtils.isEmpty(component)) {
        component = DEFAULT_CORE_ERROR_COMPONENT;
      }
    }
    return component;
  }

  private Mono<ServerResponse> createResponse(CoreException coreException) {
    Integer httpCode = coreException.getHttpStatusCode();
    if ((httpCode == null) && (coreException.getStatus() != null)) {
      httpCode = coreException.getStatus().getHttpStatus();
    }
    if (httpCode == null) {
      httpCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
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