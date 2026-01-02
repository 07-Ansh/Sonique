package com.liskovsoft.sharedutils.rx;

import io.reactivex.Scheduler;

public interface SchedulerProvider {
    Scheduler ui();
    Scheduler computation();
    Scheduler io();
}
