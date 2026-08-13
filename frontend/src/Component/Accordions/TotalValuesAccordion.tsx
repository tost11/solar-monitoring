import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../api/GraphAPI";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";
import {formatDefaultValueWithUnitSplit} from "../utils/GraphUtils";
import {useTranslation} from "react-i18next";
import {DataCard} from "../DataCard";
import {DataValue} from "../DataValue";

interface TotalValuesAccordionProps {
  graphData: GraphDataObject
  solarSystem: SolarSystemDTO
}

export default function TotalValuesAccordion({graphData, solarSystem}: TotalValuesAccordionProps) {
  const { t } = useTranslation();

  const twoDigests = (value: number) => {
    return value.toLocaleString('de-DE', {
      maximumFractionDigits: 2,
      useGrouping: false
    });
  };

  const shouldShowGridInfo = () => {
    return (solarSystem.type === "GRID_BATTERY" || solarSystem.type === "GRID") &&
           (solarSystem.viewData.showGridInfo === true);
  };

  const shouldShowConsumption = () => {
    return !solarSystem.publicFlagOnlyProduction && !shouldShowGridInfo();
  };

  const totalProduced = graphData?.totalData?.producedKWH ?? 0;
  const totalPrice = graphData?.totalData?.producedKWHPrice;
  const totalConsumed = graphData?.totalData?.consumedKWH ?? 0;
  const totalConsumedPrice = graphData?.totalData?.consumedKWHPrice;
  const totalGridConsumed = graphData?.totalData?.gridConsumedKWH ?? 0;
  const totalGridConsumedPrice = graphData?.totalData?.gridConsumedKWHPrice;
  const totalGridFeedIn = graphData?.totalData?.gridFeedInKWH ?? 0;
  const totalGridFeedInPrice = graphData?.totalData?.gridFeedInKWHPrice;
  const totalOverallConsumed = graphData?.totalData?.calcOverallConsumedKWH ?? 0;
  const totalOverallConsumedPrice = graphData?.totalData?.calcOverallConsumedKWHPrice;

  return (
    <Accordion defaultExpanded={false} style={{backgroundColor: "snow"}} className={"DetailAccordion"}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        id="total-values-header"
        style={{backgroundColor: "lightblue"}}
      >
        <Typography><b>{t("common.total")}</b></Typography>
      </AccordionSummary>
      <AccordionDetails>
        <div style={{padding: "8px"}}>
          <div className="total-values-sections totalValuesSections">

          <DataCard
            title={t("components.graph_accordion.production")}
            type="production"
            minWidth={180}
            showHeader={false}
          >
            <DataValue
              label={t("common.total")}
              value={formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").value}
              unit={formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").unit}
              subValue={totalPrice != null ? `${twoDigests(totalPrice)} EUR` : undefined}
              labelMinHeight={36}
            />
          </DataCard>

          {shouldShowConsumption() && (
            <DataCard
              title={t("components.graph_accordion.consumption")}
              type="consumption"
              minWidth={180}
              showHeader={false}
            >
              <DataValue
                label={t("common.total")}
                value={formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").value}
                unit={formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").unit}
                subValue={totalConsumedPrice != null ? `${twoDigests(totalConsumedPrice)} EUR` : undefined}
                labelMinHeight={36}
              />
            </DataCard>
          )}

          {!solarSystem.publicFlagOnlyProduction && shouldShowGridInfo() && (
            <DataCard
              title={t("common.grid")}
              type="grid"
              minWidth={360}
              showHeader={false}
            >
              <div className="values-row-3col">
                <DataValue
                  label={t("components.graph_accordion.grid_consumption")}
                  value={formatDefaultValueWithUnitSplit(totalGridConsumed * 1000, "Wh").value}
                  unit={formatDefaultValueWithUnitSplit(totalGridConsumed * 1000, "Wh").unit}
                  subValue={totalGridConsumedPrice != null ? `${twoDigests(totalGridConsumedPrice)} EUR` : undefined}
                  labelMinHeight={36}
                />
                <DataValue
                  label={t("components.graph_accordion.grid_feedin")}
                  value={formatDefaultValueWithUnitSplit(totalGridFeedIn * 1000, "Wh").value}
                  unit={formatDefaultValueWithUnitSplit(totalGridFeedIn * 1000, "Wh").unit}
                  subValue={totalGridFeedInPrice != null ? `${twoDigests(totalGridFeedInPrice)} EUR` : undefined}
                  labelMinHeight={36}
                />
                <DataValue
                  label={t("components.graph_accordion.total_overall")}
                  value={formatDefaultValueWithUnitSplit(totalOverallConsumed * 1000, "Wh").value}
                  unit={formatDefaultValueWithUnitSplit(totalOverallConsumed * 1000, "Wh").unit}
                  subValue={totalOverallConsumedPrice != null ? `${twoDigests(totalOverallConsumedPrice)} EUR` : undefined}
                  labelMinHeight={36}
                />
              </div>
            </DataCard>
          )}

          </div>
        </div>
      </AccordionDetails>
    </Accordion>
  );
}
