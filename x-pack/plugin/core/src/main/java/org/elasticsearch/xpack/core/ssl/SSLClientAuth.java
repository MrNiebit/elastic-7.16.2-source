/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ssl;

import java.util.Locale;

import javax.net.ssl.SSLParameters;

/**
 * The client authentication mode that is used for SSL servers
 */
public enum SSLClientAuth {

    NONE() {
        public boolean enabled() {
            return false;
        }

        public void configure(SSLParameters sslParameters) {
            // nothing to do here
            assert sslParameters.getWantClientAuth() == false;
            assert sslParameters.getNeedClientAuth() == false;
        }
    },
    OPTIONAL() {
        public boolean enabled() {
            return true;
        }

        public void configure(SSLParameters sslParameters) {
            sslParameters.setWantClientAuth(true);
        }
    },
    REQUIRED() {
        public boolean enabled() {
            return true;
        }

        public void configure(SSLParameters sslParameters) {
            sslParameters.setNeedClientAuth(true);
        }
    };

    /**
     * @return true if client authentication is enabled
     */
    public abstract boolean enabled();

    /**
     * Configure client authentication of the provided {@link SSLParameters}
     */
    public abstract void configure(SSLParameters sslParameters);

    public static SSLClientAuth parse(String value) {
        assert value != null;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "none":
                return NONE;
            case "optional":
                return OPTIONAL;
            case "required":
                return REQUIRED;
            default:
                throw new IllegalArgumentException("could not resolve ssl client auth. unknown value [" + value + "]");
        }
    }
}
