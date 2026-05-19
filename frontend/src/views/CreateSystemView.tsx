import React, {useState} from "react";
import {
  Box,
  Button,
  Collapse,
  Divider,
  FormControl,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material/Select';
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {
  createSystem,
  patchSystem,
  SolarSystemDTO,
  SolarSystemPublicMode,
  SolarSystemType,
  updateStatistics
} from "../api/SolarSystemAPI";
import moment from "moment";
import {toast} from "react-toastify";
import MyTimezonePicker from "../Component/time/MyTimezonePicker";
import {useNavigate} from "react-router-dom";
import NamingsManager from "../Component/NamingsManager";
import SolarSystemTypeSelect from "../Component/SolarSystemTypeSelect";
import {useTranslation} from "react-i18next";
import DeleteUserModal from "../Component/modal/DeleteUserModal";
import DeleteSystemModal from "../Component/modal/DeleteSystemModal";
import TotalFilterList from "../Component/TotalFilterList";
import GraphFilterList from "../Component/GraphFilterList";

// Helper to convert empty strings to undefined for backend
const nOF = (value: string | number | null | undefined): string | number | undefined => {
  if (value === "" || value === null) return undefined;
  return value;
};

const AVAILABLE_GRAPH_FILTERS = [
  "INPUT_WATT_DC", "INPUT_WATT_AC", "INPUT_WATT_COMBINED", "INPUT_VOLTAGE_DC", "INPUT_VOLTAGE_AC",
  "INPUT_AMPERE_DC", "INPUT_AMPERE_AC", "INPUT_FREQUENCY",
  "OUTPUT_WATT_DC", "OUTPUT_WATT_AC", "OUTPUT_WATT_COMBINED", "OUTPUT_VOLTAGE_DC", "OUTPUT_VOLTAGE_AC",
  "OUTPUT_AMPERE_DC", "OUTPUT_AMPERE_AC", "OUTPUT_FREQUENCY", "OUTPUT_TOTAL_CONSUMPTION",
  "BATTERY_WATT", "BATTERY_VOLTAGE", "BATTERY_AMPERE", "BATTERY_SOC",
  "GRID_WATT", "GRID_VOLTAGE", "GRID_AMPERE", "GRID_FREQUENCY",
  "MORE_TEMPERATURE"
];

const DEFAULT_GRAPH_FILTERS = [
  "INPUT_WATT_AC", "INPUT_VOLTAGE_AC", "INPUT_AMPERE_AC", "INPUT_FREQUENCY",
  "OUTPUT_WATT_DC", "OUTPUT_VOLTAGE_DC", "OUTPUT_AMPERE_DC",
  "INPUT_AMPERE_DC", "OUTPUT_AMPERE_AC", "BATTERY_AMPERE", "GRID_AMPERE"
];

interface editSystemProps {
  data?: SolarSystemDTO
}

export default function CreateSystemView({data}: editSystemProps) {

  const { t } = useTranslation()

  const [isLoading,setIsLoading] = useState(false)

  const [systemName, setSystemName] = useState(data?.viewName?data.viewName:"")
  const [shortener, setShortener] = useState(data?.shortener)
  const [systemType, setSystemType] = useState(data?.type?data.type:SolarSystemType.SELFMADE)
  const [buildingDate, setBuildingDate] = useState(data?.buildingDate)
  const [calculateCombinedValuesAfterwards, setCalculateCombinedValuesAfterwards] = useState(data?.calculateCombinedValuesAfterwards)
  const [defaultDelay, setDefaultDelay] = useState(data?.viewData.defaultDelay)
  const [productionForTotalPricing, setProductionForTotalPricing] = useState(data?.viewData.productionForTotalPricing)
  const [totalPricingPublicOverride, setTotalPricingPublicOverride] = useState(data?.viewData.totalPricingPublicOverride)
  const [hideTotalConsumption, setHideTotalConsumption] = useState(data?.viewData.hideTotalConsumption)
  const [showGridInfo, setShowGridInfo] = useState(data?.viewData.showGridInfo)
  const [hasTemperature, setHasTemperature] = useState(data?.viewData.hasTemperature !== undefined?data?.viewData.hasTemperature:false)
  const [voltageAC, setVoltageAC] = useState(data?.viewData.voltageAC)
  const [batteryVoltage, setBatteryVoltage] = useState(data?.viewData.batteryVoltage)
  const [maxSolarVoltage, setMaxSolarVoltage] = useState(data?.viewData.maxSolarVoltage)
  const [timezone,setTimezone] = useState(data?.timezone ? data.timezone : moment.tz.guess())
  const [publicMode,setPublicMode] = useState(data?.publicMode?data.publicMode:SolarSystemPublicMode.NONE)
  const [electricityPrice, setElectricityPrice] = useState(data?.electricityPrice)
  const [electricityPriceFeedIn, setElectricityPriceFeedIn] = useState(data?.electricityPriceFeedIn)
  const [deyeSunSerialNumbers, setDeyeSunSerialNumbers] = useState(data?.deyeSunSerialNumbers)
  //<{[key: number]: string}>
  const [namingsDevices, setNamingsDevices] = useState(data ?data.namings.devices : {})
  const [namingsInputsDC, setNamingsInputsDC] = useState(data ?data.namings.inputsDC : {})
  const [namingsInputsAC, setNamingsInputsAC] = useState(data ?data.namings.inputsAC : {})
  const [namingsOutputsDC, setNamingsOutputsDC] = useState(data ?data.namings.outputsDC : {})
  const [namingsOutputsAC, setNamingsOutputsAC] = useState(data ?data.namings.outputsAC : {})
  const [namingsBatteries, setNamingsBatteries] = useState(data ?data.namings.batteries : {})
  const [namingsGrids, setNamingsGrids] = useState(data ? data.namings.grids : {})
  const [totalFilter, setTotalFilter] = useState<string[]>(data?.viewData.totalFilter ? Array.from(data.viewData.totalFilter) : [])
  const [newFilterName, setNewFilterName] = useState<string>("")
  const [availableFiltersExpanded, setAvailableFiltersExpanded] = useState(false)
  const [graphFilter, setGraphFilter] = useState<string[]>(
    data
      ? (data.viewData.graphFilter ? Array.from(data.viewData.graphFilter) : [])
      : DEFAULT_GRAPH_FILTERS
  )

  const [deleteSystemModalOpen, setDeleteSystemModalOpen] = useState(false)

  const navigate = useNavigate();

  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as SolarSystemType);
  };

  const typeNeedsACVoltage = (type:string) => {
    return type == SolarSystemType.GRID || type == SolarSystemType.GRID_BATTERY || type == SolarSystemType.SELFMADE
  }

  const isBatteryType = (type:SolarSystemType) => {
    return type == SolarSystemType.SELFMADE || type == SolarSystemType.GRID_BATTERY;
  }

  const parseFloatFromInput = (input:any) => {
    if (input != ""){
      let ret = parseFloat(input);
      if(!isNaN(ret)){
        return ret;
      }
    }
    return undefined;
  }

  const parseIntFromInput = (input:any) => {
    if (input != ""){
      let ret = parseInt(input)
      if(!isNaN(ret)){
        return ret;
      }
    }
    return undefined;
  }


  const incorrectPrice = (price) => {
    return price != null && Number(price) < 0;
  }

  const addTotalFilter = () => {
    if (newFilterName && !totalFilter.includes(newFilterName)) {
      setTotalFilter([...totalFilter, newFilterName]);
      setNewFilterName("");
    }
  };

  const deleteTotalFilter = (filter: string) => {
    setTotalFilter(totalFilter.filter(f => f !== filter));
  };

  const addGraphFilter = (filter: string) => {
    if (filter && !graphFilter.includes(filter)) {
      setGraphFilter([...graphFilter, filter]);
    }
  };

  const deleteGraphFilter = (filter: string) => {
    setGraphFilter(graphFilter.filter(f => f !== filter));
  };

  //TODO split this in some components it is to large
  return <div className={"default-margin"}>
    <h3>{t("views.create_system.gernal_setting")}</h3>
    <div className="defaultFlex">
      <Box>
        <FormControl fullWidth className="Input">
          <InputLabel className="Input">{t("views.create_system.system_type")}</InputLabel>
          <SolarSystemTypeSelect preferredWidth="300px" fontSize="small" selected={systemType} setSelected={setSystemType}/>
        </FormControl>
      </Box>
      <div>
        <TextField className={"Input default-margin"} type="text" label={t("views.create_system.system_name")} value={systemName}
                   onChange={event => setSystemName(event.target.value)}/>
      </div>
      <TextField label={t("views.create_system.creation_date")} className={"Input default-margin"} type="date" value={buildingDate ? moment(buildingDate).format("yyyy-MM-DD"):moment(undefined)} onChange={event =>
          setBuildingDate(moment(event.target.value))
      }/>
      <MyTimezonePicker
          value={timezone}
          onChange={setTimezone}
      />
      <Box className="SolarTypeMenuBox">
        <FormControl fullWidth className="Input">
          <InputLabel className="Input">{t("system_common.public_mode")}</InputLabel>
          <Select
            labelId="demo-simple-select-label"
            value={publicMode}
            label={t("system_common.public_mode")}
            onChange={(event)=>{
              // @ts-ignore
              setPublicMode(event.target.value)
            }}
          >

            <MenuItem value={"NONE"} sx={{ whiteSpace: 'pre-wrap' }}>
              {t("system_common.public_mode_none")}
            </MenuItem>
            <MenuItem value={"PRODUCTION"} sx={{ whiteSpace: 'pre-wrap' }}>
              {t("system_common.public_mode_producion")}
            </MenuItem>
            <MenuItem value={"ALL"} sx={{ whiteSpace: 'pre-wrap' }}>
              {t("system_common.public_mode_all")}
            </MenuItem>
          </Select>
        </FormControl>
      </Box>
    </div>

    <h3>{t("views.create_system.view_settings")}</h3>
    <div className="defaultFlex">
      {systemType != SolarSystemType.VERY_SIMPLE && systemType != SolarSystemType.SIMPLE && <>
        <Typography>
          <Switch checked={productionForTotalPricing} onChange={() => {
            setProductionForTotalPricing(!productionForTotalPricing)
          }}/>
          {t("views.create_system.total_pricing")}
        </Typography>
        <Typography>
          <Switch checked={hideTotalConsumption} onChange={() => {
            setHideTotalConsumption(!hideTotalConsumption)
          }}/>
          {t("views.create_system.total_consumption")}
        </Typography>
        <Typography>
          <Switch checked={totalPricingPublicOverride} onChange={() => {
            setTotalPricingPublicOverride(!totalPricingPublicOverride)
          }}/>
          {t("views.create_system.total_pricing_override")}
        </Typography>
        {(systemType === SolarSystemType.GRID || systemType === SolarSystemType.GRID_BATTERY) &&
          <Typography>
            <Switch checked={showGridInfo} onChange={() => {
              setShowGridInfo(!showGridInfo)
            }}/>
            {t("views.create_system.show_grid_info")}
          </Typography>
        }
        <TextField className={"Input"} type={"number"} label={t("views.create_system.delay")} min={1}
                   variant="outlined" placeholder="30" value={defaultDelay?defaultDelay:""}  onChange={(event) => {
          setDefaultDelay(parseIntFromInput(event.target.value))
        }}/>
      </>}
    </div>

    {systemType != SolarSystemType.VERY_SIMPLE && <div>
      <h3>{t("views.create_system.panels")}</h3>
      <div >
        <TextField className={"Input"} type={"number"} label={t("views.create_system.max_solar_voltage")}
                   variant="outlined" placeholder="45" value={maxSolarVoltage?maxSolarVoltage:""}  onChange={(event) => {
          setMaxSolarVoltage(parseFloatFromInput(event.target.value))
        }}/>
      </div>
    </div>}

    {isBatteryType(systemType) && <div>

      <h3>{t("views.create_system.battery")}</h3>
      <div className="defaultFlex">
        <div >
          <TextField className={"Input default-margin"} label={t("system_common.battery_voltage")} variant="outlined"
                     placeholder="12" type={"number"}  value={batteryVoltage?batteryVoltage:""} onChange={(event) => {
            setBatteryVoltage(parseFloatFromInput(event.target.value))
          }}/>
        </div>
      </div>
    </div>}


    <div>
      <h3>{t("views.create_system.more")}</h3>
      <div className="defaultFlex">
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }} divider={<Divider orientation="vertical" flexItem />}>
          <Typography>
            <Switch checked={hasTemperature} onChange={() => {
              setHasTemperature(!hasTemperature)
            }}/>
            {t("views.create_system.temperature")}
          </Typography>
        </Stack>
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }} divider={<Divider orientation="vertical" flexItem />}>
          <Typography>
            <Switch checked={calculateCombinedValuesAfterwards} onChange={() => {
              setCalculateCombinedValuesAfterwards(!calculateCombinedValuesAfterwards)
            }}/>
            {t("views.create_system.calculate_afterwards")}
          </Typography>
        </Stack>
        <div>
          <TextField className={"Input default-margin"} type="text"  label={t("views.create_system.shortner")} value={shortener || ""}
                     onChange={event => setShortener(event.target.value)}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} label={t("views.create_system.electricity_price")} variant="outlined"
                     type={"number"} value={electricityPrice ?? ""}
                     slotProps={{
                       input: {
                         startAdornment: <InputAdornment position="start">€</InputAdornment>
                       }
                     }}
                     error={incorrectPrice(electricityPrice)}
                     helperText={incorrectPrice(electricityPrice)?t("views.create_system.electricity_price_error"):undefined} onChange={(event) => {
            setElectricityPrice(parseFloatFromInput(event.target.value))
          }}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} label={t("views.create_system.electricity_price_feed_in")} variant="outlined"
                     type={"number"} value={electricityPriceFeedIn ?? ""}
                     slotProps={{
                       input: {
                         startAdornment: <InputAdornment position="start">€</InputAdornment>
                       }
                     }}
                     error={incorrectPrice(electricityPriceFeedIn)}
                     helperText={incorrectPrice(electricityPriceFeedIn)?t("views.create_system.electricity_price_error"):undefined} onChange={(event) => {
            setElectricityPriceFeedIn(parseFloatFromInput(event.target.value))
          }}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} type="text" label={t("views.create_system.deye_serials")} value={deyeSunSerialNumbers || ""}  sx={{width: '400px' }}
                     onChange={event => setDeyeSunSerialNumbers(event.target.value)}/>
        </div>
      </div>
    </div>

    {typeNeedsACVoltage(systemType) &&
      <div>
        <h3>{t("views.create_system.ac")}</h3>
          <div style={{display:"flex",flexWrap:"wrap", gap:"10px"}}>
          <TextField className={"Input default-margin"} label={systemType == "GRID" ? t("views.create_system.ac_voltage_grid"):t("views.create_system.ac_voltage_inverter")} variant="outlined"
                     placeholder="30" type={"number"} value={voltageAC?voltageAC:""} onChange={(event) => {
            setVoltageAC(parseFloatFromInput(event.target.value))
          }}/>
          <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setVoltageAC(230)}>230V</Button></div>
            <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setVoltageAC(110)}>110V</Button></div>
        </div>
      </div>
    }

    <div>
      <h3>{t("views.create_system.device")}</h3>
      <h4>{t("common.devices")}</h4>
      <NamingsManager setNamings={setNamingsDevices} namings={namingsDevices} doubleId={false}/>
      <h4>{t("common.inputs")+" "+ (systemType === SolarSystemType.SELFMADE ? t("common.dc"):"")}</h4>
      <NamingsManager setNamings={setNamingsInputsDC} namings={namingsInputsDC} doubleId={true}/>
      {systemType === SolarSystemType.SELFMADE &&
        <>
          <h4>{t("common.inputs") + " " + t("common.ac")}</h4>
          <NamingsManager setNamings={setNamingsInputsAC} namings={namingsInputsAC} doubleId={true}/>
        </>
      }
      {systemType === SolarSystemType.SELFMADE &&
        <>
          <h4>{t("common.output") + " " + t("common.dc")}</h4>
          <NamingsManager setNamings={setNamingsOutputsDC} namings={namingsOutputsDC} doubleId={true}/>
        </>
      }
      {systemType !== SolarSystemType.VERY_SIMPLE &&
        <>
          <h4>{t("common.outputs") + " " + (systemType === SolarSystemType.SELFMADE ? t("common.ac"):"")}</h4>
          <NamingsManager setNamings={setNamingsOutputsAC} namings={namingsOutputsAC} doubleId={true}/>
        </>
      }
      {isBatteryType(systemType) &&
        <>
          <h4>{t("common.batteries")}</h4>
          <NamingsManager setNamings={setNamingsBatteries} namings={namingsBatteries} doubleId={true}/>
        </>
      }
      {(systemType === SolarSystemType.GRID || systemType === SolarSystemType.GRID_BATTERY) &&
        <>
          <h4>{t("common.grids")}</h4>
          <NamingsManager setNamings={setNamingsGrids} namings={namingsGrids} doubleId={true}/>
        </>
      }
    </div>

    <Paper elevation={3} style={{padding: "20px", marginTop: "20px", backgroundColor: "#f9f9f9"}}>
      <h3>{t("views.create_system.total_filters")}</h3>
      <h4>{t("views.create_system.total_filters_existing")}</h4>
      <TotalFilterList filters={totalFilter} onDelete={deleteTotalFilter} loading={isLoading}/>
      <h4>{t("views.create_system.total_filters_add")}</h4>
      <div className="defaultFlex">
        <TextField
          className={"Input default-margin"}
          type="text"
          label={t("views.create_system.total_filters_filter_name")}
          value={newFilterName}
          onChange={event => setNewFilterName(event.target.value)}
        />
        <Button
          disabled={!newFilterName || newFilterName.length === 0 || isLoading}
          variant="contained"
          onClick={addTotalFilter}
        >
          {t("views.create_system.total_filters_add_button")}
        </Button>
      </div>
      <div style={{marginTop: "10px", fontSize: "0.9em", color: "gray"}}>
        <div style={{display: "flex", alignItems: "center", cursor: "pointer"}} onClick={() => setAvailableFiltersExpanded(!availableFiltersExpanded)}>
          <IconButton size="small" style={{transform: availableFiltersExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s'}}>
            <ExpandMoreIcon />
          </IconButton>
          <span>{t("views.create_system.total_filters_available")}</span>
        </div>
        <Collapse in={availableFiltersExpanded}>
          <div style={{marginLeft: "20px"}}>
            <p>{t("views.create_system.total_filters_help")}</p>
            <ul style={{marginTop: "5px"}}>
              <li>{t("views.create_system.total_filters_produced")}</li>
              <li>{t("views.create_system.total_filters_consumed")}</li>
              <li>{t("views.create_system.total_filters_battery")}</li>
              <li>{t("views.create_system.total_filters_grid_consumed")}</li>
              <li>{t("views.create_system.total_filters_grid_feedin")}</li>
              <li>{t("views.create_system.total_filters_price")}</li>
            </ul>
          </div>
        </Collapse>
      </div>
    </Paper>

    <Paper elevation={3} style={{padding: "20px", marginTop: "20px", marginBottom: "20px", backgroundColor: "#f9f9f9"}}>
      <h3>{t("views.create_system.graph_filters")}</h3>
      <GraphFilterList
        filters={graphFilter}
        onAdd={addGraphFilter}
        onDelete={deleteGraphFilter}
        loading={isLoading}
        availableFilters={AVAILABLE_GRAPH_FILTERS}
      />
    </Paper>

    <div style={{marginTop:"10px"}}>
      <div className="defaultFlex">
        {!data ? <Button variant="contained" onClick={() => {
            setIsLoading(true)
            createSystem({
              viewData:{defaultDelay,hideTotalConsumption,showGridInfo,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage,maxSolarVoltage,totalFilter,graphFilter},
              calculateCombinedValuesAfterwards,
              deyeSunSerialNumbers: nOF(deyeSunSerialNumbers),
              shortener: nOF(shortener),
              electricityPrice: nOF(electricityPrice),
              electricityPriceFeedIn: nOF(electricityPriceFeedIn),
              publicMode, timezone, name: systemName, type: systemType,buildingDate, namings:{
                devices: namingsDevices, inputsDC: namingsInputsDC,inputsAC: namingsInputsAC, outputsDC: namingsOutputsDC, outputsAC: namingsOutputsAC, batteries: namingsBatteries, grids: namingsGrids
              }
            }).then((response) => {
              toast.success(t("views.create_system.created_message")+response.token,{draggable: false,autoClose: false,closeOnClick: false})
              navigate('/dd/'+response.id)
            }).catch(error=>{
              setIsLoading(false)
            })}
          }>{t("views.create_system.create")}</Button>:

          <>
            <Button variant="contained" disabled={isLoading} onClick={() => {
              setIsLoading(true)
              patchSystem({
                viewData:{defaultDelay,hideTotalConsumption,showGridInfo,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage,maxSolarVoltage,totalFilter,graphFilter},
                calculateCombinedValuesAfterwards,
                deyeSunSerialNumbers: nOF(deyeSunSerialNumbers),
                shortener: nOF(shortener),
                electricityPrice: nOF(electricityPrice),
                electricityPriceFeedIn: nOF(electricityPriceFeedIn),
                publicMode, timezone, name: systemName, type: systemType, id: data.id, buildingDate, namings:{
                  devices: namingsDevices,  inputsDC: namingsInputsDC,inputsAC: namingsInputsAC, outputsDC: namingsOutputsDC, outputsAC: namingsOutputsAC, batteries: namingsBatteries, grids: namingsGrids
                }
              }).then((response) => {
                toast.success(t("common.saved_succesfull"))
                setIsLoading(false)
              }).catch(error=>{
                setIsLoading(false)
              })
            }
            }>{t("views.create_system.edit")}</Button>
            <Button color={"error"} variant="contained" disabled={isLoading} onClick={() => {
               setDeleteSystemModalOpen(true);
            }
            }>{t("views.create_system.delete")}</Button>
            </>
        }

        {data && <Button variant="contained" onClick={() => {
          navigate('/dd/'+data.id)
        }}>{t("views.create_system.to_dashboard")}</Button>}
        {data && <Button variant="contained" onClick={() => {
          updateStatistics(data.id).then(() => {
            toast.info(t("views.create_system.statistic_update_message"))
          })
        }}>{t("views.create_system.statistic_update")}</Button>}
      </div>
    </div>
    <DeleteSystemModal systemId={data?.id} open={deleteSystemModalOpen} onClose={() => setDeleteSystemModalOpen(false)}/>
  </div>
}

