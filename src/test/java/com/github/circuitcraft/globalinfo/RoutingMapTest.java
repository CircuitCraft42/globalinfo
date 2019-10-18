package com.github.circuitcraft.globalinfo;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.circuitcraft.globalinfo.RoutingMap;

/**
 * Unit test for simple App.
 */
public class RoutingMapTest
{
    private RoutingMap route;
    private RoutingMap.MultiSource<Object> multi;
    private static final String CONST = "const";
    private static final String RAND = "rand";

    @Before
    public void setUpClass() throws NoSuchMethodException, IllegalAccessException {
        route = new RoutingMap();
        route.register(CONST,
        		RoutingMap.Source.fromHandle(MethodHandles.constant(Integer.class, 5)));
        route.register(RAND,
        		RoutingMap.Source.fromHandle(MethodHandles.lookup().findStatic(
                    Math.class,
                    "random",
                    MethodType.methodType(double.class)
                )));
        
        route.register("multi", multi = new RoutingMap.MultiSource<Object>()
        		.register("source1", () -> "value1")
        		.register("source2", () -> "value2")
        		);
    }
    
    @Test
    public void testConstant() {
        assertEquals(route.get(CONST), 5);
        assertNotEquals(route.get(RAND), route.get(RAND));
    }
    
    @Test
    public void testRandom() {
    	assertNotEquals(route.get(RAND), route.get(RAND));
    }
    
    @Test
    public void testFirst() {
    	multi.select("source1");
    	assertEquals(route.get("multi"), "value1");
    }
    
    @Test
    public void testSecond() {
    	multi.select("source2");
    	assertEquals(route.get("multi"), "value2");
    }

}
