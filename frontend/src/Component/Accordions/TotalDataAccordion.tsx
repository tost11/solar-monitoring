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

  let totalGridFeedIn = graphData.totalData.gridFeedInKWH;
  if(totalGridFeedIn == undefined || totalGridFeedIn <=0){
    totalGridFeedIn = graphData.totalData.calcGridFeedInKWH;
  }
  let totalGridFeedInPrice = graphData.totalData.gridFeedInKWHPrice;
  if(totalGridFeedInPrice == undefined || totalGridFeedInPrice<=0){
    totalGridFeedInPrice = graphData.totalData.calcGridFeedInKWHPrice
  }

  let totalGridConsumed = graphData.totalData.gridConsumedKWHDay;
  if(totalGridConsumed == undefined || totalGridConsumed <=0){
    totalGridConsumed = graphData.totalData.calcGridConsumedKWH;
  }
  let totalGridConsumedPrice = graphData.totalData.gridConsumedKWHPrice;
  if(totalGridConsumedPrice == undefined || totalGridConsumedPrice<=0){
    totalGridConsumedPrice = graphData.totalData.calcGridConsumedKWHPrice
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

   let totalOverallConsumed = graphData.totalData.calcOverallConsumedKWH;
   let totalOverallConsumedDay = graphData.totalData.calcOverallConsumedKWHDay;
   let totalOverallConsumedPrice = graphData.totalData.calcOverallConsumedKWHPrice;
   let totalOverallConsumedPriceDay = graphData.totalData.calcOverallConsumedKWHPriceDay;

  if(totalProducedDay == undefined){
    totalProducedDay = 0;
  }
  if(totalProduced == undefined){
    totalProduced = 0;
  }

  // Helper functions for system type checks
  const shouldShowStandardConsumption = () => {
    return solarSystem.type !== "GRID_BATTERY" && solarSystem.type !== "GRID";
  };

  const shouldShowGridInfo = () => {
    return (solarSystem.type === "GRID_BATTERY" || solarSystem.type === "GRID") &&
           (solarSystem.viewData.showGridInfo === true);
  };

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

  return (
    <div>
      {graphData && (
        <Accordion
          defaultExpanded={true}
          style={{ backgroundColor: "snow" }}
          className={"DetailAccordion"}
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon />}
            style={{ backgroundColor: "lightblue" }}
          >
            <Typography>
              <b>{t("components.graph_accordion.total_values")}</b>
            </Typography>
          </AccordionSummary>
          <AccordionDetails>
            <div
              style={{
                display: "flex",
                flexDirection: "row",
                flexFlow: "wrap",
              }}
            >
              <div className="defaultFlowColumn totalValuesBorderBox">
                <h3>{t("components.graph_accordion.daily")}</h3>
                <div className="defaultFlex">
                  <div className="totalValuesBox">
                    {t("components.graph_accordion.production")}:
                    <div className="totalValuesFontSize">
                      {twoDigests(totalProducedDay)}kwh
                    </div>
                  </div>
                  {shouldShowStandardConsumption() &&
                    totalConsumedDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.consumption")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalConsumedDay)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowStandardConsumption() &&
                    totalPriceDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalPriceDay)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridConsumedDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.grid_consumption")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridConsumedDay)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridConsumedPriceDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money_grid_consumption")}
                        :
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridConsumedPriceDay)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridFeedInDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.grid_feedin")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridFeedInDay)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridFeedInPriceDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money_grid_feedin")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridFeedInPriceDay)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalOverallConsumedDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.total_overall")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalOverallConsumedDay)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalOverallConsumedPriceDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalOverallConsumedPriceDay)}€
                        </div>
                      </div>
                    )}
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
                  {shouldShowStandardConsumption() &&
                    totalConsumed != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.consumption")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalConsumed)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowStandardConsumption() &&
                    totalPrice != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalPrice)}€
                        </div>
                      </div>
                    )}
                  {shouldShowStandardConsumption() &&
                    totalPriceDay != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalPriceDay)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridConsumed != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.grid_consumption")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridConsumed)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridConsumedPrice != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money_grid_consumption")}
                        :
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridConsumedPrice)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridFeedIn != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.grid_feedin")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridFeedIn)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalGridFeedInPrice != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money_grid_feedin")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalGridFeedInPrice)}€
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalOverallConsumed != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.total_overall")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalOverallConsumed)}kwh
                        </div>
                      </div>
                    )}
                  {shouldShowGridInfo() &&
                    totalOverallConsumedPrice != undefined && (
                      <div className="totalValuesBox">
                        {t("components.graph_accordion.money")}:
                        <div className="totalValuesFontSize">
                          {twoDigests(totalOverallConsumedPrice)}€
                        </div>
                      </div>
                    )}
                </div>
              </div>
            </div>
          </AccordionDetails>
        </Accordion>
      )}
    </div>
  );
}
