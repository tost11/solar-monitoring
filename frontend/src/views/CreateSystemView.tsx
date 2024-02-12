import React, {useEffect, useState} from "react";
import {
  Box,
  Button,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material';
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

interface editSystemProps {
  data?: SolarSystemDTO
}

export default function CreateSystemView({data}: editSystemProps) {

  const [isLoading,setIsLoading] = useState(false)

  const [systemName, setSystemName] = useState(data?.name?data.name:"")
  const [shortener, setShortener] = useState(data?.shortener)
  const [systemType, setSystemType] = useState(data?.type?data.type:SolarSystemType.SELFMADE)
  const [buildingDate, setBuildingDate] = useState(data?.buildingDate)
  const [isBatteryPercentage, setIsBatteryPercentage] = useState(data?.viewData.isBatteryPercentage)
  const [showAmpere, setShowAmpere] = useState<boolean>(data?.viewData.showAmpere !== undefined?data.viewData.showAmpere:true)
  const [hasACInput, setHasACInput] = useState(data?.viewData.hasACInput)
  const [hasACOutput, setHasACOutput] = useState(data?.viewData.hasACOutput)
  const [hasDCOutput, setHasDCOutput] = useState(data?.viewData.hasDCOutput)
  const [productionForTotalPricing, setProductionForTotalPricing] = useState(data?.viewData.productionForTotalPricing)
  const [totalPricingPublicOverride, setTotalPricingPublicOverride] = useState(data?.viewData.totalPricingPublicOverride)
  const [hideTotalConsumption, setHideTotalConsumption] = useState(data?.viewData.hideTotalConsumption)
  const [hasTemperature, setHasTemperature] = useState(data?.viewData.hasTemperature !== undefined?data?.viewData.hasTemperature:false)
  const [voltageAC, setVoltageAC] = useState(data?.viewData.voltageAC)
  const [batteryVoltage, setBatteryVoltage] = useState(data?.viewData.batteryVoltage)
  const [maxSolarVoltage, setMaxSolarVoltage] = useState(data?.viewData.maxSolarVoltage)
  const [timezone,setTimezone] = useState(data?.timezone ? data.timezone : moment.tz.guess())
  const [publicMode,setPublicMode] = useState(data?.publicMode?data.publicMode:SolarSystemPublicMode.NONE)
  const [latitude, setLatitude] = useState(data?.latitude)
  const [longitude, setLongitude] = useState(data?.longitude)
  const [electricityPrice, setElectricityPrice] = useState(data?.electricityPrice)
  const [deyeSunSerialNumbers, setDeyeSunSerialNumbers] = useState(data?.deyeSunSerialNumbers)
  //<{[key: number]: string}>
  const [namingsDevices, setNamingsDevices] = useState(data ?data.namings.devices : {})
  const [namingsInputsDC, setNamingsInputsDC] = useState(data ?data.namings.inputsDC : {})
  const [namingsInputsAC, setNamingsInputsAC] = useState(data ?data.namings.inputsAC : {})
  const [namingsOutputsDC, setNamingsOutputsDC] = useState(data ?data.namings.outputsDC : {})
  const [namingsOutputsAC, setNamingsOutputsAC] = useState(data ?data.namings.outputsAC : {})
  const [namingsBatteries, setNamingsBatteries] = useState(data ?data.namings.batteries : {})

  const navigate = useNavigate();

  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as SolarSystemType);
  };

  const geolocation = () => {
    let altitude;
    let geoinfo;

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        function (position) {
          setLatitude(position.coords.latitude);
          setLongitude(position.coords.longitude);
          if (position.coords.altitude) {
            altitude = position.coords.altitude;
          } else {
            altitude = ' Keine Höhenangaben vorhanden ';
          }
          geoinfo = 'Latitude ' + latitude + 'Longitude' + longitude;
        });
    } else {
      geoinfo = 'Dieser Browser unterstützt die Abfrage der Geolocation nicht.';
    }
  }

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

  const incorrectPrice = (price) => {
    return price != null && Number(price) <= 0;
  }

  //TODO split this in some components it is to large
  return <div className={"default-margin"}>
    <h3>General Settings</h3>
    <div className="defaultFlex">
      <Box className="SolarTypeMenuBox">
        <FormControl fullWidth className="Input">
          <InputLabel className="Input">SolarSystemType</InputLabel>

          <Select
            labelId="demo-simple-select-label"
            id="demo-simple-select"
            value={systemType}
            label="SolarSystem"
            onChange={handleChange}
          >

            <MenuItem value={"SELFMADE"}>
              <div className="menuItem">Selfmade</div>
            </MenuItem>
            <MenuItem value={"SIMPLE"}>
              <div className="menuItem">Simple Solar System</div>
            </MenuItem>
            <MenuItem value={"VERY_SIMPLE"}>
              <div className="menuItem">Very Simple only Watt</div>
            </MenuItem>
            <MenuItem value={"GRID"}>
              <div className="menuItem">Grid Solar System</div>
            </MenuItem>
            <MenuItem value={"GRID_BATTERY"}>
              <div className="menuItem">Grid Solar System with Battery</div>
            </MenuItem>

          </Select>
        </FormControl>
      </Box>
      <div>
        <TextField className={"Input default-margin"} type="text" name="systemName" placeholder="SystemName" label="SystemName" value={systemName}
                   onChange={event => setSystemName(event.target.value)}/>
      </div>
      <TextField label="Building Date" className={"Input default-margin"} type="date" name="buildingDate" value={buildingDate ? moment(buildingDate).format("yyyy-MM-DD"):moment(undefined)} onChange={event =>
          setBuildingDate(moment(event.target.value))
      }/>
      <MyTimezonePicker
          value={timezone}
          onChange={setTimezone}
      />
      <Box className="SolarTypeMenuBox">
        <FormControl fullWidth className="Input">
          <InputLabel className="Input">PublicMode</InputLabel>
          <Select
            labelId="demo-simple-select-label"
            value={publicMode}
            label="Public Mode"
            onChange={(event)=>{
              // @ts-ignore
              setPublicMode(event.target.value)
            }}
          >

            <MenuItem value={"NONE"}>
              <div className="menuItem">None</div>
            </MenuItem>
            <MenuItem value={"PRODUCTION"}>
              <div className="menuItem">Production</div>
            </MenuItem>
            <MenuItem value={"ALL"}>
              <div className="menuItem">All</div>
            </MenuItem>
          </Select>
        </FormControl>
      </Box>
    </div>

    <h3>Visualisation</h3>
    <div className="defaultFlex">
      <Typography>
        <Switch checked={showAmpere} onChange={() => {
          setShowAmpere(!showAmpere)
        }}/>
        Show Ampere Graphs
      </Typography>
      {systemType != SolarSystemType.VERY_SIMPLE && systemType != SolarSystemType.SIMPLE && <>
        <Typography>
          <Switch checked={productionForTotalPricing} onChange={() => {
            setProductionForTotalPricing(!productionForTotalPricing)
          }}/>
          Total Values use Production for Pricing
        </Typography>
        <Typography>
          <Switch checked={hideTotalConsumption} onChange={() => {
            setHideTotalConsumption(!hideTotalConsumption)
          }}/>
          Hide Total Consumption
        </Typography>
        <Typography>
          <Switch checked={totalPricingPublicOverride} onChange={() => {
            setTotalPricingPublicOverride(!totalPricingPublicOverride)
          }}/>
          Total Pricing Public Override
          </Typography>
        </>
      }
    </div>


    <h3>Position</h3>
    <div className="defaultFlex">
      <TextField className={"Input"} label="Longitude"
                 variant="outlined" value={longitude?longitude:""} onChange={(event) => {
        setLongitude(parseFloatFromInput(event.target.value))
      }}/>
      <TextField className={"Input"} label="Latitude"
                 variant="outlined" value={latitude?latitude:""}  onChange={(event) => {
        setLatitude(parseFloatFromInput(event.target.value))
      }}/>
      <Button variant="outlined" onClick={() => {
        geolocation()
      }}>get Position</Button>

    </div>

    {systemType != SolarSystemType.VERY_SIMPLE && <div>
      <h3>Panel Infos</h3>
      <div >
        <TextField className={"Input"} id="MaxSolarVoltage" type={"number"} label="Max Solar Panel Voltage"
                   variant="outlined" placeholder="45" value={maxSolarVoltage?maxSolarVoltage:""}  onChange={(event) => {
          setMaxSolarVoltage(parseFloatFromInput(event.target.value))
        }}/>
      </div>
    </div>}

    {isBatteryType(systemType) && <div>

      <h3>Battery Settings</h3>
      <div className="defaultFlex">
        <Stack direction="row" spacing={1} alignItems="center">
          <Switch checked={isBatteryPercentage} onChange={() => {
            setIsBatteryPercentage(!isBatteryPercentage)
          }}/>
          <Typography>Battery Percentage</Typography>
        </Stack>
        <div >
          <TextField className={"Input default-margin"} id="BatteryVoltage" label="Battery Voltage" variant="outlined"
                     placeholder="12" type={"number"}  value={batteryVoltage?batteryVoltage:""} onChange={(event) => {
            setBatteryVoltage(parseFloatFromInput(event.target.value))
          }}/>
        </div>
      </div>
    </div>}

    {systemType == SolarSystemType.SELFMADE &&
      <div>
        <h3>In-Output Options</h3>
        <div className="defaultFlex">
          <Stack direction="row" spacing={1} alignItems="center" divider={<Divider orientation="vertical" flexItem />}>
            <Typography>
              <Switch checked={hasACInput} onChange={() => {
                setHasACInput(!hasACInput)
              }}/>
              AC Input
            </Typography>
            <Typography>
              <Switch checked={hasACOutput} onChange={() => {
                setHasACOutput(!hasACOutput)
              }}/>
              AC Output
            </Typography>
            <Typography>
              <Switch checked={hasDCOutput} onChange={() => {
                setHasDCOutput(!hasDCOutput)
              }}/>
              DC Output
            </Typography>
          </Stack>
        </div>
      </div>
    }

    <div>
      <h3>More Settings</h3>
      <div className="defaultFlex">
        <Stack direction="row" spacing={1} alignItems="center" divider={<Divider orientation="vertical" flexItem />}>
          <Typography>
            <Switch checked={hasTemperature} onChange={() => {
              setHasTemperature(!hasTemperature)
            }}/>
            Temperature
          </Typography>
        </Stack>
        <div>
          <TextField className={"Input default-margin"} type="text" name="shortener" label="Shortener" value={shortener}
                     onChange={event => setShortener(event.target.value)}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} label={"Electricity Price"} variant="outlined"
                     type={"number"} value={electricityPrice} InputAdornment={"€"} error={incorrectPrice(electricityPrice)}
                     helperText={incorrectPrice(electricityPrice)?"Pirce cann not be negative":undefined} onChange={(event) => {
            setElectricityPrice(parseFloatFromInput(event.target.value))
          }}/>
        </div>
        <div>
          <TextField className={"Input default-margin"} type="text" name="deye" label="Deye Sun Serials (Seperated by comma)" value={deyeSunSerialNumbers}  sx={{width: '400px' }}
                     onChange={event => setDeyeSunSerialNumbers(event.target.value)}/>
        </div>
      </div>
    </div>

    {typeNeedsACVoltage(systemType,hasACInput,hasACOutput) &&
      <div>
        <h3>AC Information's</h3>
          <div style={{display:"flex",flexWrap:"wrap", gap:"10px"}}>
          <TextField className={"Input default-margin"} label={systemType == "GRID" ? "Grid Voltage":"Inverter Voltage"} variant="outlined"
                     placeholder="30" type={"number"} value={voltageAC?voltageAC:""} onChange={(event) => {
            setVoltageAC(parseFloatFromInput(event.target.value))
          }}/>
          <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setVoltageAC(230)}>230V</Button></div>
            <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setVoltageAC(110)}>110V</Button></div>
        </div>
      </div>
    }

    <div>
      <h3>DeviceNamings</h3>
      <h4>Devices</h4>
      <NamingsManager setNamings={setNamingsDevices} namings={namingsDevices} doubleId={false}/>
      <h4>{"Inputs "+ (systemType === SolarSystemType.SELFMADE && hasACInput ? "DC":"")}</h4>
      <NamingsManager setNamings={setNamingsInputsDC} namings={namingsInputsDC} doubleId={true}/>
      {systemType === SolarSystemType.SELFMADE && hasACInput &&
        <>
          <h4>Input AC</h4>
          <NamingsManager setNamings={setNamingsInputsAC} namings={namingsInputsAC} doubleId={true}/>
        </>
      }
      {systemType === SolarSystemType.SELFMADE && hasDCOutput &&
        <>
          <h4>Outputs DC</h4>
          <NamingsManager setNamings={setNamingsOutputsDC} namings={namingsOutputsDC} doubleId={true}/>
        </>
      }
      {systemType !== SolarSystemType.VERY_SIMPLE && !(systemType === SolarSystemType.SELFMADE && !hasACOutput) &&
        <>
          <h4>{"Outputs "+ (systemType === SolarSystemType.SELFMADE ? "AC":"")}</h4>
          <NamingsManager setNamings={setNamingsOutputsAC} namings={namingsOutputsAC} doubleId={true}/>
        </>
      }
      {isBatteryType(systemType) &&
        <>
          <h4>Batteries</h4>
          <NamingsManager setNamings={setNamingsBatteries} namings={namingsBatteries} doubleId={true}/>
        </>
      }
    </div>

    <div style={{marginTop:"10px"}}>
      <div className="defaultFlex">
        {!data ? <Button variant="contained" onClick={() => {
            setIsLoading(true)
            createSystem({
              viewData:{hideTotalConsumption,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage, hasACInput, hasACOutput, hasDCOutput, isBatteryPercentage,showAmpere,maxSolarVoltage},
              deyeSunSerialNumbers,shortener ,latitude, longitude,electricityPrice, publicMode, timezone, name: systemName, type: systemType,buildingDate, namings:{
                devices: namingsDevices, inputsDC: namingsInputsDC,inputsAC: namingsInputsAC, outputsDC: namingsOutputsDC, outputsAC: namingsOutputsAC, batteries: namingsBatteries
              }
            }).then((response) => {
              toast.success('Creat new System with Token: '+response.token,{draggable: false,autoClose: false,closeOnClick: false})
              navigate('/detailDashboard/'+response.id)
            }).catch(error=>{
              setIsLoading(false)
            })}
          }>Create a new SolarSystem</Button>:

          <Button variant="contained" disabled={isLoading} onClick={() => {
            setIsLoading(true)
            patchSystem({
              viewData:{hideTotalConsumption,totalPricingPublicOverride,productionForTotalPricing,hasTemperature,voltageAC, batteryVoltage, hasACInput, hasACOutput, hasDCOutput, isBatteryPercentage,showAmpere,maxSolarVoltage},
              deyeSunSerialNumbers,shortener ,latitude, longitude, electricityPrice, publicMode, timezone, name: systemName, type: systemType, id: data.id, buildingDate, namings:{
                devices: namingsDevices,  inputsDC: namingsInputsDC,inputsAC: namingsInputsAC, outputsDC: namingsOutputsDC, outputsAC: namingsOutputsAC, batteries: namingsBatteries
              }
            }).then((response) => {
              toast.success('Save successfully')
              setIsLoading(false)
            }).catch(error=>{
              setIsLoading(false)
            })
          }
          }>Edit System</Button>
        }

        {data && <Button variant="contained" onClick={() => {
          navigate('/detailDashboard/'+data.id)
        }}>To Dashboard</Button>}
        {data && <Button variant="contained" onClick={() => {
          updateStatistics(data.id).then(() => {
            toast.info('Statistic Update started. This may take some time!')
          })
        }}>Update Statistics</Button>}
      </div>
    </div>
  </div>
}

