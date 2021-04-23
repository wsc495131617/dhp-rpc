package org.dhp.common.controller;

import com.google.common.collect.Sets;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

@RestController
public class MetricsController {

    static {
        DefaultExports.initialize();
    }

    @GetMapping(value = {"/dhp/metrics"}, produces = {TextFormat.CONTENT_TYPE_004,"application/json;charset=UTF-8"})
    @ResponseBody
    public String metrics(@RequestParam(name = "name[]", required = false) String[] names) throws IOException {
        Set<String> includedNameSet = names == null ? Collections.emptySet() : Sets.newHashSet(names);

        Enumeration<Collector.MetricFamilySamples> prometheusSamples = CollectorRegistry
                .defaultRegistry
                .filteredMetricFamilySamples(includedNameSet);
        Writer writer = new StringWriter();
        TextFormat.write004(writer, prometheusSamples);
        return writer.toString();
    }
}
