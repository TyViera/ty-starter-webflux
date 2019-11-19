package pe.ty.webflux.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebExceptionHandler;
import pe.ty.webflux.error.TyReactiveExceptionHandler;

@Slf4j
@Configuration
public class TyReactiveExceptionHandlerAutoConfiguration {

  private static final int HANDLER_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

  @Bean
  @Order(HANDLER_ORDER)
  public WebExceptionHandler exceptionHandler() {
    log.info("Configuring Custom Exception Handler...");
    return new TyReactiveExceptionHandler();
  }

}
