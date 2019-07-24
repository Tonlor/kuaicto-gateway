package com.kuaicto.gateway.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;

public class LRUMapCache<T> {
    
    private final Object lock = new Object();
    
    /**
     * 默认缓存过期时间: 1min
     */
    private static final long default_expires = 1 * 60 * 1000;
    private static final int default_max_size = 1000;
    
    private final LRUMap cache;
    private long expires;
    private int maxSize;

    /**
     * 
     * @param expires key的有效期，单位毫秒
     * @param maxSize 最大数量
     */
    public LRUMapCache(long expires, int maxSize) {
        super();
        this.expires = expires > 0 ? expires : default_expires;
        this.maxSize = maxSize > 0 ? maxSize: default_max_size;
        this.cache = new LRUMap(this.maxSize);
    }
    
    public Map<String, T> getCacheMap() {
        
        long now = System.currentTimeMillis();
        Map<String, T> result = new HashMap<String, T>();
        
        MapIterator mapIterator = this.cache.mapIterator();
        while(mapIterator.hasNext()) {
            mapIterator.next();
            Object key = mapIterator.getKey();
            Object value = mapIterator.getValue();
            
            @SuppressWarnings("unchecked")
            CacheData<T> cached = (CacheData<T>)value;
            if (cached == null || now - cached.ts > expires) {
                continue;
            }
            
            result.put((String)key , cached.data);
        }
        
        return result;
    }

    /**
     * 设置新的数据缓存
     * @param key
     * @param value
     * @return 
     */
    public void put(String key, T value) {
        synchronized (lock) { // use lock because LRUMap is not thread-safe
            cache.put(key, new CacheData<T>(value, System.currentTimeMillis()));
        }
    }
    public T get(String key) {
        @SuppressWarnings("unchecked")
        CacheData<T> cached = (CacheData<T>)cache.get(key);
        
        long now = System.currentTimeMillis();
        if (cached == null || now - cached.ts > expires) {
            return null;
        }
        return cached.data;
    }

    public T get(String key, DataSource<T> dataSource) {
        T cacheData = this.get(key);
        if (cacheData == null) {
            synchronized (lock) {
                cacheData = this.get(key);
                if (cacheData == null) {
                    cacheData = dataSource.loadData();
                    this.put(key, cacheData);
                }
            }
        }
        
        return cacheData;
    }

    public long getExpires() {
        return expires;
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    public static class CacheData<T> {
        public CacheData(T data, long ts) {
            super();
            this.data = data;
            this.ts = ts;
        }
        T data;
        long ts;
    }
    
    public static interface DataSource<T> {
        T loadData();
    }
    
    public static void main(String[] args) throws InterruptedException {
        LRUMapCache<String> cache = new LRUMapCache<String>(1000, 2);
        cache.put("a1", "11");
        System.out.println(cache.get("a1"));
        
        cache.put("a1", "12");
        System.out.println(cache.get("a1"));

        cache.put("a2", "22");
        System.out.println(cache.get("a2"));
        
        System.out.println("sleeping");
        Thread.sleep(500);
        System.out.println(cache.get("a1"));
        System.out.println(cache.get("a2"));

        System.out.println(cache.get("a1", new DataSource<String>(){
            @Override
            public String loadData() {
                return "a1-3344";
            }
        }));

        System.out.println(cache.get("a2", new DataSource<String>(){
            @Override
            public String loadData() {
                return "a2-3344";
            }
        }));
    }
}
