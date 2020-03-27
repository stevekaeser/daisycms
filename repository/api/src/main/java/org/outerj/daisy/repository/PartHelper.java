/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository;

import java.io.InputStream;
import java.io.IOException;

public final class PartHelper {
    private static final int BUFFER_SIZE = 32768;

    /**
     * Reads an inputstream till the end into a byte array. Closes the input stream
     * whatever happens. Optimized for the case where we know the size of the input
     * on beforehand.
     *
     * @param is the input stream to be read
     * @param sizeHint optional, specify -1 if nothing useful
     * @throws IOException
     */
    public static byte[] streamToByteArrayAndClose(InputStream is, int sizeHint) throws IOException {
        if (is == null)
            throw new IllegalArgumentException("\"is\" param may not be null");

        try {
            byte[] buffer = new byte[sizeHint != -1 ? sizeHint : BUFFER_SIZE];

            int read = is.read(buffer);
            if (read == -1) {
                return new byte[0];
            } else {
                int c = is.read();
                if (c == -1) {
                    // we read the entire stream in one go
                    return read == buffer.length ? buffer : cutBuffer(buffer, read);
                } else {
                    // grow buffer if necessary to store c
                    if (read == buffer.length)
                        buffer = growBuffer(buffer);
                    buffer[read] = (byte)c;
                    int pos = read + 1;

                    while (true) {
                        read = is.read(buffer, pos, buffer.length - pos);
                        if (read != -1) {
                            if (pos + read == buffer.length) {
                                // before growing, check we're not at the end
                                c = is.read();
                                if (c == -1)
                                    return buffer;
                                buffer = growBuffer(buffer);
                                buffer[pos + read] = (byte)c;
                                pos = pos + read + 1;
                            } else {
                                pos = pos + read;
                            }
                        } else {
                            return cutBuffer(buffer, pos);
                        }
                    }
                }
            }
        } finally {
            is.close();
        }
    }

    private static byte[] growBuffer(byte[] buffer) {
        byte[] newBuffer = new byte[buffer.length + BUFFER_SIZE];
        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        return newBuffer;
    }

    private static byte[] cutBuffer(byte[] buffer, int length) {
        byte[] newBuffer = new byte[length];
        System.arraycopy(buffer, 0, newBuffer, 0, length);
        return newBuffer;
    }
}
