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

package org.pfj.io.net;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.net.ConnectionContext;
import org.pfj.io.async.net.ServerContext;

public interface ServerProtocol {
    /**
     * Start protocol.
     * <p>
     * WARNING: Provided {@link Proactor} instance is transient, it should not be stored or used outside the context of this method.
     *
     * @param server     server context
     * @param connection connection context
     * @param proactor   transient {@link Proactor} instance
     */
    void start(ServerContext<?> server, ConnectionContext<?> connection, Proactor proactor);
}
