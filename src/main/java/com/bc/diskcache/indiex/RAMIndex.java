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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RAMIndex<T> extends CacheIndex<T> {

    public static class RAMCache<V> implements Cache<String, Collection<V>>{
        private final Map<String, Collection<V>> delegate;
        public RAMCache(Map<String, Collection<V>> delegate) {
            this.delegate = java.util.Objects.requireNonNull(delegate);
        }
        @Override
        public void clear() throws Exception {
            delegate.clear();
        }
        @Override
        public void put(String key, Collection<V> val) throws Exception {
            delegate.put(key, val);
        }
        @Override
        public Collection<V> getOrDefault(String key, Collection<V> outputIfNone) throws Exception {
            final Collection<V> found = delegate.get(key);
            return found == null ? outputIfNone : found;
        }
    }

    public RAMIndex(int minKeySizeChars, int maxEntriesPerKey) {
        this(new SimpleTokenizer(minKeySizeChars), maxEntriesPerKey);
    }

    public RAMIndex(Tokenizer<String, String> keyTokenizer, int maxEntriesPerKey) {
        this(new HashMap(), keyTokenizer, maxEntriesPerKey);
    }

    public RAMIndex(Map ramCache, Tokenizer<String, String> keyTokenizer, int maxEntriesPerKey) {
        super(new RAMIndex.RAMCache<T>(ramCache), keyTokenizer, maxEntriesPerKey);
    }
}
