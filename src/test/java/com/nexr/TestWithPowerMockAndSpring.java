package com.nexr;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/test-context.xml" })
@PrepareForTest({ HelloWorld.class, StaticClass.class, System.class })
public class TestWithPowerMockAndSpring extends TestCase {
    
    /**
     * this is important to enable static powermock with spring.
     * @see http://www.jayway.com/2010/12/28/using-powermock-with-spring-integration-testing/
     * @see http://code.google.com/p/powermock/wiki/PowerMockAgent#Eager_loading_of_the_Agent_in_Maven
     */
    @Rule
    public PowerMockRule rule = new PowerMockRule();
    
    @Autowired
    private HelloWorld hw;
    
    @Test
    public void testHelloWorldStaticMethod() {
        
        PowerMock.mockStaticPartial(HelloWorld.class, "getenv");
        
        String thisHostName = "cnode1";
        String HADOOP_MASTER_NODES = "localhost " + thisHostName + " cnode2";
        String ZOOKEEPER_NODES = "localhost cnode2" + " " + thisHostName;
        
        EasyMock.expect(HelloWorld.getenv("HADOOP_MASTER_NODES")).andReturn(HADOOP_MASTER_NODES).anyTimes();
        EasyMock.expect(HelloWorld.getenv("ZOOKEEPER_NODES")).andReturn(ZOOKEEPER_NODES).anyTimes();
        EasyMock.expect(HelloWorld.getenv("NotExistKey")).andReturn(null).anyTimes();

        PowerMock.replayAll();
        assertEquals(HADOOP_MASTER_NODES, hw.getenv("HADOOP_MASTER_NODES"));
        assertEquals(ZOOKEEPER_NODES, hw.getenv("ZOOKEEPER_NODES"));
        assertNull(hw.getenv("NotExistKey"));
        
    }
    
    @Test
    public void testStaticClass() {
        
        PowerMock.mockStaticPartial(StaticClass.class, "getenv");
        
        String thisHostName = "cnode1";
        String HADOOP_MASTER_NODES = "localhost " + thisHostName + " cnode2";
        String ZOOKEEPER_NODES = "localhost cnode2" + " " + thisHostName;
        
        EasyMock.expect(StaticClass.getenv("HADOOP_MASTER_NODES")).andReturn(HADOOP_MASTER_NODES).anyTimes();
        EasyMock.expect(StaticClass.getenv("ZOOKEEPER_NODES")).andReturn(ZOOKEEPER_NODES).anyTimes();
        EasyMock.expect(StaticClass.getenv("NotExistKey")).andReturn(null).anyTimes();
        
        PowerMock.replayAll();
        
        assertEquals(HADOOP_MASTER_NODES, hw.getenvStaticClass("HADOOP_MASTER_NODES"));
        assertEquals(ZOOKEEPER_NODES, hw.getenvStaticClass("ZOOKEEPER_NODES"));
        assertNull(hw.getenvStaticClass("NotExistKey"));
        
    }
    
    @Test
    public void testSystemClass() {
        
        PowerMock.mockStaticPartial(System.class, "getenv");
        
        String thisHostName = "cnode1";
        String HADOOP_MASTER_NODES = "localhost " + thisHostName + " cnode2";
        String ZOOKEEPER_NODES = "localhost cnode2" + " " + thisHostName;
        
        EasyMock.expect(System.getenv("HADOOP_MASTER_NODES")).andReturn(HADOOP_MASTER_NODES).anyTimes();
        EasyMock.expect(System.getenv("ZOOKEEPER_NODES")).andReturn(ZOOKEEPER_NODES).anyTimes();
        EasyMock.expect(System.getenv("NotExistKey")).andReturn(null).anyTimes();
        
        PowerMock.replayAll();
        
        assertEquals(HADOOP_MASTER_NODES, hw.getenvStaticClass("HADOOP_MASTER_NODES"));
        assertEquals(ZOOKEEPER_NODES, hw.getenvStaticClass("ZOOKEEPER_NODES"));
        assertNull(hw.getenvStaticClass("NotExistKey"));
        
    }
}
