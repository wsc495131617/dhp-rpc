package org.dhp.common.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentMapCounter<K> {

    protected Map<K, AtomicLong> map = new ConcurrentHashMap<>();

    public Long total() {
        Long result = 0L;
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, AtomicLong> e = (Entry<String, AtomicLong>) it.next();
            result += e.getValue().get();
        }
        return result;
    }

    public Long incrementAndGet(K key) {
        AtomicLong value = getAtomic(key);
        return value.incrementAndGet();
    }

    protected AtomicLong getAtomic(K key) {
        AtomicLong value;
        if (!map.containsKey(key)) {
            value = new AtomicLong(0);
            map.put(key, value);
        } else {
            value = map.get(key);
        }

        return map.get(key);
    }

    public void set(K key, Long value) {
        AtomicLong atomicLong = getAtomic(key);
        atomicLong.set(value);
    }

    public Long get(K key) {
        AtomicLong atomicLong = getAtomic(key);
        return atomicLong.get();
    }

    public Long decrementAndGet(K key) {
        AtomicLong atomicLong = getAtomic(key);
        return atomicLong.decrementAndGet();
    }

    public Long getAndDecrement(K key) {
        AtomicLong atomicLong = getAtomic(key);
        return atomicLong.getAndDecrement();
    }

}
