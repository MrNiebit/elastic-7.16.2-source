/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.job.categorization;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractCharFilterFactory;

import java.io.Reader;

public class FirstLineWithLettersCharFilterFactory extends AbstractCharFilterFactory {

    public FirstLineWithLettersCharFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name);
    }

    @Override
    public Reader create(Reader tokenStream) {
        return new FirstLineWithLettersCharFilter(tokenStream);
    }
}
