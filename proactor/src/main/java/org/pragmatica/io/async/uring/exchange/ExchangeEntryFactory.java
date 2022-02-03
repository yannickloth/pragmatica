/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SQEntryFlags;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.utils.PlainObjectPool.objectPool;

public class ExchangeEntryFactory {
    private final PlainObjectPool<NopExchangeEntry> nopPool;
    private final PlainObjectPool<DelayExchangeEntry> delayPool;
    private final PlainObjectPool<CloseExchangeEntry> closePool;
    private final PlainObjectPool<TimeoutExchangeEntry> timeoutPool;
    private final PlainObjectPool<ReadExchangeEntry> readPool;
    private final PlainObjectPool<WriteExchangeEntry> writePool;
    private final PlainObjectPool<SpliceExchangeEntry> splicePool;
    private final PlainObjectPool<OpenExchangeEntry> openPool;
    private final PlainObjectPool<SocketExchangeEntry> socketPool;
    private final PlainObjectPool<StatExchangeEntry> statPool;
    private final PlainObjectPool<ReadVectorExchangeEntry> readVectorPool;
    private final PlainObjectPool<WriteVectorExchangeEntry> writeVectorPool;
    private final PlainObjectPool<ConnectExchangeEntry> connectPool;

    @SuppressWarnings({"rawtypes"})
    private final PlainObjectPool<ListenExchangeEntry> listenPool;
    @SuppressWarnings({"rawtypes"})
    private final PlainObjectPool<AcceptExchangeEntry> acceptPool;

    public ExchangeEntryFactory(ObjectHeap<CompletionHandler> exchangeRegistry) {
        nopPool = objectPool(NopExchangeEntry::new, exchangeRegistry);
        delayPool = objectPool(DelayExchangeEntry::new, exchangeRegistry);
        closePool = objectPool(CloseExchangeEntry::new, exchangeRegistry);
        timeoutPool = objectPool(TimeoutExchangeEntry::new, exchangeRegistry);
        readPool = objectPool(ReadExchangeEntry::new, exchangeRegistry);
        writePool = objectPool(WriteExchangeEntry::new, exchangeRegistry);
        splicePool = objectPool(SpliceExchangeEntry::new, exchangeRegistry);
        openPool = objectPool(OpenExchangeEntry::new, exchangeRegistry);
        socketPool = objectPool(SocketExchangeEntry::new, exchangeRegistry);
        statPool = objectPool(StatExchangeEntry::new, exchangeRegistry);
        readVectorPool = objectPool(ReadVectorExchangeEntry::new, exchangeRegistry);
        writeVectorPool = objectPool(WriteVectorExchangeEntry::new, exchangeRegistry);
        connectPool = objectPool(ConnectExchangeEntry::new, exchangeRegistry);
        listenPool = objectPool(ListenExchangeEntry::new, exchangeRegistry);
        acceptPool = objectPool(AcceptExchangeEntry::new, exchangeRegistry);
    }

    public NopExchangeEntry forNop(BiConsumer<Result<Unit>, Proactor> completion) {
        return nopPool.alloc()
                      .prepare(completion);
    }

    public TimeoutExchangeEntry forTimeout(Timeout timeout) {
        return timeoutPool.alloc()
                          .prepare(timeout);
    }

    public DelayExchangeEntry forDelay(BiConsumer<Result<Duration>, Proactor> completion, Timeout timeout) {
        return delayPool.alloc()
                        .prepare(completion, timeout);
    }

    public CloseExchangeEntry forClose(BiConsumer<Result<Unit>, Proactor> completion, FileDescriptor fd, Option<Timeout> timeout) {
        return closePool.alloc()
                        .prepare(completion, fd.descriptor(), calculateFlags(timeout));
    }

    private byte calculateFlags(Option<Timeout> timeout) {
        return timeout.equals(Option.empty()) ? 0 : (byte) SQEntryFlags.IO_LINK.mask();
    }

    public ReadExchangeEntry forRead(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                                     OffsetT offset, Option<Timeout> timeout) {
        return readPool.alloc()
                       .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public WriteExchangeEntry forWrite(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fd, OffHeapBuffer buffer,
                                       OffsetT offset, Option<Timeout> timeout) {
        return writePool.alloc()
                        .prepare(completion, fd.descriptor(), buffer, offset.value(), calculateFlags(timeout));
    }

    public SpliceExchangeEntry forSplice(BiConsumer<Result<SizeT>, Proactor> completion, SpliceDescriptor descriptor, Option<Timeout> timeout) {
        return splicePool.alloc()
                         .prepare(completion, descriptor, calculateFlags(timeout));
    }

    public OpenExchangeEntry forOpen(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, Set<OpenFlags> openFlags,
                                     Set<FilePermission> mode, Option<Timeout> timeout) {
        return openPool.alloc()
                       .prepare(completion, path, Bitmask.combine(openFlags), Bitmask.combine(mode), calculateFlags(timeout));
    }

    public SocketExchangeEntry forSocket(BiConsumer<Result<FileDescriptor>, Proactor> completion, AddressFamily addressFamily,
                                         SocketType socketType, Set<SocketFlag> openFlags, Set<SocketOption> options) {
        return socketPool.alloc()
                         .prepare(completion, addressFamily, socketType, openFlags, options);
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> ListenExchangeEntry<T> forListen(BiConsumer<Result<ListenContext<T>>, Proactor> completion,
                                                                    SocketAddress<T> socketAddress, SocketType socketType,
                                                                    Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        return listenPool.alloc()
                         .prepare(completion, socketAddress, socketType, openFlags, queueDepth, options);
    }

    @SuppressWarnings("unchecked")
    public <T extends InetAddress> AcceptExchangeEntry<T> forAccept(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion,
                                                                    FileDescriptor socket, Set<SocketFlag> flags, boolean v6) {
        return acceptPool.alloc()
                         .prepare(completion, socket.descriptor(), Bitmask.combine(flags), v6);
    }

    public ConnectExchangeEntry forConnect(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                           FileDescriptor socket, OffHeapSocketAddress clientAddress,
                                           Option<Timeout> timeout) {
        return connectPool.alloc()
                          .prepare(completion, socket, clientAddress, calculateFlags(timeout));
    }

    public StatExchangeEntry forStat(BiConsumer<Result<FileStat>, Proactor> completion,
                                     int descriptor, int statFlags, int statMask, OffHeapCString rawPath) {
        return statPool.alloc()
                       .prepare(completion, descriptor, statFlags, statMask, rawPath);
    }

    public ReadVectorExchangeEntry forReadVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                                                 OffsetT offset, Option<Timeout> timeout, OffHeapIoVector ioVector) {
        return readVectorPool.alloc()
                             .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public WriteVectorExchangeEntry forWriteVector(BiConsumer<Result<SizeT>, Proactor> completion, FileDescriptor fileDescriptor,
                                                   OffsetT offset, Option<Timeout> timeout, OffHeapIoVector ioVector) {
        return writeVectorPool.alloc()
                              .prepare(completion, fileDescriptor.descriptor(), offset.value(), calculateFlags(timeout), ioVector);
    }

    public void clear() {
        nopPool.clear();
        delayPool.clear();
        closePool.clear();
        timeoutPool.clear();
        readPool.clear();
        writePool.clear();
        splicePool.clear();
        openPool.clear();
        socketPool.clear();
        listenPool.clear();
        acceptPool.clear();
        connectPool.clear();
        statPool.clear();
        readVectorPool.clear();
        writeVectorPool.clear();
    }
}
