/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bc.diskcache;

import com.jakewharton.disklrucache.DiskLruCache;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 10, 2018 5:41:51 PM
 */
public class Main {
    
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

    public static void main(String... args) {
        try{
//            new Main().test(new NamedObject("Object_1"));
            new Main().b();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void test(Object... values) throws Exception {
        final File dir = Paths.get(System.getProperty("user.home"), "Desktop", "DELETE_ME_"+this.getClass().getName()).toFile();
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

    public void c() throws Exception {
        final File dir = Paths.get(System.getProperty("user.home"), "Desktop", "DELETE_ME_"+this.getClass().getName()).toFile();
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

    public void b() throws Exception {
        final File dir = Paths.get(System.getProperty("user.home"), "Desktop", "DELETE_ME_"+this.getClass().getName()).toFile();
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

    public void a() throws IOException {
        final File dir = Paths.get(System.getProperty("user.home"), "Desktop", "DELETE_ME_"+this.getClass().getName()).toFile();
        final int appVersion = 1;
        final int valueCount = 1;
        final long maxSize = 10_000;
        try(final DiskLruCache cache = DiskLruCache.open(dir, appVersion, valueCount, maxSize)) {
            
        }
    }
}
