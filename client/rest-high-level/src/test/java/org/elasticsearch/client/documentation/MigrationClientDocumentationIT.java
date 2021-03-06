/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.documentation;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.client.ESRestHighLevelClientTestCase;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.migration.DeprecationInfoRequest;
import org.elasticsearch.client.migration.DeprecationInfoResponse;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to generate the Java Migration API documentation.
 * You need to wrap your code between two tags like:
 * // tag::example
 * // end::example
 *
 * Where example is your tag name.
 *
 * Then in the documentation, you can extract what is between tag and end tags with
 * ["source","java",subs="attributes,callouts,macros"]
 * --------------------------------------------------
 * include-tagged::{doc-tests}/MigrationClientDocumentationIT.java[example]
 * --------------------------------------------------
 *
 * The column width of the code block is 84. If the code contains a line longer
 * than 84, the line will be cut and a horizontal scroll bar will be displayed.
 * (the code indentation of the tag is not included in the width)
 */
@SuppressWarnings("removal")
public class MigrationClientDocumentationIT extends ESRestHighLevelClientTestCase {

    public void testGetDeprecationInfo() throws IOException, InterruptedException {
        RestHighLevelClient client = highLevelClient();
        createIndex("test", Settings.EMPTY);

        //tag::get-deprecation-info-request
        List<String> indices = new ArrayList<>();
        indices.add("test");
        DeprecationInfoRequest deprecationInfoRequest = new DeprecationInfoRequest(indices); // <1>
        //end::get-deprecation-info-request

        // tag::get-deprecation-info-execute
        DeprecationInfoResponse deprecationInfoResponse =
            client.migration().getDeprecationInfo(deprecationInfoRequest, RequestOptions.DEFAULT);
        // end::get-deprecation-info-execute

        // tag::get-deprecation-info-response
        List<DeprecationInfoResponse.DeprecationIssue> clusterIssues =
            deprecationInfoResponse.getClusterSettingsIssues(); // <1>
        List<DeprecationInfoResponse.DeprecationIssue> nodeIssues =
            deprecationInfoResponse.getNodeSettingsIssues(); // <2>
        Map<String, List<DeprecationInfoResponse.DeprecationIssue>> indexIssues =
            deprecationInfoResponse.getIndexSettingsIssues(); // <3>
        List<DeprecationInfoResponse.DeprecationIssue> mlIssues =
            deprecationInfoResponse.getMlSettingsIssues(); // <4>
        // end::get-deprecation-info-response

        // tag::get-deprecation-info-execute-listener
        ActionListener<DeprecationInfoResponse> listener =
            new ActionListener<DeprecationInfoResponse>() {
                @Override
                public void onResponse(DeprecationInfoResponse deprecationInfoResponse1) { // <1>
                    List<DeprecationInfoResponse.DeprecationIssue> clusterIssues =
                        deprecationInfoResponse.getClusterSettingsIssues();
                    List<DeprecationInfoResponse.DeprecationIssue> nodeIssues =
                        deprecationInfoResponse.getNodeSettingsIssues();
                    Map<String, List<DeprecationInfoResponse.DeprecationIssue>> indexIssues =
                        deprecationInfoResponse.getIndexSettingsIssues();
                    List<DeprecationInfoResponse.DeprecationIssue> mlIssues =
                        deprecationInfoResponse.getMlSettingsIssues();
                }

                @Override
                public void onFailure(Exception e) {
                    // <2>
                }
            };
        // end::get-deprecation-info-execute-listener

        final CountDownLatch latch = new CountDownLatch(1);
        listener = new LatchedActionListener<>(listener, latch);

        // tag::get-deprecation-info-execute-async
        client.migration().getDeprecationInfoAsync(deprecationInfoRequest,
            RequestOptions.DEFAULT, listener); // <1>
        // end::get-deprecation-info-execute-async

        assertTrue(latch.await(30L, TimeUnit.SECONDS));
    }
}
