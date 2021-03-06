/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.example;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/**
 * This example demonstrates various {@link TraceConfig} options and how to configure them into an
 * SDK.
 */
class ConfigureTraceExample {

  public static void main(String[] args) {
    // TraceConfig handles the tracing configuration

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .build())
            .build();

    printTraceConfig(openTelemetrySdk);
    Tracer tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");

    // OpenTelemetry has a maximum of 32 Attributes by default for Spans, Links, and Events.
    Span multiAttrSpan = tracer.spanBuilder("Example Span Attributes").startSpan();
    multiAttrSpan.setAttribute("Attribute 1", "first attribute value");
    multiAttrSpan.setAttribute("Attribute 2", "second attribute value");
    multiAttrSpan.end();

    // The configuration can be changed in the trace provider.
    // For example, we can change the maximum number of Attributes per span to 1.
    TraceConfig newConf = TraceConfig.builder().setMaxNumberOfAttributes(1).build();

    openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .setTraceConfig(newConf)
                    .build())
            .build();

    printTraceConfig(openTelemetrySdk);

    // If more attributes than allowed by the configuration are set, they are dropped.
    Span singleAttrSpan = tracer.spanBuilder("Example Span Attributes").startSpan();
    singleAttrSpan.setAttribute("Attribute 1", "first attribute value");
    singleAttrSpan.setAttribute("Attribute 2", "second attribute value");
    singleAttrSpan.end();

    // OpenTelemetry offers three different default samplers:
    //  - alwaysOn: it samples all traces
    //  - alwaysOff: it rejects all traces
    //  - probability: it samples traces based on the probability passed in input
    TraceConfig alwaysOff = TraceConfig.builder().setSampler(Sampler.alwaysOff()).build();
    TraceConfig alwaysOn = TraceConfig.builder().setSampler(Sampler.alwaysOn()).build();
    TraceConfig probability =
        TraceConfig.builder().setSampler(Sampler.traceIdRatioBased(0.5)).build();

    // We build an SDK with the alwaysOff sampler.
    openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .setTraceConfig(alwaysOff)
                    .build())
            .build();

    printTraceConfig(openTelemetrySdk);

    tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");
    tracer.spanBuilder("Not forwarded to any processors").startSpan().end();
    tracer.spanBuilder("Not forwarded to any processors").startSpan().end();

    // We build an SDK with the alwaysOn sampler.
    openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .setTraceConfig(alwaysOn)
                    .build())
            .build();
    printTraceConfig(openTelemetrySdk);

    tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");
    tracer.spanBuilder("Forwarded to all processors").startSpan().end();
    tracer.spanBuilder("Forwarded to all processors").startSpan().end();

    // We build an SDK with the configuration to use the probability sampler which was configured to
    // sample
    // only 50% of the spans.
    openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .setTraceConfig(probability)
                    .build())
            .build();
    printTraceConfig(openTelemetrySdk);

    tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");

    for (int i = 0; i < 10; i++) {
      tracer
          .spanBuilder(String.format("Span %d might be forwarded to all processors", i))
          .startSpan()
          .end();
    }

    // We can also implement our own sampler. We need to implement the
    // io.opentelemetry.sdk.trace.Sampler interface.
    class MySampler implements Sampler {

      @Override
      public SamplingResult shouldSample(
          Context parentContext,
          String traceId,
          String name,
          Kind spanKind,
          Attributes attributes,
          List<LinkData> parentLinks) {
        return SamplingResult.create(
            name.contains("SAMPLE")
                ? SamplingResult.Decision.RECORD_AND_SAMPLE
                : SamplingResult.Decision.DROP);
      }

      @Override
      public String getDescription() {
        return "My Sampler Implementation!";
      }
    }

    // Add MySampler to the Trace Configuration
    TraceConfig mySampler = TraceConfig.builder().setSampler(new MySampler()).build();
    openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .setTraceConfig(mySampler)
                    .build())
            .build();
    printTraceConfig(openTelemetrySdk);

    tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");

    tracer.spanBuilder("#1 - SamPleD").startSpan().end();
    tracer
        .spanBuilder("#2 - SAMPLE this trace will be the first to be printed in the console output")
        .startSpan()
        .end();
    tracer.spanBuilder("#3 - Smth").startSpan().end();
    tracer
        .spanBuilder("#4 - SAMPLED this trace will be the second one shown in the console output")
        .startSpan()
        .end();
    tracer.spanBuilder("#5").startSpan().end();
  }

  private static void printTraceConfig(OpenTelemetrySdk sdk) {
    TraceConfig config = sdk.getTracerManagement().getActiveTraceConfig();
    System.err.println("==================================");
    System.err.print("Max number of attributes: ");
    System.err.println(config.getMaxNumberOfAttributes());
    System.err.print("Max number of attributes per event: ");
    System.err.println(config.getMaxNumberOfAttributesPerEvent());
    System.err.print("Max number of attributes per link: ");
    System.err.println(config.getMaxNumberOfAttributesPerLink());
    System.err.print("Max number of events: ");
    System.err.println(config.getMaxNumberOfEvents());
    System.err.print("Max number of links: ");
    System.err.println(config.getMaxNumberOfLinks());
    System.err.print("Sampler: ");
    System.err.println(config.getSampler().getDescription());
  }
}
