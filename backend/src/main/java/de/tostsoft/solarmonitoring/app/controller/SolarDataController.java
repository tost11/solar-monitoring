package de.tostsoft.solarmonitoring.app.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.app.dtos.MultDataResponseDTO;
import de.tostsoft.solarmonitoring.app.model.MultSolarDataWrapper;
import de.tostsoft.solarmonitoring.app.model.SolarSampleWrapper;
import de.tostsoft.solarmonitoring.app.monitoring.ApiMeterRegistry;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.lib.controller.BaseSolarDataController;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.*;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.influx.*;
import de.tostsoft.solarmonitoring.lib.service.SolarDataValidator;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static de.tostsoft.solarmonitoring.app.controller.SolarDataConverter.MAX_MULT_REQUEST_SAMPLES_SIZE;
import static de.tostsoft.solarmonitoring.app.controller.SolarDataConverter.setGenericInfluxPointBaseClassAttributes;

@RestController
public class SolarDataController extends BaseSolarDataController {

    @Autowired
    private ApiMeterRegistry apiMeterRegistry;

    @Autowired
    private SolarDataValidator solarDataValidator;

    @Autowired
    private SolarDataConverter solarDataConverter;

    @Value("${api.tokens.deye:}")
    private String deyeSunEndpointApiToken;

    @Value("${api.tokens.proxy:}")
    private String proxyEndpointApiToken;

    private Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private InfluxService influxService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Validator validator;

    private SolarInInputACInfluxPoint convertInputDTO(InputACDTO solarSample, Long deviceId) {
        return SolarInInputACInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .frequency(solarSample.getFrequency())
                .phase(solarSample.getPhase())
                .totalKWH(solarSample.getTotalKWH())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    private SolarInInputDCInfluxPoint convertInputDTO(InputDCDTO solarSample, Long deviceId) {
        return SolarInInputDCInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .totalKWH(solarSample.getTotalKWH())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    private SolarBatteryInfluxPoint convertBatteryDTO(BatteryDTO solarSample, Long deviceId) {
        return SolarBatteryInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .totalKWH(solarSample.getTotalKWH())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    private SolarGridInfluxPoint convertGridDTO(GridDTO solarSample, Long deviceId) {
        return SolarGridInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .dailyConsumptionKWH(solarSample.getDailyConsumption())
                .dailyFeedInKWH(solarSample.getDailyFeedIn())
                .totalConsumptionKWH(solarSample.getTotalConsumptionKWH())
                .totalFeedInKWH(solarSample.getTotalFeedInKWH())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    private SolarOutputDCInfluxPoint convertOutputDTO(OutputDCDTO solarSample, Long deviceId) {
        return SolarOutputDCInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .totalKWH(solarSample.getTotalKWH())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    private SolarOutputACInfluxPoint convertOutputDTO(OutputACDTO solarSample, Long deviceId) {
        return SolarOutputACInfluxPoint.builder()
                .watt(solarSample.getWatt())
                .ampere(solarSample.getAmpere())
                .voltage(solarSample.getVoltage())
                .totalKWH(solarSample.getTotalKWH())
                .frequency(solarSample.getFrequency())
                .phase(solarSample.getPhase())
                .id(solarSample.getId())
                .deviceId(deviceId)
                .build();
    }

    static public Float calculateMean(List<Float> values) {
        if (values.isEmpty()) {
            return null;
        }
        float res = 0;
        int num = 0;
        for (Float value : values) {
            if (value == null) {
                continue;
            }
            res += value;
            num++;
        }
        if (num == 0) {
            return null;
        }
        return res / num;
    }

    static public Float calculateMeanByPercentage(List<Pair<Float, Float>> values, Float max) {
        if (max == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        if (max == 0) {
            return calculateMean(values.stream().map(Pair::getLeft).collect(Collectors.toList()));
        }
        Float res = null;
        for (var value : values) {
            if (value.getLeft() == null || value.getRight() == null) {
                continue;
            }
            float v = value.getLeft() * value.getRight() / max;
            if (res == null) {
                res = v;
            } else {
                res += v;
            }
        }
        return res;
    }

    static public Float calculateSum(List<Float> values) {
        if (values.isEmpty()) {
            return null;
        }
        Float res = null;
        for (Float value : values) {
            if (value == null) {
                continue;
            }
            if (res == null) {
                res = value;
            } else {
                res += value;
            }
        }
        return res;
    }

    static public Float addWithZeroCheck(Float old, Float toAdd) {
        if (toAdd == null) {
            return old;
        }
        if (old == null) {
            return toAdd;
        }
        return old + toAdd;
    }

    static public Integer addWithZeroCheck(Integer old, Integer toAdd) {
        if (toAdd == null) {
            return old;
        }
        if (old == null) {
            return toAdd;
        }
        return old + toAdd;
    }

    private List<GenericInfluxPoint> convertToInfluxPoint(final SampleDTO solarSample, String systemId, boolean combineTotalValuesAfterwards) {

        List<GenericInfluxPoint> res = new ArrayList<>();

        List<SolarDeviceInfluxPoint> devicePoints = new ArrayList<>();
        Float inputDCTotalKWHs = null;
        Float outputDCTotalKWHs = null;
        Float inputACTotalKWHs = null;
        Float outputACTotalKWHs = null;
        Float gridFeedInTotalKWHs = null;
        Float gridConsumptionTotalKWHs = null;
        Float batteryTotalKWHs = null;

        long timestamp = solarSample.getTimestamp();
        if (solarSample.getTimeUnit() != null) {
            timestamp = TimeUnit.MILLISECONDS.convert(solarSample.getTimestamp(), solarSample.getTimeUnit());
        }

        for (DeviceDTO device : solarSample.getDevices()) {

            Float deviceInputDCTotalKWHs = null;
            Float deviceOutputDCTotalKWHs = null;
            Float deviceInputACTotalKWHs = null;
            Float deviceOutputACTotalKWHs = null;
            Float deviceBatteryTotalKWHs = null;
            Float deviceGridFeedInTotalKWHs = null;
            Float deviceGridConsumptionTotalKWHs = null;
            Integer numActiveConnectsions = null;

            for (var input : device.getInputsDC()) {
                var point = convertInputDTO(input, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceInputDCTotalKWHs = addWithZeroCheck(deviceInputDCTotalKWHs, input.getTotalKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            for (var input : device.getInputsAC()) {
                var point = convertInputDTO(input, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceInputACTotalKWHs = addWithZeroCheck(deviceInputACTotalKWHs, input.getTotalKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            for (var battery : device.getBatteries()) {
                var point = convertBatteryDTO(battery, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceBatteryTotalKWHs = addWithZeroCheck(deviceBatteryTotalKWHs, battery.getTotalKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            for (var grid : device.getGrids()) {
                var point = convertGridDTO(grid, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceGridFeedInTotalKWHs = addWithZeroCheck(deviceGridFeedInTotalKWHs, grid.getTotalFeedInKWH());
                deviceGridConsumptionTotalKWHs = addWithZeroCheck(deviceGridConsumptionTotalKWHs, grid.getTotalConsumptionKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            for (var output : device.getOutputsDC()) {
                var point = convertOutputDTO(output, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceOutputDCTotalKWHs = addWithZeroCheck(deviceOutputDCTotalKWHs, output.getTotalKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            for (var output : device.getOutputsAC()) {
                var point = convertOutputDTO(output, device.getId());
                setGenericInfluxPointBaseClassAttributes(point, solarSample.getDuration(),
                        timestamp, systemId);
                res.add(point);

                deviceOutputACTotalKWHs = addWithZeroCheck(deviceOutputACTotalKWHs, output.getTotalKWH());
                numActiveConnectsions = addWithZeroCheck(numActiveConnectsions, 1);
            }

            var devicePoint = SolarDeviceInfluxPoint.builder()
                    .inputVoltageAC(device.getInputVoltageAC())
                    .inputAmpereAC(device.getInputAmpereAC())
                    .inputWattAC(device.getInputWattAC())
                    .inputVoltageDC(device.getInputVoltageDC())
                    .inputAmpereDC(device.getInputAmpereDC())
                    .inputWattDC(device.getInputWattDC())
                    .outputVoltageAC(device.getOutputVoltageAC())
                    .outputAmpereAC(device.getOutputAmpereAC())
                    .outputWattAC(device.getOutputWattAC())
                    .outputVoltageDC(device.getOutputVoltageDC())
                    .outputAmpereDC(device.getOutputAmpereDC())
                    .outputWattDC(device.getOutputWattDC())
                    .outputWatt(device.getOutputWatt())
                    .inputWatt(device.getInputWatt())
                    .inputDCTotalKWH(device.getInputDCTotalKWH())
                    .outputDCTotalKWH(device.getOutputDCTotalKWH())
                    .inputACTotalKWH(device.getInputACTotalKWH())
                    .outputACTotalKWH(device.getOutputACTotalKWH())
                    .inputTotalKWH(device.getInputTotalKWH())
                    .outputTotalKWH(device.getOutputTotalKWH())
                    .batteryTotalKWH(device.getBatteryTotalKWH())
                    .totalOH(device.getTotalOH())
                    .temperature(device.getTemperature())
                    .batteryTemperature(device.getBatteryTemperature())
                    .batteryVoltage(device.getBatteryVoltage())
                    .batteryAmpere(device.getBatteryAmpere())
                    .batteryWatt(device.getBatteryWatt())
                    .inputFrequency(device.getInputFrequency())
                    .outputFrequency(device.getOutputFrequency())
                    .batteryPercentage(device.getBatteryPercentage())
                    .gridVoltage(device.getGridVoltage())
                    .gridAmpere(device.getGridAmpere())
                    .gridWatt(device.getGridWatt())
                    .gridTotalConsumptionKWH(device.getGridTotalConsumptionKWH())
                    .gridTotalFeedInKWH(device.getGridTotalFeedInKWH())
                    .id(device.getId())
                    .build();

            if (devicePoint.getInputWattDC() == null) {
                devicePoint.setInputWattDC(calculateSum(device.getInputsDC().stream().map(InputDCDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getInputVoltageDC() == null) {
                devicePoint.setInputVoltageDC(calculateMeanByPercentage(device.getInputsDC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattDC()));
            }
            if (devicePoint.getInputAmpereDC() == null && devicePoint.getInputWattDC() != null && devicePoint.getInputVoltageDC() != null) {
                devicePoint.setInputAmpereDC(devicePoint.getInputVoltageDC() <= 0 ? 0 : devicePoint.getInputWattDC() / devicePoint.getInputVoltageDC());
            }

            if (devicePoint.getInputWattAC() == null) {
                devicePoint.setInputWattAC(calculateSum(device.getInputsAC().stream().map(InputACDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getInputVoltageAC() == null) {
                devicePoint.setInputVoltageAC(calculateMeanByPercentage(device.getInputsAC().stream().map(i -> new ImmutablePair<Float, Float>(i.getVoltage(), i.getWatt())).collect(Collectors.toList()), devicePoint.getInputWattAC()));
            }
            if (devicePoint.getInputAmpereAC() == null && devicePoint.getInputWattAC() != null && devicePoint.getInputVoltageAC() != null) {
                devicePoint.setInputAmpereAC(devicePoint.getInputVoltageAC() <= 0 ? 0 : devicePoint.getInputWattAC() / devicePoint.getInputVoltageAC());
            }

            if (devicePoint.getBatteryWatt() == null) {
                devicePoint.setBatteryWatt(calculateSum(device.getBatteries().stream().map(BatteryDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getBatteryVoltage() == null) {
                devicePoint.setBatteryVoltage(calculateMean(device.getBatteries().stream().map(BatteryDTO::getVoltage).collect(Collectors.toList())));
            }
            if (devicePoint.getBatteryAmpere() == null && devicePoint.getBatteryWatt() != null && devicePoint.getBatteryVoltage() != null) {
                devicePoint.setBatteryAmpere(devicePoint.getBatteryWatt() / devicePoint.getBatteryVoltage());
            }

            if (devicePoint.getGridWatt() == null) {
                devicePoint.setGridWatt(calculateSum(device.getGrids().stream().map(GridDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getGridVoltage() == null) {
                devicePoint.setGridVoltage(calculateMean(device.getGrids().stream().map(GridDTO::getVoltage).collect(Collectors.toList())));
            }
            if (devicePoint.getGridAmpere() == null && devicePoint.getGridWatt() != null && devicePoint.getGridVoltage() != null && devicePoint.getGridVoltage() != 0) {
                devicePoint.setGridAmpere(devicePoint.getGridWatt() / devicePoint.getGridVoltage());
            }

            if (devicePoint.getOutputWattDC() == null) {
                devicePoint.setOutputWattDC(calculateSum(device.getOutputsDC().stream().map(OutputDCDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getOutputVoltageDC() == null) {
                devicePoint.setOutputVoltageDC(calculateMeanByPercentage(device.getOutputsDC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattDC()));
            }
            if (devicePoint.getOutputAmpereDC() == null && devicePoint.getOutputWattDC() != null && devicePoint.getOutputVoltageDC() != null) {
                devicePoint.setOutputAmpereDC(devicePoint.getOutputVoltageDC() <= 0 ? 0 : devicePoint.getOutputWattDC() / devicePoint.getOutputVoltageDC());
            }

            if (devicePoint.getOutputWattAC() == null) {
                devicePoint.setOutputWattAC(calculateSum(device.getOutputsAC().stream().map(OutputACDTO::getWatt).collect(Collectors.toList())));
            }
            if (devicePoint.getOutputVoltageAC() == null) {
                devicePoint.setOutputVoltageAC(calculateMeanByPercentage(device.getOutputsAC().stream().map(o -> new ImmutablePair<Float, Float>(o.getVoltage(), o.getWatt())).collect(Collectors.toList()), devicePoint.getOutputWattAC()));
            }
            if (devicePoint.getOutputAmpereAC() == null && devicePoint.getOutputWattAC() != null && devicePoint.getOutputVoltageAC() != null) {
                devicePoint.setOutputAmpereAC(devicePoint.getOutputVoltageAC() <= 0 ? 0 : devicePoint.getOutputWattAC() / devicePoint.getOutputVoltageAC());
            }

            if (devicePoint.getInputWatt() == null) {
                devicePoint.setInputWatt(calculateSum(Arrays.asList(devicePoint.getInputWattDC(), devicePoint.getInputWattAC())));
            }

            if (devicePoint.getOutputWatt() == null) {
                devicePoint.setOutputWatt(calculateSum(Arrays.asList(devicePoint.getOutputWattDC(), devicePoint.getOutputWattAC())));
            }

            if (devicePoint.getInputFrequency() == null) {
                devicePoint.setInputFrequency(calculateMean(device.getInputsAC().stream().map(InputACDTO::getFrequency).collect(Collectors.toList())));
            }

            if (devicePoint.getOutputFrequency() == null) {
                devicePoint.setOutputFrequency(calculateMean(device.getOutputsAC().stream().map(OutputACDTO::getFrequency).collect(Collectors.toList())));
            }

            if (devicePoint.getInputACTotalKWH() == null) {
                devicePoint.setInputACTotalKWH(deviceInputACTotalKWHs);
            }
            if (devicePoint.getOutputACTotalKWH() == null) {
                devicePoint.setOutputACTotalKWH(deviceOutputACTotalKWHs);
            }
            if (devicePoint.getInputDCTotalKWH() == null) {
                devicePoint.setInputDCTotalKWH(deviceInputDCTotalKWHs);
            }
            if (devicePoint.getOutputDCTotalKWH() == null) {
                devicePoint.setOutputDCTotalKWH(deviceOutputDCTotalKWHs);
            }
            if (devicePoint.getInputTotalKWH() == null) {
                devicePoint.setInputTotalKWH(addWithZeroCheck(devicePoint.getInputACTotalKWH(), devicePoint.getInputDCTotalKWH()));
            }
            if (devicePoint.getOutputTotalKWH() == null) {
                devicePoint.setOutputTotalKWH(addWithZeroCheck(devicePoint.getOutputACTotalKWH(), devicePoint.getOutputDCTotalKWH()));
            }
            if (devicePoint.getBatteryTotalKWH() == null) {
                devicePoint.setBatteryTotalKWH(deviceBatteryTotalKWHs);
            }
            if (devicePoint.getGridTotalConsumptionKWH() == null) {
                devicePoint.setGridTotalConsumptionKWH(deviceGridConsumptionTotalKWHs);
            }
            if (devicePoint.getGridTotalFeedInKWH() == null) {
                devicePoint.setGridTotalFeedInKWH(deviceGridFeedInTotalKWHs);
            }

            devicePoint.setNumActiveConnections(numActiveConnectsions);

            setGenericInfluxPointBaseClassAttributes(devicePoint, solarSample.getDuration(), timestamp, systemId);

            res.add(devicePoint);

            devicePoints.add(devicePoint);

            //some values of main device
            inputACTotalKWHs = addWithZeroCheck(inputACTotalKWHs, devicePoint.getInputACTotalKWH());
            inputDCTotalKWHs = addWithZeroCheck(inputDCTotalKWHs, devicePoint.getInputDCTotalKWH());
            outputDCTotalKWHs = addWithZeroCheck(outputDCTotalKWHs, devicePoint.getOutputDCTotalKWH());
            outputACTotalKWHs = addWithZeroCheck(outputACTotalKWHs, devicePoint.getOutputACTotalKWH());
            gridFeedInTotalKWHs = addWithZeroCheck(gridFeedInTotalKWHs, devicePoint.getGridTotalFeedInKWH());
            gridConsumptionTotalKWHs = addWithZeroCheck(gridConsumptionTotalKWHs, devicePoint.getGridTotalConsumptionKWH());
            batteryTotalKWHs = addWithZeroCheck(batteryTotalKWHs, devicePoint.getBatteryTotalKWH());
        }

        if (!combineTotalValuesAfterwards) {

            var influxPoint = SolarInfluxPoint.builder()
                    .inputVoltageDC(solarSample.getInputVoltageDC())
                    .inputAmpereDC(solarSample.getInputAmpereDC())
                    .inputWattDC(solarSample.getInputWattDC())
                    .inputVoltageAC(solarSample.getInputVoltageAC())
                    .inputAmpereAC(solarSample.getInputAmpereAC())
                    .inputWattAC(solarSample.getInputWattAC())
                    .inputWatt(solarSample.getInputWatt())
                    .outputVoltageDC(solarSample.getOutputVoltageDC())
                    .outputAmpereDC(solarSample.getOutputAmpereDC())
                    .outputWattDC(solarSample.getOutputWattDC())
                    .outputVoltageAC(solarSample.getOutputVoltageAC())
                    .outputAmpereAC(solarSample.getOutputAmpereAC())
                    .outputWattAC(solarSample.getOutputWattAC())
                    .inputDCTotalKWH(solarSample.getInputDCTotalKWH())
                    .outputDCTotalKWH(solarSample.getOutputDCTotalKWH())
                    .inputACTotalKWH(solarSample.getInputACTotalKWH())
                    .outputACTotalKWH(solarSample.getOutputACTotalKWH())
                    .inputTotalKWH(solarSample.getInputTotalKWH())
                    .outputTotalKWH(solarSample.getOutputTotalKWH())
                    .totalOH(solarSample.getTotalOH())
                    .outputFrequency(solarSample.getOutputFrequency())
                    .inputFrequency(solarSample.getInputFrequency())
                    .temperature(solarSample.getTemperature())
                    .batteryTemperature(solarSample.getBatteryTemperature())
                    .batteryVoltage(solarSample.getBatteryVoltage())
                    .batteryAmpere(solarSample.getBatteryAmpere())
                    .batteryWatt(solarSample.getBatteryWatt())
                    .batteryPercentage(solarSample.getBatteryPercentage())
                    .batteryTotalKWH(solarSample.getBatteryTotalKWH())
                    .gridVoltage(solarSample.getGridVoltage())
                    .gridAmpere(solarSample.getGridAmpere())
                    .gridWatt(solarSample.getGridWatt())
                    .gridTotalConsumptionKWH(solarSample.getGridTotalConsumptionKWH())
                    .gridTotalFeedInKWH(solarSample.getGridTotalFeedInKWH())
                    .build();

            if (influxPoint.getInputWattDC() == null) {
                influxPoint.setInputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattDC).collect(Collectors.toList())));
            }
            if (influxPoint.getInputVoltageDC() == null) {
                influxPoint.setInputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getInputVoltageDC(), d.getInputWattDC())).collect(Collectors.toList()), influxPoint.getInputWattDC()));
            }
            if (influxPoint.getInputAmpereDC() == null && influxPoint.getInputWattDC() != null && influxPoint.getInputVoltageDC() != null) {
                influxPoint.setInputAmpereDC(influxPoint.getInputVoltageDC() <= 0 ? 0 : influxPoint.getInputWattDC() / influxPoint.getInputVoltageDC());
            }

            if (influxPoint.getInputWattAC() == null) {
                influxPoint.setInputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattAC).collect(Collectors.toList())));
            }
            if (influxPoint.getInputVoltageAC() == null) {
                influxPoint.setInputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getInputVoltageAC(), d.getInputWattAC())).collect(Collectors.toList()), influxPoint.getInputWattAC()));
            }
            if (influxPoint.getInputAmpereAC() == null && influxPoint.getInputWattAC() != null && influxPoint.getInputVoltageAC() != null) {
                influxPoint.setInputAmpereAC(influxPoint.getInputVoltageAC() <= 0 ? 0 : influxPoint.getInputWattAC() / influxPoint.getInputVoltageAC());
            }

            if (influxPoint.getBatteryWatt() == null) {
                influxPoint.setBatteryWatt(calculateSum(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryWatt).collect(Collectors.toList())));
            }
            if (influxPoint.getBatteryVoltage() == null) {
                influxPoint.setBatteryVoltage(calculateMean(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryVoltage).collect(Collectors.toList())));
            }
            if (influxPoint.getBatteryAmpere() == null && influxPoint.getBatteryWatt() != null && influxPoint.getBatteryVoltage() != null) {
                influxPoint.setBatteryAmpere(influxPoint.getBatteryWatt() / influxPoint.getBatteryVoltage());
            }

            if (influxPoint.getGridWatt() == null) {
                influxPoint.setGridWatt(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getGridWatt).collect(Collectors.toList())));
            }
            if (influxPoint.getGridVoltage() == null) {
                influxPoint.setGridVoltage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getGridVoltage).collect(Collectors.toList())));
            }
            if (influxPoint.getGridAmpere() == null && influxPoint.getGridWatt() != null && influxPoint.getGridVoltage() != null && influxPoint.getGridVoltage() != 0) {
                influxPoint.setGridAmpere(influxPoint.getGridWatt() / influxPoint.getGridVoltage());
            }

            if (influxPoint.getOutputWattDC() == null) {
                influxPoint.setOutputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattDC).collect(Collectors.toList())));
            }
            if (influxPoint.getOutputVoltageDC() == null) {
                influxPoint.setOutputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getOutputVoltageDC(), d.getOutputWattDC())).collect(Collectors.toList()), influxPoint.getOutputWattDC()));
            }
            if (influxPoint.getOutputAmpereDC() == null && influxPoint.getOutputWattDC() != null && influxPoint.getOutputVoltageDC() != null) {
                influxPoint.setOutputAmpereDC(influxPoint.getOutputVoltageDC() <= 0 ? 0 : influxPoint.getOutputWattDC() / influxPoint.getOutputVoltageDC());
            }

            if (influxPoint.getOutputWattAC() == null) {
                influxPoint.setOutputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattAC).collect(Collectors.toList())));
            }
            if (influxPoint.getOutputVoltageAC() == null) {
                influxPoint.setOutputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getOutputVoltageAC(), d.getOutputWattAC())).collect(Collectors.toList()), influxPoint.getOutputWattAC()));
            }
            if (influxPoint.getOutputAmpereAC() == null && influxPoint.getOutputWattAC() != null && influxPoint.getOutputVoltageAC() != null) {
                influxPoint.setOutputAmpereAC(influxPoint.getOutputVoltageAC() <= 0 ? 0 : influxPoint.getOutputWattAC() / influxPoint.getOutputVoltageAC());
            }

            influxPoint.setInputWatt(solarSample.getInputWatt());
            if (influxPoint.getInputWatt() == null) {
                influxPoint.setInputWatt(calculateSum(Arrays.asList(influxPoint.getInputWattDC(), influxPoint.getInputWattAC())));
            }

            influxPoint.setOutputWatt(solarSample.getOutputWatt());
            if (influxPoint.getOutputWatt() == null) {
                influxPoint.setOutputWatt(calculateSum(Arrays.asList(influxPoint.getOutputWattDC(), influxPoint.getOutputWattAC())));
            }

            if (influxPoint.getInputFrequency() == null) {
                influxPoint.setInputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getInputFrequency).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getOutputFrequency() == null) {
                influxPoint.setOutputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputFrequency).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getBatteryPercentage() == null) {
                influxPoint.setBatteryPercentage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryPercentage).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getTemperature() == null) {
                influxPoint.setTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTemperature).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getBatteryTemperature() == null) {
                influxPoint.setBatteryTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryTemperature).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getTotalOH() == null) {
                influxPoint.setTotalOH(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTotalOH).filter(
                        Objects::nonNull).collect(Collectors.toList())));
            }

            if (influxPoint.getInputACTotalKWH() == null) {
                influxPoint.setInputACTotalKWH(inputACTotalKWHs);
            }
            if (influxPoint.getOutputACTotalKWH() == null) {
                influxPoint.setOutputACTotalKWH(outputACTotalKWHs);
            }
            if (influxPoint.getInputDCTotalKWH() == null) {
                influxPoint.setInputDCTotalKWH(inputDCTotalKWHs);
            }
            if (influxPoint.getOutputDCTotalKWH() == null) {
                influxPoint.setOutputDCTotalKWH(outputDCTotalKWHs);
            }
            if (influxPoint.getGridTotalConsumptionKWH() == null) {
                influxPoint.setGridTotalConsumptionKWH(gridConsumptionTotalKWHs);
            }
            if (influxPoint.getGridTotalFeedInKWH() == null) {
                influxPoint.setGridTotalFeedInKWH(gridFeedInTotalKWHs);
            }
            if (influxPoint.getBatteryTotalKWH() == null) {
                influxPoint.setBatteryTotalKWH(batteryTotalKWHs);
            }
            if (solarSample.getInputTotalKWH() == null) {
                influxPoint.setInputTotalKWH(addWithZeroCheck(influxPoint.getInputACTotalKWH(), influxPoint.getInputDCTotalKWH()));
            }
            if (solarSample.getOutputTotalKWH() == null) {
                influxPoint.setOutputTotalKWH(addWithZeroCheck(influxPoint.getOutputACTotalKWH(), influxPoint.getOutputDCTotalKWH()));
            }

            if (solarSample.getDevices() != null) {
                influxPoint.setNumActiveDevices(solarSample.getDevices().size());
            }

            setGenericInfluxPointBaseClassAttributes(influxPoint, solarSample.getDuration(), timestamp, systemId);

            res.add(influxPoint);

        }

        return res;
    }

    SampleDTO checkThisDayDataIsFull(SampleDTO sample,SolarSystem solarSystem) {
        if(sample.getTimestamp() == null){
            sample.setTimestamp(Instant.now().toEpochMilli());
            sample.setTimeUnit(TimeUnit.MILLISECONDS);
        }
        if(sample.getTimeUnit() == null){
            sample.setTimeUnit(TimeUnit.MILLISECONDS);
        }

        if(solarSystem.getMaxSamplesOnDay() == null || solarSystem.getMaxSamplesOnDay() < 0){
            return sample; //no checking for max samples needed
        }

        ZoneId zoneId;
        if(solarSystem.getTimezone() == null){
            zoneId = ZoneId.of("UTC");
        }else{
            zoneId = ZoneId.of(solarSystem.getTimezone());
        }
        Instant instant = Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(sample.getTimestamp(), sample.getTimeUnit()));
        LocalDate localDate = instant.atZone(zoneId).toLocalDate();

        long samplesOnDy = influxService.getSolarDataPointsForDay(solarSystem,localDate);
        if(samplesOnDy + 1 <= solarSystem.getMaxSamplesOnDay()){
            //add all values
            return sample;
        }
        return null;
    }

    void checkThisDayDataIsFullMult(MultSolarDataWrapper multSolarDataWrapper,SolarSystem solarSystem) {

        //set timestamp if missing
        for (var sample : multSolarDataWrapper.getSamples()) {
            if(sample.getSample().getTimestamp() == null){
                sample.getSample().setTimestamp(Instant.now().toEpochMilli());
                sample.getSample().setTimeUnit(TimeUnit.MILLISECONDS);
            }
            if(sample.getSample().getTimeUnit() == null){
                sample.getSample().setTimeUnit(TimeUnit.MILLISECONDS);
            }
        }

        if(solarSystem.getMaxSamplesOnDay() == null || solarSystem.getMaxSamplesOnDay() <0){
            return;
        }

        ZoneId zoneId;
        if(solarSystem.getTimezone() == null){
            zoneId = ZoneId.of("UTC");
        }else{
            zoneId = ZoneId.of(solarSystem.getTimezone());
        }

        for (var sample : multSolarDataWrapper.getSamples()) {
            if(sample.getSample().getTimestamp() <=0 || !sample.isValid()){
               continue;//is not valid and will be filtered later
            }
            Instant instant = Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(sample.getSample().getTimestamp(), sample.getSample().getTimeUnit()));
            LocalDate localDate = instant.atZone(zoneId).toLocalDate();
            multSolarDataWrapper.getCurrentSamplesDay().put(localDate,new AtomicLong());
        }

        for (var entry : multSolarDataWrapper.getCurrentSamplesDay().entrySet()) {
            entry.getValue().set(influxService.getSolarDataPointsForDay(solarSystem,entry.getKey()));
        }
    }

    public void PostDevice(String systemId, SampleDTO solarSample, String clientToken) {
        apiMeterRegistry.incrementApiEndpointCallData();

        solarDataConverter.genericHandleMulti(systemId, solarSample, clientToken, (sample, solarSystem) -> {
            solarDataValidator.validateAndFillMissing(sample);
            return convertToInfluxPoint(sample, systemId, Boolean.TRUE.equals(solarSystem.getCalculateCombinedValuesAfterwards()));
        },this::checkThisDayDataIsFull);
        apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
    }

    //post mapping in base class
    public ResponseEntity<String> PostDeviceMult(String systemId, List<SampleDTO> solarSamples, String clientToken){

        apiMeterRegistry.incrementApiEndpointCallData();

        if(solarSamples.size() <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Samples list is empty");
        }

        if(solarSamples.size() > MAX_MULT_REQUEST_SAMPLES_SIZE){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"To many sample for mult request, max is "+MAX_MULT_REQUEST_SAMPLES_SIZE);
        }

        return handleMultRequest(systemId,solarSamples,(a,b,c,d)->solarDataConverter.genericHandleMultipleMulti(a,b,clientToken,c,d));
    }

    //post mapping in base class
    public void PostDeviceDeye(String serialId, SampleDTO solarSample, String clientToken) {

        apiMeterRegistry.incrementApiEndpointCallData();

        if (StringUtils.isEmpty(deyeSunEndpointApiToken)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not activated");
        }

        if (!StringUtils.equals(deyeSunEndpointApiToken, clientToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This Endpoint requires Authentication");
        }

        long serial;

        try {
            serial = Long.parseLong(serialId);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serialId must be numeric");
        }
        solarDataConverter.genericHandleDeye(serial, solarSample, (system, sample) -> {
            solarDataValidator.validateAndFillMissing(sample);
            return convertToInfluxPoint(sample, system.getId(), Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards()));
        },this::checkThisDayDataIsFull);

        apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
    }

    public interface GenericMultDataHandler{
        void handle(String systemId, MultSolarDataWrapper multSolarDataWrapper,
                  SolarDataConverter.MultiValidateAndConvertWithWrapperInterface validateAndConvertInterface,
                  SolarDataConverter.MultiPreValidateAndConvertInterface multiPreValidateAndConvertInterface);
    }

    ResponseEntity<String> handleMultRequest(String systemId,List<SampleDTO> solarSamples,GenericMultDataHandler handler){
        MultSolarDataWrapper multSolarDataWrapper = new MultSolarDataWrapper();
        for (SampleDTO solarSample : solarSamples) {
            var samp = SolarSampleWrapper.builder()
                    .valid(true)
                    .limitReached(false)
                    .sample(solarSample)
                    .build();
            if(solarSample.getTimestamp() <= 0){
                samp.setValid(false);
            }
            multSolarDataWrapper.getSamples().add(samp);
        }

        handler.handle(systemId, multSolarDataWrapper, (sample, solarSystem) -> {
            if(!sample.isValid()){
                return new ArrayList<>();//sample alredy not valid (happens on mult with timestamp <= 0)
            }
            try{
                var errors = validator.validate(sample.getSample());
                if(!errors.isEmpty()){
                    throw new RuntimeException("Validation failed");
                }
                solarDataValidator.validateAndFillMissing(sample.getSample());
            }catch(Exception e){
                sample.setValid(false);
                return new ArrayList<>();
            }

            ZoneId zoneId;
            if(solarSystem.getTimezone() == null){
                zoneId = ZoneId.of("UTC");
            }else{
                zoneId = ZoneId.of(solarSystem.getTimezone());
            }

            Instant instant = Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(sample.getSample().getTimestamp(), sample.getSample().getTimeUnit()));
            LocalDate localDate = instant.atZone(zoneId).toLocalDate();

            if(solarSystem.getMaxSamplesOnDay() != null && solarSystem.getMaxSamplesOnDay() != 0){
                var currentDayMax = multSolarDataWrapper.getCurrentSamplesDay().get(localDate);
                if(currentDayMax.get() >= solarSystem.getMaxSamplesOnDay()){
                    sample.setLimitReached(true);
                    return new ArrayList<>();
                }
                currentDayMax.incrementAndGet();
            }


            return convertToInfluxPoint(sample.getSample(), systemId, Boolean.TRUE.equals(solarSystem.getCalculateCombinedValuesAfterwards()));
        },this::checkThisDayDataIsFullMult);

        int countInvalid = 0;
        MultDataResponseDTO res = new MultDataResponseDTO();
        for(int i=0;i<multSolarDataWrapper.getSamples().size();i++){
            var sample = multSolarDataWrapper.getSamples().get(i);
            if(!sample.isValid()){
                res.getInvalidSamplesIndexes().add(i);
                countInvalid++;
            }
            if(sample.isLimitReached()){
                res.getDailyLimitReachedIndexes().add(i);
                countInvalid++;
            }
        }

        var status = countInvalid != solarSamples.size() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        if(status == HttpStatus.OK){
            apiMeterRegistry.incrementApiEndpointCallDataSuccessful();
        }
        try {
            return new ResponseEntity<>(objectMapper.writeValueAsString(res),status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);//this can not happen
        }
    }

    @PostMapping("/proxy")
    public ResponseEntity<String> PostDeviceProxy(@RequestParam String systemId, @RequestBody List<SampleDTO> solarSamples, @RequestHeader String proxyToken) {

        apiMeterRegistry.incrementApiEndpointCallData();

        if(StringUtils.isEmpty(proxyToken)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not activated");
        }

        if(!StringUtils.equals(proxyEndpointApiToken,proxyToken)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This Endpoint requires Authentication");
        }

        LOG.info("Proxy Endpoint called with "+solarSamples.size()+" samples on system: "+systemId);

        return handleMultRequest(systemId,solarSamples,(a,b,c,d)->solarDataConverter.genericHandleProxy(a,b,c,d));
    }
}
