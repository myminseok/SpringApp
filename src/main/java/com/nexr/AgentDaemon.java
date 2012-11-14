package com.nexr;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * ndap agent daemon
 * 
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.VariableNamingConventions", "PMD.ShortVariable",
        "PMD.InsufficientStringBuilderDeclaration", "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.SignatureDeclareThrowsException", "PMD.ExcessiveMethodLength", "PMD.AvoidCatchingGenericException",
        "PMD.DoNotCallSystemExit" })
public class AgentDaemon extends Daemon {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentDaemon.class);
    
    private static final String SPRING_ROOT_CONTEXT_PATH = "classpath:/spring/root-context.xml";
    
    private static final String STOP_REQUEST_FILENAME = "temp_requesting_stop_AgentDaemon";
    
    private static Daemon daemon = null;
    
    // ------------------------------------------------------------------->
    // Start Main() Helper Static Methods
    // ------------------------------------------------------------------->
    /**
     * This method returns true if the command line arguments are valid, and false otherwise.
     * 
     * Please change this method to meet your implementation's requirements.
     */
    private static boolean checkCommandLineArguments(String[] args) {
        boolean ok = true;
        ok = args.length == 1;
        return ok;
    }
    
    /**
     * This prints to STDERR (a common practice), the command line usage of the program.
     * 
     * Please change this to meet your implementation's command line arguments.
     */
    private static void printUsage() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nUsage: java ");
        sb.append(Daemon.class.getName());
        sb.append(" {start|stop}");
        sb.append("\n\n");
        logger.error(sb.toString());
    }
    
    /**
     * I usually like the Batch and Daemon Processes or Utilities to print a small Banner at the top of their output.
     * 
     * Please change this to suit your needs.
     */
    private static void printWelcome() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n*********************************************\n");
        sb.append("*       NDAP Agent Daemon        *\n");
        sb.append("*********************************************\n\n");
        logger.info(sb.toString());
    }
    
    /**
     * This method simple prints the process startup time. I found this to be very useful in batch job logs. I probably
     * wouldn't change it, but you can if you really need to.
     */
    private static void printStartupTime() {
        StringBuilder sb = new StringBuilder();
        sb.append("Startup Time: ");
        sb.append(Daemon.getTimeStamp());
        sb.append("\n\n");
    }
    
    private static ApplicationContext springContext = null;
    
    @Override
    protected synchronized void customProcessInit() {
        logger.debug("######### loading spring context...");
        
        daemon.deleteStopRequestFile();
        
        if (springContext != null) {
            logger.debug("already loaded spring");
            return;
        }
        springContext = new ClassPathXmlApplicationContext(SPRING_ROOT_CONTEXT_PATH);
        logger.info("loaded spring context");
    }
    
    @Override
    protected synchronized void customProcessCleanup() throws Exception {
        logger.info("shuddown spring");
        daemon.deleteStopRequestFile();
    }
    
    private static int start() {
        int exitCode;
        try {
            printWelcome();
            printStartupTime();
            daemon = new AgentDaemon();
            
            // I don't believe cleanup exceptions
            // area really fatal, but that's up to you...
            daemon.setTreatCleanupExceptionsAsFatal(false);
            
            // Load properties using the file way.
            // daemon.loadProperties(args[0]);
            
            // Set process loop sleep seconds
            // daemon.setProcessLoopSleepSecond(Integer.parseInt(args[1]));
            daemon.setProcessLoopSleepSecond(7);
            
            // Set the stop file watcher file path
            daemon.setStopFilePath(STOP_REQUEST_FILENAME);
            
            // Set the stop file watcher sleep seconds
            daemon.setStopFileWatcherSleepSeconds(5);
            
            // Performance daemon Initialization,
            // again I don't like over use of the constructor.
            daemon.init();
            
            // Do the actually business logic execution!
            // If we made it to this point without an exception, that means
            // we are successful, the daemon exit code should be ZERO for
            // SUCCESS!
            // daemon.startProcessingLoop();
            daemon.startProcessingLoopFaked();
            
            // Star the Stop File Watcher!
            // It is not enabled automatically
            // to make this template more flexible
            // if you want to embedded it in a larger component
            daemon.startStopFileWatcher();
            // daemon.startStopFileWatcherFaked();
            
            daemon.addShutdownHook(); // Just in case we get an interrupt
                                      // signal...
            
            // Wait while the execution loop is running!
            daemon.waitWhileExecuting();
            
            exitCode = 0;
        } // End try block
        catch (Exception e) {
            exitCode = 1; // If there was an exception, the daemon exit code
                          // should
            // be NON-ZERO for FAILURE!
            
            e.printStackTrace(); // Log the exception, if you have an Exception
            // email utility like I do, use that instead.
        } finally {
            if (daemon != null) {
                try {
                    daemon.stopStopFileWatcher(); // Just in case stop file
                                                  // watcher
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                try {
                    daemon.stopProcessingLoop(); // Just in case stop processing
                                                 // loop
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                try {
                    // Technically we don't need to do this because
                    // of the shutdown hook
                    // But I like to be explicit here to show when during a
                    // normal execution, when the call
                    // to cleanup should happen.
                    daemon.cleanup();
                } catch (Exception e) {
                    // We shouldn't receive an exception
                    // But in case there is a runtime exception
                    // Just print it, but treat it as non-fatal.
                    // Technically most if not all resources
                    // will be reclaimed by the operating system as an
                    // absolute last resort
                    // so we did our best attempt at cleaning things up,
                    // but we don't want to wake our developers or our
                    // production services team
                    // up at 3 in the morning because something weird
                    // happened during cleanup.
                    e.printStackTrace();
                    
                    // If we set the daemon to treat cleanup exception as fatal
                    // the exit code will be set to 1...
                    if (daemon != null && daemon.isTreatCleanupExceptionsAsFatal()) {
                        exitCode = 1;
                    }
                }
            }
        } // End finally block
        
        return exitCode;
    }
    
    private static int stop() {
        return stopByFile() | stopByPid();
        // return stopByPid();
    }
    
    /**
     * @return
     */
    private static int stopByFile() {
        int exitCode = 0;
        try {
            // create stop file
            File file = new File(STOP_REQUEST_FILENAME);
            file.createNewFile();
            exitCode = 0;
        } // End try block
        catch (Exception e) {
            exitCode = 1; // If there was an exception, the daemon exit code
                          // should
            // be NON-ZERO for FAILURE!
            
            e.printStackTrace(); // Log the exception, if you have an Exception
            // email utility like I do, use that instead.
        }
        
        return exitCode;
    }
    
    private static boolean isDaemonProcess(String psDisplayName) {
        boolean startedByJar = psDisplayName.indexOf("ndap-agent-") >= 0 && psDisplayName.indexOf("start") >= 0 ? true
                : false;
        boolean startedByClass = psDisplayName.indexOf("AgentDaemon start") >= 0 ? true : false;
        return startedByJar || startedByClass;
    }
    
    private static int stopByPid() {
        
        int exitCode = 0;
        try {
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            for (VirtualMachineDescriptor desc : vms) {
                logger.debug(desc.toString());
                if (isDaemonProcess(desc.displayName())) {
                    logger.info(desc.toString());
                    // String killCommand = "kill -KILL "+desc.id();
                    String killCommand = "kill -SIGTERM " + desc.id();
                    logger.info(killCommand);
                    Runtime.getRuntime().exec(killCommand);
                }
                
            }
        } // End try block
        catch (Exception e) {
            exitCode = 1; // If there was an exception, the daemon exit code
                          // should
            // be NON-ZERO for FAILURE!
            
            e.printStackTrace(); // Log the exception, if you have an Exception
            // email utility like I do, use that instead.
        }
        
        return exitCode;
    }
    
    // Start Main() Method
    // ------------------------------------------------------------------->
    /**
     * Here's your standard main() method which allows you to start a Java program from the command line. You can
     * probably use this as is, once you rename the DoNothingStandaloneProcess class name to a proper name to represent
     * your implementation correctly.
     * 
     * MAKE SURE: To change the data type of the process object reference to the name of your process implementation
     * class. Other than that, you are good to go with this main method!
     */
    public static void main(String[] args) {
        
        int exitCode = 0;
        logger.info(args[0]);
        if (checkCommandLineArguments(args)) {
            String command = args[0];
            if ("start".equals(command)) {
                exitCode = AgentDaemon.start();
            } else if ("stop".equals(command)) {
                exitCode = AgentDaemon.stop();
            } else {
                logger.warn("Unknown command: " + command);
                printUsage();
                exitCode = 1;
            }
            
        } else {
            printUsage();
            exitCode = 1;
        }
        // Make sure our standard streams are flushed
        // so we don't miss anything in the logs.
        System.out.flush();
        System.err.flush();
        logger.info("Daemon Exit Code = " + exitCode);
        System.out.flush();
        
        // Make sure to return the exit code to the parent process
        System.exit(exitCode);
        
    }
    // ------------------------------------------------------------------->
    
}
