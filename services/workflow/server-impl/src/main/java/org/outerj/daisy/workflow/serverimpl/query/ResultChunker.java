/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow.serverimpl.query;

import java.util.List;

/**
 * Gets a chunk out of the results and returns information about the applied chunk.
 */
public class ResultChunker {
    /**
     *
     * @param chunkOffset 1-based!
     */
    public static <T> ChunkResult<T> getChunk(List<T> items, int chunkOffset, int chunkLength) {
        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.requestChunkOffset = chunkOffset;
        chunkInfo.requestChunkLength = chunkLength;
        chunkInfo.size = items.size();

        if (chunkOffset < 1)
            chunkOffset = 1;
        if (chunkLength == -1)
            chunkLength = items.size();
        if (chunkLength < 0)
            chunkLength = 0;

        if (chunkOffset + chunkLength - 1 > items.size())
            chunkLength = items.size() - chunkOffset + 1;

        chunkInfo.chunkOffset = chunkOffset;
        chunkInfo.chunkLength = chunkLength;

        ChunkResult<T> result = new ChunkResult<T>();
        result.chunk = items.subList(chunkOffset - 1, chunkOffset - 1 + chunkLength);
        result.chunkInfo = chunkInfo;
        return result;
    }

    public static class ChunkInfo {
        public int chunkOffset;
        public int chunkLength;
        public int requestChunkOffset;
        public int requestChunkLength;
        public int size;
    }

    public static class ChunkResult<T> {
        public List<T> chunk;
        public ChunkInfo chunkInfo;
    }
}
