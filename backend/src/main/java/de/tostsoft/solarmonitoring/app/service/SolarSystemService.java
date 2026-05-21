package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.configuration.TaskSchedulerConfiguration;
import de.tostsoft.solarmonitoring.app.controller.StatusController;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.CurrentValuesDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ManagesSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.NewTokenDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PatchSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.PublicSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.RegisterSolarSystemResponseDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.ViewDataDTO;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.TotalValues;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import de.tostsoft.solarmonitoring.lib.service.InfluxTaskService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;


@Service
public class SolarSystemService {

    @Autowired
    private InfluxTaskService influxTaskService;

    @Autowired
    private InfluxService influxService;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StatusController statusController;

    @Value("${system.defaultMaxSamplesDay}")
    private long defaultMaxSamplesDaySysgtem;

    @Autowired
    private TaskSchedulerConfiguration taskSchedulerConfiguration;

    private static final Logger LOG = LoggerFactory.getLogger(SolarSystemService.class);

    public RegisterSolarSystemResponseDTO createSystemForUser(RegisterSolarSystemDTO registerSolarSystemDTO, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (user.getOwns().size() >= user.getNumAllowedSystems()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have to much Systems");
        }

        if (registerSolarSystemDTO.getShortener() != null) {
            if (solarSystemRepository.existsByShortener(registerSolarSystemDTO.getShortener())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shortener name already taken");
            }
        }

        String token = UUID.randomUUID().toString();

        var vd = Converter.convertToViewData(registerSolarSystemDTO.getViewData());

        var objectId = new ObjectId();

        var solarSystem = SolarSystem.builder()
                .id(objectId.toString())
                .influxTagName(objectId.toString())
                .viewName(registerSolarSystemDTO.getName())
                .name(StringUtils.lowerCase(registerSolarSystemDTO.getName()))
                .shortener(StringUtils.lowerCase(registerSolarSystemDTO.getShortener()))
                .creationDate(LocalDateTime.now())
                .type(registerSolarSystemDTO.getType())
                .buildingDate(registerSolarSystemDTO.getBuildingDate() != null ? registerSolarSystemDTO.getBuildingDate().toLocalDateTime() : null)
                .ownedBy(user)
                .token(passwordEncoder.encode(token))
                .viewData(vd)
                .timezone(registerSolarSystemDTO.getTimezone())
                .publicMode(registerSolarSystemDTO.getPublicMode())
                .namings(Converter.convertDTOtoNamings(registerSolarSystemDTO.getNamings()))
                .electricityPrice(registerSolarSystemDTO.getElectricityPrice())
                .electricityPriceFeedIn(registerSolarSystemDTO.getElectricityPriceFeedIn())
                .maxInstalledSolarPower(registerSolarSystemDTO.getMaxInstalledSolarPower())
                .maxInverterOutputPower(registerSolarSystemDTO.getMaxInverterOutputPower())
                .deyeSunSerials(Converter.convertStringToDeyeSerials(registerSolarSystemDTO.getDeyeSunSerialNumbers()))
                .calculateCombinedValuesAfterwards(registerSolarSystemDTO.getCalculateCombinedValuesAfterwards())
                .tags(new ArrayList<>())
                .totalValues(TotalValues.builder().build())
                .maxSamplesOnDay(user.getIsAdmin() ? -1L : defaultMaxSamplesDaySysgtem)
                .build();

        solarSystem = solarSystemRepository.save(solarSystem);

        if (solarSystem.getElectricityPrice() != null) {
            influxService.updatePrice(solarSystem, null);
        }

        if (solarSystem.getElectricityPriceFeedIn() != null) {
            influxService.updatePriceFeedIn(solarSystem, null);
        }

        return RegisterSolarSystemResponseDTO.builder()
                .id(solarSystem.getId())
                .buildingDate(solarSystem.getBuildingDate() != null ? ZonedDateTime.of(solarSystem.getBuildingDate(), ZoneId.of(solarSystem.getTimezone())) : null)
                .creationDate(ZonedDateTime.of(solarSystem.getCreationDate(), ZoneId.of(solarSystem.getTimezone())))
                .name(solarSystem.getName())
                .shortener(solarSystem.getShortener())
                .viewName(solarSystem.getViewName())
                .type(solarSystem.getType())
                .token(token)
                .viewData(Converter.convertToViewDataDTO(solarSystem.getViewData()))
                .namings(Converter.convertNamingsToDTO(solarSystem.getNamings()))
                .publicMode(solarSystem.getPublicMode())
                .electricityPrice(solarSystem.getElectricityPrice())
                .electricityPriceFeedIn(solarSystem.getElectricityPriceFeedIn())
                .deyeSunSerialNumbers(Converter.convertDeyeSerialsToString(solarSystem.getDeyeSunSerials()))
                .build();
    }

    public RegisterSolarSystemResponseDTO createSystem(RegisterSolarSystemDTO registerSolarSystemDTO) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return createSystemForUser(registerSolarSystemDTO, user);
    }


    public Pair<PublicSolarSystemDTO, SolarSystem> getSystemWithUserFromContextOrPublic(String id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        var optSolarSystem = solarSystemRepository.findAllByIdOrShortener(id);
        if (optSolarSystem.isEmpty()) {
            return null;//then throws forbidden
        }

        var solarSystem = optSolarSystem.get(0);

        if (auth != null) {
            var user = (User) auth.getPrincipal();

            var managesOpt = solarSystem.getManagedBy().stream().filter(man -> StringUtils.equals(man.getUser().getId(), user.getId())).findAny();
            boolean isOwner = StringUtils.equals(solarSystem.getOwnedBy().getId(), user.getId());

            if (isOwner || managesOpt.isPresent()) {
                if (isOwner || managesOpt.get().getPermission() == Permissions.ADMIN) {
                    var ret = Converter.convertSystemToDTO(solarSystem);
                    ret.setStatus(statusController.getAllStatusInternal(solarSystem));
                    return new ImmutablePair<>(ret, solarSystem);
                } else if (managesOpt.get().getPermission() == Permissions.MANAGE) {
                    var ret = Converter.convertSystemToManagerDTO(solarSystem);
                    ret.setStatus(statusController.getAllStatusInternal(solarSystem));
                    return new ImmutablePair<>(ret, solarSystem);
                } else {
                    var ret = Converter.convertSystemToViewDTO(solarSystem);
                    return new ImmutablePair<>(ret, solarSystem);
                }
            }
        }

        //this is public available

        if (solarSystem.getPublicMode() == PublicMode.NONE) {
            return null;
        }

        var onlyProduction = solarSystem.getPublicMode() == PublicMode.PRODUCTION;

        var res = Converter.convertSystemToPublicDTO(solarSystem);

        if (onlyProduction) {
            res.setPublicFlagOnlyProduction(true);
            // Graph filters are now automatically handled by the converter
            // Consumption-related graphs are hidden via graphFilter
            if (solarSystem.getViewData().getTotalPricingPublicOverride() != Boolean.TRUE) {
                res.getViewData().setProductionForTotalPricing(null);
            }
            res.getNamings().getBatteries().clear();
            res.getNamings().getInputsAC().clear();
            res.getNamings().getOutputsDC().clear();
            res.getNamings().getOutputsAC().clear();
        }

        return new ImmutablePair<>(res, solarSystem);
    }

    public List<SolarSystemListItemDTO> getSystemsWithUserFromContext() {
        var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //TODO disable lazy loading for this query
        var user = userRepository.findById(authUser.getId()).get();

        ArrayList<SolarSystemListItemDTO> res = new ArrayList<>();

        for (var system : user.getOwns()) {
            var dto = Converter.convertSystemToListItemDTO(system, "owns");
            if (system.isOnline()) {
                dto.setCurrentValues(Converter.converterToCurrentValuesDTO(system.getCurrentValues()));
            }
            res.add(dto);
        }

        for (var manages : user.getManges()) {
            var system = manages.getSolarSystem();
            var dto = Converter.convertSystemToListItemDTO(system, "owns");
            if (system.isOnline()) {
                dto.setCurrentValues(Converter.converterToCurrentValuesDTO(system.getCurrentValues()));
            }
            res.add(dto);
        }

        return res;
    }

    public SolarSystemListItemDTO solarSystemToListItemDTO(SolarSystem solarSystem, User user) {
        String mode = "public";

        if (user != null) {
            if (StringUtils.equals(solarSystem.getOwnedBy().getId(), user.getId())) {
                mode = "owns";
            } else if (solarSystem.getManagedBy().stream().anyMatch(man -> StringUtils.equals(man.getUser().getId(), user.getId()) && man.getPermission() == Permissions.ADMIN)) {
                mode = "owns";
            } else if (solarSystem.getManagedBy().stream().anyMatch(man -> StringUtils.equals(man.getUser().getId(), user.getId()) && man.getPermission() == Permissions.MANAGE)) {
                mode = "manages";
            } else if (solarSystem.getManagedBy().stream().anyMatch(man -> StringUtils.equals(man.getUser().getId(), user.getId()) && man.getPermission() == Permissions.VIEW)) {
                mode = "view";
            }
        }
        var dto = Converter.convertSystemToListItemDTO(solarSystem, mode);

        if (solarSystem.isOnline()) {
            dto.setCurrentValues(CurrentValuesDTO.builder()
                    .inputWatt(solarSystem.getCurrentValues().getInputWatt())
                    .batteryVoltage(!mode.equals("public") || solarSystem.getPublicMode() != PublicMode.PRODUCTION ? solarSystem.getCurrentValues().getBatteryVoltage() : null)
                    .build());
        }
        if (solarSystem.getTotalValues() != null && solarSystem.getTotalValues().getProducedKWH() != null) {
            dto.setTotalProducedWH(solarSystem.getTotalValues().getProducedKWH() * 1000);
        }

        return dto;
    }

    public List<SolarSystemListItemDTO> getPublicSystems() {

        List<SolarSystem> solarSystems = solarSystemRepository.findAllByPublicModeIsNot(PublicMode.NONE);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = auth != null && auth.isAuthenticated() ? (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;

        List<SolarSystemListItemDTO> res = new ArrayList<>();

        for (SolarSystem solarSystem : solarSystems) {
            res.add(solarSystemToListItemDTO(solarSystem, user));
        }
        return res;
    }

    public ResponseEntity<String> deleteSystem(SolarSystem solarSystem) {
        solarSystem.setDeletedAt(LocalDateTime.now());
        solarSystemRepository.save(solarSystem);
        return ResponseEntity.status(HttpStatus.OK).body("System is Deleted");
    }

    public ManagesSolarSystemDTO patchSolarSystem(PatchSolarSystemDTO newSolarSystemDTO, SolarSystem solarSystem) {

        boolean timeZoneChanged = !StringUtils.equals(newSolarSystemDTO.getTimezone(), solarSystem.getTimezone());

        solarSystem.setName(StringUtils.lowerCase(newSolarSystemDTO.getName()));
        solarSystem.setViewName(newSolarSystemDTO.getName());
        solarSystem.setBuildingDate(newSolarSystemDTO.getBuildingDate() != null ? newSolarSystemDTO.getBuildingDate().toLocalDateTime() : null);
        solarSystem.setType(newSolarSystemDTO.getType());
        solarSystem.setShortener(newSolarSystemDTO.getShortener());
        solarSystem.setCalculateCombinedValuesAfterwards(newSolarSystemDTO.getCalculateCombinedValuesAfterwards());

        boolean firstElectricityPrice = solarSystem.getElectricityPrice() == null;
        boolean firstElectricityPriceFeedIn = solarSystem.getElectricityPriceFeedIn() == null;
        boolean electricityPricesUpdated = !StringUtils.equals("" + newSolarSystemDTO.getElectricityPrice(), "" + solarSystem.getElectricityPrice());
        boolean electricityPricesFeedInUpdated = !StringUtils.equals("" + newSolarSystemDTO.getElectricityPriceFeedIn(), "" + solarSystem.getElectricityPriceFeedIn());

        if (newSolarSystemDTO.getElectricityPrice() != null) {
            solarSystem.setElectricityPrice(newSolarSystemDTO.getElectricityPrice());
        }

        if (newSolarSystemDTO.getElectricityPriceFeedIn() != null) {
            solarSystem.setElectricityPriceFeedIn(newSolarSystemDTO.getElectricityPriceFeedIn());
        }

        solarSystem.setMaxInstalledSolarPower(newSolarSystemDTO.getMaxInstalledSolarPower());
        solarSystem.setMaxInverterOutputPower(newSolarSystemDTO.getMaxInverterOutputPower());

        var vd = Converter.convertToViewData(newSolarSystemDTO.getViewData());
        solarSystem.setViewData(vd);

        solarSystem.setTimezone(newSolarSystemDTO.getTimezone());
        solarSystem.setPublicMode(newSolarSystemDTO.getPublicMode());
        solarSystem.setNamings(Converter.convertDTOtoNamings(newSolarSystemDTO.getNamings()));

        solarSystem.setDeyeSunSerials(Converter.convertStringToDeyeSerials(newSolarSystemDTO.getDeyeSunSerialNumbers()));

        if (newSolarSystemDTO.getShortener() != null) {
            if (solarSystemRepository.existsByShortenerAndIdNotWithDeleted(newSolarSystemDTO.getShortener(), solarSystem.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shortener name already taken");
            }
        }

        var res = solarSystemRepository.save(solarSystem);

        if (timeZoneChanged) {
            LOG.info("System timezone changed run full generation of day values");

            if (influxTaskService.runInitial(res,taskSchedulerConfiguration.recalculateStatisticsThreadPool())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This calculation is only allowed once a day try tomorrow");
            }
        }

        if (electricityPricesUpdated) {
            influxService.updatePrice(res, firstElectricityPrice ? solarSystem.getCreationDateZoned() : null);
        }

        if (electricityPricesFeedInUpdated) {
            influxService.updatePriceFeedIn(res, firstElectricityPriceFeedIn ? solarSystem.getCreationDateZoned() : null);
        }


        //managed is ok because manger still loaded
        return Converter.convertSystemToManagerDTO(res);
    }

    public NewTokenDTO createNewToken(SolarSystem solarSystem) {
        String token = UUID.randomUUID().toString();
        solarSystemRepository.updateToken(solarSystem.getId(), passwordEncoder.encode(token));
        return new NewTokenDTO(token);
    }

    public SolarSystem findSystemWithOwnedBy(String systemId) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var solarSystemOptional = solarSystemRepository.findByIdAndOwnedById(systemId, user.getId());
        return solarSystemOptional.get();
    }

    public SolarSystem findSystemWithFullAccess(String systemId) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var solarSystemOpt = solarSystemRepository.findById(systemId);
        if (solarSystemOpt.isEmpty()) {
            return null;
        }
        var system = solarSystemOpt.get();
        if (StringUtils.equals(system.getOwnedBy().getId(), user.getId())) {
            return system;
        }
        if (system.getManagedBy().stream()
                .anyMatch(m -> m.getPermission() == Permissions.ADMIN && StringUtils.equals(m.getUser().getId(), user.getId()))) {
            return system;
        }
        return null;
    }

    public SolarSystem findSystemWithMangeAccess(String systemId) {
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var solarSystemOpt = solarSystemRepository.findById(systemId);
        if (solarSystemOpt.isEmpty()) {
            return null;
        }
        var system = solarSystemOpt.get();
        if (StringUtils.equals(system.getOwnedBy().getId(), user.getId())) {
            return system;
        }
        if (system.getManagedBy().stream()
                .anyMatch(m -> (m.getPermission() == Permissions.ADMIN || m.getPermission() == Permissions.MANAGE) && StringUtils.equals(m.getUser().getId(), user.getId()))) {
            return system;
        }
        return null;
    }

    public Pair<SolarSystem, PublicMode> sysemtToAccesPair(SolarSystem system) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            var user = (User) auth.getPrincipal();
            if (StringUtils.equals(system.getOwnedBy().getId(), user.getId())) {
                return new ImmutablePair(system, null);
            }

            if (system.getManagedBy().stream()
                    .anyMatch(m -> StringUtils.equals(m.getUser().getId(), user.getId()))) {
                return new ImmutablePair(system, null);
            }
        }

        if (system.getPublicMode() == PublicMode.PRODUCTION || system.getPublicMode() == PublicMode.ALL) {
            return new ImmutablePair(system, system.getPublicMode());
        }
        return null;
    }

    public Pair<SolarSystem, PublicMode> findSolarSystemByWithAccess(String systemId) {

        var solarSystemOpt = solarSystemRepository.findById(systemId);
        if (solarSystemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }

        var pair = sysemtToAccesPair(solarSystemOpt.get());
        if (pair == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on this System");
        }
        return pair;
    }

    public List<Pair<SolarSystem, PublicMode>> findSolarSystemsByWithAccess(Collection<String> systemIds) {

        var systems = solarSystemRepository.findAllByIdOrShortenerIn(systemIds);
        if (systems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on any of the System (or they dose not exist)");
        }

        var ret = new ArrayList<Pair<SolarSystem, PublicMode>>();

        for (SolarSystem system : systems) {
            var pair = sysemtToAccesPair(system);
            if (pair != null) {
                ret.add(pair);
            }
        }

        if (ret.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have no access on any of the System (or they dose not exist)");
        }

        return ret;
    }
}