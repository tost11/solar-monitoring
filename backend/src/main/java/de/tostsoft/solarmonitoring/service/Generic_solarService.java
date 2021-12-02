package de.tostsoft.solarmonitoring.service;


import de.tostsoft.solarmonitoring.Connection;
import de.tostsoft.solarmonitoring.model.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarSystem;
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

    private float lerp (float a, float b, float f) {
        return (a * (1.0f - f)) + (b * f);
    }


    private SelfMadeSolarSystem lastTestData;
    public SelfMadeSolarSystem addTestSolar(int iteration){
        if(lastTestData==null){
            lastTestData= new SelfMadeSolarSystem (new Date().getTime(),20,2,40.f,15.f,12.f,1.33333f,16.f,15.f,15.f,12.f,2.f,24.f,15.f,430.f);
        }
        else {

            //solar data
            float value = (float) (3 * Math.random());
            if(Math.random()>0.5){
                value = value *-1;
            }
            value = lastTestData.getChargeVolt()+value;
            value = Math.min(Math.max(0,value),40);
            lastTestData.setChargeVolt(value);
            if(iteration%10==0){
                float val = lastTestData.getChargeAmpere() + (float)(Math.random()>0.5?Math.random()*0.5:Math.random()*-0.5);
                val = Math.min(Math.max(0,val),10);
                lastTestData.setDeviceTemperature(val);
            }
            lastTestData.setChargeWatt(lastTestData.getChargeVolt()*lastTestData.getChargeAmpere());

            value = lerp(10,14,0.5f+((lastTestData.getChargeWatt()-lastTestData.getTotalConsumption())/(40*2)));

            lastTestData.setBatteryVoltage(value);
            lastTestData.setConsumptionDcVoltage(value);
            value = lastTestData.getConsumptionDcAmpere() + (float)(Math.random()>0.5?Math.random()*0.25f:Math.random()*-0.25f);
            value = Math.min(Math.max(0,value),10);
            lastTestData.setConsumptionDcAmpere(value);
            lastTestData.setConsumptionDcWatt(lastTestData.getConsumptionDcAmpere()* lastTestData.getConsumptionDcVoltage());

            lastTestData.setBatteryWatt(lastTestData.getChargeWatt()-lastTestData.getConsumptionDcWatt());
            lastTestData.setBatteryAmpere(lastTestData.getBatteryWatt() / lastTestData.getBatteryAmpere());

            if(iteration % 100 == 0){
                float val = lastTestData.getBatteryTemperature() + (float)(Math.random()>0.5?Math.random():Math.random()*-1);
                val = Math.min(Math.max(-20,val),40);
                lastTestData.setBatteryTemperature(val);
            }

            lastTestData.setChargeTemperature(lastTestData.getBatteryTemperature()+ lastTestData.getChargeWatt()/200.f);

            lastTestData.setTimeStep(new Date().getTime());

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
                 int i=0;
                 while (true) {
                     try {
                         connection.newPoint(addTestSolar(i),"e18253aa-4e89-4fec-97a4-d750fe73ea10");
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                     try {
                         Thread.sleep(5000);
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                     i++;
                     if(i>100){
                         i=0;
                     }
                 }
             });
             thread.run();

         }
        }
    }

    public void addSolarData(GenericInfluxPoint solarSystem,String token) throws Exception {
        System.out.println(solarSystem);
        connection.newPoint(solarSystem,token);

    }
}
