/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.watcher.actions.throttler;

import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.core.watcher.WatcherConstants;
import org.elasticsearch.xpack.core.watcher.execution.WatchExecutionContext;

import java.time.Clock;

import static org.elasticsearch.xpack.core.watcher.actions.throttler.Throttler.Type.LICENSE;

public class ActionThrottler implements Throttler {

    private static final AckThrottler ACK_THROTTLER = new AckThrottler();

    private final XPackLicenseState licenseState;
    private final PeriodThrottler periodThrottler;
    private final AckThrottler ackThrottler;

    public ActionThrottler(Clock clock, @Nullable TimeValue throttlePeriod, XPackLicenseState licenseState) {
        this(new PeriodThrottler(clock, throttlePeriod), ACK_THROTTLER, licenseState);
    }

    ActionThrottler(PeriodThrottler periodThrottler, AckThrottler ackThrottler, XPackLicenseState licenseState) {
        this.periodThrottler = periodThrottler;
        this.ackThrottler = ackThrottler;
        this.licenseState = licenseState;
    }

    public TimeValue throttlePeriod() {
        return periodThrottler != null ? periodThrottler.period() : null;
    }

    @Override
    public Result throttle(String actionId, WatchExecutionContext ctx) {
        if (WatcherConstants.WATCHER_FEATURE.check(licenseState) == false) {
            return Result.throttle(LICENSE, "watcher license does not allow action execution");
        }
        if (periodThrottler != null) {
            Result throttleResult = periodThrottler.throttle(actionId, ctx);
            if (throttleResult.throttle()) {
                return throttleResult;
            }
        }
        return ackThrottler.throttle(actionId, ctx);
    }
}
