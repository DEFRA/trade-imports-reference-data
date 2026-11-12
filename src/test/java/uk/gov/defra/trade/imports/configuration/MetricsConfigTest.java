package uk.gov.defra.trade.imports.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.loggedBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import uk.gov.defra.trade.imports.metrics.EmfMeterRegistry;

class MetricsConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(MetricsConfig.class)
          .withBean(
              ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
              ConversionService.class,
              ApplicationConversionService::getSharedInstance)
          .withBean(
              SimpleMeterRegistry.class, SimpleMeterRegistry::new, bean -> bean.setPrimary(true))
          .withPropertyValues("aws.emf.namespace=test-namespace", "aws.emf.step=1m");

  private MetricsConfig metricsConfig;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    metricsConfig = new MetricsConfig();
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  void emfMeterRegistry_shouldBeCreated_whenEmfIsEnabled() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(EmfMeterRegistry.class));
  }

  @Test
  void emfMeterRegistry_shouldBeLeftOut_whenEmfIsDisabled() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(EmfMeterRegistry.class);
            });
  }

  @Test
  void emfPublishingOffLog_shouldLogThatMetricsAreNotPublished_whenEmfIsNotEnabled() {
    contextRunner.run(
        context -> {
          ApplicationRunner offLog = context.getBean(ApplicationRunner.class);

          List<ILoggingEvent> events =
              loggedBy(MetricsConfig.class, () -> runWithNoArguments(offLog));

          assertThat(events)
              .extracting(ILoggingEvent::getFormattedMessage)
              .containsExactly("Not publishing metrics to CloudWatch: aws.emf.enabled is not true");
        });
  }

  @Test
  void emfMeterRegistry_shouldStopStartUp_whenTheNamespaceIsBlank() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=true", "aws.emf.namespace=")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(InvalidNamespaceException.class));
  }

  @Test
  void emfMeterRegistry_shouldNameTheProperty_whenTheNamespaceIsInvalid() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=true", "aws.emf.namespace=bad namespace!")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .hasStackTraceContaining(
                        "aws.emf.namespace is not a valid CloudWatch namespace"));
  }

  @Test
  void emfMeterRegistry_shouldBeCreated_whenTheEnvironmentIsInLowerCase() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=true", "aws.emf.environment=local")
        .run(context -> assertThat(context).hasSingleBean(EmfMeterRegistry.class));
  }

  @Test
  void emfPublishingOffLog_shouldBeCreated_whenEmfEnabledIsNotABoolean() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=yes")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(EmfMeterRegistry.class);
              assertThat(context).hasSingleBean(ApplicationRunner.class);
            });
  }

  @Test
  void emfPublishingOffLog_shouldBeLeftOut_whenEmfIsEnabled() {
    contextRunner
        .withPropertyValues("aws.emf.enabled=true")
        .run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
  }

  @Test
  void timedAspect_shouldRecordATimedMetric_whenIntercepted() {
    Target target = advisedBy(metricsConfig.timedAspect(meterRegistry));

    target.timedOperation();

    assertThat(meterRegistry.get("test.timed.operation").timer().count()).isEqualTo(1);
  }

  @Test
  void countedAspect_shouldRecordACountedMetric_whenIntercepted() {
    Target target = advisedBy(metricsConfig.countedAspect(meterRegistry));

    target.countedOperation();

    assertThat(meterRegistry.get("test.counted.operation").counter().count()).isEqualTo(1);
  }

  private static Target advisedBy(Object aspect) {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new Target());
    proxyFactory.setProxyTargetClass(true);
    proxyFactory.addAspect(aspect);
    return proxyFactory.getProxy();
  }

  private static void runWithNoArguments(ApplicationRunner runner) {
    try {
      runner.run(new DefaultApplicationArguments());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static class Target {
    @Timed("test.timed.operation")
    String timedOperation() {
      return "done";
    }

    @Counted("test.counted.operation")
    String countedOperation() {
      return "done";
    }
  }
}
