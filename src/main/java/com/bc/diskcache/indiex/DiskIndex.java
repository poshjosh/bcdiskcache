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

import java.io.IOException;
import java.util.Collection;
import com.bc.diskcache.DiskLruCacheIx;

public class DiskIndex<T> extends CacheIndex<T> {

    public static class DiskCache<V> implements Cache<String, Collection<V>>{
        private final DiskLruCacheIx delegate;
        public DiskCache(DiskLruCacheIx delegate) {
            this.delegate = java.util.Objects.requireNonNull(delegate);
        }
        @Override
        public void clear() throws Exception {
            delegate.clear();
        }
        @Override
        public void put(String key, Collection<V> val) throws Exception {
//            Logx.getInstance().debug(this.getClass(), "Putting: {0} = {1}", key, val);
            delegate.putIfNone(key, val);
        }
        @Override
        public Collection<V> getOrDefault(String key, Collection<V> outputIfNone) throws Exception {
            final Collection<V> val = (Collection<V>)delegate.getObject(key, null);
//            Logx.getInstance().debug(this.getClass(), "Found: {0} = {1}", key, val);
            return val == null ? outputIfNone : val;
        }
    }

    private final DiskLruCacheIx diskCacheLru;

    public DiskIndex(DiskLruCacheIx diskCacheLru, Tokenizer<String, String> keyTokenizer, int maxEntriesPerKey) {
        super(new DiskCache<T>(diskCacheLru), keyTokenizer, maxEntriesPerKey);
        this.diskCacheLru = java.util.Objects.requireNonNull(diskCacheLru);
    }

    @Override
    public void flush() throws IOException {
        diskCacheLru.flush();
    }

    @Override
    public boolean isClosed() {
        return diskCacheLru.isClosed();
    }

    @Override
    public void close() throws IOException {
        diskCacheLru.close();
    }
}
