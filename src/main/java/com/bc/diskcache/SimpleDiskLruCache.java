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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapted from https://github.com/fhucho/simple-disk-cache
 * @author Chinomso Bassey Ikwuagwu on Oct 4, 2018 12:41:18 PM
 */
public class SimpleDiskLruCache implements DiskLruCacheIx {

    private transient static final Logger LOG = Logger.getLogger(SimpleDiskLruCache.class.getName());

    private static final int VALUE_COUNT = 2;
    private static final int VALUE_IDX = 0;
    private static final int METADATA_IDX = 1;
    private static final Set<File> USED_DIRS = new LinkedHashSet<>();
    
    private final Lock editorLock = new ReentrantLock();

    private com.jakewharton.disklrucache.DiskLruCache diskLruCache;
    private final int mAppVersion;

    private SimpleDiskLruCache(File dir, int appVersion, long maxSizeBytes) throws IOException {
        mAppVersion = appVersion;
        diskLruCache = com.jakewharton.disklrucache.DiskLruCache.open(dir, appVersion, VALUE_COUNT, maxSizeBytes);
    }

    public static DiskLruCacheIx open(File dir, int appVersion, long maxSizeBytes)
            throws IOException {
        if (USED_DIRS.contains(dir)) {
            throw new IllegalStateException("Cache dir " + dir.getAbsolutePath() + " was used before.");
        }

        USED_DIRS.add(dir);

        return new SimpleDiskLruCache(dir, appVersion, maxSizeBytes);
    }
    
    public static boolean containsCacheDir(File dir) {
        return USED_DIRS.contains(dir);
    }
    
    public static boolean removeCacheDir(File dir) {
        return USED_DIRS.remove(dir);
    }
    
    public static Set<File> getCacheDirs() {
        return Collections.unmodifiableSet(USED_DIRS);
    }

    @Override
    public void flush() throws IOException {
        diskLruCache.flush();
    }

    @Override
    public boolean isClosed() {
        return diskLruCache.isClosed();
    }

    @Override
    public void close() throws IOException {
        diskLruCache.close();
    }

    @Override
    public void clear() throws IOException {
        final File dir = diskLruCache.getDirectory();
        final long maxSize = diskLruCache.getMaxSize();
        diskLruCache.delete();
        diskLruCache = com.jakewharton.disklrucache.DiskLruCache.open(dir, mAppVersion, VALUE_COUNT, maxSize);
    }

    @Override
    public File getDirectory() {
        return diskLruCache.getDirectory();
    }

    @Override
    public synchronized long getSize() {
        return diskLruCache.size();
    }

    @Override
    public synchronized long getMaxSize() {
        return diskLruCache.getMaxSize();
    }

    @Override
    public synchronized boolean remove(String key) throws IOException {
        try{
            editorLock.lock();
            return diskLruCache.remove(this.toInternalKey(key));
        }finally{
            editorLock.unlock();
        }
    }

    @Override
    public void delete() throws IOException {
        diskLruCache.delete();
    }

    public com.jakewharton.disklrucache.DiskLruCache getCache() {
        return diskLruCache;
    }

    @Override
    public SnapshotEntry<InputStream> getStreamEntry(
            String key, SnapshotEntry<InputStream> outputIfNone) throws IOException {
        com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot = diskLruCache.get(toInternalKey(key));
        if (snapshot == null) {
            return outputIfNone;
        }
        return new InputStreamEntry(snapshot, readMetadata(snapshot));
    }

    @Override
    public SnapshotEntry<String> getStringEntry(String key, SnapshotEntry<String> outputIfNone) throws IOException {
        com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot = diskLruCache.get(toInternalKey(key));
        if (snapshot == null) {
            return outputIfNone;
        }
        try {
            return new SnapshotEntryImpl(snapshot.getString(VALUE_IDX), readMetadata(snapshot));
        } finally {
            snapshot.close();
        }
    }

    @Override
    public SnapshotEntry<ObjectInputStream> getObjectStreamEntry(
            String key, SnapshotEntry<ObjectInputStream> outputIfNone) throws IOException {
        com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot = diskLruCache.get(toInternalKey(key));
        if (snapshot == null) {
            return outputIfNone;
        }
        return new ObjectInputStreamEntry(snapshot, readMetadata(snapshot));
    }


    @Override
    public Object getObject(String key, Object outputIfNone) throws IOException, ClassNotFoundException {
        SnapshotEntry<ObjectInputStream> snapshotEntry = this.getObjectStreamEntry(key, null);
        if(snapshotEntry == null) {
            return outputIfNone;
        }
        final ObjectInputStream ois = snapshotEntry.getData();
        try{
            return ois.readObject();
        }finally{
            snapshotEntry.close();
        }
    }

    @Override
    public String getString(String key, String outputIfNone) throws IOException {
        SnapshotEntry<String> snapshotEntry = this.getStringEntry(key, null);
        if (snapshotEntry == null) {
            return outputIfNone;
        }
        try {
            final String result = snapshotEntry.getData();
            return result;
        } finally {
            snapshotEntry.close();
        }
    }

    @Override
    public boolean contains(String key) throws IOException {
        com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot = diskLruCache.get(toInternalKey(key));
        if(snapshot==null) {
            return false;
        }else{
            snapshot.close();
            return true;
        }
    }

    @Override
    public void put(String key, InputStream is) throws IOException {
        put(key, is, Collections.EMPTY_MAP);
    }

    @Override
    public void put(String key, InputStream is, Map<String, Serializable> annotations)
            throws IOException {
        this.remove(key);
        this.putIfNone(key, is, annotations);
    }

    @Override
    public void put(String key, String value) throws IOException {
        put(key, value, Collections.EMPTY_MAP);
    }

    @Override
    public void put(String key, Object value) throws IOException {
        put(key, value, Collections.EMPTY_MAP);
    }

    @Override
    public void put(String key, String value, Map<String, ? extends Serializable> annotations)
            throws IOException {
        this.remove(key);
        this.putIfNone(key, value, annotations);
    }

    @Override
    public void put(String key, Object value, Map<String, ? extends Serializable> annotations)
            throws IOException {
        this.remove(key);
        this.putIfNone(key, value, annotations);
    }

    @Override
    public void putIfNone(String key, InputStream is) throws IOException {
        putIfNone(key, is, Collections.EMPTY_MAP);
    }

    @Override
    public void putIfNone(String key, InputStream is, Map<String, Serializable> annotations)
            throws IOException {
        try{
            editorLock.lock();
            final OutputStream os = openStream(key, annotations);
            try {
                this.copy(is, os);
            }finally {
                os.close();
            }
        }finally{
            editorLock.unlock();
        }
    }

    @Override
    public void putIfNone(String key, String value) throws IOException {
        putIfNone(key, value, Collections.EMPTY_MAP);
    }

    @Override
    public void putIfNone(String key, Object value) throws IOException {
        putIfNone(key, value, Collections.EMPTY_MAP);
    }

    @Override
    public void putIfNone(String key, String value, Map<String, ? extends Serializable> annotations)
            throws IOException {
        try{
            editorLock.lock();
            final OutputStream cos = openStream(key, annotations);
            try {
                cos.write(value.getBytes());
            }finally {
                cos.close();
            }
        }finally{
            editorLock.unlock();
        }
    }

    @Override
    public void putIfNone(String key, Object value, Map<String, ? extends Serializable> annotations)
            throws IOException {
        try{
            editorLock.lock();
            final CacheObjectOutputStream cos = openObjectStream(key, annotations);
            try {
                cos.writeObject(value);
            }finally{
                cos.close();
            }
        }finally{
            editorLock.unlock();
        }
    }
    
    public OutputStream openStream(String key) throws IOException {
        return openStream(key, Collections.EMPTY_MAP);
    }

    public OutputStream openStream(String key, Map<String, ? extends Serializable> metadata)
            throws IOException, ConcurrentModificationException {
        com.jakewharton.disklrucache.DiskLruCache.Editor editor = diskLruCache.edit(toInternalKey(key));
        if(editor == null) {
            throw new ConcurrentModificationException("Could not acquire an editor, probably due to an ongoing edit");
        }
        try {
            writeMetadata(metadata, editor);
            BufferedOutputStream bos = new BufferedOutputStream(editor.newOutputStream(VALUE_IDX));
            return new CacheOutputStream(bos, editor);
        } catch (IOException e) {
            editor.abort();
            throw e;
        }
    }

    private CacheObjectOutputStream openObjectStream(String key, Map<String, ? extends Serializable> metadata)
            throws IOException, ConcurrentModificationException {
        com.jakewharton.disklrucache.DiskLruCache.Editor editor = diskLruCache.edit(toInternalKey(key));
        if(editor == null) {
            throw new ConcurrentModificationException("Could not acquire an editor, probably due to an ongoing edit");
        }
        try {
            writeMetadata(metadata, editor);
            ObjectOutputStream bos = new ObjectOutputStream(new BufferedOutputStream(editor.newOutputStream(VALUE_IDX)));
            return new CacheObjectOutputStream(bos, editor);
        } catch (IOException e) {
            editor.abort();
            throw e;
        }
    }

    private void writeMetadata(Map<String, ? extends Serializable> metadata,
                               com.jakewharton.disklrucache.DiskLruCache.Editor editor) throws IOException {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(
                    editor.newOutputStream(METADATA_IDX)));
            oos.writeObject(metadata);
        } finally {
            this.closeQuietly(oos);
        }
    }

    private Map<String, Serializable> readMetadata(com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot)
            throws IOException {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(
                    snapshot.getInputStream(METADATA_IDX)));
            @SuppressWarnings("unchecked")
            Map<String, Serializable> annotations = (Map<String, Serializable>) ois.readObject();
            return annotations;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            this.closeQuietly(ois);
        }
    }

    /**
     * Copies bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * </p>
     * <p>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     * </p>
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    private int copy(final InputStream input, final OutputStream output) throws IOException {
        final long count = copyLarge(input, output, new byte[8192]);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    /**
     * Copies bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    private long copyLarge(final InputStream input, final OutputStream output, final byte[] buffer)
            throws IOException {
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private void closeQuietly(Closeable c) {
        if(c != null) {
            try{
                c.close();
            }catch(IOException e) {
                LOG.log(Level.WARNING, "Unexpected exception closing instance of " +
                        c.getClass().getName(), e);
            }
        }
    }

    private String toInternalKey(String key) {
        return md5(key);
    }

    private String md5(String input) {
        try {
            final MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(input.getBytes("UTF-8"));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            final String result =  bigInt.toString(16);
//            System.out.println(" INPUT: " + input + "\nOUTPUT: " + result);
            return result;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    private class CacheObjectOutputStream extends CacheOutputStream {

        private final ObjectOutputStream os;

        public CacheObjectOutputStream(ObjectOutputStream os, com.jakewharton.disklrucache.DiskLruCache.Editor editor) {
            super(os, editor);
            this.os = os;
        }

        public final void writeObject(Object obj) throws IOException {
            try {
                this.os.writeObject(obj);
            } catch (IOException e) {
                this.setFailed(true);
                throw e;
            }
        }
    }

    private class CacheOutputStream extends FilterOutputStream {

        private final com.jakewharton.disklrucache.DiskLruCache.Editor editor;
        private boolean failed = false;

        private CacheOutputStream(OutputStream os, com.jakewharton.disklrucache.DiskLruCache.Editor editor) {
            super(os);
            this.editor = editor;
        }

        @Override
        public void close() throws IOException {
            IOException closeException = null;
            try {
                super.close();
            } catch (IOException e) {
                closeException = e;
            }

            if (failed) {
                editor.abort();
            } else {
                editor.commit();
            }

            if (closeException != null) throw closeException;
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(int oneByte) throws IOException {
            try {
                super.write(oneByte);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            try {
                super.write(buffer);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            try {
                super.write(buffer, offset, length);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        protected void setFailed(boolean b) {
            this.failed = b;
        }
    }

    private class InputStreamEntry implements SnapshotEntry<InputStream> {
        private final com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot;
        private final Map<String, Serializable> metadata;

        public InputStreamEntry(com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot, Map<String, Serializable> metadata) {
            this.metadata = metadata;
            this.snapshot = snapshot;
        }
        @Override
        public InputStream getData() {
            return snapshot.getInputStream(VALUE_IDX);
        }
        @Override
        public Map<String, Serializable> getMetadata() {
            return metadata;
        }
        @Override
        public void close() {
            snapshot.close();
        }
    }

    private class SnapshotEntryImpl<T> implements SnapshotEntry<T> {
        private final T data;
        private final Map<String, Serializable> metadata;

        public SnapshotEntryImpl(T data, Map<String, Serializable> metadata) {
            this.data = data;
            this.metadata = metadata;
        }
        @Override
        public T getData() {
            return data;
        }
        @Override
        public Map<String, Serializable> getMetadata() {
            return metadata;
        }
        @Override
        public void close() { }
    }

    private class ObjectInputStreamEntry implements SnapshotEntry<ObjectInputStream> {
        private final com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot;
        private final Map<String, Serializable> metadata;

        public ObjectInputStreamEntry(com.jakewharton.disklrucache.DiskLruCache.Snapshot snapshot, Map<String, Serializable> metadata) {
            this.metadata = metadata;
            this.snapshot = snapshot;
        }
        @Override
        public ObjectInputStream getData() {
            try {
                return new ObjectInputStream(snapshot.getInputStream(VALUE_IDX));
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public Map<String, Serializable> getMetadata() {
            return metadata;
        }
        @Override
        public void close() {
            snapshot.close();
        }
    }

}