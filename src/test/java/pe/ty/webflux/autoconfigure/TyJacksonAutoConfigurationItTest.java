package pe.ty.webflux.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pe.ty.core.exception.CoreException;
import pe.ty.core.exception.CoreExceptionStatus;
import pe.ty.webflux.TestApplication;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
class TyJacksonAutoConfigurationItTest {

  @Autowired
  private TyJacksonAutoConfiguration jacksonAutoConfiguration;

  private ObjectMapper mapper;

  @BeforeEach
  void init() {
    mapper = jacksonAutoConfiguration.getMapper();
  }

  @Test
  void testValues() throws Exception {
    String code = "XXX";
    String message = "message";
    String component = "component";
    CoreException exception = CoreException.builder()
        .code(code)
        .message(message)
        .component(component)
        .build();
    String json = mapper.writeValueAsString(exception);

    log.info("Result test json is {}", json);

    Assert.assertNotNull(json);
    Assert.assertThat(json, CoreMatchers.containsString("code"));
    Assert.assertThat(json, CoreMatchers.containsString("message"));
    Assert.assertThat(json, CoreMatchers.containsString("component"));
    Assert.assertThat(json, CoreMatchers.containsString("errorType"));

    Assertions.assertEquals(code, JsonPath.read(json, "$.code"));
    Assertions.assertEquals(message, JsonPath.read(json, "$.message"));
    Assertions.assertEquals(component, JsonPath.read(json, "$.component"));
  }

  @Test
  void testNullValues() throws Exception {
    CoreException exception = CoreException.builder().build();
    String json = mapper.writeValueAsString(exception);

    log.info("Result test json is {}", json);

    Assert.assertNotNull(json);

    Assert.assertThat(json, CoreMatchers.containsString("errorType"));

    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("code")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("message")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("component")));
  }

  @Test
  void testIgnoreFieldValues() throws Exception {
    CoreException exception = CoreException.builder()
        .cause(new RuntimeException("RuntimeException"))
        .status(CoreExceptionStatus.BAD_REQUEST)
        .headers(Collections.singletonMap("x-token", "18926387162873612873"))
        .build();
    String json = mapper.writeValueAsString(exception);

    log.info("Result test json is {}", json);

    Assert.assertNotNull(json);
    Assert.assertNotNull(Arrays.toString(exception.getStackTrace()));

    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("headers")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("status")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("cause")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("suppressed")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("localizedMessage")));
    Assert.assertThat(json, CoreMatchers.not(CoreMatchers.containsString("stackTrace")));
  }


}