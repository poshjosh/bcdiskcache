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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

public interface DiskLruCacheIx extends Closeable {

    DiskLruCacheIx NO_OP = new DiskLruCacheIx() {
        @Override
        public void flush() { }
        @Override
        public boolean isClosed() { return false; }
        @Override
        public void close() { }
        @Override
        public void clear() { }
        @Override
        public File getDirectory() { return null; }
        @Override
        public long getSize() { return 0; }
        @Override
        public long getMaxSize() { return 0; }
        @Override
        public boolean remove(String key) { return false; }
        @Override
        public void delete() { }
        @Override
        public SnapshotEntry<InputStream> getStreamEntry(String key, SnapshotEntry<InputStream> outputIfNone) {
            return outputIfNone;
        }
        @Override
        public SnapshotEntry<String> getStringEntry(String key, SnapshotEntry<String> outputIfNone) {
            return outputIfNone;
        }
        @Override
        public SnapshotEntry<ObjectInputStream> getObjectStreamEntry(String key, SnapshotEntry<ObjectInputStream> outputIfNone) {
            return outputIfNone;
        }
        @Override
        public Object getObject(String key, Object outputIfNone) {
            return outputIfNone;
        }
        @Override
        public String getString(String key, String outputIfNone) {
            return outputIfNone;
        }
        @Override
        public boolean contains(String key) { return false; }
        @Override
        public void put(String key, InputStream is) { }
        @Override
        public void put(String key, InputStream is, Map<String, Serializable> annotations) { }
        @Override
        public void put(String key, String value) { }
        @Override
        public void put(String key, Object value) { }
        @Override
        public void put(String key, String value, Map<String, ? extends Serializable> annotations) { }
        @Override
        public void put(String key, Object value, Map<String, ? extends Serializable> annotations) { }
        @Override
        public void putIfNone(String key, InputStream is) { }
        @Override
        public void putIfNone(String key, InputStream is, Map<String, Serializable> annotations) { }
        @Override
        public void putIfNone(String key, String value) { }
        @Override
        public void putIfNone(String key, Object value) { }
        @Override
        public void putIfNone(String key, String value, Map<String, ? extends Serializable> annotations) { }
        @Override
        public void putIfNone(String key, Object value, Map<String, ? extends Serializable> annotations) { }
    };

    void flush() throws IOException;

    boolean isClosed();

    @Override
    void close() throws IOException;

    /**
     * User should be sure there are no outstanding operations.
     * @throws IOException
     */
    void clear() throws IOException;

    File getDirectory();

    long getSize();
            
    long getMaxSize();

    boolean remove(String key) throws IOException;

    void delete() throws IOException;
    
    SnapshotEntry<InputStream> getStreamEntry(
            String key, SnapshotEntry<InputStream> outputIfNone) throws IOException;

    SnapshotEntry<String> getStringEntry(String key, SnapshotEntry<String> outputIfNone) throws IOException;

    SnapshotEntry<ObjectInputStream> getObjectStreamEntry(
            String key, SnapshotEntry<ObjectInputStream> outputIfNone) throws IOException;

    Object getObject(String key, Object outputIfNone) throws IOException, ClassNotFoundException;

    String getString(String key, String outputIfNone) throws IOException;
    
    boolean contains(String key) throws IOException;

    void put(String key, InputStream is) throws IOException;

    void put(String key, InputStream is, Map<String, Serializable> annotations)
                            throws IOException;

    void put(String key, String value) throws IOException;

    void put(String key, Object value) throws IOException;

    void put(String key, String value, Map<String, ? extends Serializable> annotations)
                                    throws IOException;

    void put(String key, Object value, Map<String, ? extends Serializable> annotations)
                                            throws IOException;

    void putIfNone(String key, InputStream is) throws IOException;

    void putIfNone(String key, InputStream is, Map<String, Serializable> annotations)
                            throws IOException;

    void putIfNone(String key, String value) throws IOException;

    void putIfNone(String key, Object value) throws IOException;

    void putIfNone(String key, String value, Map<String, ? extends Serializable> annotations)
                                    throws IOException;

    void putIfNone(String key, Object value, Map<String, ? extends Serializable> annotations)
                                            throws IOException;

    public static interface SnapshotEntry<T> extends Closeable {
        T getData();
        Map<String, Serializable> getMetadata();
    }
}
