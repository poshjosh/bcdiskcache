/*
 * Copyright 2018 NUROX Ltd.
 *
 * Licensed under the NUROX Ltd Software License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.looseboxes.com/legal/licenses/software.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bc.diskcache.indiex;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Index<T> extends Closeable {

    class IndexException extends Exception {
        public IndexException() { }
        public IndexException(String message) {
            super(message);
        }
        public IndexException(String message, Throwable cause) {
            super(message, cause);
        }
        public IndexException(Throwable cause) {
            super(cause);
        }
    }

    interface IndexConsumer<I> {
        boolean accept(String key,  I index);
    }

    class RangeCollector<T> implements Index.IndexConsumer<T> {
        private int found = 0;
        private int collected = 0;
        private final Index.IndexConsumer<T> collector;
        private final int offset;
        private final int limit;
        public RangeCollector(Index.IndexConsumer<T> collector, int offset, int limit) {
            this.collector = java.util.Objects.requireNonNull(collector);
            this.offset = offset;
            this.limit = limit;
        }
        @Override
        public boolean accept(String key, T index) {
            ++found;
            boolean result = false;
            if(found > offset && collected < limit) {
                if(result = collector.accept(key, index)) {
                    ++collected;
                }
            }
            return result;
        }
    };

    class MapCollector<T> implements Index.IndexConsumer<T> {
        private final Map<String, List<T>> map;
        private final int maxEntriesPerKey;
        public MapCollector(Map<String, List<T>> map, int maxEntriesPerKey) {
            this.map = java.util.Objects.requireNonNull(map);
            this.maxEntriesPerKey = maxEntriesPerKey;
        }
        @Override
        public boolean accept(String key, T index) {
            List<T> values = map.get(key);
            if (values == null) {
                values = new ArrayList<>(maxEntriesPerKey);
                map.put(key, values);
            }
            return values.add(index);
        }
    };

    Index NO_OP = new Index() {
        @Override
        public void flush() { }
        @Override
        public boolean isClosed() { return false; }
        @Override
        public void close() { }
        @Override
        public int index(String phrase, Collection values) { return 0; }
        @Override
        public int index(String [] wordTokens, Collection values) { return 0; }
        @Override
        public int indexToken(String wordToken, Collection values) { return 0; }
        @Override
        public Map find(String phrase, int offset, int limit) { return Collections.EMPTY_MAP; }
        @Override
        public int find(String phrase, IndexConsumer collector, int offset, int limit) { return 0; }
        @Override
        public int find(String [] wordTokens, Index.IndexConsumer collector, int offset, int limit) {  return 0; }
        @Override
        public int findToken(String wordToken, Index.IndexConsumer collector, int offset, int limit) {  return 0; }
    };

    void flush() throws IOException;

    boolean isClosed();

    @Override
    void close() throws IOException;

    int index(String phrase, Collection<T> values) throws IndexException;

    int index(String [] wordTokens, Collection<T> values) throws IndexException;

    int indexToken(String wordToken, Collection<T> values) throws IndexException;

    Map<String, List<T>> find(String phrase, int offset, int limit) throws IndexException;

    int find(String phrase, IndexConsumer<T> collector, int offset, int limit) throws IndexException;

    int find(String [] wordTokens, Index.IndexConsumer<T> collector, int offset, int limit) throws IndexException;

    int findToken(String wordToken, Index.IndexConsumer<T> collector, int offset, int limit) throws IndexException;
}
