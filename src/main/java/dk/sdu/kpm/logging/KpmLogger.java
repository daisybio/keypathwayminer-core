package dk.sdu.kpm.logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.*;

/**
 * Common logger class for all of KPM.
 * Created by Martin on 31-10-2014.
 */
public class KpmLogger {

//    private static volatile boolean handlersAdded = false;
//    private static volatile Date logDate;
//    static private FileHandler fileHandler;

    public static void log(Level logLevel, String message){
//        try {
//            setup();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        System.out.println(logLevel.getName() + ": " + message);
    }

    public static void log(Level logLevel, Exception e) {
        log(logLevel, (Throwable) e);
    }

    public static void log(Level logLevel, Throwable e){
        String detail = e.getClass().getName() + ": " + e.getMessage();
        for( final StackTraceElement s : e.getStackTrace() )
            detail += "\n\t" + s.toString();
        while( ( e = e.getCause() ) != null ) {
            detail += "\nCaused by: ";
            for( final StackTraceElement s : e.getStackTrace() )
                detail += "\n\t" + s.toString();
        }

        log(logLevel, detail);
    }

//    static synchronized private void setup() throws IOException {
//
//        File logDir = new File(".\\logs");
//
//        if(!logDir.exists() || !logDir.isDirectory()){
//            if(!logDir.mkdir()){
//                System.out.println("Logging directory could not be created.");
//            }
//        }
//
//        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
//        logger.setLevel(Level.INFO);
//
//        if(logDate == null || getDatePart(logDate).compareTo(getDatePart(new Date())) < 0) {
//            if(handlersAdded){
//                logger.removeHandler(fileHandler);
//                handlersAdded = false;
//            }
//
//            String formattedTime = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
//            fileHandler = new FileHandler(String.format("%s\\log_%s.txt", logDir.getCanonicalPath(), formattedTime), true);
//
//            KpmLogFormatter logFormatter = new KpmLogFormatter();
//            fileHandler.setFormatter(logFormatter);
//            logDate = new Date();
//        }
//
//        if(!handlersAdded){
//            logger.addHandler(fileHandler);
//            handlersAdded = true;
//        }
//
//    }
//
//    public static Date getDatePart(Date date) {
//        Calendar calendar = Calendar.getInstance();
//
//        calendar.setTime( date );
//        calendar.set(Calendar.HOUR_OF_DAY, 0);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MILLISECOND, 0);
//
//        return calendar.getTime();
//    }

}
