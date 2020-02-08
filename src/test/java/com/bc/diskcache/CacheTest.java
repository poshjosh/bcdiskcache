package com.bc.diskcache;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 10, 2018 5:41:51 PM
 */
public class CacheTest {
    
    private static class NamedObject implements Serializable {
        private final String name;
        public NamedObject(String name) {
            this.name = Objects.requireNonNull(name);
        }
        @Override
        public String toString() {
            return "NamedObject{"+name+'}';
        }
    }
    
    @Test
    public void test3() {
        try{
            test(new NamedObject("Object_1"));
        }catch(Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    public void test(Object... values) throws Exception {
        final File dir = Paths.get(TestDirs.CACHE, "CACHE_FOR_"+this.getClass().getName() + "_1").toFile();
        
        final int appVersion = 1;
        final long maxSizeBytes = 10_000;
        try(final DiskLruCacheIx cache = SimpleDiskLruCache.open(dir, appVersion, maxSizeBytes)) {
        
            int index = 0;
            
            for(Object value : values) {
                
                final String name = "Name_" + (index++);

                cache.put(name, value);

                cache.flush();

                System.out.println(cache.getObject(name, null));

                cache.put(name, new NamedObject(name + "Jiiiiii"));
                
                cache.flush();

                System.out.println(cache.getObject(name, null));

                System.out.println(cache.getObject(name, null));

                System.out.println("contains("+name+") = " + cache.contains(name) + 
                        ", size: " + cache.getSize());
                
                cache.flush();

                System.out.println("contains("+name+") = " + cache.contains(name) + 
                        ", size: " + cache.getSize());
            }

            System.out.println("BEFORE CLEAR Size: " + cache.getSize());

            cache.clear();

            System.out.println(" AFTER CLEAR Size: " + cache.getSize());
        }
    }

    @Test
    public void test2() throws Exception {
        final File dir = Paths.get(TestDirs.CACHE, "CACHE_FOR_"+this.getClass().getName() + "_2").toFile();
        final int appVersion = 1;
        final long maxSizeBytes = 10_000;
        try(final DiskLruCacheIx cache = SimpleDiskLruCache.open(dir, appVersion, maxSizeBytes)) {
        
            final String name = "Nonso";
            
            cache.put(name, "Object_1");

            cache.flush();

            System.out.println(cache.getString(name, null));

            cache.put(name, "Object_2");

            cache.flush();

            System.out.println(cache.getString(name, null));

            System.out.println(cache.getString(name, null));

            System.out.println("contains("+name+") = " + cache.contains(name) + 
                    ", size: " + cache.getSize());

            System.out.println("BEFORE CLEAR Size: " + cache.getSize());

            cache.clear();

            System.out.println(" AFTER CLEAR Size: " + cache.getSize());
        }
    }

    @Test
    public void test1() throws Exception {
        final File dir = Paths.get(TestDirs.CACHE, "CACHE_FOR_"+this.getClass().getName() + "_3").toFile();
        final int appVersion = 1;
        final long maxSizeBytes = 10_000;
        try(final DiskLruCacheIx cache = SimpleDiskLruCache.open(dir, appVersion, maxSizeBytes)) {
        
            final String name = "Nonso";

            cache.put(name, new NamedObject("Object_1"));

            cache.flush();

            System.out.println(cache.getObject(name, null));

            cache.put(name, new NamedObject("Object_2"));

            cache.flush();

            System.out.println(cache.getObject(name, null));

            System.out.println(cache.getObject(name, null));

            System.out.println("contains("+name+") = " + cache.contains(name) + 
                    ", size: " + cache.getSize());

            System.out.println("BEFORE CLEAR Size: " + cache.getSize());

            cache.clear();

            System.out.println(" AFTER CLEAR Size: " + cache.getSize());
        }
    }
}
