import React, {useState} from "react";
import {Box, Button, FormControl, IconButton, Input, InputLabel, MenuItem, Popover, Typography} from '@mui/material';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {createSystem} from "../api/SolarSystemAPI";
import InfoIcon from '@mui/icons-material/Info';

interface IHash {
[deltails:string] :string
}


export default function CreateNewSystemComponent({}:IHash) {
  const [systemName,setsystemName]=useState("");
  const [systemType, setSystemType] = useState("");
  const [creationDate,setCreationDate]= useState("");
  console.log(creationDate)
  console.log(systemType)
  let date =new Date(creationDate).getTime()/1000;
  console.log(date)


  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as string);
  };

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [text,setText]=useState("");

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





  return <div>
    <Box className="SolarTypeMenuBox ">
      <FormControl fullWidth  className="Input">
        <InputLabel className="Input menuContant">SolarSystemType</InputLabel>
        <Select
          className="menuContant"
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={systemType}
          label="SolarSystem"
          onChange={handleChange}
        >
          <MenuItem className="menuContant" value={"SELFMADE"}  ><div className="menuItem"> Salfmade Solarsystem </div> <IconButton color="primary"  onClick={event=>handleClick(event,
            "Salfmade Solar system is a System with Solar")}><InfoIcon  color="primary"></InfoIcon></IconButton>
          </MenuItem>
            <MenuItem value={"SELFMADE_CONSUMPTION"} >Salfmade with Consumption<IconButton color="primary" onClick={event=>handleClick(event,
              "This Solar System Produce Energy, when you not use your energy")}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>
            <MenuItem value={"SELFMADE_INVERTER"} >Salfmade with inverter<IconButton color="primary" onClick={event=>handleClick(event,
              "text")}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>
            <MenuItem value={"SELFMADE_DEVICE"} >Salfmade without converter<IconButton color="primary" onClick={event=>handleClick(event,
              "text")}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>
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

    <Input className="default-margin Input" type="text" name="systemName" placeholder="SystemName"  value={systemName} onChange={event=>setsystemName(event.target.value)}/>
    <Input className="default-margin Input" type="date" name="creationDate" placeholder="creationDate"  value={creationDate} onChange={event=>setCreationDate(event.target.value)}/>

    <Button variant="outlined" onClick={() => {
      createSystem(systemName,date,systemType).then((response)=>{
        console.log(response)
      })}
    }>Create a new SolarSystem</Button>
  </div>
}

