package de.tostsoft.solarmonitoring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import de.tostsoft.solarmonitoring.dtos.status.AllStatusResponseDTO;
import de.tostsoft.solarmonitoring.dtos.status.BooleanStatus;
import de.tostsoft.solarmonitoring.model.SolarSystem;
import de.tostsoft.solarmonitoring.service.SolarService;
import de.tostsoft.solarmonitoring.service.SolarSystemService;
import de.tostsoft.solarmonitoring.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController("/api/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @Autowired
    private SolarService solarService;

    private List<BooleanStatus> convertToBooleanStatusDTOs(final List<FluxTable> fluxResult){
        if(fluxResult.size()==0){
            return new ArrayList<>();
        }

        var res = new ArrayList<BooleanStatus>();

        for (FluxTable r : fluxResult) {
            for (FluxRecord record : r.getRecords()) {
                System.out.println(r);
                res.add(BooleanStatus.builder()
                        .name(record.getField())
                        .value((Boolean)record.getValue())
                        .lastSet(record.getTime().atZone(TimeZone.getDefault().toZoneId()))
                        .build());
            }
        }

        return res;
    }

    @PostMapping("/{systemId}")
    public void setStatus(@PathVariable long systemId,@RequestParam String name, @RequestHeader String clientToken,@PathVariable boolean setValue) {
        var system = solarService.findMatchingSystemWithToken(systemId, clientToken);//throws exception in unauthorized

        if (system == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found");
        }

        Pattern p = Pattern.compile("^[a-z0-9_-äüöÄÜÖßé]{3,30}$");
        Matcher m = p.matcher(name);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }

        statusService.setStatus(name,setValue,systemId,system.getId());
    }

    @GetMapping("/{systemId}")
    public BooleanStatus getStatus(@PathVariable long systemId,@RequestParam String name, @RequestHeader String clientToken) {
        var system = solarService.findMatchingSystemWithToken(systemId, clientToken);//throws exception in unauthorized

        if (system == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found");
        }

        Pattern p = Pattern.compile("^[a-z0-9_-äüöÄÜÖßé]{3,30}$");
        Matcher m = p.matcher(name);
        if(!m.matches()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name dose not match requirements");
        }

        var booleanInfluxRes = statusService.getStatus(system.getRelationOwnedBy().getId(),systemId,name);
        var res = convertToBooleanStatusDTOs(booleanInfluxRes);
        if(res.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Status not found");
        }
        if(res.size() > 1){
            throw new RuntimeException("Multiple results returned for status ist should only be one");
        }
        return res.get(0);
    }

    @GetMapping("/all/{systemId}")
    public AllStatusResponseDTO getAllStatus(@PathVariable long systemId, @RequestHeader String clientToken){
        var system = solarService.findMatchingSystemWithToken(systemId,clientToken);//throws exception in unauthorized

        if(system == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"System not found");
        }

        return getAllStatusInternal(system);
    }

    public AllStatusResponseDTO getAllStatusInternal(SolarSystem system){

        var booleanInfluxRes = statusService.getStatus(system.getRelationOwnedBy().getId(),system.getId());

        return AllStatusResponseDTO.builder()
                .booleans(convertToBooleanStatusDTOs(booleanInfluxRes))
                .build();
    }
}
