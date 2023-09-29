import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";

interface TotalDataAccordionProps {
  graphData: GraphDataObject
  publicFlag: boolean
}

export default function TotalDataAccordion({graphData,publicFlag}: TotalDataAccordionProps) {

  let totalProduced = graphData.totalData.producedKWH;
  if(totalProduced == undefined || totalProduced <=0){
    totalProduced = graphData.totalData.calcProducedKWH;
  }
  let totalProducedPrice = graphData.totalData.producedKWHPrice;
  if(totalProducedPrice == undefined || totalProducedPrice <=0){
    totalProducedPrice = graphData.totalData.calcProducedKWHPrice;
  }

  let totalConsumed = graphData.totalData.consumedKWH;
  if(totalConsumed == undefined || totalConsumed <=0){
    totalConsumed = graphData.totalData.calcConsumedKWH;
  }
  let totalConsumedPrice = graphData.totalData.consumedKWHPrice;
  if(totalConsumedPrice == undefined || totalConsumedPrice <=0){
    totalConsumedPrice = graphData.totalData.calcConsumedKWHPrice;
  }

  let totalProducedDay = graphData.totalData.producedKWHDay;
  if(totalProducedDay == undefined || totalProducedDay <=0){
    totalProducedDay = graphData.totalData.calcProducedKWHDay;
  }
  let totalProducedPriceDay = graphData.totalData.producedKWHPriceDay;
  if(totalProducedPriceDay == undefined || totalProducedPriceDay <=0){
    totalProducedPriceDay = graphData.totalData.calcProducedKWHPriceDay;
  }

  let totalConsumedDay = graphData.totalData.consumedKWHDay;
  if(totalConsumedDay == undefined || totalConsumedDay <=0){
    totalConsumedDay = graphData.totalData.calcConsumedKWHDay;
  }
  let totalConsumedPriceDay = graphData.totalData.consumedKWHPriceDay;
  if(totalConsumedPriceDay == undefined || totalConsumedPriceDay <=0){
    totalConsumedPriceDay = graphData.totalData.calcConsumedKWHPriceDay;
  }

  const twoDigests = (value:number) => {
    return value.toLocaleString('de-DE', {
      maximumFractionDigits: 2,
      useGrouping: false
    })
  }

  return<div>{graphData &&
    <Accordion defaultExpanded={true} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
      >
        <Typography><b>Total Values</b></Typography>
      </AccordionSummary>
      <AccordionDetails>
        <div style={{display:"flex",flexDirection:"column"}}>
          <h3>Day</h3>
          <div className="defaultFlex">
            {totalProducedDay && <div className="totalValuesBox">
              Production:
              <div className="totalValuesFontSize">
                {twoDigests(totalProducedDay)}kwh
              </div>
            </div>}
            {totalProducedPriceDay && <div className="totalValuesBox">
              Saved Money:
              <div className="totalValuesFontSize">
                {twoDigests(totalProducedPriceDay)}€
              </div>
            </div>}
          </div>
          <h3>Total</h3>
          <div className="defaultFlex">
            {totalProduced && <div className="totalValuesBox">
              Today Consumption:
              <div className="totalValuesFontSize">
                {twoDigests(totalProduced)}kwh
              </div>
            </div>}
            {totalProducedPrice && <div className="totalValuesBox">
              Total Saved Money:
              <div className="totalValuesFontSize">
                {twoDigests(totalProducedPrice)}€
              </div>
            </div>}
          </div>
        </div>
      </AccordionDetails>
    </Accordion>}
  </div>
}
