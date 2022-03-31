import moment from "moment";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import React from "react";


interface ShowTimePickerComponentProps {
  creationDate: Date,
  setTimeRange: (value: string) => void,
  setSelectDate: (n: number) => void;
  selectDate?: number;
}

export default function ShowTimePickerComponent({setSelectDate,setTimeRange, creationDate}: ShowTimePickerComponentProps) {
  const [selectTimeRange, setSelectTimeRange] = React.useState("1w")

  const today = moment();
  let newCreationDate = moment(creationDate);

  const handleChange = (event: SelectChangeEvent) => {
    setSelectTimeRange(event.target.value)
    setTimeRange(event.target.value)
  }

  return <div>
    <input id="date" type="date" min={newCreationDate.format( "YYYY-MM-DD")} max={today.format( "YYYY-MM-DD")} defaultValue={today.format( "YYYY-MM-DD")} onChange={event=>setSelectDate(moment(event.target.value).valueOf())}/>

    <Box>
      <InputLabel id="demo-simple-select-label">Select Time Range</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={selectTimeRange}
        label="RefreshTime"
        onChange={handleChange}
      >

        <MenuItem value={"1w"}>Week</MenuItem>
        <MenuItem value={"1M"}>Month</MenuItem>
        <MenuItem value={"1y"}>Year</MenuItem>
      </Select>
    </Box>
  </div>


}
