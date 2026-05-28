package de.tostsoft.solarmonitoring.app.controller;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.app.configuration.TaskSchedulerConfiguration;
import de.tostsoft.solarmonitoring.app.model.MultSolarDataWrapper;
import de.tostsoft.solarmonitoring.app.model.SolarSampleWrapper;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.app.service.SolarService;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.lib.model.CurrentValues;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.GenericSolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarDeviceInfluxPoint;
import de.tostsoft.solarmonitoring.lib.model.influx.SolarInfluxPoint;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.tostsoft.solarmonitoring.app.controller.SolarDataController.*;

//TODO reorder class this here is more of the solar service than the original solar service
@Service
public class SolarDataConverter {

    private static final Logger LOG = LoggerFactory.getLogger(SolarDataConverter.class);

    @Autowired
    private TaskSchedulerConfiguration taskSchedulerConfiguration;

    @Autowired
    private SolarService solarService;
    @Autowired
    private SolarSystemRepository solarSystemRepository;
    @Autowired
    private InfluxService influxService;

    public static final int AFTERWARDS_CALCULATION_WAIT = 7;

    public static final int MAX_MULT_REQUEST_SAMPLES_SIZE = 30;

    static public void setGenericInfluxPointBaseClassAttributes(GenericInfluxPoint influxPoint, float duration, Long timestamp, String systemId) {
        influxPoint.setTimestamp(timestamp);
        influxPoint.setDuration(duration);
        influxPoint.setSystemId(systemId);
    }

    public interface MultiValidateAndConvertInterface {
        List<GenericInfluxPoint> validateAndConvert(SampleDTO solarSample, SolarSystem solarSystem);
    }

    public interface MultiValidateAndConvertWithWrapperInterface {
        List<GenericInfluxPoint> validateAndConvert(SolarSampleWrapper solarSample, SolarSystem solarSystem);
    }

    public interface MultiPreValidateAndConvertInterface {
        void preValidate(MultSolarDataWrapper multSolarDataWrapper, SolarSystem solarSystem);
    }

    public interface PreValidateAndConvertInterface {
        SampleDTO preValidate(SampleDTO sampleDTO, SolarSystem solarSystem);
    }

    public interface DeyeValidateAndConvertInterface {
        List<GenericInfluxPoint> validateAndConvert(SolarSystem solarSystem, SampleDTO solarSample);
    }

    void updateMongo(SolarSystem system, SolarInfluxPoint lastPoint) {
        solarSystemRepository.updateNeedsStatisticRecalculation(system.getId(), true);
        if (lastPoint != null) {
      /*var now = Instant.now();
      var last = Instant.ofEpochMilli(lastPoint.getTimestamp());
      if(Duration.between(now,last).toMinutes() > 3){//when failewise to large values are written
        return;
      }*/

            if (lastPoint.getInputWatt() == null &&
                lastPoint.getBatteryWatt() == null &&
                lastPoint.getGridWatt() == null &&
                lastPoint.getOutputWatt() == null
            ) {
                return;
            }

            solarSystemRepository.updateCurrentValuesIfNewer(system.getId(), lastPoint.getTimestamp(),
                    CurrentValues.builder()
                            .lastSet(lastPoint.getTimestamp())
                            .inputWatt(lastPoint.getInputWatt())
                            .batteryVoltage(lastPoint.getBatteryVoltage())
                            .outputWatt(lastPoint.getOutputWatt())
                            .gridWatt(lastPoint.getGridWatt())
                            .build());
        }
    }

    public void genericHandleMulti(String systemId, SampleDTO solarSample, String clientToken,
                                   MultiValidateAndConvertInterface validateAndConvertInterface,
                                   PreValidateAndConvertInterface preValidateAndConvertInterface) {
        var system = solarService.findMatchingSystemWithToken(systemId, clientToken);

        solarSample = preValidateAndConvertInterface.preValidate(solarSample, system);
        if(solarSample == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request not handled because limit of samples on this day is reached: "+system.getMaxSamplesOnDay());
        }

        var influxPoint = validateAndConvertInterface.validateAndConvert(solarSample, system);

        if (Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards())) {
            taskSchedulerConfiguration.solarDataCalculationAfterwardsExecutor().getScheduledExecutor().schedule(() -> {
                var additionalPoints = generateSumPoint(system, influxPoint);
                var last = solarService.addSolarData(system, additionalPoints);
                updateMongo(system, last);
            }, AFTERWARDS_CALCULATION_WAIT, TimeUnit.SECONDS);
        }

        var last = solarService.addSolarData(system, influxPoint);
        updateMongo(system, last);
    }

    public void genericHandleMultipleMulti(String systemId, MultSolarDataWrapper multSolarDataWrapper, String clientToken,
                                           MultiValidateAndConvertWithWrapperInterface validateAndConvertInterface,
                                           MultiPreValidateAndConvertInterface multiPreValidateAndConvertInterface) {
        var system = solarService.findMatchingSystemWithToken(systemId, clientToken);

        multiPreValidateAndConvertInterface.preValidate(multSolarDataWrapper, system);

        List<GenericInfluxPoint> influxPoints = new ArrayList<>();
        for (var solarSample : multSolarDataWrapper.getSamples()) {
            var points = validateAndConvertInterface.validateAndConvert(solarSample, system);
            influxPoints.addAll(points);
        }

        if (Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards())) {
            taskSchedulerConfiguration.solarDataCalculationAfterwardsExecutor().getScheduledExecutor().schedule(() -> {
                var additionalPoints = generateSumPoint(system, influxPoints);
                var last = solarService.addSolarData(system, additionalPoints);
                updateMongo(system, last);
            }, AFTERWARDS_CALCULATION_WAIT, TimeUnit.SECONDS);
        }

        var last = solarService.addSolarData(system, influxPoints);
        updateMongo(system, last);
    }

    public void genericHandleProxy(String systemId, MultSolarDataWrapper multSolarDataWrapper,
                                   MultiValidateAndConvertWithWrapperInterface validateAndConvertInterface,
                                   MultiPreValidateAndConvertInterface multiPreValidateAndConvertInterface) {
        var sysOpt = solarSystemRepository.findById(systemId);
        if (sysOpt.isEmpty()) {
            LOG.warn("No system with id: " + systemId + " found for proxy reqeust");
            return;
        }
        var system = sysOpt.get();

        multiPreValidateAndConvertInterface.preValidate(multSolarDataWrapper, system);

        List<GenericInfluxPoint> influxPoints = new ArrayList<>();
        for (var solarSample : multSolarDataWrapper.getSamples()) {
            var points = validateAndConvertInterface.validateAndConvert(solarSample, system);
            influxPoints.addAll(points);
        }

        if (Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards())) {
            taskSchedulerConfiguration.solarDataCalculationAfterwardsExecutor().getScheduledExecutor().schedule(() -> {
                var additionalPoints = generateSumPoint(system, influxPoints);
                var last = solarService.addSolarData(system, additionalPoints);
                updateMongo(system, last);
            }, AFTERWARDS_CALCULATION_WAIT, TimeUnit.SECONDS);
        }

        var last = solarService.addSolarData(system, influxPoints);
        updateMongo(system, last);
    }


    public void genericHandleDeye(Long serial, SampleDTO solarSample,
                                  DeyeValidateAndConvertInterface validateAndConvertInterface,
                                  PreValidateAndConvertInterface preValidateAndConvertInterface) {

        var system = solarService.findMatchingSystemWithDeyeSunSerial(serial);

        solarSample = preValidateAndConvertInterface.preValidate(solarSample, system);
        if(solarSample == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request not handled because limit of samples on this day is reached: "+system.getMaxSamplesOnDay());
        }

        var influxPoint = validateAndConvertInterface.validateAndConvert(system, solarSample);

        if (Boolean.TRUE.equals(system.getCalculateCombinedValuesAfterwards())) {
            taskSchedulerConfiguration.solarDataCalculationAfterwardsExecutor().getScheduledExecutor().schedule(() -> {
                var additionalPoints = generateSumPoint(system, influxPoint);
                var last = solarService.addSolarData(system, additionalPoints);
                updateMongo(system, last);
            }, AFTERWARDS_CALCULATION_WAIT, TimeUnit.SECONDS);
        }

        var last = solarService.addSolarData(system, influxPoint);
        updateMongo(system, last);
    }

    private void setValueByReflection(SolarDeviceInfluxPoint device, String methodName, Number value) {
        try {
            Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, Float.class);
            testMethod.invoke(device, value.floatValue());
            return;
        } catch (Exception ex) {
            LOG.debug("Error while calling method via reflection", ex);
        }
        try {
            Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, float.class);
            testMethod.invoke(device, value.floatValue());
            return;
        } catch (Exception ex) {
            LOG.debug("Error while calling method via reflection", ex);
        }
        try {
            Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, Integer.class);
            testMethod.invoke(device, value.intValue());
            return;
        } catch (Exception ex) {
            LOG.debug("Error while calling method via reflection", ex);
        }
        try {
            Method testMethod = SolarDeviceInfluxPoint.class.getMethod(methodName, int.class);
            testMethod.invoke(device, value.intValue());
        } catch (Exception ex) {
            LOG.debug("Error while calling method via reflection", ex);
        }
    }

    private List<GenericInfluxPoint> generateSumPoint(SolarSystem system, List<GenericInfluxPoint> influxPoints) {
        var stamps = new HashMap<Long, GenericInfluxPoint>();
        for (GenericInfluxPoint influxPoint : influxPoints) {
            stamps.put(influxPoint.getTimestamp(), influxPoint);
        }

        var resPoints = new ArrayList<GenericInfluxPoint>();
        for (var stamp : stamps.entrySet()) {
            try {
                var points = influxService.getDevicePointsInTimeRange(system, Instant.ofEpochMilli(stamp.getKey()), Duration.ofMinutes(15));
                var convertedPoints = readableDeviceInfluxPoints(points);
                if (convertedPoints.isEmpty()) {
                    continue;
                }

                var filteredConvertedPoints = new ArrayList<SolarDeviceInfluxPoint>();
                for (SolarDeviceInfluxPoint convertedPoint : convertedPoints) {
                    Duration dif = Duration.ofMillis(stamp.getKey() - convertedPoint.getTimestamp());
                    float tolerance = convertedPoint.getDuration() * 0.2f;
                    tolerance = Math.max(tolerance, 5);
                    tolerance = Math.min(tolerance, 30);
                    if (dif.get(ChronoUnit.SECONDS) <= convertedPoint.getDuration() + tolerance) {
                        filteredConvertedPoints.add(convertedPoint);
                    }
                }

                var point = combineDeviceInfluxPoints(filteredConvertedPoints, stamp.getValue().getDuration(), stamp.getKey(), system.getInfluxTagName());
                resPoints.add(point);
            } catch (Exception exception) {
                LOG.error("Exception while calculating sum points afterwards on system: {}", system.getId(), exception);
            }
        }
        return resPoints;
    }

    private SolarInfluxPoint combineDeviceInfluxPoints(Collection<SolarDeviceInfluxPoint> devicePoints, float duration, Long timestamp, String systemId) {

        //this is mostly just a copy of converter function in conroller

        var influxPoint = SolarInfluxPoint.builder().build();

        influxPoint.setInputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattDC).collect(Collectors.toList())));
        influxPoint.setInputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getInputVoltageDC(), d.getInputWattDC())).collect(Collectors.toList()), influxPoint.getInputWattDC()));
        if (influxPoint.getInputWattDC() != null && influxPoint.getInputVoltageDC() != null) {
            influxPoint.setInputAmpereDC(influxPoint.getInputVoltageDC() <= 0 ? 0 : influxPoint.getInputWattDC() / influxPoint.getInputVoltageDC());
        }

        influxPoint.setInputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputWattAC).collect(Collectors.toList())));
        influxPoint.setInputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getInputVoltageAC(), d.getInputWattAC())).collect(Collectors.toList()), influxPoint.getInputWattAC()));
        if (influxPoint.getInputWattAC() != null && influxPoint.getInputVoltageAC() != null) {
            influxPoint.setInputAmpereAC(influxPoint.getInputVoltageAC() <= 0 ? 0 : influxPoint.getInputWattAC() / influxPoint.getInputVoltageAC());
        }

        influxPoint.setBatteryWatt(calculateSum(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryWatt).collect(Collectors.toList())));
        influxPoint.setBatteryVoltage(calculateMean(devicePoints.stream().map(SolarDeviceInfluxPoint::getBatteryVoltage).collect(Collectors.toList())));
        if (influxPoint.getBatteryWatt() != null && influxPoint.getBatteryVoltage() != null) {
            influxPoint.setBatteryAmpere(influxPoint.getBatteryWatt() / influxPoint.getBatteryVoltage());
        }

        influxPoint.setGridWatt(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getGridWatt).collect(Collectors.toList())));
        influxPoint.setGridVoltage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getGridVoltage).collect(Collectors.toList())));
        if (influxPoint.getGridWatt() != null && influxPoint.getGridVoltage() != null && influxPoint.getGridVoltage() != 0) {
            influxPoint.setGridAmpere(influxPoint.getGridWatt() / influxPoint.getGridVoltage());
        }
        //influxPoint.setGridTotalConsumptionKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getGridTotalConsumptionKWH).collect(Collectors.toList())));
        //influxPoint.setGridTotalFeedInKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getGridTotalFeedInKWH).collect(Collectors.toList())));

        influxPoint.setOutputWattDC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattDC).collect(Collectors.toList())));
        influxPoint.setOutputVoltageDC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getOutputVoltageDC(), d.getOutputWattDC())).collect(Collectors.toList()), influxPoint.getOutputWattDC()));
        if (influxPoint.getOutputWattDC() != null && influxPoint.getOutputVoltageDC() != null) {
            influxPoint.setOutputAmpereDC(influxPoint.getOutputVoltageDC() <= 0 ? 0 : influxPoint.getOutputWattDC() / influxPoint.getOutputVoltageDC());
        }

        influxPoint.setOutputWattAC(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputWattAC).collect(Collectors.toList())));
        influxPoint.setOutputVoltageAC(calculateMeanByPercentage(devicePoints.stream().map(d -> new ImmutablePair<Float, Float>(d.getOutputVoltageAC(), d.getOutputWattAC())).collect(Collectors.toList()), influxPoint.getOutputWattAC()));
        if (influxPoint.getOutputWattAC() != null && influxPoint.getOutputVoltageAC() != null) {
            influxPoint.setOutputAmpereAC(influxPoint.getOutputVoltageAC() <= 0 ? 0 : influxPoint.getOutputWattAC() / influxPoint.getOutputVoltageAC());
        }

        influxPoint.setInputWatt(calculateSum(Arrays.asList(influxPoint.getInputWattDC(), influxPoint.getInputWattAC())));
        influxPoint.setOutputWatt(calculateSum(Arrays.asList(influxPoint.getOutputWattDC(), influxPoint.getOutputWattAC())));

        influxPoint.setInputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getInputFrequency).filter(
                Objects::nonNull).collect(Collectors.toList())));

        influxPoint.setOutputFrequency(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputFrequency).filter(
                Objects::nonNull).collect(Collectors.toList())));

        influxPoint.setBatteryPercentage(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryPercentage).filter(
                Objects::nonNull).collect(Collectors.toList())));

        influxPoint.setTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTemperature).filter(
                Objects::nonNull).collect(Collectors.toList())));

        influxPoint.setBatteryTemperature(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryTemperature).filter(
                Objects::nonNull).collect(Collectors.toList())));

        if (influxPoint.getTotalOH() == null) {
            influxPoint.setTotalOH(calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getTotalOH).filter(
                    Objects::nonNull).collect(Collectors.toList())));
        }

        //DO NOT USE THIS code it calculates wrong values if one device is missing
    /*
    //total values
    //input
    if(influxPoint.getInputDCTotalKWH() == null){
      influxPoint.setInputDCTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputDCTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getInputACTotalKWH() == null){
      influxPoint.setInputACTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputACTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getInputTotalKWH() == null){
      influxPoint.setInputTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getInputTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    //output
    if(influxPoint.getOutputDCTotalKWH() == null){
      influxPoint.setOutputDCTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputDCTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getOutputACTotalKWH() == null){
      influxPoint.setOutputACTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputACTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    if(influxPoint.getOutputTotalKWH() == null){
      influxPoint.setOutputTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getOutputTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }

    //battery
    if(influxPoint.getBatteryTotalKWH() == null){
      influxPoint.setBatteryTotalKWH(calculateSum(devicePoints.stream().map(GenericSolarInfluxPoint::getBatteryTotalKWH).filter(
              Objects::nonNull).collect(Collectors.toList())));
    }*/

        //TODO think about total values and duration stuff and implement that within thinking of different durations
        float dur = calculateMean(devicePoints.stream().map(GenericSolarInfluxPoint::getDuration).collect(Collectors.toUnmodifiableList()));

        //TODO replace deviating by amount with better implementation that is more accurate
        setGenericInfluxPointBaseClassAttributes(influxPoint, dur, timestamp, systemId);

        return influxPoint;
    }

    private Collection<SolarDeviceInfluxPoint> readableDeviceInfluxPoints(List<FluxTable> fluxTables) {

        Map<Instant, Map<Long, SolarDeviceInfluxPoint>> resMap = new HashMap<>();

        for (var fluxTable : fluxTables) {
            for (var record : fluxTable.getRecords()) {

                Number number = (Number) record.getValueByKey("_value");
                if (number == null) {
                    continue;
                }

                var time = ((Instant) record.getValueByKey("_time"));
                if (!resMap.containsKey(time)) {
                    resMap.put(time, new HashMap<>());
                }
                var res = resMap.get(time);
                Long id = Long.parseLong("" + record.getValueByKey("id"));

                if (!res.containsKey(id)) {
                    res.put(id, SolarDeviceInfluxPoint.builder().id(id).timestamp(((Instant) record.getValueByKey("_time")).toEpochMilli()).build());
                }
                var deviceDTO = res.get(id);

                String name = record.getField();
                if (StringUtils.length(name) < 1) {
                    continue;
                }
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                name = "set" + name;
                LOG.debug("Try to find via reflection on SolarInfluxPoint: " + name);
                setValueByReflection(deviceDTO, name, number);
            }
        }

        var ret = new ArrayList<SolarDeviceInfluxPoint>();

        for (Map<Long, SolarDeviceInfluxPoint> value : resMap.values()) {
            for (SolarDeviceInfluxPoint solarDeviceInfluxPoint : value.values()) {
                if (solarDeviceInfluxPoint.getDuration() <= 0) {
                    continue;
                }
                ret.add(solarDeviceInfluxPoint);
            }
        }
        return ret;
    }
}
