/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.license;

import org.elasticsearch.client.TimedRequest;

public class PutLicenseRequest extends TimedRequest {

    private String licenseDefinition;
    private boolean acknowledge = false;

    public PutLicenseRequest() {}

    public void setLicenseDefinition(String licenseDefinition) {
        this.licenseDefinition = licenseDefinition;
    }

    public String getLicenseDefinition() {
        return licenseDefinition;
    }

    public void setAcknowledge(boolean acknowledge) {
        this.acknowledge = acknowledge;
    }

    public boolean isAcknowledge() {
        return acknowledge;
    }
}
