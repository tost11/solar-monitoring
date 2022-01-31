package de.tostsoft.solarmonitoring.controller;

import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionBothDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleConsumptionInverterDTO;
import de.tostsoft.solarmonitoring.dtos.SelfMadeSolarSampleDTO;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarIfluxPoint;
import de.tostsoft.solarmonitoring.model.SolarSystemType;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.service.SolarService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solar")
public class SolarController {

    @Autowired
    SolarService solarService;
    @Autowired
    InfluxConnection influxConnection;

    @PostMapping("/data/selfmade")
    public void PostData(@RequestBody SelfMadeSolarSampleDTO solarSample, @RequestHeader String clientToken) {
        //validation
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }

        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }

        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }

        var influxPoint = SelfMadeSolarIfluxPoint.builder()
                .chargeVolt(solarSample.getChargeVoltage())
                .chargeAmpere(solarSample.getChargeAmpere())
                .chargeWatt(solarSample.getChargeWatt())
                .batteryVoltage(solarSample.getBatteryVoltage())
                .batteryAmpere(solarSample.getBatteryAmpere())
                .batteryWatt(solarSample.getBatteryWatt())
                .batteryPercentage(solarSample.getBatteryPercentage())
                .deviceTemperature(solarSample.getDeviceTemperature())
                .batteryTemperature(solarSample.getBatteryTemperature()).build();

        influxPoint.setType(SolarSystemType.SELFMADE);
        influxPoint.setTimestamp(solarSample.getTimestamp());
        influxPoint.setDuration(solarSample.getDuration());

        solarService.addSolarData(influxPoint, clientToken);
    }


    @PostMapping("/data/selfmade/consumption/device")
    public void PostData(@RequestBody SelfMadeSolarSampleConsumptionDeviceDTO solarSample, @RequestHeader String clientToken) {

        //validation
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }

        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVolt() * solarSample.getChargeAmpere());
        }

        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }

        if (solarSample.getConsumptionVoltage() == null) {
            solarSample.setConsumptionVoltage(solarSample.getBatteryVoltage());
        }

        if (solarSample.getConsumptionWatt() == null) {
            solarSample.setConsumptionWatt(solarSample.getConsumptionVoltage() * solarSample.getConsumptionAmpere());
        }


        var influxPoint = SelfMadeSolarIfluxPoint.builder()
                .chargeVolt(solarSample.getChargeVolt())
                .chargeAmpere(solarSample.getChargeAmpere())
                .chargeWatt(solarSample.getChargeWatt())
                .batteryVoltage(solarSample.getBatteryVoltage())
                .batteryAmpere(solarSample.getBatteryAmpere())
                .batteryWatt(solarSample.getBatteryWatt())
                .batteryPercentage(solarSample.getBatteryPercentage())
                .batteryTemperature(solarSample.getBatteryTemperature())
                .consumptionVoltage(solarSample.getConsumptionVoltage())
                .consumptionAmpere(solarSample.getConsumptionAmpere())
                .consumptionWatt(solarSample.getConsumptionWatt())
                .deviceTemperature(solarSample.getDeviceTemperature())
                .totalConsumption(solarSample.getConsumptionWatt()).build();

        influxPoint.setType(SolarSystemType.SELFMADE_DEVICE);
        influxPoint.setTimestamp(solarSample.getTimestamp());
        influxPoint.setDuration(solarSample.getDuration());

        solarService.addSolarData(influxPoint, clientToken);
    }


    @PostMapping("/data/selfmade/consumption/inverter")
    public void PostData(@RequestBody SelfMadeSolarSampleConsumptionInverterDTO solarSample, @RequestHeader String clientToken) {

        //validation
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }

        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVolt() * solarSample.getChargeAmpere());
        }

        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }

        if (solarSample.getConsumptionInverterVoltage() == null) {
            solarSample.setConsumptionInverterAmpere(230.f);
        }

        if (solarSample.getConsumptionInverterWatt() == null) {
            solarSample.setConsumptionInverterWatt(solarSample.getConsumptionInverterVoltage() * solarSample.getConsumptionInverterAmpere());
        }


        var influxPoint = SelfMadeSolarIfluxPoint.builder()
                .chargeVolt(solarSample.getChargeVolt())
                .chargeAmpere(solarSample.getChargeAmpere())
                .chargeWatt(solarSample.getChargeWatt())
                .batteryVoltage(solarSample.getBatteryVoltage())
                .batteryAmpere(solarSample.getBatteryAmpere())
                .batteryWatt(solarSample.getBatteryWatt())
                .batteryPercentage(solarSample.getBatteryPercentage())
                .batteryTemperature(solarSample.getBatteryTemperature())
                .consumptionInverterVoltage(solarSample.getConsumptionInverterVoltage())
                .consumptionInverterAmpere(solarSample.getConsumptionInverterAmpere())
                .consumptionInverterWatt(solarSample.getConsumptionInverterWatt())
                .inverterTemperature(solarSample.getInverterTemperature())
                .deviceTemperature(solarSample.getDeviceTemperature())
                .totalConsumption(solarSample.getConsumptionInverterWatt()).build();

        influxPoint.setType(SolarSystemType.SELFMADE_INVERTER);
        influxPoint.setTimestamp(solarSample.getTimestamp());
        influxPoint.setDuration(solarSample.getDuration());


        solarService.addSolarData(influxPoint, clientToken);
    }

    @PostMapping("/data/selfmade/consumption")
    public void PostData(@RequestBody SelfMadeSolarSampleConsumptionBothDTO solarSample, @RequestHeader String clientToken) {

        //validation
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }

        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVolt() * solarSample.getChargeAmpere());
        }

        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getChargeVolt() * solarSample.getChargeAmpere());
        }

        if (solarSample.getConsumptionVoltage() == null) {
            solarSample.setConsumptionVoltage(solarSample.getBatteryVoltage());
        }

        if (solarSample.getConsumptionWatt() == null) {
            solarSample.setConsumptionWatt(solarSample.getConsumptionAmpere() * solarSample.getConsumptionVoltage());
        }

        if (solarSample.getConsumptionInverterVoltage() == null) {
            solarSample.setConsumptionInverterAmpere(230.f);
        }

        if (solarSample.getConsumptionInverterWatt() == null) {
            solarSample.setConsumptionInverterWatt(solarSample.getConsumptionInverterVoltage() * solarSample.getConsumptionInverterAmpere());
        }


        var influxPoint = SelfMadeSolarIfluxPoint.builder()
                .chargeVolt(solarSample.getChargeVolt())
                .chargeAmpere(solarSample.getChargeAmpere())
                .chargeWatt(solarSample.getChargeWatt())
                .batteryVoltage(solarSample.getBatteryVoltage())
                .batteryAmpere(solarSample.getBatteryAmpere())
                .batteryWatt(solarSample.getBatteryWatt())
                .batteryPercentage(solarSample.getBatteryPercentage())
                .batteryTemperature(solarSample.getBatteryTemperature())
                .consumptionVoltage(solarSample.getConsumptionVoltage())
                .consumptionAmpere(solarSample.getConsumptionAmpere())
                .consumptionWatt(solarSample.getConsumptionWatt())
                .consumptionInverterVoltage(solarSample.getConsumptionInverterVoltage())
                .consumptionInverterAmpere(solarSample.getConsumptionInverterAmpere())
                .consumptionInverterWatt(solarSample.getConsumptionInverterWatt())
                .inverterTemperature(solarSample.getInverterTemperature())
                .deviceTemperature(solarSample.getDeviceTemperature())
                .totalConsumption(solarSample.getConsumptionWatt() + solarSample.getConsumptionInverterWatt()).build();

        influxPoint.setType(SolarSystemType.SELFMADE_CONSUMPTION);
        influxPoint.setTimestamp(solarSample.getTimestamp());
        influxPoint.setDuration(solarSample.getDuration());

        solarService.addSolarData(influxPoint, clientToken);
    }

}

