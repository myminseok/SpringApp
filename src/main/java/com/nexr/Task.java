package com.nexr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Task {

    private final Logger logger = LoggerFactory.getLogger(Task.class);
    @Autowired
    private HelloWorld hello;

    /**
     * Update the hosts list every 10 minutes
     */
    @Transactional
    @Scheduled(fixedDelay =  20 * 1000)
    public void update()  {
        logger.info("@@@@@@ run Task");
        hello.printHello();
    }

}
