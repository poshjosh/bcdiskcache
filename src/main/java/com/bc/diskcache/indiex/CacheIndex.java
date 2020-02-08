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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CacheIndex<T> implements Index<T> {
    
    private transient static final Logger LOG = Logger.getLogger(CacheIndex.class.getName());

    public static interface Cache<K, V> {
        void clear() throws Exception;
        void put(K key, V val) throws Exception;
        V getOrDefault(K key, V outputIfNone) throws Exception;
    }

    public static interface Tokenizer<S, T> {
        Tokenizer NO_OP = new Tokenizer(){
            @Override
            public Object[] tokenize(Object source) {
                return new Object[]{source};
            }
        };
        T [] tokenize(S sourse);
    }

    private final int maxEntriesPerKey;

    private final Tokenizer<String, String> keyTokenizer;

    private final Cache<String, Collection<T>> cache;

    private boolean closed;

    public CacheIndex(Cache<String, Collection<T>> cache, Tokenizer<String, String> keyTokenizer, int maxEntriesPerKey) {
        this.cache = java.util.Objects.requireNonNull(cache);
        if(maxEntriesPerKey < 1) {
            throw new IllegalArgumentException("Max entries per key < 1");
        }
        this.maxEntriesPerKey = maxEntriesPerKey;
        this.keyTokenizer = java.util.Objects.requireNonNull(keyTokenizer);
    }

    @Override
    public void flush() throws IOException { }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    @Override
    public int index(String phrase, Collection<T> values) throws IndexException{

        if(phrase == null || phrase.isEmpty()) {
            return 0;
        }

        final String [] wordTokens = this.keyTokenizer.tokenize(phrase);

        return this.index(wordTokens, values);
    }

    @Override
    public int index(String [] wordTokens, Collection<T> values) throws IndexException{

        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "DiskCacheLru. To addAll: {0} = {1}",
                    new Object[]{Arrays.toString(wordTokens), values});
            
        }
        
        int added = 0;

        for(String wordToken : wordTokens) {

            added += this.indexToken(wordToken, values);
        }

        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "DiskCacheLru. Added {0} values for: {1}",
                    new Object[]{added, Arrays.toString(wordTokens)});
        }
        
        return added;
    }

    @Override
    public int indexToken(String wordToken, Collection<T> values)
            throws IndexException{

        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "DiskCacheLru. To index token: {0} = {1}", 
                    new Object[]{wordToken, values});
        }
        
        try{

            Collection<T> val = cache.getOrDefault(wordToken, null);
            if (val == null) {
                val = new LinkedHashSet(maxEntriesPerKey);
                cache.put(wordToken, val);
            }

            val.addAll(values);

            if(val.size() >= maxEntriesPerKey) {
                final int toRemove = val.size() - maxEntriesPerKey;
                int removed = 0;
                final Iterator iter = val.iterator();
                while(iter.hasNext()) {
                    iter.next();
                    iter.remove();
                    ++removed;
                    if(removed >= toRemove) {
                        break;
                    }
                }
            }

            final int added = values.size() < maxEntriesPerKey ? values.size() : maxEntriesPerKey;

            if(LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "DiskCacheLru. Indexed: {0} values for token: {1}", 
                        new Object[]{added, wordToken});
            }
            
            return added;
        }catch(Exception e) {

            throw new IndexException(e);
        }
    }

    @Override
    public Map<String, List<T>> find(String phrase, int offset, int limit)
            throws IndexException{

        final String [] wordTokens = this.keyTokenizer.tokenize(phrase);

        final Map<String, List<T>> output;

        if(wordTokens.length == 0) {
            output = Collections.EMPTY_MAP;
        }else {

            output = new LinkedHashMap<>(wordTokens.length);

            final Index.IndexConsumer<T> mapCollector = new MapCollector<>(output, maxEntriesPerKey);
            final Index.IndexConsumer<T> rangeCollector = new RangeCollector<>(mapCollector, offset, limit);

            int collected = 0;

            for(String wordToken : wordTokens) {

                final int toFind = limit - collected;

                if(toFind < 1) {
                    break;
                }

                collected += this.findToken(wordToken, rangeCollector, 0, toFind);
            }
        }

        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "For: {0}, offset: {1}, {2}\nFound: {3}",
                    new Object[]{phrase, offset, limit, output});
        }

        return output;
    }

    @Override
    public int find(String phrase, Index.IndexConsumer<T> collector, int offset, int limit)
            throws IndexException {

        final String [] wordTokens = this.keyTokenizer.tokenize(phrase);

        return this.find(wordTokens, collector, offset, limit);
    }

    @Override
    public int find(String [] wordTokens, Index.IndexConsumer<T> collector, int offset, int limit) throws IndexException {

        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "DiskCacheLru. To search for: {0}, limit: {1}",
                    new Object[]{Arrays.toString(wordTokens), limit});
        }
        
        int collected = 0;

        final Index.IndexConsumer<T> rangeCollector = new RangeCollector<>(collector, offset, limit);

        for(String wordToken : wordTokens) {

            final int toFind = limit - collected;

            if(toFind < 1) {
                break;
            }

            collected += this.findToken(wordToken, rangeCollector, 0, toFind);
        }

        if(LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "DiskCacheLru. Found {0} results, after searching for: {1}, limit: {2}",
                    new Object[]{collected, Arrays.toString(wordTokens), limit});
        }    
        return collected;
    }

    @Override
    public int findToken(String wordToken, Index.IndexConsumer<T> collector, int offset, int limit)
            throws IndexException {

        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "DiskCacheLru. To search for token: {0}, offset: {1}, limit: {2}",
                    new Object[]{wordToken, offset, limit});
        }
        
        int collected = 0;
        try {

            final Collection<T> current = (Collection<T>)cache.getOrDefault(wordToken, Collections.EMPTY_SET);
            if(LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "DiskCacheLru. For token {0} found: {1}", 
                        new Object[]{wordToken, current});
            }
            for(T e : current) {
                if(limit > -1 && collected >= limit) {
                    break;
                }
                if(collected >= offset) {
                    if(collector.accept(wordToken, e)) {
                        ++collected;
                    }
                }
            }
        }catch(Exception e) {
            throw new IndexException(e);
        }

        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, 
                    "DiskCacheLru. Found {0} results, after searching for token: {1}, offset: {2}, limit: {3}",
                    new Object[]{collected, wordToken, offset, limit});
        }
        
        return collected;
    }
}
