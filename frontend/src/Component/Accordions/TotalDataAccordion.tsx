import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";

interface TotalDataAccordionProps {
  graphData: GraphDataObject
  solarSystem: SolarSystemDTO
}

export default function TotalDataAccordion({graphData,solarSystem}: TotalDataAccordionProps) {

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

  if(totalProducedDay == undefined){
    totalProducedDay = 0;
  }
  if(totalProduced == undefined){
    totalProduced = 0;
  }

  if(solarSystem.electricityPrice == null){
    totalConsumedPrice = undefined;
    totalProducedPrice = undefined;
    totalProducedPriceDay = undefined;
    totalConsumedPriceDay = undefined;
  }

  const twoDigests = (value:number) => {
    return value.toLocaleString('de-DE', {
      maximumFractionDigits: 2,
      useGrouping: false
    })
  }

  let totalPrice = totalConsumedPrice;
  let totalPriceDay = totalConsumedPriceDay;
  if(solarSystem.viewData.productionForTotalPricing){
    totalPrice = totalProducedPrice;
    totalPriceDay = totalProducedPriceDay;
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
        <div style={{display:"flex",flexDirection:"row",flexFlow:"wrap"}}>
          <div className="defaultFlowColumn totalValuesBorderBox">
            <h3>Day</h3>
            <div className="defaultFlex">
              <div className="totalValuesBox">
                Production:
                <div className="totalValuesFontSize">
                  {twoDigests(totalProducedDay)}kwh
                </div>
              </div>
              {totalConsumedDay != undefined && <div className="totalValuesBox">
                Consumption:
                <div className="totalValuesFontSize">
                  {twoDigests(totalConsumedDay)}kwh
                </div>
              </div>}
              {totalPriceDay != undefined && <div className="totalValuesBox">
                Saved Money:
                <div className="totalValuesFontSize">
                  {twoDigests(totalPriceDay)}€
                </div>
              </div>}
            </div>
          </div>
          <div className="defaultFlowColumn totalValuesBorderBox">
            <h3>Total</h3>
            <div className="defaultFlex">
              <div className="totalValuesBox">
                Production:
                <div className="totalValuesFontSize">
                  {twoDigests(totalProduced)}kwh
                </div>
              </div>
              {totalConsumed != undefined && <div className="totalValuesBox">
                Consumption:
                <div className="totalValuesFontSize">
                  {twoDigests(totalConsumed)}kwh
                </div>
              </div>}
              {totalPrice != undefined && <div className="totalValuesBox">
                Saved Money:
                <div className="totalValuesFontSize">
                  {twoDigests(totalPrice)}€
                </div>
              </div>}
            </div>
          </div>
        </div>
      </AccordionDetails>
    </Accordion>}
  </div>
}
