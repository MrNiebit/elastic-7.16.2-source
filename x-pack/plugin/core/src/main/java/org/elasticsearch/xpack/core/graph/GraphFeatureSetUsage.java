/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.graph;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.xpack.core.XPackFeatureSet;
import org.elasticsearch.xpack.core.XPackField;

import java.io.IOException;

public class GraphFeatureSetUsage extends XPackFeatureSet.Usage {

    public GraphFeatureSetUsage(StreamInput input) throws IOException {
        super(input);
    }

    public GraphFeatureSetUsage(boolean available, boolean enabled) {
        super(XPackField.GRAPH, available, enabled);
    }

    @Override
    public Version getMinimalSupportedVersion() {
        return Version.V_6_0_0;
    }

}
