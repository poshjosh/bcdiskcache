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
import java.util.Set;

public interface DiskLruCacheContext {

    interface FileProvider{
        File createFile(String key);
    }

    DiskLruCacheContext NO_OP = new DiskLruCacheContext() {
        @Override
        public boolean containsCacheNamed(String key) { return false; }
        @Override
        public Set<String> getCacheNames() { return Collections.EMPTY_SET; }
        @Override
        public boolean closeAndRemove(String key) { return false; }
        @Override
        public int getDefaultMaxCacheSizeBytes() {
            return 0;
        }
        @Override
        public DiskLruCacheIx getInstance(String key) {
            return DiskLruCacheIx.NO_OP;
        }
        @Override
        public DiskLruCacheIx getInstance(String key, boolean createIfNone) {
            return DiskLruCacheIx.NO_OP;
        }
        @Override
        public DiskLruCacheIx getInstance(String key, int maxSize, boolean createIfNone) {
            return DiskLruCacheIx.NO_OP;
        }
        @Override
        public int count() { return 0; }
        @Override
        public void closeAndRemoveAll() { }
        @Override
        public int computeMaxSize(String key, int outputIfNone) { return 0;  }
        @Override
        public FileProvider getFileProvider() { 
            return new FileProvider() {
                @Override
                public File createFile(String key) {
                    return new File(key);
                }
            };  
        }
    };
    
    DiskLruCacheIx getInstance(String key);

    DiskLruCacheIx getInstance(String key, boolean createIfNone) throws IOException;
    
    DiskLruCacheIx getInstance(String key, int maxSize, boolean createIfNone) throws IOException;

    int getDefaultMaxCacheSizeBytes();
    
    boolean containsCacheNamed(String key);

    Set<String> getCacheNames();
    
    int count();

    void closeAndRemoveAll();

    boolean closeAndRemove(String key);

    int computeMaxSize(String key, int outputIfNone);

    FileProvider getFileProvider();
}
