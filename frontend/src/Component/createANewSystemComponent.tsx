import React, {useState} from "react";
import {
  Alert,
  Box,
  Button,
  FormControl,
  IconButton,
  Input,
  InputLabel,
  MenuItem,
  Popover,
  Typography
} from '@mui/material';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {createSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import InfoIcon from '@mui/icons-material/Info';


export default function CreateNewSystemComponent() {
  const [systemName,setSystemName]=useState("");
  const [systemType, setSystemType] = useState("");
  const [creationDate,setCreationDate]= useState("");
  const [alertOpen,setAlertOpen]= useState(false);
  console.log(creationDate)
  console.log(systemType)
  let date =new Date(creationDate).getTime()/1000;
  console.log(date)


  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as string);
  };

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [text,setText]=useState("");
  const [newSystem,setNewSystem]=useState<SolarSystemDTO>();
  const handleClick = (event: React.MouseEvent<HTMLElement>,text:string) => {
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


  const geolocation =()=> {
    let latitude;
    let longitude;
    let altitude;
    let geoinfo;

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        function (position) {
          latitude = position.coords.latitude;
          longitude = position.coords.longitude;
          if (position.coords.altitude) {
            altitude = position.coords.altitude;
          } else {
            altitude = ' Keine Höhenangaben vorhanden ';
          }
          geoinfo = 'Latitude ' + latitude + 'Longitude'+longitude;
        });
    } else {
      geoinfo = 'Dieser Browser unterstützt die Abfrage der Geolocation nicht.';
    }
    console.log(geoinfo)
  }


  return <div>
    {alertOpen&& <Alert severity={"success"}>Creat new System{"\n token: "+newSystem?.token}</Alert>}
    <Box className="SolarTypeMenuBox ">
      <FormControl fullWidth  className="Input">
        <InputLabel className="Input">SolarSystemType</InputLabel>

        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={systemType}
          label="SolarSystem"
          onChange={handleChange}
        >

          <MenuItem value={"SELFMADE"} ><div className="menuItem"> Selfmade SolarSystem </div><IconButton color="primary" onClick={event=>handleClick(event,
            "Salfmade Solar system is a System with Solar")}><InfoIcon  color="primary"></InfoIcon></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_CONSUMPTION"} ><div className="menuItem">Selfmade with Consumption</div><IconButton color="primary" onClick={event=>handleClick(event,
              "This Solar System Produce Energy, when you not use your energy")}><InfoIcon color="primary"></InfoIcon></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_INVERTER"} ><div className="menuItem">Selfmade with inverter</div><IconButton color="primary" onClick={event=>handleClick(event,
              "text")}><InfoIcon color="primary"></InfoIcon></IconButton>
          </MenuItem>
          <MenuItem value={"SELFMADE_DEVICE"} ><div className="menuItem">Selfmade without converter</div><IconButton color="primary" onClick={event=>handleClick(event,
              "text")}><InfoIcon color="primary"></InfoIcon></IconButton>
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
            <Typography sx={{ p: 2 }}>{text}</Typography>
          </Popover>
        </Select>
      </FormControl>
    </Box>

    {/* max voltage*/}
    {systemType === "SELFMADE" && <div>


    </div>}

    {systemType === "SELFMADE_CONSUMPTION" && <div>


    </div>}

    {systemType === "SELFMADE_INVERTER" && <div>
      {/* Baterie voltage / kaperzität*/}
    </div>}

    {systemType === "SELFMADE_DEVICE" && <div>

    </div>}

    <Input className="default-margin Input" type="text" name="systemName" placeholder="SystemName"  value={systemName} onChange={event=>setSystemName(event.target.value)}/>

    <Button variant="outlined" onClick={()=>{geolocation()}}>get Position</Button>


    <Input className="default-margin Input" type="date" name="creationDate" placeholder="creationDate"  value={creationDate} onChange={event=>setCreationDate(event.target.value)}/>

    <Button variant="outlined" onClick={() => {
      createSystem(systemName,date,systemType).then((response)=>{
        setAlertOpen(true)
        setNewSystem(response);
      })}
    }>Create a new SolarSystem</Button>
  </div>
}

