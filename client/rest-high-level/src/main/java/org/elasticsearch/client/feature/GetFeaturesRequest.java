/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.feature;

import org.elasticsearch.client.TimedRequest;

/**
 * A {@link TimedRequest} to get the list of features available to be included in snapshots in the cluster.
 */
public class GetFeaturesRequest extends TimedRequest {}
