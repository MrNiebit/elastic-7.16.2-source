/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.client;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.rollup.DeleteRollupJobRequest;
import org.elasticsearch.client.rollup.GetRollupCapsRequest;
import org.elasticsearch.client.rollup.GetRollupIndexCapsRequest;
import org.elasticsearch.client.rollup.GetRollupJobRequest;
import org.elasticsearch.client.rollup.PutRollupJobRequest;
import org.elasticsearch.client.rollup.StartRollupJobRequest;
import org.elasticsearch.client.rollup.StopRollupJobRequest;

import java.io.IOException;

import static org.elasticsearch.client.RequestConverters.REQUEST_BODY_CONTENT_TYPE;
import static org.elasticsearch.client.RequestConverters.createEntity;

final class RollupRequestConverters {

    private RollupRequestConverters() {}

    static Request putJob(final PutRollupJobRequest putRollupJobRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "job")
            .addPathPart(putRollupJobRequest.getConfig().getId())
            .build();
        Request request = new Request(HttpPut.METHOD_NAME, endpoint);
        request.setEntity(createEntity(putRollupJobRequest, REQUEST_BODY_CONTENT_TYPE));
        return request;
    }

    static Request startJob(final StartRollupJobRequest startRollupJobRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "job")
            .addPathPart(startRollupJobRequest.getJobId())
            .addPathPartAsIs("_start")
            .build();
        return new Request(HttpPost.METHOD_NAME, endpoint);
    }

    static Request stopJob(final StopRollupJobRequest stopRollupJobRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "job")
            .addPathPart(stopRollupJobRequest.getJobId())
            .addPathPartAsIs("_stop")
            .build();

        Request request = new Request(HttpPost.METHOD_NAME, endpoint);
        RequestConverters.Params parameters = new RequestConverters.Params();
        parameters.withTimeout(stopRollupJobRequest.timeout());
        if (stopRollupJobRequest.waitForCompletion() != null) {
            parameters.withWaitForCompletion(stopRollupJobRequest.waitForCompletion());
        }
        request.addParameters(parameters.asMap());
        return request;
    }

    static Request getJob(final GetRollupJobRequest getRollupJobRequest) {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "job")
            .addPathPart(getRollupJobRequest.getJobId())
            .build();
        return new Request(HttpGet.METHOD_NAME, endpoint);
    }

    static Request deleteJob(final DeleteRollupJobRequest deleteRollupJobRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "job")
            .addPathPart(deleteRollupJobRequest.getId())
            .build();
        return new Request(HttpDelete.METHOD_NAME, endpoint);
    }

    static Request search(final SearchRequest request) throws IOException {
        if (request.types().length > 0) {
            /*
             * Ideally we'd check this with the standard validation framework
             * but we don't have a special request for rollup search so that'd
             * be difficult.
             */
            ValidationException ve = new ValidationException();
            ve.addValidationError("types are not allowed in rollup search");
            throw ve;
        }
        return RequestConverters.search(request, "_rollup_search");
    }

    static Request getRollupCaps(final GetRollupCapsRequest getRollupCapsRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addPathPartAsIs("_rollup", "data")
            .addPathPart(getRollupCapsRequest.getIndexPattern())
            .build();
        return new Request(HttpGet.METHOD_NAME, endpoint);
    }

    static Request getRollupIndexCaps(final GetRollupIndexCapsRequest getRollupIndexCapsRequest) throws IOException {
        String endpoint = new RequestConverters.EndpointBuilder().addCommaSeparatedPathParts(getRollupIndexCapsRequest.indices())
            .addPathPartAsIs("_rollup", "data")
            .build();
        return new Request(HttpGet.METHOD_NAME, endpoint);
    }
}
