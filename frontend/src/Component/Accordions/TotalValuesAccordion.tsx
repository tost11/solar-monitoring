import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../api/GraphAPI";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";
import {formatDefaultValueWithUnitSplit} from "../utils/GraphUtils";
import {useTranslation} from "react-i18next";
import {Colors} from "../utils/ColorUtils";

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
    return solarSystem.type === "GRID" || solarSystem.type === "GRID_BATTERY";
  };

  const shouldShowConsumption = () => {
    return !solarSystem.publicFlagOnlyProduction && !shouldShowGridInfo();
  };

  const totalProduced = graphData?.totalData?.produced ?? 0;
  const totalPrice = graphData?.totalData?.price ?? 0;
  const totalConsumed = graphData?.totalData?.consumed ?? 0;
  const totalConsumedPrice = graphData?.totalData?.consumedPrice ?? 0;
  const totalGridConsumed = graphData?.totalData?.gridConsumed ?? 0;
  const totalGridConsumedPrice = graphData?.totalData?.gridConsumedPrice ?? 0;
  const totalGridFeedIn = graphData?.totalData?.gridFeedIn ?? 0;
  const totalGridFeedInPrice = graphData?.totalData?.gridFeedInPrice ?? 0;
  const totalOverallConsumed = graphData?.totalData?.calcOverallConsumed ?? 0;
  const totalOverallConsumedPrice = graphData?.totalData?.calcOverallConsumedPrice ?? 0;

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

          <div className="total-values-section total-values-section--production">
            <div className="total-values__value-group">
              <div className="total-values__label total-values__label--production">{t("components.graph_accordion.production")}</div>
              <div className="total-values__label-sub">
                {t("common.total")}
              </div>
              <div className="total-values__value">
                {formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").value}
                <span className="total-values__value-unit">
                  {formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").unit}
                </span>
              </div>
              {totalPrice !== undefined && (
                <div className="total-values__subvalue">{twoDigests(totalPrice)} EUR</div>
              )}
            </div>
          </div>

          {shouldShowConsumption() && (
            <div className="total-values-section total-values-section--consumption">
              <div className="total-values__value-group">
                <div className="total-values__label total-values__label--consumption">{t("components.graph_accordion.consumption")}</div>
                <div className="total-values__label-sub">
                  {t("common.total")}
                </div>
                <div className="total-values__value">
                  {formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").value}
                  <span className="total-values__value-unit">
                    {formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").unit}
                  </span>
                </div>
                {totalConsumedPrice !== undefined && (
                  <div className="total-values__subvalue">{twoDigests(totalConsumedPrice)} EUR</div>
                )}
              </div>
            </div>
          )}

          {!solarSystem.publicFlagOnlyProduction && shouldShowGridInfo() && (
            <div className="total-values-section total-values-section--grid total-values-section--grid-border">
              <div className="total-values__label total-values__label--grid">{t("common.grid")}</div>
              <div className="totalValuesGrid" style={{display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px"}}>
                <div className="total-values__value-group">
                  <div className="total-values__grid-label">{t("components.graph_accordion.grid_consumption")}</div>
                  <div className="total-values__grid-value">
                    {formatDefaultValueWithUnitSplit(totalGridConsumed * 1000, "Wh").value}
                    <span className="total-values__grid-value-unit">
                      {formatDefaultValueWithUnitSplit(totalGridConsumed * 1000, "Wh").unit}
                    </span>
                  </div>
                  {totalGridConsumedPrice !== undefined && (
                    <div className="total-values__subvalue">{twoDigests(totalGridConsumedPrice)} EUR</div>
                  )}
                </div>
                <div className="total-values__value-group">
                  <div className="total-values__grid-label">{t("components.graph_accordion.grid_feedin")}</div>
                  <div className="total-values__grid-value">
                    {formatDefaultValueWithUnitSplit(totalGridFeedIn * 1000, "Wh").value}
                    <span className="total-values__grid-value-unit">
                      {formatDefaultValueWithUnitSplit(totalGridFeedIn * 1000, "Wh").unit}
                    </span>
                  </div>
                  {totalGridFeedInPrice !== undefined && (
                    <div className="total-values__subvalue">{twoDigests(totalGridFeedInPrice)} EUR</div>
                  )}
                </div>
                <div className="total-values__value-group">
                  <div className="total-values__grid-label">{t("components.graph_accordion.total_overall")}</div>
                  <div className="total-values__grid-value">
                    {formatDefaultValueWithUnitSplit(totalOverallConsumed * 1000, "Wh").value}
                    <span className="total-values__grid-value-unit">
                      {formatDefaultValueWithUnitSplit(totalOverallConsumed * 1000, "Wh").unit}
                    </span>
                  </div>
                  {totalOverallConsumedPrice !== undefined && (
                    <div className="total-values__subvalue">{twoDigests(totalOverallConsumedPrice)} EUR</div>
                  )}
                </div>
              </div>
            </div>
          )}

          </div>
        </div>
      </AccordionDetails>
    </Accordion>
  );
}
