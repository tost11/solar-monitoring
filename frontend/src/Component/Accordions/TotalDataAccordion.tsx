import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../api/GraphAPI";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";
import {useTranslation} from "react-i18next";

interface TotalDataAccordionProps {
  graphData: GraphDataObject
  solarSystem: SolarSystemDTO
}

export default function TotalDataAccordion({graphData,solarSystem}: TotalDataAccordionProps) {

  const { t } = useTranslation()

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

  let totalGridConsumed = graphData.totalData.gridConsumedKWH;
  if(totalGridConsumed == undefined || totalGridConsumed <=0){
    totalGridConsumed = graphData.totalData.calcGridConsumedKWH;
  }
  let totalGridConsumedPrice = graphData.totalData.gridConsumedKWHPrice;
  if(totalGridConsumedPrice == undefined || totalGridConsumedPrice <=0){
    totalGridConsumedPrice = graphData.totalData.calcGridConsumedKWHPrice;
  }

  let totalGridFeedIn = graphData.totalData.gridFeedInKWH;
  if(totalGridFeedIn == undefined || totalGridFeedIn <=0){
    totalGridFeedIn = graphData.totalData.calcGridFeedInKWH;
  }
  let totalGridFeedInPriceDay = graphData.totalData.gridFeedInKWHPriceDay;
  if(totalGridFeedInPriceDay == undefined || totalGridFeedInPriceDay <=0){
    totalGridFeedInPriceDay = graphData.totalData.calcGridFeedInKWHPriceDay;
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

  let totalGridConsumedDay = graphData.totalData.gridConsumedKWHDay;
  if(totalGridConsumedDay == undefined || totalGridConsumedDay <=0){
    totalGridConsumedDay = graphData.totalData.calcGridConsumedKWHDay;
  }
  let totalGridConsumedPriceDay = graphData.totalData.gridConsumedKWHPriceDay;
  if(totalGridConsumedPriceDay == undefined || totalGridConsumedPriceDay <=0){
    totalGridConsumedPriceDay = graphData.totalData.calcGridConsumedKWHPriceDay;
  }

  let totalGridFeedInDay = graphData.totalData.gridFeedInKWHDay;
  if(totalGridFeedInDay == undefined || totalGridFeedInDay <=0){
    totalGridFeedInDay = graphData.totalData.calcGridFeedInKWHDay;
  }
  let totalGridFeedInPriceDay = graphData.totalData.gridFeedInKWHPriceDay;
  if(totalGridFeedInPriceDay == undefined || totalGridFeedInPriceDay <=0){
    totalGridFeedInPriceDay = graphData.totalData.calcGridFeedInKWHPriceDay;
  }


  if(totalProducedDay == undefined){
    totalProducedDay = 0;
  }
  if(totalProduced == undefined){
    totalProduced = 0;
  }

  //sub and add grid data if available
  if(typeof totalConsumedDay === 'number'){
    if(typeof totalGridConsumedDay === 'number'){
      totalConsumedDay += totalGridConsumedDay;
    }
    if(typeof totalGridFeedInDay === 'number'){
      totalConsumedDay -= totalGridFeedInDay;
    }
  }

  //sub and add grid data if available
  if(typeof totalConsumedPriceDay === 'number'){
    if(typeof totalGridConsumedPriceDay === 'number'){
      totalConsumedPriceDay += totalGridConsumedPriceDay;
    }
    if(typeof totalGridFeedInPriceDay === 'number'){
      totalConsumedPriceDay -= totalGridFeedInPriceDay;
    }
  }

  if(typeof totalConsumed === 'number'){
    if(typeof totalGridConsumed === 'number'){
      totalConsumed += totalGridConsumed;
    }
    if(typeof totalGridFeedIn === 'number'){
      totalConsumed -= totalGridFeedIn;
    }
  }

  //sub and add grid data if available
  if(typeof totalConsumedPrice === 'number'){
    if(typeof totalGridConsumedPrice === 'number'){
      totalConsumedPrice += totalGridConsumedPrice;
    }
    if(typeof totalGridFeedInPrice === 'number'){
      totalConsumedPrice -= totalGridFeedInPrice;
    }
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

  if(solarSystem.viewData.hideTotalConsumption){
    totalConsumed = undefined;
    totalConsumedDay = undefined;
  }

  return<div>{graphData &&
    <Accordion defaultExpanded={true} style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        style={{backgroundColor:"lightblue"}}
      >
        <Typography><b>{t("components.graph_accordion.total_values")}</b></Typography>
      </AccordionSummary>
      <AccordionDetails>
        <div style={{display:"flex",flexDirection:"row",flexFlow:"wrap"}}>
          <div className="defaultFlowColumn totalValuesBorderBox">
            <h3>{t("components.graph_accordion.daily")}</h3>
            <div className="defaultFlex">
              <div className="totalValuesBox">
                {t("components.graph_accordion.production")}:
                <div className="totalValuesFontSize">
                  {twoDigests(totalProducedDay)}kwh
                </div>
              </div>
              {totalConsumedDay != undefined && <div className="totalValuesBox">
                {t("components.graph_accordion.consumption")}:
                <div className="totalValuesFontSize">
                  {twoDigests(totalConsumedDay)}kwh
                </div>
              </div>}
              {totalPriceDay != undefined && <div className="totalValuesBox">
                {t("components.graph_accordion.money")}:
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
                {t("components.graph_accordion.production")}:
                <div className="totalValuesFontSize">
                  {twoDigests(totalProduced)}kwh
                </div>
              </div>
              {totalConsumed != undefined && <div className="totalValuesBox">
                {t("components.graph_accordion.consumption")}:
                <div className="totalValuesFontSize">
                  {twoDigests(totalConsumed)}kwh
                </div>
              </div>}
              {totalPrice != undefined && <div className="totalValuesBox">
                {t("components.graph_accordion.money")}:
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
