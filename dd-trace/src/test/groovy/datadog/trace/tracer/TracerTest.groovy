package datadog.trace.tracer

import datadog.trace.api.Config
import datadog.trace.tracer.sampling.AllSampler
import datadog.trace.tracer.writer.LoggingWriter
import spock.lang.Shared
import spock.lang.Specification

class TracerTest extends Specification {

  @Shared
  def config = Config.get()
  @Shared
  def tracer = new Tracer(config)

  def "create tracer config constructor"() {
    when:
    def tracer = new Tracer(config)

    then:
    tracer.writer instanceof LoggingWriter
    tracer.sampler instanceof AllSampler
    tracer.config == config

    // TODO: add more tests for different config options and interceptors
  }

  def "create trace"() {
    when:
    def span = tracer.buildTrace(null)

    then:
    span.isErrored() == false;
    span.isFinished() == false;
    span.getService() == config.getServiceName()
    span.getStartTimestamp() != null
  }
}
