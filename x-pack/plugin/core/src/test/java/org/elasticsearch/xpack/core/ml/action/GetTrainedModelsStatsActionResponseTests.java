/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ml.action;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.ingest.IngestStats;
import org.elasticsearch.xpack.core.action.util.QueryPage;
import org.elasticsearch.xpack.core.ml.AbstractBWCWireSerializationTestCase;
import org.elasticsearch.xpack.core.ml.action.GetTrainedModelsStatsAction.Response;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.InferenceStatsTests;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.xpack.core.ml.action.GetTrainedModelsStatsAction.Response.RESULTS_FIELD;

public class GetTrainedModelsStatsActionResponseTests extends AbstractBWCWireSerializationTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<Response.TrainedModelStats> trainedModelStats = Stream.generate(() -> randomAlphaOfLength(10))
            .limit(listSize)
            .map(
                id -> new Response.TrainedModelStats(
                    id,
                    randomBoolean() ? randomIngestStats() : null,
                    randomIntBetween(0, 10),
                    randomBoolean() ? InferenceStatsTests.createTestInstance(id, null) : null
                )
            )
            .collect(Collectors.toList());
        return new Response(new QueryPage<>(trainedModelStats, randomLongBetween(listSize, 1000), RESULTS_FIELD));
    }

    private IngestStats randomIngestStats() {
        List<String> pipelineIds = Stream.generate(() -> randomAlphaOfLength(10))
            .limit(randomIntBetween(0, 10))
            .collect(Collectors.toList());
        return new IngestStats(
            new IngestStats.Stats(randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong()),
            pipelineIds.stream().map(id -> new IngestStats.PipelineStat(id, randomStats())).collect(Collectors.toList()),
            pipelineIds.stream().collect(Collectors.toMap(Function.identity(), (v) -> randomProcessorStats()))
        );
    }

    private IngestStats.Stats randomStats() {
        return new IngestStats.Stats(randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong());
    }

    private List<IngestStats.ProcessorStat> randomProcessorStats() {
        return Stream.generate(() -> randomAlphaOfLength(10))
            .limit(randomIntBetween(0, 10))
            .map(name -> new IngestStats.ProcessorStat(name, "inference", randomStats()))
            .collect(Collectors.toList());
    }

    @Override
    protected Writeable.Reader<Response> instanceReader() {
        return Response::new;
    }

    @Override
    protected Response mutateInstanceForVersion(Response instance, Version version) {
        if (version.equals(Version.CURRENT)) {
            return instance;
        }
        if (version.before(Version.V_7_8_0)) {
            List<Response.TrainedModelStats> stats = instance.getResources()
                .results()
                .stream()
                .map(
                    s -> new Response.TrainedModelStats(
                        s.getModelId(),
                        adjustForVersion(s.getIngestStats(), version),
                        s.getPipelineCount(),
                        null
                    )
                )
                .collect(Collectors.toList());
            return new Response(new QueryPage<>(stats, instance.getResources().count(), RESULTS_FIELD));
        }
        List<Response.TrainedModelStats> stats = instance.getResources()
            .results()
            .stream()
            .map(
                s -> new Response.TrainedModelStats(
                    s.getModelId(),
                    adjustForVersion(s.getIngestStats(), version),
                    s.getPipelineCount(),
                    InferenceStatsTests.mutateForVersion(s.getInferenceStats(), version)
                )
            )
            .collect(Collectors.toList());
        return new Response(new QueryPage<>(stats, instance.getResources().count(), RESULTS_FIELD));
    }

    IngestStats adjustForVersion(IngestStats stats, Version version) {
        if (version.before(Version.V_7_6_0)) {
            return new IngestStats(
                stats.getTotalStats(),
                stats.getPipelineStats(),
                stats.getProcessorStats()
                    .entrySet()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            (kv) -> kv.getValue()
                                .stream()
                                .map(pstats -> new IngestStats.ProcessorStat(pstats.getName(), "_NOT_AVAILABLE", pstats.getStats()))
                                .collect(Collectors.toList())
                        )
                    )
            );
        }
        return stats;
    }

}
