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

package org.pfj.io.uring.struct.raw;

import org.pfj.io.async.net.SocketAddress;
import org.pfj.io.uring.struct.ExternalRawStructure;
import org.pfj.lang.Result;

public interface RawSocketAddress<T extends SocketAddress<?>, R extends ExternalRawStructure<?>> {
    void assign(final T input);

    Result<T> extract();

    R shape();
}
