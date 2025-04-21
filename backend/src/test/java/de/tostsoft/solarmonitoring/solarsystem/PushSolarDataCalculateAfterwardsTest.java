package de.tostsoft.solarmonitoring.solarsystem;

import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.ApplicationBaseRestTest;
import de.tostsoft.solarmonitoring.app.controller.SolarDataController;
import de.tostsoft.solarmonitoring.app.controller.SolarDataConverter;
import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.*;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PushSolarDataCalculateAfterwardsTest extends ApplicationBaseRestTest {

    private Logger LOG = LoggerFactory.getLogger(PushSolarDataCalculateAfterwardsTest.class);

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Value("${api.tokens.deye:}")
    private String deyeSunEndpointApiToken;

    @Value("${api.tokens.proxy:}")
    private String proxyEndpointApiToken;

    @Autowired
    private SolarDataController solarDataController;

    @Autowired
    private InfluxConnection influxConnection;

    private List<FluxTable> getAllCombinedSamplesFromInflux(String bucketName){
        return influxConnection.getClient().getQueryApi().query("" +
                "from(bucket: \"" +bucketName + "\")\n" +
                "  |> range(start: 0, stop: now())\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"solar-data\")\n\n");
    }

    Double getValueFromFluxTables(List<FluxTable> tables,String key,int column,Integer validationColumSize) {

        for (FluxTable table : tables) {
            var record = table.getRecords().stream().filter(v -> StringUtils.equals((String) v.getValueByKey("_field"), key)).findFirst();
            if (record.isEmpty() || record.get().getValue() == null) {
                continue;
            }

            if (validationColumSize != null) {
                Assertions.assertThat(table.getRecords()).hasSize(validationColumSize);
            }
            return ((Number)table.getRecords().get(column).getValue()).doubleValue();
        }
        return null;
    }

    private SampleDTO createDefaultSampleDTO() {
        return createDefaultSampleDTO(false);
    }

    private SampleDTO createDefaultSampleDTO(boolean all) {
        SampleDTO sampleDTO = new SampleDTO();

        List<DeviceDTO> deviceDTOList = new ArrayList<>();
        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTOList.add(deviceDTO);
        sampleDTO.setDevices(deviceDTOList);

        List<InputDCDTO> inputDCDTOList = new ArrayList<>();
        var inputDCDTO = new InputDCDTO();
        inputDCDTO.setId(0L);
        inputDCDTOList.add(inputDCDTO);
        deviceDTO.setInputsDC(inputDCDTOList);

        List<OutputACDTO> outputACDTOList = new ArrayList<>();
        var outputACDTO = new OutputACDTO();
        outputACDTO.setId(0L);
        outputACDTOList.add(outputACDTO);
        deviceDTO.setOutputsAC(outputACDTOList);

        if(all){
            List<InputACDTO> inputACDTOList = new ArrayList<>();
            var inputACDTO = new InputACDTO();
            inputACDTO.setId(0L);
            inputACDTOList.add(inputACDTO);
            deviceDTO.setInputsAC(inputACDTOList);

            List<OutputDCDTO> outputDCDTOList = new ArrayList<>();
            var outputDCDTO = new OutputDCDTO();
            outputDCDTO.setId(0L);
            outputDCDTOList.add(outputDCDTO);
            deviceDTO.setOutputsDC(outputDCDTOList);
        }

        return sampleDTO;
    }

    private enum Endpoints{
        NORMAL,
        DEYE,
        MULT,
        PROXY
    }

    private void sendSample(Endpoints endpoint, List<SampleDTO> sampleDTOList, SolarSystem system){
        if(endpoint == Endpoints.NORMAL) {
            for (SampleDTO sampleDTO : sampleDTOList) {
                solarDataController.PostDevice(system.getId(), sampleDTO, "token");
            }
        }else if(endpoint == Endpoints.DEYE){
            for (SampleDTO sampleDTO : sampleDTOList) {
                solarDataController.PostDeviceDeye("1234", sampleDTO, deyeSunEndpointApiToken);
            }
        }else if(endpoint == Endpoints.MULT){
            solarDataController.PostDeviceMult(system.getId(), sampleDTOList, "token");
        }else if(endpoint == Endpoints.PROXY){
            solarDataController.PostDeviceProxy(system.getId(), sampleDTOList, proxyEndpointApiToken);
        }
    }

    @ParameterizedTest
    @EnumSource(Endpoints.class)
    public void checkCalculateAfterwards(Endpoints endpoint) throws InterruptedException {

        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system.setDeyeSunSerials(Collections.singleton(1234L));
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createDefaultSampleDTO();
        var deviceDTO = sampleDTO.getDevices().get(0);
        var inputDCDTO = deviceDTO.getInputsDC().get(0);
        var outputACDTO = deviceDTO.getOutputsAC().get(0);

        sampleDTO.setDuration(300.f);
        inputDCDTO.setWatt(10.f);
        outputACDTO.setWatt(11.f);

        deviceDTO.setId(0L);

        sampleDTO.setTimestamp(start.toEpochMilli());

        sendSample(endpoint, Collections.singletonList(sampleDTO),system);

        var res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());
        Assertions.assertThat(res).isEmpty();

        LOG.info("Waiting for calculation afterwards to be done");
        Thread.sleep(SolarDataConverter.AFTERWARDS_CALCULATION_WAIT * 1000 + 5000);

        res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());

        Assertions.assertThat(getValueFromFluxTables(res,"InputWatt",0,1)).isEqualTo(10);
        Assertions.assertThat(getValueFromFluxTables(res,"OutputWatt", 0,1)).isEqualTo(11);
    }

    @ParameterizedTest
    @EnumSource(Endpoints.class)
    public void checkCalculateAfterwardsTimeRange(Endpoints endpoint) throws InterruptedException {

        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system.setDeyeSunSerials(Collections.singleton(1234L));
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO1 = createDefaultSampleDTO();
        var deviceDTO1 = sampleDTO1.getDevices().get(0);
        var inputDCDTO1 = deviceDTO1.getInputsDC().get(0);
        var outputACDTO1 = deviceDTO1.getOutputsAC().get(0);

        sampleDTO1.setDuration(300.f);
        inputDCDTO1.setWatt(10.f);
        outputACDTO1.setWatt(11.f);

        deviceDTO1.setId(0L);
        sampleDTO1.setTimestamp(start.toEpochMilli());

        SampleDTO sampleDTO2 = createDefaultSampleDTO();
        var deviceDTO2 = sampleDTO2.getDevices().get(0);
        var inputDCDTO2 = deviceDTO2.getInputsDC().get(0);
        var outputACDTO2 = deviceDTO2.getOutputsAC().get(0);

        sampleDTO2.setDuration(300.f);
        inputDCDTO2.setWatt(20.f);
        outputACDTO2.setWatt(22.f);

        deviceDTO2.setId(1L);
        sampleDTO2.setTimestamp(start.minus(6, ChronoUnit.MINUTES).toEpochMilli());

        sendSample(endpoint, Arrays.asList(sampleDTO1,sampleDTO2),system);

        var res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());
        Assertions.assertThat(res).isEmpty();

        LOG.info("Waiting for calculation afterwards to be done");
        Thread.sleep(SolarDataConverter.AFTERWARDS_CALCULATION_WAIT * 1000 + 5000);

        res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());

        Assertions.assertThat(getValueFromFluxTables(res,"InputWatt",0,2)).isEqualTo(20);
        Assertions.assertThat(getValueFromFluxTables(res,"OutputWatt",0,2)).isEqualTo(22);

        Assertions.assertThat(getValueFromFluxTables(res,"InputWatt",1,2)).isEqualTo(10);
        Assertions.assertThat(getValueFromFluxTables(res,"OutputWatt",1,2)).isEqualTo(11);
    }

    @ParameterizedTest
    @EnumSource(Endpoints.class)
    public void checkCalculateAfterwardsSummarizeMultiple(Endpoints endpoint) throws InterruptedException {

        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system.setDeyeSunSerials(Collections.singleton(1234L));
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO1 = createDefaultSampleDTO(true);
        var deviceDTO1 = sampleDTO1.getDevices().get(0);
        var inputDCDTO1 = deviceDTO1.getInputsDC().get(0);
        var inputACDTO1 = deviceDTO1.getInputsAC().get(0);
        var outputACDTO1 = deviceDTO1.getOutputsAC().get(0);
        var outputDCDTO1 = deviceDTO1.getOutputsDC().get(0);

        sampleDTO1.setDuration(300.f);

        inputDCDTO1.setWatt(1.f);
        inputACDTO1.setWatt(10.f);
        outputACDTO1.setWatt(2.f);
        outputDCDTO1.setWatt(20.f);

        deviceDTO1.setId(0L);
        sampleDTO1.setTimestamp(start.toEpochMilli());

        SampleDTO sampleDTO2 = createDefaultSampleDTO(true);
        var deviceDTO2 = sampleDTO2.getDevices().get(0);
        var inputDCDTO2 = deviceDTO2.getInputsDC().get(0);
        var inputACDTO2 = deviceDTO2.getInputsAC().get(0);
        var outputACDTO2 = deviceDTO2.getOutputsAC().get(0);
        var outputDCDTO2 = deviceDTO2.getOutputsDC().get(0);

        sampleDTO2.setDuration(300.f);

        inputDCDTO2.setWatt(100.f);
        inputACDTO2.setWatt(1000.f);
        outputACDTO2.setWatt(200.f);
        outputDCDTO2.setWatt(2000.f);

        deviceDTO2.setId(1L);
        sampleDTO2.setTimestamp(start.toEpochMilli());

        sendSample(endpoint, Arrays.asList(sampleDTO1,sampleDTO2),system);

        var res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());
        Assertions.assertThat(res).isEmpty();

        LOG.info("Waiting for calculation afterwards to be done");
        Thread.sleep(SolarDataConverter.AFTERWARDS_CALCULATION_WAIT * 1000 + 5000);

        res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());

        Assertions.assertThat(getValueFromFluxTables(res,"InputWatt",0,1)).isEqualTo(1111);
        Assertions.assertThat(getValueFromFluxTables(res,"OutputWatt",0,1)).isEqualTo(2222);
    }

    @ParameterizedTest
    @EnumSource(Endpoints.class)
    public void checkCalculateAfterwardsSummarize(Endpoints endpoint) throws InterruptedException {

        Instant start = Instant.now();
        var user = addUser(false);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID);

        system.setCalculateCombinedValuesAfterwards(true);
        system.setDeyeSunSerials(Collections.singleton(1234L));
        system = solarSystemRepository.save(system);

        SampleDTO sampleDTO = createDefaultSampleDTO(true);
        var deviceDTO = sampleDTO.getDevices().get(0);
        var inputDCDTO = deviceDTO.getInputsDC().get(0);
        var inputACDTO = deviceDTO.getInputsAC().get(0);
        var outputACDTO = deviceDTO.getOutputsAC().get(0);
        var outputDCDTO = deviceDTO.getOutputsDC().get(0);

        sampleDTO.setDuration(300.f);

        inputDCDTO.setWatt(1.f);
        inputACDTO.setWatt(10.f);
        outputACDTO.setWatt(2.f);
        outputDCDTO.setWatt(20.f);

        deviceDTO.setId(0L);
        sampleDTO.setTimestamp(start.toEpochMilli());
        sendSample(endpoint, Collections.singletonList(sampleDTO),system);

        var res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());
        Assertions.assertThat(res).isEmpty();

        LOG.info("Waiting for calculation afterwards to be done");
        Thread.sleep(SolarDataConverter.AFTERWARDS_CALCULATION_WAIT * 1000 + 5000);

        res = getAllCombinedSamplesFromInflux(system.getInfluxTagName());

        Assertions.assertThat(getValueFromFluxTables(res,"InputWatt",0,1)).isEqualTo(11);
        Assertions.assertThat(getValueFromFluxTables(res,"OutputWatt",0,1)).isEqualTo(22);
    }
}
