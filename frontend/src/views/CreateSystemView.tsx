import React, {useEffect, useState} from "react";
import {
  Box,
  Button,
  Collapse,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material';
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import Select, {SelectChangeEvent} from '@mui/material/Select';
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
  const [isBatteryPercentage, setIsBatteryPercentage] = useState(data?.viewData.isBatteryPercentage)
  const [showAmpere, setShowAmpere] = useState<boolean>(data?.viewData.showAmpere !== undefined?data.viewData.showAmpere:true)
  const [hasACInput, setHasACInput] = useState(data?.viewData.hasACInput)
  const [hasACOutput, setHasACOutput] = useState(data?.viewData.hasACOutput)
  const [hasDCOutput, setHasDCOutput] = useState(data?.viewData.hasDCOutput)
  const [calculateCombinedValuesAfterwards, setCalculateCombinedValuesAfterwards] = useState(data?.calculateCombinedValuesAfterwards)
  const [defaultDelay, setDefaultDelay] = useState(data?.viewData.defaultDelay)
  const [productionForTotalPricing, setProductionForTotalPricing] = useState(data?.viewData.productionForTotalPricing)
  const [totalPricingPublicOverride, setTotalPricingPublicOverride] = useState(data?.viewData.totalPricingPublicOverride)
  const [hideTotalConsumption, setHideTotalConsumption] = useState(data?.viewData.hideTotalConsumption)
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

  const [deleteSystemModalOpen, setDeleteSystemModalOpen] = useState(false)

  const navigate = useNavigate();

  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as SolarSystemType);
  };

  const typeNeedsACVoltage = (type:string,acInputSelection?:boolean,acOutputSelection?:boolean) => {
    return type == SolarSystemType.GRID || type == SolarSystemType.GRID_BATTERY ||
      (type == SolarSystemType.SELFMADE && (acInputSelection || acOutputSelection))
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
    return price != null && Number(price) <= 0;
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

            <MenuItem value={"NONE"}>
              <div className="menuItem">{t("system_common.public_mode_none")}</div>
            </MenuItem>
            <MenuItem value={"PRODUCTION"}>
              <div className="menuItem">{t("system_common.public_mode_producion")}</div>
            </MenuItem>
            <MenuItem value={"ALL"}>
              <div className="menuItem">{t("system_common.public_mode_all")}</div>
            </MenuItem>
          </Select>
        </FormControl>
      </Box>
    </div>

    <h3>{t("views.create_system.view_settings")}</h3>
    <div className="defaultFlex">
      <Typography>
        <Switch checked={showAmpere} onChange={() => {
          setShowAmpere(!showAmpere)
        }}/>
        {t("views.create_system.show_ampere")}
      </Typography>
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
        <Typography>
          <TextField className={"Input"} type={"number"} label={t("views.create_system.delay")} min={1}
                     variant="outlined" placeholder="30" value={defaultDelay?defaultDelay:""}  onChange={(event) => {
            setDefaultDelay(parseIntFromInput(event.target.value))
          }}/>
        </Typography>
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
        <Stack direction="row" spacing={1} alignItems="center">
          <Switch checked={isBatteryPercentage} onChange={() => {
            setIsBatteryPercentage(!isBatteryPercentage)
          }}/>
          <Typography>{t("views.create_system.battery_percentage")}</Typography>
        </Stack>
        <div >
          <TextField className={"Input default-margin"} {t("system_common.battery_voltage")} variant="outlined"
                     placeholder="12" type={"number"}  value={batteryVoltage?batteryVoltage:""} onChange={(event) => {
            setBatteryVoltage(parseFloatFromInput(event.target.value))
          }}/>
        </div>
      </div>
    </div>}

    {systemType == SolarSystemType.SELFMADE &&
      <div>
        <h3>{t("views.create_system.in_outputs")}</h3>
        <div className="defaultFlex">
          <Stack direction="row" spacing={1} alignItems="center" divider={<Divider orientation="vertical" flexItem />}>
            <Typography>
              <Switch checked={hasACInput} onChange={() => {
                setHasACInput(!hasACInput)
              }}/>
              {t("views.create_system.ac_input_show")}
            </Typography>
            <Typography>
              <Switch checked={hasACOutput} onChange={() => {
                setHasACOutput(!hasACOutput)
              }}/>
              {t("views.create_system.ac_output_show")}
            </Typography>
            <Typography>
              <Switch checked={hasDCOutput} onChange={() => {
                setHasDCOutput(!hasDCOutput)
              }}/>
              {t("views.create_system.dc_output_show")}
            </Typography>
          </Stack>
        </div>
      </div>
    }

    <div>
      <h3>{t("views.create_system.more")}</h3>
      <div className="defaultFlex">
        <Stack direction="row" spacing={1} alignItems="center" divider={<Divider orientation="vertical" flexItem />}>
          <Typography>
            <Switch checked={hasTemperature} onChange={() => {
              setHasTemperature(!hasTemperature)
            }}/>
            {t("views.create_system.temperature")}
          </Typography>
        </Stack>
        <Stack direction="row" spacing={1} alignItems="center" divider={<Divider orientation="vertical" flexItem />}>
          <Typography>
            <Switch checked={calculateCombinedValuesAfterwards} onChange={() => {
              setCalculateCombinedValuesAfterwards(!calculateCombinedValuesAfterwards)
            }}/>
            {t("views.create_system.calculate_afterwards")}
          </Typography>
        </Stack>
        <div>
          <TextField className={"Input default-margin"} type="text"  label={t("views.create_system.shortner")} value={shortener}
                     onChange={event => setShortener(event.target.value)}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} label={t("views.create_system.electricity_price")} variant="outlined"
                     type={"number"} value={electricityPrice} InputAdornment={"€"} error={incorrectPrice(electricityPrice)}
                     helperText={incorrectPrice(electricityPrice)?t("views.create_system.electricity_price_error"):undefined} onChange={(event) => {
            setElectricityPrice(parseFloatFromInput(event.target.value))
          }}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} label={t("views.create_system.electricity_price_feed_in")} variant="outlined"
                     type={"number"} value={electricityPriceFeedIn} InputAdornment={"€"} error={incorrectPrice(electricityPriceFeedIn)}
                     helperText={incorrectPrice(electricityPriceFeedIn)?t("views.create_system.electricity_price_error"):undefined} onChange={(event) => {
            setElectricityPriceFeedIn(parseFloatFromInput(event.target.value))
          }}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} type="text" label={t("views.create_system.deye_serials")} value={deyeSunSerialNumbers}  sx={{width: '400px' }}
                     onChange={event => setDeyeSunSerialNumbers(event.target.value)}/>
        </div>
      </div>
    </div>

    {typeNeedsACVoltage(systemType,hasACInput,hasACOutput) &&
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
      <h4>{t("common.inputs")+" "+ (systemType === SolarSystemType.SELFMADE && hasACInput ? t("common.dc"):"")}</h4>
      <NamingsManager setNamings={setNamingsInputsDC} namings={namingsInputsDC} doubleId={true}/>
      {systemType === SolarSystemType.SELFMADE && hasACInput &&
        <>
          <h4>{t("common.inputs") + " " + t("common.ac")}</h4>
          <NamingsManager setNamings={setNamingsInputsAC} namings={namingsInputsAC} doubleId={true}/>
        </>
      }
      {systemType === SolarSystemType.SELFMADE && hasDCOutput &&
        <>
          <h4>{t("common.output") + " " + t("common.dc")}</h4>
          <NamingsManager setNamings={setNamingsOutputsDC} namings={namingsOutputsDC} doubleId={true}/>
        </>
      }
      {systemType !== SolarSystemType.VERY_SIMPLE && !(systemType === SolarSystemType.SELFMADE && !hasACOutput) &&
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

    <div>
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
    </div>

    <div style={{marginTop:"10px"}}>
      <div className="defaultFlex">
        {!data ? <Button variant="contained" onClick={() => {
            setIsLoading(true)
            createSystem({
              viewData:{defaultDelay,hideTotalConsumption,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage, hasACInput, hasACOutput, hasDCOutput, isBatteryPercentage,showAmpere,maxSolarVoltage,totalFilter},
              calculateCombinedValuesAfterwards,deyeSunSerialNumbers,shortener ,electricityPrice,electricityPriceFeedIn, publicMode, timezone, name: systemName, type: systemType,buildingDate, namings:{
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
                viewData:{defaultDelay,hideTotalConsumption,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage, hasACInput, hasACOutput, hasDCOutput, isBatteryPercentage,showAmpere,maxSolarVoltage,totalFilter},
                calculateCombinedValuesAfterwards,deyeSunSerialNumbers,shortener, electricityPrice,electricityPriceFeedIn, publicMode, timezone, name: systemName, type: systemType, id: data.id, buildingDate, namings:{
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

