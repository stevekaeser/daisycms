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
import java.io.ByteArrayInputStream;

/**
 * An implementation of the {@link PartDataSource} interface that takes
 * its input from a byte array. Provided for your convenience.
 */
public final class ByteArrayPartDataSource implements PartDataSource {
    private byte[] data;

    public ByteArrayPartDataSource(byte[] data) {
        this.data = data;
    }

    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }

    public long getSize() {
        return data.length;
    }
}
