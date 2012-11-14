package com.nexr;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.context.ContextConfiguration;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {HelloWorld.class} )
@ContextConfiguration(locations = { "classpath:/spring/test-context.xml" })
public class TestWithPowerMock extends TestCase {
    
    @Test
    public void testSystemEnv() {
        
        PowerMock.mockStaticPartial(System.class, "getenv");
        
        String thisHostName = "cnode1";
        String HADOOP_MASTER_NODES = "localhost " + thisHostName + " cnode2";
        String ZOOKEEPER_NODES = "localhost cnode2" + " " + thisHostName;
        
        EasyMock.expect(System.getenv("HADOOP_MASTER_NODES")).andReturn(HADOOP_MASTER_NODES).anyTimes();
        EasyMock.expect(System.getenv("ZOOKEEPER_NODES")).andReturn(ZOOKEEPER_NODES).anyTimes();
        EasyMock.expect(System.getenv("NotExistKey")).andReturn(null).anyTimes();
        
        PowerMock.replayAll();
        HelloWorld hw = new HelloWorld();
        assertEquals(HADOOP_MASTER_NODES, hw.getenv("HADOOP_MASTER_NODES"));
        assertEquals(ZOOKEEPER_NODES, hw.getenv("ZOOKEEPER_NODES"));
        assertNull(hw.getenv("NotExistKey"));
        
    }
}
