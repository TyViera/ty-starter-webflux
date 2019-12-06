package pe.ty.webflux.rest;

import static pe.ty.webflux.rest.RestTest.BASE_TEST_URI;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(BASE_TEST_URI)
public class RestTest {

  public final static String BASE_TEST_URI = "/test";
  public final static String HELLO_TEST_URI = "hello";
  public final static String HELLO_PARAMETERS_TEST_URI = "hello-params";

  public static Throwable errorResponse = null;

  @GetMapping(HELLO_TEST_URI)
  public Mono<?> sayHello() {
    if (errorResponse != null) {
      return Mono.error(errorResponse);
    }
    return Mono.fromCallable(() -> {
      Map<String, String> result = new HashMap<>();
      result.put("greeting", "Hello world!");
      return result;
    });
  }

  @GetMapping(HELLO_PARAMETERS_TEST_URI)
  public Mono<?> respondParameters(@RequestParam("param.string") String param1,
      @RequestParam("param.integer") Integer param2) {
    if (errorResponse != null) {
      return Mono.error(errorResponse);
    }
    return Mono.fromCallable(() -> {
      Map<String, Object> result = new HashMap<>();
      result.put("greeting", "Hello world!");
      result.put("param.string", param1);
      result.put("param.integer", param2);
      return result;
    });
  }

}
