import React, {useEffect, useState} from "react";
import {
  Box,
  Button,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Popover,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {createSystem, patchSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import ManagersOfTheSystem from "./ManagersOfTheSystem";
import moment from "moment";
import {toast} from "react-toastify";
import MyTimezonePicker from "./MyTimezonePicker";

interface editSystemProps {
  data?: SolarSystemDTO
}

export default function CreateNewSystemComponent({data}: editSystemProps) {
  const [systemName, setSystemName] = useState("");
  const [systemType, setSystemType] = useState("");
  const [buildingDate, setBuildingDate] = useState<Date|string>("");
  const [isBatteryPercentage, setIsBatteryPercentage] = useState(true)
  const [inverterVoltage, setInverterVoltage] = useState(0)
  const [batteryVoltage, setBatteryVoltage] = useState(0)
  const [maxSolarVoltage, setMaxSolarVoltage] = useState(0)
  const [isLoading,setIsLoading]=useState(false)
  const [timeZone,setTimeZone]=useState<string|null>(moment.tz.guess())

  let date:number
  useEffect(()=>{
    date = new Date(buildingDate?buildingDate:"").getTime();
  },[buildingDate])


  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as string);
  };

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [text, setText] = useState("");

  const handleClick = (event: React.MouseEvent<HTMLElement>, text: string) => {
    setAnchorEl(anchorEl ? null : event.currentTarget);
    setText(text)
    event.stopPropagation()
  };
  const handleClose = () => {
    setAnchorEl(null);
    setText("")
  };

  const open = Boolean(anchorEl);
  const id = open ? 'simple-popper' : undefined;

  const [latitude, setLatitude] = useState(0)
  const [longitude, setLongitude] = useState(0)
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

  const isSelfmadeType = (type:string) => {
    return type == "SELFMADE_CONSUMPTION" || type == "SELFMADE" || type == "SELFMADE_DEVICE" || type == "SELFMADE_INVERTER"
  }

  const typeNeedsACVoltage = (type:string) => {
    return type == "SELFMADE_CONSUMPTION" || type == "GRID" || type == "SELFMADE_INVERTER"
  }

  useEffect(() => {
    if (data != null) {
      setSystemName(data.name);
      setSystemType(data.type)
      setBuildingDate(data.buildingDate as Date)
      setInverterVoltage(data.inverterVoltage)
      setBatteryVoltage(data.batteryVoltage)
      setIsBatteryPercentage(data.isBatteryPercentage)
      setMaxSolarVoltage(data.maxSolarVoltage)
      setTimeZone(data.timezone)
    }
    setIsLoading(true)
  }, [])

  //TODO split this in some components it is to large
  return <div className={"default-margin"}>
    {isLoading&&<div>
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
                <div className="menuItem"> Selfmade SolarSystem</div>
              </MenuItem>
              <MenuItem value={"SELFMADE_CONSUMPTION"}>
                <div className="menuItem">Selfmade with Consumption</div>
              </MenuItem>
              <MenuItem value={"SELFMADE_INVERTER"}>
                <div className="menuItem">Selfmade with inverter</div>
              </MenuItem>
              <MenuItem value={"SELFMADE_DEVICE"}>
                <div className="menuItem">Selfmade without converter</div>
              </MenuItem>
              <MenuItem value={"SIMPLE"}>
                <div className="menuItem">Simple Solar System</div>
              </MenuItem>
              <MenuItem value={"VERY_SIMPLE"}>
                <div className="menuItem">Very Simple Solar System</div>
              </MenuItem>
              <MenuItem value={"GRID"}>
                <div className="menuItem">Grid Solar System</div>
              </MenuItem>

              <Popover
                id={id}
                open={open}
                anchorEl={anchorEl}
                onClose={handleClose}
                anchorOrigin={{
                  vertical: 'bottom',
                  horizontal: 'left',
                }}
              >
                <Typography sx={{p: 2}}>{text}</Typography>
              </Popover>
            </Select>
          </FormControl>
        </Box>
        {/*TODO refactor to date picker*/}
        <div>
          <TextField className={"Input default-margin"} type="text" name="systemName" placeholder="SystemName" label="SystemName" value={systemName}
                     onChange={event => setSystemName(event.target.value)}/>
        </div>
        <TextField label="Building Date" className={"Input default-margin"} type="date" name="buildingDate" value={moment(buildingDate).format("yyyy-MM-DD")} onChange={event =>
            setBuildingDate(event.target.value)}/>
        <MyTimezonePicker
            value={timeZone}
            onChange={setTimeZone}
        />
      </div>

      <h3> Postion </h3>
      <div className="defaultFlex">
        <TextField className={"Input"} type={"number"} label="Longitude"
                   variant="outlined" value={longitude}  onChange={(event) => {
          if (!isNaN(parseFloat(event.target.value))) {
            setLongitude(Number(event.target.value))
          }
        }}/>
        <TextField className={"Input"} type={"number"} label="Latitude"
                   variant="outlined" value={latitude}  onChange={(event) => {
          if (!isNaN(parseFloat(event.target.value))) {
            setLatitude(Number(event.target.value))
          }
        }}/>
        <Button variant="outlined" onClick={() => {
          geolocation()
        }}>get Position</Button>

      </div>

      <h3>Panel Infos</h3>

      <div >
        <TextField className={"Input"} id="MaxSolarVoltage" type={"number"} label="Max Solar Panel Voltage"
                   variant="outlined" placeholder="45" value={maxSolarVoltage}  onChange={(event) => {
          if (!isNaN(parseFloat(event.target.value))) {
            setMaxSolarVoltage(Number(event.target.value))
          }
        }}/>
      </div>

      {isSelfmadeType(systemType) && <div>

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
                       placeholder="12" type={"number"}  value={batteryVoltage} onChange={(event) => {
              if (!isNaN(parseFloat(event.target.value))) {
                setBatteryVoltage(Number(event.target.value))
              }
            }}/>
          </div>
        </div>
      </div>}

      {typeNeedsACVoltage(systemType) &&
        <div>
          <h3>{systemType == "GRID" ? "Grid Informations":"Inverter Informations"}</h3>
            <div style={{display:"flex",flexWrap:"wrap", gap:"10px"}}>
            <TextField className={"Input default-margin"} id="InverterVoltage" label={systemType == "GRID" ? "Grid Voltage":"Inverter Voltage"} variant="outlined"
                       placeholder="30" type={"number"}  value={inverterVoltage} onChange={(event) => {
              if (!isNaN(parseFloat(event.target.value))) {
                setInverterVoltage(Number(event.target.value))
              }
            }}/>
            <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setInverterVoltage(230)}>230V</Button></div>
              <div style={{marginTop: "auto",marginBottom: "auto"}}><Button variant="outlined" onClick={() => setInverterVoltage(110)}>110V</Button></div>
          </div>
        </div>
      }

      <div style={{marginTop:"10px"}}>
        {!data ? <Button variant="contained" onClick={() => {
            createSystem(systemName, date, systemType, isBatteryPercentage, inverterVoltage, batteryVoltage, maxSolarVoltage).then((response) => {
              toast.success('Creat new System with Token: '+response.token,{draggable: false,autoClose: false,closeOnClick: false})
            })}
          }>Create a new SolarSystem</Button>:

          <Button variant="contained" onClick={() => {
            console.log(data)
            patchSystem(systemName, date, systemType, isBatteryPercentage, inverterVoltage, batteryVoltage, maxSolarVoltage,timeZone,data?.id).then((response) => {
              toast.success('Save successfully')
            })
          }
          }>Edit System</Button>
        }
      </div>

      {//TODO move this to child component in this component
      data?.managers&&<div style={{marginTop:"10px"}}>
          <Divider />
          <h3>Permission Management</h3>
          <div style={{backgroundColor: "whitesmoke", overflow: "scroll", maxHeight: "400px", width: "40%",justifyContent:"center"}}>
            <ManagersOfTheSystem initManagers={data.managers} systemId={data?.id}/>
          </div>

      </div>
      }
    </div>}
  </div>
}

