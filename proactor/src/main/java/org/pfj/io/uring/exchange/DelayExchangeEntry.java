/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.uring.exchange;

import org.pfj.io.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.NativeFailureType;
import org.pfj.io.async.Submitter;
import org.pfj.io.scheduler.Timeout;
import org.pfj.io.uring.struct.offheap.OffHeapTimeSpec;
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.time.Duration;
import java.util.function.BiConsumer;

import static org.pfj.io.scheduler.Timeout.timeout;
import static org.pfj.io.uring.AsyncOperation.IORING_OP_TIMEOUT;
import static org.pfj.lang.Result.success;

public class DelayExchangeEntry extends AbstractExchangeEntry<DelayExchangeEntry, Duration> {
    private final OffHeapTimeSpec timeSpec = OffHeapTimeSpec.uninitialized();
    private long startNanos;

    protected DelayExchangeEntry(final PlainObjectPool<DelayExchangeEntry> pool) {
        super(IORING_OP_TIMEOUT, pool);
    }

    @Override
    public void close() {
        timeSpec.dispose();
    }

    @Override
    protected void doAccept(final int res, final int flags, final Submitter submitter) {
        final var totalNanos = System.nanoTime() - startNanos;

        final var result = Math.abs(res) != NativeFailureType.ETIME.code()
                           ? NativeFailureType.<Duration>result(res)
                           : success(timeout(totalNanos).nanos().asDuration());

        completion.accept(result, submitter);
    }

    public DelayExchangeEntry prepare(final BiConsumer<Result<Duration>, Submitter> completion, final Timeout timeout) {
        startNanos = System.nanoTime();

        timeout.asSecondsAndNanos()
               .map(timeSpec::setSecondsNanos);

        return super.prepare(completion);
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
                    .addr(timeSpec.address())
                    .fd(-1)
                    .len(1)
                    .off(1);
    }
}
