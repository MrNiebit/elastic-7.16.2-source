/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.documentation;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentType;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.instanceOf;

/**
 * This class is used to generate the Java indices administration documentation.
 * You need to wrap your code between two tags like:
 * // tag::example[]
 * // end::example[]
 *
 * Where example is your tag name.
 *
 * Then in the documentation, you can extract what is between tag and end tags
 * with ["source","java",subs="attributes,callouts,macros"]
 * --------------------------------------------------
 * include-tagged::{client-tests}/IndicesDocumentationIT.java[your-example-tag-here]
 * --------------------------------------------------
 */
public class IndicesDocumentationIT extends ESIntegTestCase {

    /**
     * This test method is used to generate the Put Mapping Java Indices API documentation
     * at "docs/java-api/admin/indices/put-mapping.asciidoc" so the documentation gets tested
     * so that it compiles and runs without throwing errors at runtime.
     */
    public void testPutMappingDocumentation() throws Exception {
        Client client = client();

        // tag::index-with-mapping
        client.admin().indices().prepareCreate("twitter")    // <1>
                .addMapping("_doc", "message", "type=text") // <2>
                .get();
        // end::index-with-mapping
        GetMappingsResponse getMappingsResponse = client.admin().indices().prepareGetMappings("twitter").get();
        assertEquals(1, getMappingsResponse.getMappings().size());
        ImmutableOpenMap<String, MappingMetadata> indexMapping = getMappingsResponse.getMappings().get("twitter");
        assertThat(indexMapping.get("_doc"), instanceOf(MappingMetadata.class));

        // we need to delete in order to create a fresh new index with another type
        client.admin().indices().prepareDelete("twitter").get();
        client.admin().indices().prepareCreate("twitter").get();

        // tag::putMapping-request-source
        client.admin().indices().preparePutMapping("twitter")   // <1>
        .setType("_doc")
        .setSource("{\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\n" +                           // <2>
                "      \"type\": \"text\"\n" +
                "    }\n" +
                "  }\n" +
                "}", XContentType.JSON)
        .get();

        // You can also provide the type in the source document
        client.admin().indices().preparePutMapping("twitter")
        .setType("_doc")
        .setSource("{\n" +
                "    \"_doc\":{\n" +                            // <3>
                "        \"properties\": {\n" +
                "            \"name\": {\n" +
                "                \"type\": \"text\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}", XContentType.JSON)
        .get();
        // end::putMapping-request-source
        getMappingsResponse = client.admin().indices().prepareGetMappings("twitter").get();
        assertEquals(1, getMappingsResponse.getMappings().size());
        indexMapping = getMappingsResponse.getMappings().get("twitter");
        assertEquals(
            singletonMap("properties", singletonMap("name", singletonMap("type", "text"))),
            indexMapping.get("_doc").getSourceAsMap()
        );
    }

}
