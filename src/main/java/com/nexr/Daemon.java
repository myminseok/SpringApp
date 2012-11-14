/*
 * Copyright 2012 Robert C. Ilardi Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * Created Aug 19, 2012
 */
package com.nexr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert C. Ilardi
 * 
 *         This is a Sample Class for a Standalone *Daemon* Process. Implementations that use this template may be run
 *         from a scheduler such as Cron or Autosys or as Manual Utility Processes using the UNIX Command NOHUP.
 * 
 *         IMPORTANT: This Java Process is intended to be ran with NOHUP.
 * 
 *         I have released this code under the Apache 2.0 Open Source License. Please feel free to use this as a
 *         template for your own Daemons or Utility Process Implementations.
 * 
 *         Finally, as you will notice I used STDOUT AND STDERR for all logging. This is for simplicity of the template.
 *         You can use Log4J or Java Logging or any other log library you prefer. In my professional experience, I also
 *         include an Exception or "Throwable" emailer mechanism so that our development team receives all exceptions
 *         from any process even front-ends in real time.
 * 
 */
@SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel", "PMD.AvoidCatchingGenericException", "PMD.EmptyCatchBlock",
        "PMD.SignatureDeclareThrowsException", "PMD.DoNotUseThreads", "PMD.NPathComplexity",
        "PMD.CyclomaticComplexity", "PMD.AvoidThreadGroup" })
public class Daemon {
    
    private final Logger logger = LoggerFactory.getLogger(Daemon.class);
    
    /*
     * I personally like having a single property file for the configuration of all my batch jobs and utilities. In my
     * professional projects, I actually have a more complex method of properties management, where all properties are
     * stored in a database table, and I have something called a Resource Bundle and Resource Helper facility to manage
     * it.
     * 
     * My blog at EnterpriseProgrammer.com has more information on properties and connection management using this
     * concept.
     * 
     * However for demonstration purposes I am using a simple Properties object to manage all configuration data for the
     * Standalone Process Template. Feel free to replace this field with a more advanced configuration management
     * mechanism that means your needs.
     */
    private Properties appProps;
    
    /*
     * This flag ensures that the Cleanup method only runs once. This is because I wanted to have a shutdown hook, in
     * case the process receives an interrupt signal and in the main method, I explicitly call cleanup() from the
     * finally block. Technically the shutdown hook based on my implementation is only a backup so it actually will
     * never run unless there's a situation like an interrupt signal.
     */
    private boolean ranCleanup = false;
    
    /*
     * If this variable is set to true, any exception caused in the cleanup routine will cause the entire process to
     * exit non-zero.
     * 
     * However in my professional experience, we usually just want to log these exceptions, perhaps even email them to
     * the team for investigation later, and allow the process to exit ZERO, so that the batch job scheduler can
     * continue onto the next job, especially is the real execution is completed.
     */
    private boolean treatCleanupExceptionsAsFatal = false;
    
    /*
     * We need a object monitor to control the background thread used to run the execution loop.
     */
    private final Object loopControlLock = new Object();
    
    /*
     * A flag with tells the start and stop methods if the execution loop thread has started or not.
     */
    private boolean loopStarted;
    
    /*
     * This flag tells the start, stop, and waitWhileExecution methods if the process loop is running. It is also used
     * to STOP the process loop from running.
     */
    private boolean runProcessing = false;
    
    /*
     * This parameter needs to be set in order for the process loop to sleep a certain number of seconds between each
     * consecutive call to the actual processing logic method.
     */
    private int processLoopSleepSecs;
    
    /*
     * This field is used as a counter for the number of processing loop iterations. For debugging, logging, and even
     * custom logic implementation purposes, this is a nice piece of information to have.
     */
    private long loopIterationCnt;
    
    /*
     * This is the file path for the stop file watcher to watch. When the stop file watcher thread finds the stop file
     * at this location, it will gracefully shutdown the daemon process.
     */
    private String stopFilePath;
    
    /*
     * We don't want to spend too many cycles watching for a stop file especially since a daemon process normally runs
     * for hours, days, or even weeks, so we have a separate sleep seconds variable to control the interval between file
     * system checks.
     */
    private int stopFileSleepSecs;
    
    /*
     * This flag tells the start, stop file watcher methods if the file watcher loop is running.
     */
    private boolean runStopFileWatcher;
    
    /*
     * We need a object monitor to control the background thread used to run the stop file watcher loop.
     */
    private final Object stopFileWatcherControlLock = new Object();
    
    /*
     * A flag with tells the start and stop methods if the stop file watcher loop thread has started or not.
     */
    private boolean stopFileWarcherLoopStarted;
    
    // Start public methods that shouldn't be customized by the user
    // ------------------------------------------------------------------->
    
    /**
     * The init method wraps two user customizable methods: 1. readProperties(); - Use this to add reads from the
     * appProps object. 2. customProcessInit() - Use this to customize your process before the execution logic runs.
     * 
     * As stated previously, so not touch these methods, they are simple wrappers around the methods you should
     * customize instead and provide what in my professional experience are good log messages for batch jobs or
     * utilities to print out, such as the execution timing information. This is especially useful for long running
     * jobs. You can eventually take average over the course of many runs of the batch job, and then you will know when
     * your batch job is behaving badly, when it's taking too long to finish execution.
     */
    public synchronized void init() {
        long start, end, total;
        
        logger.debug("Initialization at: " + getTimeStamp());
        start = System.currentTimeMillis();
        
        readProperties(); // Hook to the user's read properties method.
        customProcessInit(); // Hook to the user's custom process init method!
        
        end = System.currentTimeMillis();
        total = end - start;
        
        logger.debug("Initialization Completed at: " + getTimeStamp());
        logger.debug("Total Init Execution Time: " + compactHumanReadableTimeWithMs(total));
    }
    
    /**
     * Because we aren't using a more advanced mechanism for properties management, I have included this method to allow
     * the main() method to set the path to the main properties file used by the batch jobs.
     * 
     * In my professional versions of this template, this method is embedded in the init() method which basically will
     * initialize the Resource Helper component and obtain the properties from the configuration tables instead.
     * 
     * Again you shouldn't touch this method's implementation, instead use readProperties() to customize what you do
     * with the properties after the properties load.
     */
    public void loadProperties(String appPropsPath) throws IOException {
        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(appPropsPath);
            appProps = new Properties();
            appProps.load(fis);
        } // End try block
        finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    /**
     * This method sets the number of seconds the process loop will sleep between each call to the logic processing
     * method.
     * 
     * @param processLoopSleepSecs
     */
    public void setProcessLoopSleepSecond(int processLoopSleepSecs) {
        this.processLoopSleepSecs = processLoopSleepSecs;
    }
    
    /**
     * This method sets the number of seconds between each stop file check by the stop file watcher.
     * 
     * @param stopFileSleepSecs
     */
    public void setStopFileWatcherSleepSeconds(int stopFileSleepSecs) {
        this.stopFileSleepSecs = stopFileSleepSecs;
    }
    
    /**
     * This method sets the file for the stop file watcher to loop for.
     * 
     * @param stopFilePath
     */
    public void setStopFilePath(String stopFilePath) {
        this.stopFilePath = stopFilePath;
    }
    
    /**
     * This method performs the cleanup of any JDBC connections, files, sockets, and other resources that your execution
     * process or your initialization process may have opened or created.
     * 
     * Once again do not touch this method directly, instead put your cleanup code in the customProcessCleanup() method.
     * 
     * This method is called automatically in the last finally block of the main method, and if there's an interrupt
     * signal or other fatal issue where somehow the finally block didn't get called the Runtime shutdown hook will
     * invoke this method on System.exit...
     * 
     * @throws Exception
     */
    public synchronized void cleanup() throws Exception {
        long start, end, total;
        
        // This prevents cleanup from running more than onces.
        if (ranCleanup) {
            return;
        }
        
        try {
            logger.info("Starting Cleanup at: " + getTimeStamp());
            start = System.currentTimeMillis();
            
            stopStopFileWatcher(); // Make sure the stop file watcher is stopped!
            
            stopProcessingLoop(); // Make sure the processing loop is stopped!
            
            customProcessCleanup(); // Hook to the users Process Cleanup Method
            
            end = System.currentTimeMillis();
            total = end - start;
            
            logger.debug("Cleanup Completed at: " + getTimeStamp());
            logger.debug("Total Cleanup Execution Time: " + compactHumanReadableTimeWithMs(total));
            
            ranCleanup = true;
        } // End try block
        catch (Exception e) {
            /*
             * It is in my experience that the Operating System will cleanup anything we have "forgotten" to clean up.
             * Therefore I do not want to waste my production support team members time at 3AM in the morning to handle
             * "why did a database connection not close" It will close eventually, since it is just a socket, and even
             * if it doesn't we'll catch this in other jobs which may fail due to the database running out of
             * connections.
             * 
             * However I usually have these exceptions emailed to our development team for investigation the next day.
             * For demo purposes I did not include my Exception/Stacktrace Emailing utility, however I encourage you to
             * add your own.
             * 
             * If you really need the process to exit non-ZERO because of the cleanup failing, set the
             * treatCleanupExceptionsAsFatal to true.
             */
            e.printStackTrace();
            
            if (treatCleanupExceptionsAsFatal) {
                throw e;
            }
        }
    }
    
    public void startStopFileWatcher() throws InterruptedException {
        Thread t;
        
        synchronized (stopFileWatcherControlLock) {
            if (runStopFileWatcher) {
                return;
            }
            
            stopFileWarcherLoopStarted = false;
            runStopFileWatcher = true;
            
            logger.info("Starting Stop File Watcher at: " + getTimeStamp());
            
            t = new Thread(stopFileWatcherRunner);
            t.start();
            
            while (!stopFileWarcherLoopStarted) {
                stopFileWatcherControlLock.wait();
            }
        }
        
        logger.info("Stop File Watcher Thread Started Running at: " + getTimeStamp());
    }
    
    public void startStopFileWatcherFaked() throws InterruptedException {
        
        synchronized (stopFileWatcherControlLock) {
            if (runStopFileWatcher) {
                return;
            }
            
            stopFileWarcherLoopStarted = true;
            runStopFileWatcher = true;
        }
    }
    
    public void stopStopFileWatcher() throws InterruptedException {
        synchronized (stopFileWatcherControlLock) {
            if (!stopFileWarcherLoopStarted || !runStopFileWatcher) {
                return;
            }
            
            logger.info("Requesting Stop File Watcher Stop at: " + getTimeStamp());
            
            runStopFileWatcher = false;
            
            while (stopFileWarcherLoopStarted) {
                stopFileWatcherControlLock.wait();
            }
            
            logger.info("Stop File Watcher Stop Request Completed at: " + getTimeStamp());
        }
    }
    
    /**
     * This method is used to start the processing loop's thread.
     * 
     * Again like the other methods in this section of the class, do not modify this method directly.
     * 
     * @throws InterruptedException
     * 
     * @throws Exception
     */
    public void startProcessingLoop() throws InterruptedException {
        Thread t;
        
        synchronized (loopControlLock) {
            if (runProcessing) {
                return;
            }
            
            loopStarted = false;
            runProcessing = true;
            ranCleanup = false;
            
            logger.info("@@Starting Processing Loop at: " + getTimeStamp());
            
            t = new Thread(executionLoopRunner);
            t.start();
            
            while (!loopStarted) {
                loopControlLock.wait();
            }
        }
        
        logger.info("Execution Processing Loop Thread Started Running at: " + getTimeStamp());
    }
    
    public void startProcessingLoopFaked() throws InterruptedException {
        
        synchronized (loopControlLock) {
            if (runProcessing) {
                return;
            }
            
            loopStarted = false;
            runProcessing = true;
            ranCleanup = false;
        }
    }
    
    /**
     * This method is used to stop or actually "request to stop" the processing loop thread.
     * 
     * It waits while the processing loop is running.
     * 
     * @throws InterruptedException
     */
    public void stopProcessingLoop() throws InterruptedException {
        synchronized (loopControlLock) {
            if (!runProcessing) {
                return;
            }
            
            logger.info("Requesting Execution Loop Stop at: " + getTimeStamp());
            
            runProcessing = false;
            
            while (loopStarted) {
                loopControlLock.wait();
            }
            
            logger.info("Execution Loop Stop Request Completed at: " + getTimeStamp());
        }
    }
    
    /**
     * This method will wait while the processing loop is running. Yes, I know we can use Thread.join(), however, what
     * if you want to embedded this class in some other larger component, then you might not want to use the join method
     * directly. I personally like this implementation better, it tells me exactly what I'm waiting on.
     * 
     * @throws InterruptedException
     */
    public void waitWhileExecuting() throws InterruptedException {
        synchronized (loopControlLock) {
            // while (loopStarted) {
            while (this.runProcessing) {
                loopControlLock.wait(1000);
            }
        }
    }
    
    /**
     * This is the runnable implementation as an anon inner class which contains the actual execution loop of the
     * Daemon. This execution loop is what really separates the Daemon Process from the Standalone Process batch
     * template. While the Standalone Process template was meant for processes which run a task and then exit once
     * completed. This implementation is method to keep on running for extended periods of time, re-executing the custom
     * processing logic over and over again after some sleep period.
     */
    private final Runnable executionLoopRunner = new Runnable() {
        public void run() {
            try {
                synchronized (loopControlLock) {
                    loopStarted = true;
                    loopControlLock.notifyAll();
                }
                
                logger.debug("Executing Loop Thread Running!");
                
                while (runProcessing) {
                    // Hook to the User's Custom Execute Processing
                    // Method! - Where the magic happens!
                    customExecuteProcessing();
                    
                    loopIterationCnt++;
                    
                    // Sleep between execution cycles
                    try {
                        for (int i = 1; runProcessing && i <= processLoopSleepSecs; i++) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                    }
                } // End while runProcessing loop
            } // End try block
            catch (Exception e) {
                e.printStackTrace();
            } finally {
                logger.debug("!!!!!!!!!!Execution Processing Loop Exit at: " + getTimeStamp());
                
                synchronized (loopControlLock) {
                    runProcessing = false;
                    loopStarted = false;
                    loopControlLock.notifyAll();
                }
            }
        }
    };
    
    /**
     * This is the runnable implementation as an anon inner class which contains the Stop File Watcher loop. A Stop File
     * Watcher is simply a standard file watcher, except when it finds the target file, it will execute the daemon
     * shutdown routine. This is a form of inter-process communication via the file system to enable a separate process
     * or even a simple script to control (or at least stop) the daemon process when it's running under NOHUP. You can
     * simple create a script which creates an empty file using the unix TOUCH command.
     */
    private final Runnable stopFileWatcherRunner = new Runnable() {
        public void run() {
            File f;
            
            try {
                synchronized (stopFileWatcherControlLock) {
                    stopFileWarcherLoopStarted = true;
                    stopFileWatcherControlLock.notifyAll();
                }
                
                logger.debug("Stop File Watcher Thread Running!");
                
                f = new File(stopFilePath);
                
                while (runStopFileWatcher) {
                    logger.debug("checking Stop File " + stopFilePath);
                    
                    // If we find the stop file
                    // stop the processing loop
                    // and exit this thread as well.
                    if (f.exists()) {
                        logger.debug("@@@@ Stop File: '" + stopFilePath + "'  Found at: " + getTimeStamp());
                        stopProcessingLoop();
                        deleteStopRequestFile();
                        break;
                    }
                    
                    // Sleep between file existence checks
                    try {
                        for (int i = 1; runStopFileWatcher && i <= stopFileSleepSecs; i++) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                    }
                } // End while runStopFileWatcher loop
            } // End try block
            catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (stopFileWatcherControlLock) {
                    runStopFileWatcher = false;
                    stopFileWarcherLoopStarted = false;
                    stopFileWatcherControlLock.notifyAll();
                }
            }
        }
    };
    
    protected void deleteStopRequestFile() {
        File f = new File(stopFilePath);
        
        try {
            if (f.exists()) {
                f.delete();
                logger.info("deleted file:" + stopFilePath);
            }
        } catch (Exception e) {
        }
        
    }
    
    /**
     * This is the method that adds the shutdown hook.
     * 
     * All this method does it property invokes the Runtime.getRuntime().addShutdownHook(Thread t); method by adding an
     * anonymous class implementation of a thread.
     * 
     * This thread's run method simply calls the Process's cleanup method.
     * 
     * Whenever I create a class like this, I envision it being ran two ways, either directly from the main() method or
     * as part of a larger component, which may wrap this entire class (A HAS_A OOP relationship).
     * 
     * In the case of the wrapper, adding the shutdown hook might be optional since the wrapper may want to handle
     * shutdown on it's own.
     * 
     */
    public synchronized void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    cleanup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * This method is only provided in case you are loading properties from an input stream or other non-standard source
     * that is not a File.
     * 
     * It becomes very useful in the wrapper class situation I described in the comments about the addShutdownHook
     * method.
     * 
     * Perhaps the wrapping process reads properties from a Database or a URL?
     * 
     * @param appProps
     */
    public void setAppProperties(Properties appProps) {
        this.appProps = appProps;
    }
    
    /**
     * Used to detect which mode the cleanup exceptions are handled in.
     * 
     * @return
     */
    public boolean isTreatCleanupExceptionsAsFatal() {
        return treatCleanupExceptionsAsFatal;
    }
    
    /**
     * Use this method to set if you want to treat cleanup exception as fatal. The default, and my personal preference
     * is not to make these exception fatal. But I added the flexibility into the template for your usage.
     * 
     * @param treatCleanupExceptionsAsFatal
     */
    public void setTreatCleanupExceptionsAsFatal(boolean treatCleanupExceptionsAsFatal) {
        this.treatCleanupExceptionsAsFatal = treatCleanupExceptionsAsFatal;
    }
    
    // ------------------------------------------------------------------->
    // Start methods that need to be customized by the user
    // ------------------------------------------------------------------->
    /**
     * In general for performance reasons and for clarity even above performance, I like pre-caching the properties as
     * Strings or parsed Integers, etc, before running any real business logic.
     * 
     * This is why I provide the hook to readProperties which should read properties from the appProps field (member
     * variable).
     * 
     * If you don't want to pre-cache your property values you can leave this method blank. However I believe it's a
     * good practice especially if your batch process is a high speed ETL Loader process where every millisecond counts
     * when loading millions of records.
     */
    private synchronized void readProperties() {
        logger.debug("Add Your Property Reads Here!");
    }
    
    /**
     * After the properties are read from the readProperties() method this method is called.
     * 
     * It is provided for the user to add custom initialization processing.
     * 
     * Let's say you want to open all JDBC connections at the start of a process, this is probably the right place to do
     * so.
     * 
     * For more complex implementations, this is the best place to create and initialize all your sub-components of your
     * process.
     * 
     * Let's say you have a DbConnectionPool, a Country Code Mapping utility, an Address Fuzzy Logic Matching library.
     * 
     * This is where I would initialize these components.
     * 
     * The idea is to fail-fast in your batch processes, you don't want to wait until you processed 10,000 records
     * before some logic statement is triggered to lazy instantiate these components, and because of a network issue or
     * a configuration mistake you get a fatal exception and your process exists, and your data is only partially loaded
     * and you or your production support team members have to debug not only the process but debug the portion of the
     * data already loaded make it in ok. This is extremely important if your batch process interacts is real-time
     * system components such as message publishers, maybe you started publishing the updated records to downstream
     * consumers?
     * 
     * Fail-Fast my friends... And as soon as the process starts if possible!
     */
    protected synchronized void customProcessInit() {
        logger.debug("Add Custom Initialization Logic Here!");
    }
    
    /**
     * This is where you would add your custom cleanup processing. If you open and connections, files, sockets, etc and
     * keep references to these objects/resources opened as fields in your class which is a good idea in some cases
     * especially long running batch processes you need a hook to be able to close these resources before the process
     * exits.
     * 
     * This is where that type of logic should be placed.
     * 
     * Now you can throw any exception you like, however the cleanup wrapper method will simply log these exceptions,
     * the idea here is that, even though cleanup is extremely important, the next step of the process is a System.exit
     * and the operating system will most-likely reclaim any resources such as files and sockets which have been left
     * opened, after some bit of time.
     * 
     * Now my preference is usually not to wake my production support guys up because a database connection (on the
     * extremely rare occasion) didn't close correctly. The process still ran successfully at this point, so just exit
     * and log it.
     * 
     * However if you really need to make the cleanup be truly fatal to the process you will have to set
     * treatCleanupExceptionsAsFatal to true.
     * 
     * @throws Exception
     */
    protected synchronized void customProcessCleanup() throws Exception {
        logger.debug("Add Custom Cleanup Logic Here!");
    }
    
    protected synchronized void customExecuteProcessing() throws Exception {
        logger.info("Loop Iteration Count = " + loopIterationCnt + " - Add Custom Processing Logic Here!");
        
        // Uncomment for testing if you want to see the behavior...
        // if (loopIterationCnt == 5) {
        // throw new Exception(
        // "Testing what happens if an exception gets thrown here!");
        // }
    }
    
    // ------------------------------------------------------------------->
    /*
     * Start String Utility Methods These are methods I have in my custom "StringUtils.java" class I extracted them and
     * embedded them in this class for demonstration purposes.
     * 
     * I encourage everyone to build up their own set of useful String Utility Functions please feel free to add these
     * to your own set if you need them.
     */
    // ------------------------------------------------------------------->
    /**
     * This will return a string that is a human readable time sentence. It is the "compact" version because instead of
     * having leading ZERO Days, Hours, Minutes, Seconds, it will only start the sentence with the first non-zero time
     * unit.
     * 
     * In my string utils I have a non-compact version as well that prints the leading zero time units.
     * 
     * All depends on how you need to presented in your logs.
     */
    public static String compactHumanReadableTimeWithMs(long milliSeconds) {
        long days, hours, inpSecs, leftOverMs;
        int minutes, seconds;
        StringBuffer sb = new StringBuffer();
        
        inpSecs = milliSeconds / 1000; // Convert Milliseconds into Seconds
        days = inpSecs / 86400;
        hours = (inpSecs - (days * 86400)) / 3600;
        minutes = (int) (((inpSecs - (days * 86400)) - (hours * 3600)) / 60);
        seconds = (int) (((inpSecs - (days * 86400)) - (hours * 3600)) - (minutes * 60));
        leftOverMs = milliSeconds - (inpSecs * 1000);
        
        if (days > 0) {
            sb.append(days);
            sb.append((days == 1 ? " Day" : " Days"));
        }
        
        if (sb.length() > 0) {
            sb.append(", ");
        }
        
        if (hours > 0 || sb.length() > 0) {
            sb.append(hours);
            sb.append((hours == 1 ? " Hour" : " Hours"));
        }
        
        if (sb.length() > 0) {
            sb.append(", ");
        }
        
        if (minutes > 0 || sb.length() > 0) {
            sb.append(minutes);
            sb.append((minutes == 1 ? " Minute" : " Minutes"));
        }
        
        if (sb.length() > 0) {
            sb.append(", ");
        }
        
        if (seconds > 0 || sb.length() > 0) {
            sb.append(seconds);
            sb.append((seconds == 1 ? " Second" : " Seconds"));
        }
        
        if (sb.length() > 0) {
            sb.append(", ");
        }
        
        sb.append(leftOverMs);
        sb.append((seconds == 1 ? " Millisecond" : " Milliseconds"));
        
        return sb.toString();
    }
    
    /**
     * NVL = Null Value, in my experience, most times, we want to treat empty or whitespace only strings are NULLs
     * 
     * So this method is here to avoid a lot of if (s == null || s.trim().length() == 0) all over the place, instead you
     * will find if(isNVL(s)) instead.
     */
    public static boolean isNVL(String s) {
        return s == null || s.isEmpty();
    }
    
    /**
     * Check is "s" is a numeric value We could use Integer.praseInt and just capture the exception if it's not a
     * number, but I think that's a hack...
     * 
     * @param s
     * @return
     */
    public static boolean isNumeric(String src) {
        boolean numeric = false;
        char c;
        
        if (!isNVL(src)) {
            numeric = true;
            String s = src.trim();
            
            for (int i = 0; i < s.length(); i++) {
                c = s.charAt(i);
                
                if (i == 0 && (c == '-' || c == '+')) {
                    // Ignore signs...
                    continue;
                } else if (c < '0' || c > '9') {
                    numeric = false;
                    break;
                }
            }
        }
        
        return numeric;
    }
    
    /**
     * Simply returns a timestamp as a String.
     * 
     * @return
     */
    public static String getTimeStamp() {
        return new java.util.Date().toString();
    }
    
}