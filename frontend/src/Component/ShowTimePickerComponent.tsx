import moment from "moment";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import {DashboardRange} from "./Accordions/StatisticsAccordion";
import React from "react";

interface ShowTimePickerComponentProps {
  creationDate: Date,
  setSelectDashboard: (value: DashboardRange) => void,
  setSelectDate: (n: number) => void;
  selectDate?: number;
}

export default function ShowTimePickerComponent({setSelectDate,setSelectDashboard, creationDate,selectDate}: ShowTimePickerComponentProps) {
  const [selectTimeRange, setSelectTimeRange] = React.useState<DashboardRange>("Week")

  const today = moment();
  let newCreationDate = moment(creationDate);

  const handleChange = (event: SelectChangeEvent) => {

    let d:DashboardRange = event.target.value as DashboardRange
    setSelectTimeRange(d)
    setSelectDashboard(d)
  }

  return <div>
    <input id="date" type="date" defaultValue={moment().format("YYYY-MM-DD").toString()} min={newCreationDate.format( "YYYY-MM-DD")} max={today.format( "YYYY-MM-DD")} onChange={event=>setSelectDate(moment(event.target.value).valueOf())}/>
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
