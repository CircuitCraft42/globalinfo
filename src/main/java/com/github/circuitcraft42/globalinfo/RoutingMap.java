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
 * GlobalInfo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with GlobalInfo.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package com.github.circuitcraft42.globalinfo;

import java.lang.invoke.*;
import java.util.*;

/**
 * A Map that routes gets to registered targets.
 * @author Nathan
 *
 */
public class RoutingMap extends AbstractMap<String,Object> {
	
	/**
	 * A Source implementation that invokes a MethodHandle to retrieve the target.
	 * @author Nathan
	 *
	 */
    private static class HandleSource implements Source<Object> {
        private MethodHandle handle;
        
        /**
         * Constructs a new HandleSource using the specified handle as a target.
         * @param handle the handle to be invoked on a get.
         */
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
    
    /**
     * A Source is a target to be invoked within a RoutingMap.
     * @author Nathan
     *
     * @param <T> The type returned by the Source
     */
    public static interface Source<T> {
        /**
         * Retrieves the value connected to this Source.
         * There are no guarantees concerning determinism or thread-safety.
         * @return the retrieved value
         */
    	public T get();

        /**
         * Creates a Source that transparently invokes the specified MethodHandle.
         * @param handle the handle to be invoked on get. It should take no parameters and return the retrieved value.
         * @return the source object that will invoke the handle
         */
    	public static Source<Object> fromHandle(MethodHandle handle) {
    		Objects.requireNonNull(handle, "Cannot route to null handle");
            return new HandleSource(handle);
        }
        
    }
    
    /**
     * A Source that supports selecting one of many potential sources to retrieve from on get.
     * Potential end sources are added with register and a choice is made with select.
     * @author Nathan
     *
     * @param <T> the return type of the Source
     */
    public static class MultiSource<T> implements Source<T> {
    	
    	private Map<String, Source<T>> mappings = new HashMap<>();
    	private String selection;

		@Override
		public T get() {
			Objects.requireNonNull(selection, "a selection has not yet been made");
			return mappings.get(selection).get();
		}
		
		/**
		 * Registers a source to the MultiSource as a potential selection.
		 * @param name the name this source will be selected with
		 * @param source the source to be routed to if it is selected
		 * @return this MultiSource, for chaining
		 */
		public MultiSource<T> register(String name, Source<T> source) {
			Objects.requireNonNull(name, "null is not a valid registry name");
			if(mappings.containsKey(name))
				throw new IllegalStateException("option " + name + " already exists");
			mappings.put(name, source);
			return this;
		}
		
		/**
		 * Selects a source for routing. A source can be selected before it is
		 * added as long as it is chosen under the same name.
		 * @param name the name of the source to select
		 * @return this MultiSource, for chaining
		 */
		public MultiSource<T> select(String name) {
			if(selection != null)
				throw new IllegalStateException("cannot select a source multiple times");
			Objects.requireNonNull(name, "cannot select null");
			selection = name;
			return this;
		}
    	
    }

    private Map<String, Source<Object>> mappings = new HashMap<>();

    /**
     * Constructor
     */
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

    /**
     * Registers a Source to be invoked when the name is retrieved.
     * @param name the name to be registered under
     * @param source the Source to be routed to
     * @return whether the Source was registered successfully (false if name already exists)
     */
    @SuppressWarnings("unchecked")
    public boolean register(String name, Source<?> source) {
        if(mappings.containsKey(name)) {
            return false;
        }

        mappings.put(name, (Source<Object>)source);
        return true;
    }

    /**
     * A Set implementation, just to implement the method on AbstractMap
     * @author Nathan
     *
     * @param <K> Key type
     * @param <V> Value type
     */
    private class SourceSet<K,V> extends AbstractSet<Map.Entry<K,V>> {
    	
        private Collection<Map.Entry<K,Source<V>>> source;
        
        /**
         * Constructs a new SourceSet that iterates the given collection
         * @param source the collection to be iterated over
         */
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
        
        /**
         * The iterator returned by the SourceSet iterator method
         * @author Nathan
         *
         */
        private class Itr implements Iterator<Map.Entry<K,V>> {
        	
            private Iterator<Map.Entry<K,Source<V>>> itr;
            
            /**
             * Constructs a new Itr.
             * @param itr the iterator to forward all method calls to
             */
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
