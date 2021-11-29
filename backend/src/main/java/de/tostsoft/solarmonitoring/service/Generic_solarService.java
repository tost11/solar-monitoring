package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.Connection;
import de.tostsoft.solarmonitoring.module.Generic_solar;
import de.tostsoft.solarmonitoring.repository.Generic_solarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;

@Service
public class Generic_solarService implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(Generic_solarService.class);
    private Thread thread;
    @Autowired
    private Connection connection;

    private Generic_solar lastTestData;
    public Generic_solar addTestSolar(){
        if(lastTestData==null){
            lastTestData= new Generic_solar(20,2,40,12.f,230.f,1000.f,12.f,25.f,51.f,51.f,61.f,6.f,16,1,6,15,new Date().getTime(),443.f);
        }
        else {
            float value = (float) (3 * Math.random());
            if(Math.random()>0.5){
                value = value *-1;
            }
            value = lastTestData.getSolarVoltage()+value;
            if (value<0){
                value=0;
            }
            if (value>40){
                value=40;
            }
            lastTestData.setSolarVoltage(value);
            lastTestData.setTimestamp(new Date().getTime());
            lastTestData.setSolarWatt(lastTestData.getSolarVoltage()*lastTestData.getSolarAmpere());
        }
        return lastTestData;
    }



    @Override
    public void run(String...args) throws Exception {
        LOG.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        for (String arg : args) {
         if(arg.equals("debug")){
             LOG.info("Runnig in deBugMode");
             thread = new Thread(() -> {
                 while (true) {
                     try {
                         connection.newPoint(addTestSolar());
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                     try {
                         Thread.sleep(5000);
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                 }
             });
             thread.run();

         }
        }
    }
}
