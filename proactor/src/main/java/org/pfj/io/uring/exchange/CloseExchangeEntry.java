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
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;
import org.pfj.lang.Unit;

import java.util.function.BiConsumer;

import static org.pfj.io.uring.AsyncOperation.IORING_OP_CLOSE;

public class CloseExchangeEntry extends AbstractExchangeEntry<CloseExchangeEntry, Unit> {
    private int descriptor;
    private byte flags;

    protected CloseExchangeEntry(final PlainObjectPool<CloseExchangeEntry> pool) {
        super(IORING_OP_CLOSE, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Submitter submitter) {
        completion.accept(res == 0 ? UNIT_RESULT : NativeFailureType.result(res), submitter);
    }

    public CloseExchangeEntry prepare(final BiConsumer<Result<Unit>, Submitter> completion,
                                      final int descriptor,
                                      final byte flags) {
        this.descriptor = descriptor;
        this.flags = flags;
        return super.prepare(completion);
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor);
    }
}
