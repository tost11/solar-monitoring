import moment from "moment";
import {format} from 'fecha';
import React, {useEffect, useState} from "react";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import {getSystem} from "../api/SolarSystemAPI";
import {DashboardRange} from "./Accordions/StatisticsAccordion";

interface ShowTimePickerComponentProps {
  creationDate: Date,
  setSelectDashboard: (value: DashboardRange) => void,
  setSelectDate: (n: number) => void;
}

export default function ShowTimePickerComponent({setSelectDate,setSelectDashboard, creationDate}: ShowTimePickerComponentProps) {
  const [selectTimeRange, setSelectTimeRange] = React.useState<DashboardRange>("Week")


  const today = moment();
  let newCreationDate = moment(creationDate);


  const handleChange = (event: SelectChangeEvent) => {

    let d:DashboardRange = event.target.value as DashboardRange
    setSelectTimeRange(d)
    setSelectDashboard(d)

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

        <MenuItem value={"Week"}>Week</MenuItem>
        <MenuItem value={"Month"}>Month</MenuItem>
        <MenuItem value={"Year"}>Year</MenuItem>
      </Select>
    </Box>
  </div>


}