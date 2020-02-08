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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Objects;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author Chinomso Bassey Ikwuagwu on Dec 10, 2018 8:42:29 PM
 */
public class DiskLruCacheContextImplTest {

    public static class CacheObject implements Serializable{
        private final Integer id;
        private final String name;
        public CacheObject(Integer id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = name;
        }
        public String getName() {
            return name;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheObject that = (CacheObject) o;
            return Objects.equals(id, that.id);
        }
        @Override
        public int hashCode() { return Objects.hash(id); }
        @Override
        public String toString() { return "CacheObject{id=" + id + '}'; }
    }

//    public static void main(String... args) {
//        new DiskCacheContextImplTest().test();
//    }

    @Test
    public void test() {

        final DiskLruCacheContext.FileProvider fileProvider = 
                (fname) -> Paths.get(TestDirs.CACHE, fname).toFile();

        final DiskLruCacheContext dcc = new DiskLruCacheContextImpl(fileProvider, 100_000_000);

        final String cacheName = "CACHE_FOR_com.bc.diskcacheDiskLruCacheContextImplTest";

        final String key = "one";

        try {

            final DiskLruCacheIx cache = dcc.getInstance(cacheName, true);

            cache.put(key, new CacheObject(1, "First Object"));

            cache.put(key, new CacheObject(1, "Second Object"));

            final Object firstObj = cache.getObject(key, null);
            System.out.println(firstObj);

            final Object secondObj = cache.getObject(key, null);
            System.out.println(secondObj);

        }catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
            fail(e.toString());
        }finally{
            dcc.closeAndRemoveAll();
        }
    }
}
