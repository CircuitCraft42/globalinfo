/*
 * Copyright (C) 2019 Nathan Poje
 * 
 * This file is part of GlobalInfo.
 * 
 * GlobalInfo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package com.github.circuitcraft42.globalinfo;

import java.lang.invoke.*;
import java.util.*;

/**
 * Hello world!
 *
 */
public class RoutingMap extends AbstractMap<String,Object> {
	
    private static class HandleSource implements Source<Object> {
        private MethodHandle handle;
        public HandleSource(MethodHandle handle) {
            this.handle = handle;
        }

        @Override
        public Object get() {
            try {
                return handle.invoke();
            } catch(Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public static interface Source<T> {
        public T get();

        public static Source<Object> fromHandle(MethodHandle handle) {
            return new HandleSource(handle);
        }
        
    }
    
    public static class MultiSource<T> implements Source<T> {
    	
    	private Map<String, Source<T>> mappings = new HashMap<>();
    	private String selection;

		@Override
		public T get() {
			return mappings.get(selection).get();
		}
		
		public MultiSource<T> register(String name, Source<T> source) {
			mappings.put(name, source);
			return this;
		}
		
		public MultiSource<T> select(String name) {
			if(selection != null)
				throw new IllegalStateException("cannot select a source multiple times");
			Objects.requireNonNull(name, "cannot select null");
			selection = name;
			return this;
		}
    	
    }

    private Map<String, Source<Object>> mappings = new HashMap<>();

    public RoutingMap() {}

    @Override
    public boolean containsKey(Object key) {
        return mappings.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return mappings.get(key).get();
    }

    @Override
    public Set<Map.Entry<String,Object>> entrySet() {
        return new SourceSet<String,Object>(mappings.entrySet());
    }

    @SuppressWarnings("unchecked")
    public boolean register(String name, Source<?> source) {
        if(mappings.containsKey(name)) {
            return false;
        }

        mappings.put(name, (Source<Object>)source);
        return true;
    }

    private class SourceSet<K,V> extends AbstractSet<Map.Entry<K,V>> {
        private Collection<Map.Entry<K,Source<V>>> source;
        public SourceSet(Collection<Map.Entry<K,Source<V>>> source) {
            this.source = source;
        }
        @Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return new Itr(source.iterator());
        }
        @Override
        public int size() {
            return mappings.size();
        }
        private class Itr implements Iterator<Map.Entry<K,V>> {
            private Iterator<Map.Entry<K,Source<V>>> itr;
            public Itr(Iterator<Map.Entry<K,Source<V>>> itr) {
                this.itr = itr;
            }
            @Override
            public Map.Entry<K,V> next() {
                Map.Entry<K,Source<V>> entry = itr.next();
                return new AbstractMap.SimpleImmutableEntry<K,V>(entry.getKey(), entry.getValue().get());
            }
            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }
        }
    }
}
