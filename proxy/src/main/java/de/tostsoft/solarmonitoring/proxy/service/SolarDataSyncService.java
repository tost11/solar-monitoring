package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SolarDataSyncService {

    @Value("${proxy.token}")
    private String proxyToken;

    @Value("${proxy.url}")
    private String proxyUrl;

    @Value("${proxy.sync.amount:20}")
    private int syncAmount;

    @Value("${proxy.sync.retries:10}")
    private int syncRetries;

    @Value("${proxy.sync.wait:500}")
    private int syncWaitTime;
    @Value("${proxy.sync.error:5000}")
    private int syncWaitTimeError;

    private final Logger LOG = LoggerFactory.getLogger(SolarDataSyncService.class);

    @Autowired
    private ProxySolarSampleRepository proxySolarSampleRepository;

    @Autowired
    private SystemSyncService systemSyncService;

    static private RestTemplate defaultRestTemplate;

    @PostConstruct
    private void setup(){
        defaultRestTemplate = new RestTemplateBuilder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT,"solar-proxy")
                .defaultHeader("proxyToken",proxyToken).build();
    }

    @Scheduled(fixedDelayString = "${proxy.sync.data}")
    void resendMissingData(){
        if(!systemSyncService.getIsOnline()){
            LOG.info("Skipping syncing because other application is offline");
            return;
        }

        var groupedSamples = proxySolarSampleRepository.getSampleSums();

        LOG.info("Found samples grouped by SystemId: "+groupedSamples);

        int retries = 0;

        for(var sampleGroup : groupedSamples){
            while(true){
                var toSend = proxySolarSampleRepository.findSomeSamplesWithSystemId(sampleGroup.id,syncAmount);

                if(toSend.isEmpty()){
                    break;
                }

                HttpEntity<List<SampleDTO>> entity = new HttpEntity<>(toSend.stream().map(t->t.getSample().getSampleDTO()).toList());

                try{
                    var res = defaultRestTemplate.exchange(proxyUrl+"/api/solar/data/proxy?systemId=" + sampleGroup.id, HttpMethod.POST, entity, String.class);
                    LOG.info("Synced " + toSend.size() + " samples from system: " + sampleGroup.id + " Body: "+res.getBody());
                }catch (Exception e){
                    LOG.debug(e.getMessage());
                    LOG.error("Could not post systems from main application");
                    boolean ok = false;
                    if(e instanceof HttpStatusCodeException statusCodeException){
                        //when this is thrown validation or limit failed -> ignore this //TODO find better way to do this, also add unit tests for it
                        if(statusCodeException.getStatusCode() == HttpStatus.BAD_REQUEST &&
                                StringUtils.containsIgnoreCase(statusCodeException.getResponseBodyAsString(),"invalidSamplesIndexes") &&
                                StringUtils.containsIgnoreCase(statusCodeException.getResponseBodyAsString(),"dailyLimitReachedIndexes")
                        ){
                            ok = true;
                            LOG.info("Status: " + statusCodeException.getStatusCode() + " Body: " + statusCodeException.getResponseBodyAsString()+" but was handled as ok because limit of day is reached");
                        }else{
                            LOG.error("Status: " + statusCodeException.getStatusCode() + " Body: " + statusCodeException.getResponseBodyAsString());
                        }
                    }

                    if(!ok) {
                        retries++;

                        if (retries >= syncRetries) {
                            LOG.error("Skip syncing data because of to many errors");
                            try {
                                Thread.sleep(syncWaitTimeError);
                            } catch (InterruptedException ex) {
                            }
                            return;
                        }

                        try {
                            Thread.sleep(syncWaitTime);
                        } catch (InterruptedException ex) {
                        }
                        continue;
                    }
                }

                proxySolarSampleRepository.deleteAll(toSend);

                if(toSend.size() < syncAmount){
                    break;
                }
            }
        }
    }

    public boolean syncEntries(String systemId,List<SampleDTO> samples){
        HttpEntity<List<SampleDTO>> entity = new HttpEntity<>(samples);
        try{
            defaultRestTemplate.exchange(proxyUrl+"/api/solar/data/proxy?systemId=" + systemId, HttpMethod.POST, entity, String.class);
        }catch (Exception e){
            LOG.info("Directly sync not possible for system {} so store {} entries in database", systemId, samples.size());
            return false;
        }
        LOG.info("Directly Synced " + samples.size() + " samples from system: " + systemId);
        return true;
    }
}
