/**
 * Facebook Re-post Bot - Bot that checks some pages and copies posts to other pages.
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.fbrepostbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

    private ScheduledExecutorService executorService;

    public Scheduler() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("Creating scheduler with {} threads.", availableProcessors);
        this.executorService = Executors.newScheduledThreadPool(availableProcessors);
    }

    public void shutdown() {
        log.info("Shutting down scheduler...");
        this.executorService.shutdown();
    }

    public ScheduledFuture<?> periodic(Runnable command, long period, TimeUnit timeUnit) {
        return this.executorService.scheduleAtFixedRate(command, TimeUnit.SECONDS.convert(5, timeUnit), period, timeUnit);
    }

    public ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit timeUnit) {
        return this.executorService.schedule(command, delay, timeUnit);
    }
}
