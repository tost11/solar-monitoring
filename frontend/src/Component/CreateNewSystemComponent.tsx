import React, {useEffect, useState} from "react";
import moment from "moment";
import {
  Alert,
  Box,
  Button,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Popover,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {createSystem, patchSystem, RegisterSolarSystemDTO, SolarSystemDTO} from "../api/SolarSystemAPI";
import InfoIcon from '@mui/icons-material/Info';
import ManagersOfTheSystem from "./ManagersOfTheSystem";

interface editSystemProps {
  data?: SolarSystemDTO
}

export default function CreateNewSystemComponent({data}: editSystemProps) {
  const [systemName, setSystemName] = useState("");
  const [systemType, setSystemType] = useState("");
  const [buildingDate, setBuildingDate] = useState<Date|string>("");
  const [alertOpen, setAlertOpen] = useState(false);
  const [isBatteryPercentage, setIsBatteryPercentage] = useState(true)
  const [inverterVoltage, setInverterVoltage] = useState(0)
  const [batteryVoltage, setBatteryVoltage] = useState(0)
  const [maxSolarVoltage, setMaxSolarVoltage] = useState(0)
  const [isLoading,setIsLoading]=useState(false)


  let date:number
  useEffect(()=>{
     date = new Date(buildingDate).getTime();
  },[buildingDate])


  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as string);
  };

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [text, setText] = useState("");
  const [response, setResponse] = useState<string>("");

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

  useEffect(() => {
    if (data != null) {
      setSystemName(data.name);
      setSystemType(data.type)
      setBuildingDate(data.buildingDate as Date)
      setInverterVoltage(data.inverterVoltage)
      setBatteryVoltage(data.batteryVoltage)
      setIsBatteryPercentage(data.isBatteryPercentage)
      setMaxSolarVoltage(data.maxSolarVoltage)
    }
    setIsLoading(true)
  }, [])

  //TODO split this in some components it is to large
  return <div className={"default-margin"}>
    {alertOpen && <Alert severity={"success"}>Creat new System{"\n token: " + response}</Alert>}
    {isLoading&&<div>
    <Box className="SolarTypeMenuBox ">
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
            <IconButton color="primary" onClick={event => handleClick(event,
              "Salfmade Solar system is a System with Solar")}><InfoIcon color="primary"/></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_CONSUMPTION"}>
            <div className="menuItem">Selfmade with Consumption</div>
            <IconButton color="primary" onClick={event => handleClick(event,
              "This Solar System Produce Energy, when you not use your energy")}><InfoIcon
              color="primary"/></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_INVERTER"}>
            <div className="menuItem">Selfmade with inverter</div>
            <IconButton color="primary" onClick={event => handleClick(event,
              "text")}><InfoIcon color="primary"/></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_DEVICE"}>
            <div className="menuItem">Selfmade without converter</div>
            <IconButton color="primary" onClick={event => handleClick(event,
              "text")}><InfoIcon color="primary"/></IconButton>
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
    <br/>
    {systemType === "SELFMADE" && <div>
      <TextField className={"Input default-margin"} id="MaxSolarVoltage" type={"number"} label="Max Solar Panel Voltage"
                 variant="outlined" placeholder="45" value={maxSolarVoltage}  onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setMaxSolarVoltage(Number(event.target.value))
        }


      }}/>
    </div>}

    {systemType === "SELFMADE_CONSUMPTION" && <div>
      <TextField className={"Input default-margin"} id="MaxSolarVoltage" label="Max Solar Panel Voltage"
                 variant="outlined" placeholder="45" type={"number"} value={maxSolarVoltage} onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setMaxSolarVoltage(Number(event.target.value))
        }

      }}
      />

      <h3>Is a Battery percentage Present?</h3>
      <Stack direction="row" spacing={1} alignItems="center">
        <Typography>no</Typography>
        <Switch checked={isBatteryPercentage} onChange={() => {
          setIsBatteryPercentage(!isBatteryPercentage)
        }}/>
        <Typography>yes</Typography>
      </Stack>



      <Button variant="outlined" onClick={() => setInverterVoltage(230)}>230V</Button>
      <Button variant="outlined" onClick={() => setInverterVoltage(110)}>110V</Button>
      <TextField className={"Input default-margin"} id="InverterVoltage" label="Inverter Voltage" variant="outlined"
                 placeholder="30" type={"number"}  value={inverterVoltage} onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setInverterVoltage(Number(event.target.value))
        }
      }}/>
      <div >
        <TextField className={"Input default-margin"} id="BatteryVoltage" label="Battery Voltage" variant="outlined"
                   placeholder="12" type={"number"}  value={batteryVoltage} onChange={(event) => {
          if (!isNaN(parseFloat(event.target.value))) {
            setBatteryVoltage(Number(event.target.value))
          }
        }}/>
      </div>
    </div>}

    {systemType === "SELFMADE_INVERTER" && <div>
      <TextField className={"Input default-margin"} id="MaxSolarVoltage" label="Max Solar Panel Voltage"
                 variant="outlined" placeholder="45" value={maxSolarVoltage} onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setMaxSolarVoltage(Number(event.target.value))
        }
      }}
      />

      <h3>Is a Battery percentage Present?</h3>
      <Stack direction="row" spacing={1} alignItems="center">
        <Typography>no</Typography>
        <Switch checked={isBatteryPercentage} onChange={() => {
          setIsBatteryPercentage(!isBatteryPercentage)
        }}/>
        <Typography>yes</Typography>
      </Stack>


      <Button variant="outlined" onClick={() => setInverterVoltage(230)}>230V</Button>
      <Button variant="outlined" onClick={() => setInverterVoltage(110)}>110V</Button>
      <TextField className={"Input default-margin"} id="InverterVoltage" label="Inverter Voltage" variant="outlined"
                 placeholder="30" type={"number"}  value={inverterVoltage} onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setInverterVoltage(Number(event.target.value))
        }
      }}/>
      <div>
        <TextField className={"Input default-margin"} id="BatteryVoltage" label="Battery Voltage" variant="outlined" placeholder="12"
                   type={"number"}  value={batteryVoltage} onChange={(event) => {
                     if (!isNaN(parseFloat(event.target.value))) {
                       setBatteryVoltage(Number(event.target.value))
                     }
                   }}/>
      </div>
    </div>}

    {systemType === "SELFMADE_DEVICE" && <div>
      <TextField className={"Input default-margin"} id="MaxSolarVoltage" label="Max Solar Panel Voltage"
                 variant="outlined" placeholder="45" type={"number"}  value={maxSolarVoltage} onChange={(event) => {
        if (!isNaN(parseFloat(event.target.value))) {
          setMaxSolarVoltage(Number(event.target.value))
        }
      }}
      />

      <h3>Is a Battery percentage Present?</h3>
      <Stack direction="row" spacing={1} alignItems="center">
        <Typography>no</Typography>
        <Switch checked={isBatteryPercentage} onChange={() => {
          setIsBatteryPercentage(!isBatteryPercentage)
        }}/>
        <Typography>yes</Typography>
      </Stack>


      <TextField className={"Input default-margin"} type={"number"} id="BatteryVoltage" label="Battery Voltage" variant="outlined"
                 placeholder="12" value={batteryVoltage} onChange={(event) => {
        if (!isNaN(Number(event.target.value))) {
          setBatteryVoltage(Number(event.target.value))
        }
      }}/>
    </div>}

    <div>
      <TextField className={"Input default-margin"} type="text" name="systemName" placeholder="SystemName" label="SystemName" value={systemName}
                 onChange={event => setSystemName(event.target.value)}/>
      <Button variant="outlined" onClick={() => {
        geolocation()
      }}>get Position</Button>
      {latitude != 0 && longitude != 0 &&
      <p>{"latitude:" + latitude + "\n"
      + "longitude:" + longitude}</p>
      }
    </div>


    <TextField className={"Input default-margin"} type="date" name="buildingDate" value={moment(buildingDate).format("yyyy-MM-DD")} onChange={event =>
      setBuildingDate(event.target.value)}/>

<div className={"default-margin"}>
  {!data ? <Button variant="outlined" onClick={() => {
      createSystem(systemName, date, systemType, isBatteryPercentage, inverterVoltage, batteryVoltage, maxSolarVoltage).then((response) => {
        setAlertOpen(true)
        setResponse(response.token.toString());
      })}
    }>Create a new SolarSystem</Button>:
    <Button variant="outlined" onClick={() => {
      patchSystem(systemName, date, systemType, isBatteryPercentage, inverterVoltage, batteryVoltage, maxSolarVoltage,data?.id).then((response) => {
        setAlertOpen(true)
        setResponse("Save successfully");
      })
    }
    }>Edit System</Button>
  }
</div>




    {//TODO move this to child component in this component
    data?.managers&&<div>
        <div style={{backgroundColor: "whitesmoke", overflow: "scroll", maxHeight: "400px", width: "40%",justifyContent:"center"}}>
          <ManagersOfTheSystem initManagers={data.managers} systemId={data?.id}/>
        </div>

    </div>
    }
  </div>}
  </div>
}

