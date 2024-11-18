package de.tostsoft.solarmonitoring.proxy.service;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import de.tostsoft.solarmonitoring.proxy.repository.ProxySolarSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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

    @Value("${proxy.sync.wait:5000}")
    private int syncWaitTime;

    private Logger LOG = LoggerFactory.getLogger(SolarDataSyncService.class);

    @Autowired
    private ProxySolarSampleRepository proxySolarSampleRepository;

    @Autowired
    private SystemSyncService systemSyncService;

    @Scheduled(fixedDelay = 1000 * 60 * 11)
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

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.add("user-agent", "solar-proxy");
                headers.set("proxyToken",proxyToken);

                HttpEntity<List<SampleDTO>> entity = new HttpEntity<>(toSend.stream().map(t->t.getSample().getSampleDTO()).toList(), headers);

                ResponseEntity<String> res;
                try{
                    res = restTemplate.exchange(proxyUrl+"/api/solar/data/proxy?systemId=" + sampleGroup.id, HttpMethod.POST, entity, String.class);
                }catch (Exception e){
                    LOG.debug(e.getMessage());
                    LOG.error("Could not post systems from main application");
                    if(e instanceof HttpStatusCodeException statusCodeException){
                        LOG.error("Status: " + statusCodeException.getStatusCode() + " Body: " + statusCodeException.getResponseBodyAsString());
                    }
                    retries++;

                    if(retries >= syncRetries){
                        LOG.error("Skip syncing data because of to many errors");
                        return;
                    }

                    try {
                        Thread.sleep(syncWaitTime);
                    } catch (InterruptedException ex) {
                    }
                    continue;
                }

                LOG.info("Synced " + toSend.size() + " samples from system: " + sampleGroup.id + " Body: "+res.getBody());

                proxySolarSampleRepository.deleteAll(toSend);

                if(toSend.size() < syncAmount){
                    break;
                }
            }
        }
    }
}
