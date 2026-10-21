package com.shopper.aggregator.presentation.rest;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
class TraceIdHeaderFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Trace-Id";

    private final ObjectProvider<Tracer> tracer;

    TraceIdHeaderFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        tracer.ifAvailable(t -> {
            var span = t.currentSpan();
            if (span != null) {
                response.setHeader(HEADER, span.context().traceId());
            }
        });
        chain.doFilter(request, response);
    }
}
