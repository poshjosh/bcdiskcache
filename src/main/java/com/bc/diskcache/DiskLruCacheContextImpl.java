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

package com.bc.diskcache;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiskLruCacheContextImpl implements DiskLruCacheContext {

    private static final Logger LOG = Logger.getLogger(DiskLruCacheContextImpl.class.getName());
    
    private static class FileProviderImpl implements FileProvider{
        @Override
        public File createFile(String key) {
            return new File(key);
        }
    }

    private static Map<String, DiskLruCacheIx> instances;

    private final int defaultMaxCacheSizeBytes;
    
    private final FileProvider fileProvider;

    public DiskLruCacheContextImpl(int defaultMaxCacheSizeBytes) {
        this(new FileProviderImpl(), defaultMaxCacheSizeBytes);
    }
    
    public DiskLruCacheContextImpl(FileProvider fileProvider, int defaultMaxCacheSizeBytes) {
        this.fileProvider = Objects.requireNonNull(fileProvider);
        this.defaultMaxCacheSizeBytes = defaultMaxCacheSizeBytes;
    }

    @Override
    public DiskLruCacheIx getInstance(String key) {
        try{
            return this.getInstance(key, true);
        }catch(IOException e) {
            LOG.log(Level.WARNING, null, e);
            return DiskLruCacheIx.NO_OP;
        }
    }

    @Override
    public DiskLruCacheIx getInstance(String key, boolean createIfNone) throws IOException {
        return this.getInstance(key, this.computeMaxSize(key, defaultMaxCacheSizeBytes), createIfNone);
    }
    
    @Override
    public DiskLruCacheIx getInstance(String key, int maxSize, boolean createIfNone) throws IOException {
        if(instances == null) {
            instances = new HashMap<>();
        }
        DiskLruCacheIx instance = instances.get(key);
        if(instance == null && createIfNone) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "DiskCacheLru. Creating Disk Cache named: {0}, maxCacheSizeBytes: {1}",
                        new Object[]{key, maxSize});
            }
            final File file = this.getFileProvider().createFile(key);
            instance = SimpleDiskLruCache.open(file, 1, maxSize);
            instances.put(key, instance);
        }
        return instance;
    }
    
    @Override
    public Set<String> getCacheNames() {
        return instances == null ? Collections.EMPTY_SET : instances.keySet();
    }
    
    @Override
    public boolean containsCacheNamed(String key) {
        return instances == null ? false : instances.containsKey(key);
    }

    @Override
    public int count() {
        return instances == null ? 0 : instances.size();
    }

    @Override
    public void closeAndRemoveAll() {

        if(instances == null || instances.isEmpty()) {
            return;
        }

        LOG.log(Level.FINE, "DiskCacheLru. Closing and removing {0} Disk Caches", count());

        final Set<String> keys = new HashSet(instances.keySet());
        for(String key : keys) {
            this.closeAndRemove(key);
        }
        instances = null;

//        final Iterator<String> iter = instances.keySet().iterator();
//        while(iter.hasNext()) {
//            final String key = iter.next();
//            this.close(key, false);
//            iter.remove();
//        }
//        instances = null;
    }
    
    @Override
    public boolean closeAndRemove(String key) {
        return this.close(key, true);
    }

    public boolean close(String key, boolean remove) {
        final boolean result;
        final DiskLruCacheIx cache = instances == null ? null : instances.get(key);
        if(cache == null) {
            result = false;
        }else{
            if(!cache.isClosed()) {
                try {
                    if(LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Closing cache named: {0} of type: {1}", 
                                new Object[]{key, cache.getClass().getName()});
                    }
                    cache.flush();
                    cache.close();
                }catch(IOException e) {
                    LOG.log(Level.WARNING, null, e);
                }
            }
            if(remove) {
                instances.remove(key);
                final File file = this.getFileProvider().createFile(key);
                SimpleDiskLruCache.removeCacheDir(file);
            }
            result = true;
        }
        return result;
    }

    @Override
    public int computeMaxSize(String key, int outputIfNone) {
        return this.defaultMaxCacheSizeBytes;
    }

    @Override
    public FileProvider getFileProvider() {
        return this.fileProvider;
    }

    @Override
    public int getDefaultMaxCacheSizeBytes() {
        return defaultMaxCacheSizeBytes;
    }
}
