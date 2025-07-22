package de.tostsoft.solarmonitoring.testlib;

import java.time.Instant;

public class Waiter {

    public interface DoWaitStuff{
        boolean isDone();
    }

    public static void waitToHappen(DoWaitStuff toWaitFor,int millisecondsToWait){

        var end = Instant.now().plusMillis(millisecondsToWait);

        do{
            boolean isDone = toWaitFor.isDone();
            if(isDone){
                return;
            }
            try {
                Thread.sleep(100);
            }catch (InterruptedException e){
            }
        }while(Instant.now().isBefore(end));

        throw new RuntimeException("What waited for did not happen!");
    }
}
