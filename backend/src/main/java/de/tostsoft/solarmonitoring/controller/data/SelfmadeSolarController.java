package de.tostsoft.solarmonitoring.controller.data;

import static de.tostsoft.solarmonitoring.controller.data.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionBothDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionDeviceDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleConsumptionInverterDTO;
import de.tostsoft.solarmonitoring.dtos.solarsystem.data.selfmade.SelfMadeSolarSampleDTO;
import de.tostsoft.solarmonitoring.model.SelfMadeSolarInfluxPoint;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/solar/data/selfmade")
public class SelfmadeSolarController {

    @Autowired
    private SolarDataConverter solarDataConverter;

    //------------------------------- selfmade -------------------------------------------------

    private void validateAndFillMissing(SelfMadeSolarSampleDTO solarSample){
        if(solarSample.getDuration() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
        }
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }
        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }
        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }
    }

    private SelfMadeSolarInfluxPoint convertToInfluxPoint(SelfMadeSolarSampleDTO solarSample,long systemId){
        var influxPoint = SelfMadeSolarInfluxPoint.builder()
            .chargeVolt(solarSample.getChargeVoltage())
            .chargeAmpere(solarSample.getChargeAmpere())
            .chargeWatt(solarSample.getChargeWatt())
            .batteryVoltage(solarSample.getBatteryVoltage())
            .batteryAmpere(solarSample.getBatteryAmpere())
            .batteryWatt(solarSample.getBatteryWatt())
            .batteryPercentage(solarSample.getBatteryPercentage())
            .deviceTemperature(solarSample.getDeviceTemperature())
            .batteryTemperature(solarSample.getBatteryTemperature())
            .build();

        setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

        return influxPoint;
    }

    @PostMapping()
    public void PostDataSelfmade(@RequestParam long systemId, @RequestBody @Valid SelfMadeSolarSampleDTO solarSample, @RequestHeader String clientToken) {
        solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.SELFMADE,(SelfMadeSolarSampleDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    @PostMapping("/mult")
    public void PostDataSelfmadeMult(@RequestParam long systemId, @RequestBody @Valid List<SelfMadeSolarSampleDTO> solarSamples, @RequestHeader String clientToken) {
        solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.SELFMADE,(SelfMadeSolarSampleDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    //----------------------------------------- device ---------------------------------------------------------

    private void validateAndFillMissing(SelfMadeSolarSampleConsumptionDeviceDTO solarSample){
        if(solarSample.getDuration() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
        }
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }

        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }
        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }
        if (solarSample.getConsumptionVoltage() == null) {
            solarSample.setConsumptionVoltage(solarSample.getBatteryVoltage());
        }
        if (solarSample.getConsumptionWatt() == null && solarSample.getConsumptionVoltage() != null && solarSample.getConsumptionAmpere() == null) {
            solarSample.setConsumptionWatt(solarSample.getConsumptionVoltage() * solarSample.getConsumptionAmpere());
        }
    }

    private SelfMadeSolarInfluxPoint convertToInfluxPoint(SelfMadeSolarSampleConsumptionDeviceDTO solarSample,long systemId){
        var influxPoint = SelfMadeSolarInfluxPoint.builder()
            .chargeVolt(solarSample.getChargeVoltage())
            .chargeAmpere(solarSample.getChargeAmpere())
            .chargeWatt(solarSample.getChargeWatt())
            .batteryVoltage(solarSample.getBatteryVoltage())
            .batteryAmpere(solarSample.getBatteryAmpere())
            .batteryWatt(solarSample.getBatteryWatt())
            .batteryPercentage(solarSample.getBatteryPercentage())
            .batteryTemperature(solarSample.getBatteryTemperature())
            .consumptionDeviceVoltage(solarSample.getConsumptionVoltage())
            .consumptionDeviceAmpere(solarSample.getConsumptionAmpere())
            .consumptionDeviceWatt(solarSample.getConsumptionWatt())
            .deviceTemperature(solarSample.getDeviceTemperature())
            .totalConsumption(solarSample.getConsumptionWatt())
            .build();

        setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

        return influxPoint;
    }

    @PostMapping("/consumption/device")
    public void PostDataDevice(@RequestParam long systemId,@RequestBody @Valid SelfMadeSolarSampleConsumptionDeviceDTO solarSample, @RequestHeader String clientToken) {
        solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.SELFMADE_DEVICE,(SelfMadeSolarSampleConsumptionDeviceDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    @PostMapping("/consumption/device/mult")
    public void PostDataDeviceMult(@RequestParam long systemId,@RequestBody @Valid List<SelfMadeSolarSampleConsumptionDeviceDTO> solarSamples, @RequestHeader String clientToken) {
        solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.SELFMADE_DEVICE,(SelfMadeSolarSampleConsumptionDeviceDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    //----------------------------------------- inverter ---------------------------------------------------------

    private void validateAndFillMissing(SelfMadeSolarSampleConsumptionInverterDTO solarSample){
        if(solarSample.getDuration() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
        }
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }
        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }
        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getBatteryVoltage() * solarSample.getBatteryAmpere());
        }
        if (solarSample.getConsumptionInverterWatt() == null && solarSample.getConsumptionInverterAmpere() != null && solarSample.getConsumptionInverterVoltage() != null) {
            solarSample.setConsumptionInverterWatt(solarSample.getConsumptionInverterVoltage() * solarSample.getConsumptionInverterAmpere());
        }
    }

    private SelfMadeSolarInfluxPoint convertToInfluxPoint(SelfMadeSolarSampleConsumptionInverterDTO solarSample,long systemId){
        var influxPoint = SelfMadeSolarInfluxPoint.builder()
            .chargeVolt(solarSample.getChargeVoltage())
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
            .inverterFrequency(solarSample.getInverterFrequency())
            .totalConsumption(solarSample.getConsumptionInverterWatt() == null ? 0 : solarSample.getConsumptionInverterWatt())
            .build();

        setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

        return influxPoint;
    }

    @PostMapping("/inverter")
    public void PostDataInverter(@RequestParam long systemId,@RequestBody @Valid SelfMadeSolarSampleConsumptionInverterDTO solarSample, @RequestHeader String clientToken) {
        solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.SELFMADE_INVERTER,(SelfMadeSolarSampleConsumptionInverterDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    @PostMapping("/inverter/mult")
    public void PostDataInverterMult(@RequestParam long systemId,@RequestBody @Valid List<SelfMadeSolarSampleConsumptionInverterDTO> solarSamples, @RequestHeader String clientToken) {
        solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.SELFMADE_INVERTER,(SelfMadeSolarSampleConsumptionInverterDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    //----------------------------------------- consumption ---------------------------------------------------------

    private void validateAndFillMissing(SelfMadeSolarSampleConsumptionBothDTO solarSample){
        if(solarSample.getDuration() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duration can not be negative");
        }
        //validation
        if (solarSample.getTimestamp() == null || solarSample.getTimestamp() <= 0) {
            solarSample.setTimestamp(new Date().getTime());
        }
        if (solarSample.getChargeWatt() == null) {
            solarSample.setChargeWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }
        if (solarSample.getBatteryWatt() == null) {
            solarSample.setBatteryWatt(solarSample.getChargeVoltage() * solarSample.getChargeAmpere());
        }
        if (solarSample.getConsumptionVoltage() == null) {
            solarSample.setConsumptionVoltage(solarSample.getBatteryVoltage());
        }
        if (solarSample.getConsumptionWatt() == null && solarSample.getConsumptionVoltage() != null && solarSample.getConsumptionAmpere() != null) {
            solarSample.setConsumptionWatt(solarSample.getConsumptionAmpere() * solarSample.getConsumptionVoltage());
        }
        if (solarSample.getConsumptionInverterWatt() == null && solarSample.getConsumptionInverterVoltage() != null && solarSample.getConsumptionInverterAmpere() != null) {
            solarSample.setConsumptionInverterWatt(solarSample.getConsumptionInverterVoltage() * solarSample.getConsumptionInverterAmpere());
        }
    }

    private SelfMadeSolarInfluxPoint convertToInfluxPoint(SelfMadeSolarSampleConsumptionBothDTO solarSample,long systemId){
        var influxPoint = SelfMadeSolarInfluxPoint.builder()
            .chargeVolt(solarSample.getChargeVoltage())
            .chargeAmpere(solarSample.getChargeAmpere())
            .chargeWatt(solarSample.getChargeWatt())
            .batteryVoltage(solarSample.getBatteryVoltage())
            .batteryAmpere(solarSample.getBatteryAmpere())
            .batteryWatt(solarSample.getBatteryWatt())
            .batteryPercentage(solarSample.getBatteryPercentage())
            .batteryTemperature(solarSample.getBatteryTemperature())
            .consumptionDeviceVoltage(solarSample.getConsumptionVoltage())
            .consumptionDeviceAmpere(solarSample.getConsumptionAmpere())
            .consumptionDeviceWatt(solarSample.getConsumptionWatt())
            .consumptionInverterVoltage(solarSample.getConsumptionInverterVoltage())
            .consumptionInverterAmpere(solarSample.getConsumptionInverterAmpere())
            .consumptionInverterWatt(solarSample.getConsumptionInverterWatt())
            .inverterTemperature(solarSample.getInverterTemperature())
            .deviceTemperature(solarSample.getDeviceTemperature())
            .totalConsumption(solarSample.getConsumptionWatt() + (solarSample.getConsumptionInverterWatt() == null ? 0 : solarSample.getConsumptionInverterWatt()))
            .inverterFrequency(solarSample.getInverterFrequency())
            .build();

        setGenericInfluxPointBaseClassAttributes(influxPoint,solarSample.getDuration(),solarSample.getTimestamp(),systemId);

        return influxPoint;
    }

    @PostMapping("/consumption")
    public void PostDataConsumption(@RequestParam long systemId,@RequestBody @Valid SelfMadeSolarSampleConsumptionBothDTO solarSample, @RequestHeader String clientToken) {
        solarDataConverter.genericHandle(systemId,solarSample,clientToken,SolarSystemType.SELFMADE_CONSUMPTION,(SelfMadeSolarSampleConsumptionBothDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

    @PostMapping("/consumption/mult")
    public void PostDataConsumptionMult(@RequestParam long systemId,@RequestBody @Valid List<SelfMadeSolarSampleConsumptionBothDTO> solarSamples, @RequestHeader String clientToken) {
        solarDataConverter.genericHandleMultiple(systemId,solarSamples,clientToken,SolarSystemType.SELFMADE_CONSUMPTION,(SelfMadeSolarSampleConsumptionBothDTO sample)->{
            validateAndFillMissing(sample);
            return convertToInfluxPoint(sample,systemId);
        });
    }

}

