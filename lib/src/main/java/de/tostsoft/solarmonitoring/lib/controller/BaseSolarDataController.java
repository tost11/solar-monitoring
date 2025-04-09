package de.tostsoft.solarmonitoring.lib.controller;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/solar/data")
@Validated
public abstract class BaseSolarDataController {

  @PostMapping
  public abstract void PostDevice(@RequestParam String systemId, @RequestBody @Valid SampleDTO solarSample, @RequestHeader String clientToken);

  @PostMapping("/mult")
  public abstract void PostDeviceMult(@RequestParam String systemId, @RequestBody @Valid List<SampleDTO> solarSamples, @RequestHeader String clientToken);

  @PostMapping("/deye")
  public abstract void PostDeviceDeye(@RequestParam String serialId, @RequestBody SampleDTO solarSample,  @RequestHeader String clientToken);
}
