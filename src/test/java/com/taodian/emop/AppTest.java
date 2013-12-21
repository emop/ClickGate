package com.taodian.emop;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.taodian.route.DefaultRouter.CLI;
import com.taodian.route.RouteException;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    /**
     * Rigourous Test :-)
     * @throws RouteException 
     */
    public void testRouteCommnadLineParse() throws RouteException
    {
    	MocRouter r = new MocRouter();
    	r.initRoute();
    	CLI c = r.parseCommandLineTest("route -A cpc -user_id 1 -j reject");
    	assertEquals("chain name", "cpc", c.chainName);
    	assertEquals("user id", 1, c.rule.sourceUserId);
    	assertEquals("action", "reject", c.rule.targetAction);
    	assertEquals("expire", 0, c.rule.expired);
    	
    	CLI c2 = r.parseCommandLineTest("route -A cpc -user_id 1 -j reject -expire 40min");
    	assertTrue("action", c2.rule.expired > System.currentTimeMillis() + 1000 * 60 * 39);

    	CLI c3 = r.parseCommandLineTest("route -A cpc -user_id 1 -j reject -expire 40mins");
    	assertTrue("action", c3.rule.expired > System.currentTimeMillis() + 1000 * 60 * 39);

    	
    	CLI c4 = r.parseCommandLineTest("route -D cpc -shop_id 1 -num_iid 2 -user_id 3 -j reject -expire 40mins");
    	assertEquals("chain name", "cpc", c4.chainName);
    	assertEquals("shop_id", 1, c4.rule.sourceShopId);
    	assertEquals("num_iid", 2, c4.rule.sourceNumIid);
    	assertEquals("user_id", 3, c4.rule.sourceUserId);

    	CLI c5 = r.parseCommandLineTest("route -D cpc -shop_id 1 -num_iid 2 -user_id 3 -j forward -next no_money -expire 40mins");
    	assertEquals("action", "forward", c5.rule.targetAction);
    	assertEquals("next", "no_money", c5.rule.nextUrl);	    	

    }
    
}
