import moment from "moment";
import {format} from 'fecha';
import React, {useEffect, useState} from "react";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import {getSystem} from "../api/SolarSystemAPI";

interface ShowTimePickerComponentProps {
  creationDate: number,
  setSelectDashboard: (value: string) => void,
  setSelectDate: (n: number) => void;
}

export default function ShowTimePickerComponent({setSelectDate,setSelectDashboard, creationDate}: ShowTimePickerComponentProps) {
  const [selectTimeRange, setSelectTimeRange] = React.useState("")


  const today = moment();
  let newCreationDate = moment(creationDate);


  const handleChange = (event: SelectChangeEvent) => {
    setSelectTimeRange(event.target.value)
    setSelectDashboard(event.target.value)

  }


  return <div>

    <input id="date" type="date" min={newCreationDate.format( "YYYY-MM-DD")} max={today.format( "YYYY-MM-DD")} onChange={event=>setSelectDate(moment(event.target.value).valueOf())}/>
    <Box>
      <InputLabel id="demo-simple-select-label">Select Time Range</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={selectTimeRange}
        label="RefreshTime"
        onChange={handleChange}
      >

        <MenuItem value={"w"}>Week</MenuItem>
        <MenuItem value={"m"}>Month</MenuItem>
        <MenuItem value={"y"}>Year</MenuItem>
      </Select>
    </Box>
  </div>


}
