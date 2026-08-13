import React from "react";
import {GraphDataObject} from "../../api/GraphAPI";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";
import {formatDefaultValueWithUnitSplit} from "../utils/GraphUtils";
import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Typography from "@mui/material/Typography";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {useTranslation} from "react-i18next";
import {
  getValueColorProduction,
  getValueColorConsumption,
  getValueColorSOC,
  getValueColorGrid,
  getValueColorBatteryWatt,
  Colors,
} from "../utils/ColorUtils";

interface TotalDataAccordionProps {
  graphData: GraphDataObject
  solarSystem: SolarSystemDTO
}

function PowerCard({title, type, children, minWidth = 280}: {title: string, type: string, children: React.ReactNode, minWidth?: number}) {
  const cardClass = `power-card power-card--${type}${minWidth === 180 ? ' power-card--min-180' : ''}`;
  const titleClass = `power-card__title power-card__title--${type}`;

  return (
    <div className={cardClass} style={minWidth !== 280 && minWidth !== 180 ? { flex: `1 1 ${minWidth}px` } : undefined}>
      <div className={titleClass}>
        {title}
      </div>
      {children}
    </div>
  );
}

function ValueGroup({label, value, unit, subValue, barPercent, barColor, valueColor}: {
  label: string
  value: string
  unit?: string
  subValue?: string
  barPercent?: number
  barColor?: string
  valueColor?: string
}) {
  return (
    <div className="value-group">
      <span className="value-group__label">
        {label}
      </span>
      <span>
        <span className="value-group__value" style={valueColor ? { color: valueColor } : undefined}>{value}</span>
        {unit && <span className="value-group__unit" style={valueColor ? { color: valueColor } : undefined}>{unit}</span>}
      </span>
      {barPercent != null && (
        <div className="value-group__bar">
          <div
            className="value-group__bar-fill value-group__bar-fill--production"
            style={{
              width: `${Math.min(100, Math.max(0, barPercent))}%`,
              background: barColor || Colors.productionGreen
            }}
          />
        </div>
      )}
      {subValue && (
        <span className="value-group__subvalue">
          {subValue}
        </span>
      )}
    </div>
  );
}

export default function TotalDataAccordion({graphData, solarSystem}: TotalDataAccordionProps) {
  const { t } = useTranslation();

  const totalData = graphData.totalData;

  let totalProduced = totalData.producedKWH;
  if (totalProduced == undefined || totalProduced <= 0) {
    totalProduced = totalData.calcProducedKWH;
  }
  let totalProducedPrice = totalData.producedKWHPrice;
  if (totalProducedPrice == undefined || totalProducedPrice <= 0) {
    totalProducedPrice = totalData.calcProducedKWHPrice;
  }

  let totalConsumed = totalData.consumedKWH;
  if (totalConsumed == undefined || totalConsumed <= 0) {
    totalConsumed = totalData.calcConsumedKWH;
  }
  let totalConsumedPrice = totalData.consumedKWHPrice;
  if (totalConsumedPrice == undefined || totalConsumedPrice <= 0) {
    totalConsumedPrice = totalData.calcConsumedKWHPrice;
  }

  let totalGridFeedIn = totalData.gridFeedInKWH;
  if (totalGridFeedIn == undefined || totalGridFeedIn <= 0) {
    totalGridFeedIn = totalData.calcGridFeedInKWH;
  }
  let totalGridFeedInPrice = totalData.gridFeedInKWHPrice;
  if (totalGridFeedInPrice == undefined || totalGridFeedInPrice <= 0) {
    totalGridFeedInPrice = totalData.calcGridFeedInKWHPrice;
  }

  let totalGridConsumed = totalData.gridConsumedKWH;
  if (totalGridConsumed == undefined || totalGridConsumed <= 0) {
    totalGridConsumed = totalData.calcGridConsumedKWH;
  }
  let totalGridConsumedPrice = totalData.gridConsumedKWHPrice;
  if (totalGridConsumedPrice == undefined || totalGridConsumedPrice <= 0) {
    totalGridConsumedPrice = totalData.calcGridConsumedKWHPrice;
  }

  let totalProducedDay = totalData.producedKWHDay;
  if (totalProducedDay == undefined || totalProducedDay <= 0) {
    totalProducedDay = totalData.calcProducedKWHDay;
  }
  let totalProducedPriceDay = totalData.producedKWHPriceDay;
  if (totalProducedPriceDay == undefined || totalProducedPriceDay <= 0) {
    totalProducedPriceDay = totalData.calcProducedKWHPriceDay;
  }

  let totalConsumedDay = totalData.consumedKWHDay;
  if (totalConsumedDay == undefined || totalConsumedDay <= 0) {
    totalConsumedDay = totalData.calcConsumedKWHDay;
  }
  let totalConsumedPriceDay = totalData.consumedKWHPriceDay;
  if (totalConsumedPriceDay == undefined || totalConsumedPriceDay <= 0) {
    totalConsumedPriceDay = totalData.calcConsumedKWHPriceDay;
  }

  let totalGridConsumedDay = totalData.gridConsumedKWHDay;
  if (totalGridConsumedDay == undefined || totalGridConsumedDay <= 0) {
    totalGridConsumedDay = totalData.calcGridConsumedKWHDay;
  }
  let totalGridConsumedPriceDay = totalData.gridConsumedKWHPriceDay;
  if (totalGridConsumedPriceDay == undefined || totalGridConsumedPriceDay <= 0) {
    totalGridConsumedPriceDay = totalData.calcGridConsumedKWHPriceDay;
  }

  let totalGridFeedInDay = totalData.gridFeedInKWHDay;
  if (totalGridFeedInDay == undefined || totalGridFeedInDay <= 0) {
    totalGridFeedInDay = totalData.calcGridFeedInKWHDay;
  }
  let totalGridFeedInPriceDay = totalData.gridFeedInKWHPriceDay;
  if (totalGridFeedInPriceDay == undefined || totalGridFeedInPriceDay <= 0) {
    totalGridFeedInPriceDay = totalData.calcGridFeedInKWHPriceDay;
  }

  let totalOverallConsumed = totalData.calcOverallConsumedKWH;
  let totalOverallConsumedDay = totalData.calcOverallConsumedKWHDay;
  let totalOverallConsumedPrice = totalData.calcOverallConsumedKWHPrice;
  let totalOverallConsumedPriceDay = totalData.calcOverallConsumedKWHPriceDay;

  if (totalProducedDay == undefined) totalProducedDay = 0;
  if (totalProduced == undefined) totalProduced = 0;

  let totalPrice = totalConsumedPrice;
  let totalPriceDay = totalConsumedPriceDay;
  if (solarSystem.viewData.productionForTotalPricing) {
    totalPrice = totalProducedPrice;
    totalPriceDay = totalProducedPriceDay;
  }

  if (solarSystem.viewData.hideTotalConsumption) {
    totalConsumed = undefined;
    totalConsumedDay = undefined;
  }

  const shouldShowGridInfo = () => {
    return (solarSystem.type === "GRID_BATTERY" || solarSystem.type === "GRID") &&
           (solarSystem.viewData.showGridInfo === true);
  };

  const twoDigests = (value: number) => {
    return value.toLocaleString('de-DE', {
      maximumFractionDigits: 2,
      useGrouping: false
    });
  };

  const cv = graphData.currentValues;
  const currentInputWatt = cv?.inputWatt;
  const currentOutputWatt = cv?.outputWatt;
  const currentBatteryWatt = cv?.batteryWatt;
  const currentBatteryPercentage = cv?.batteryPercentage;
  const currentBatteryVoltage = cv?.batteryVoltage;
  const currentGridWatt = cv?.gridWatt;
  const isOnline = graphData.isOnline;

  const maxSolar = solarSystem.maxInstalledSolarPower;
  const productionPercent = (maxSolar && maxSolar > 0 && currentInputWatt != null)
    ? (currentInputWatt / maxSolar) * 100 : undefined;

  const remainingCapacity = solarSystem.batteryCapacity != null && currentBatteryPercentage != null
    ? (currentBatteryPercentage / 100) * solarSystem.batteryCapacity
    : null;

  const capacitySubValue = remainingCapacity !== null && solarSystem.batteryCapacity != null
    ? `${formatDefaultValueWithUnitSplit(remainingCapacity * 1000, "Wh").value}/${formatDefaultValueWithUnitSplit(solarSystem.batteryCapacity * 1000, "Wh").value} ${formatDefaultValueWithUnitSplit(solarSystem.batteryCapacity * 1000, "Wh").unit}`
    : undefined;


  return (
    <div>
      {graphData && (
        <Accordion
          defaultExpanded
          style={{ backgroundColor: "snow" }}
          className={"DetailAccordion"}
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon />}
            style={{ backgroundColor: "lightblue" }}
          >
            <Typography>
              <b>{t("common.current")} {t("common.values")}</b>
            </Typography>
          </AccordionSummary>
          <AccordionDetails>
            <>
              {isOnline === false && (
                <div className="power-overview__offline-notice">
                  {t("common.offline")}
                </div>
              )}
              <div className={`power-overview-grid${isOnline === false ? ' power-overview-grid--offline' : ''}`}>

              <PowerCard title={t("components.graph_accordion.production")} type="production" minWidth={180}>
                <div className="values-row-2col">
                  <ValueGroup
                    label={t("common.current")}
                    value={formatDefaultValueWithUnitSplit(currentInputWatt ?? 0, "W", 0).value}
                    unit={formatDefaultValueWithUnitSplit(currentInputWatt ?? 0, "W", 0).unit}
                    valueColor={maxSolar && maxSolar > 0
                      ? getValueColorProduction(currentInputWatt ?? 0, maxSolar) : undefined}
                    barPercent={productionPercent}
                    subValue={productionPercent != null ? `${productionPercent.toFixed(0)}% installed` : undefined}
                  />
                  <ValueGroup
                    label={t("common.day")}
                    value={formatDefaultValueWithUnitSplit(totalProducedDay * 1000, "Wh").value}
                    unit={formatDefaultValueWithUnitSplit(totalProducedDay * 1000, "Wh").unit}
                    subValue={totalProducedPriceDay != undefined ? `${twoDigests(totalProducedPriceDay)} EUR` : undefined}
                  />
                </div>
              </PowerCard>

              {!solarSystem.publicFlagOnlyProduction && !shouldShowGridInfo() && (
                <PowerCard title={t("components.graph_accordion.consumption")} type="consumption" minWidth={180}>
                  <div className="values-row-2col">
                    <ValueGroup
                      label={t("common.current")}
                      value={formatDefaultValueWithUnitSplit(currentOutputWatt ?? 0, "W", 0).value}
                      unit={formatDefaultValueWithUnitSplit(currentOutputWatt ?? 0, "W", 0).unit}
                      valueColor={solarSystem.maxInverterOutputPower && solarSystem.maxInverterOutputPower > 0
                        ? getValueColorConsumption(currentOutputWatt ?? 0, solarSystem.maxInverterOutputPower) : undefined}
                      barPercent={solarSystem.maxInverterOutputPower && solarSystem.maxInverterOutputPower > 0 && currentOutputWatt != null
                        ? (currentOutputWatt / solarSystem.maxInverterOutputPower) * 100 : undefined}
                      subValue={solarSystem.maxInverterOutputPower && solarSystem.maxInverterOutputPower > 0 && currentOutputWatt != null
                        ? `${(((currentOutputWatt / solarSystem.maxInverterOutputPower) * 100)).toFixed(0)}% of max` : undefined}
                    />
                    <ValueGroup
                      label={t("common.day")}
                      value={totalConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalConsumedDay * 1000, "Wh").value : "0"}
                      unit={totalConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalConsumedDay * 1000, "Wh").unit : "Wh"}
                      subValue={totalConsumedPriceDay != undefined ? `${twoDigests(totalConsumedPriceDay)} EUR` : undefined}
                    />
                  </div>
                </PowerCard>
              )}

              {!solarSystem.publicFlagOnlyProduction &&
               (solarSystem.type === "SELFMADE" || solarSystem.type === "GRID_BATTERY") && (
                <PowerCard title={t("common.battery")} type="battery">
                  <div className="values-row-3col">
                    <ValueGroup
                      label={t("common.current")}
                      value={`${(currentBatteryWatt ?? 0) < 0 ? "-" : ""}${formatDefaultValueWithUnitSplit(Math.abs(currentBatteryWatt ?? 0), "W", 0).value}`}
                      unit={formatDefaultValueWithUnitSplit(Math.abs(currentBatteryWatt ?? 0), "W", 0).unit}
                      valueColor={getValueColorBatteryWatt(currentBatteryWatt ?? 0)}
                      subValue={(currentBatteryWatt ?? 0) > 0
                        ? t("views.tag_aggregation.charging")
                        : (currentBatteryWatt ?? 0) < 0
                          ? t("views.tag_aggregation.discharging")
                          : ""}
                    />
                    <ValueGroup
                      label={t("common.soc")}
                      value={(currentBatteryPercentage ?? 0).toFixed(0)}
                      unit="%"
                      valueColor={getValueColorSOC(currentBatteryPercentage ?? 0)}
                      barPercent={currentBatteryPercentage}
                      subValue={capacitySubValue}
                    />
                    <ValueGroup
                      label={t("components.graph_accordion.battery_label_voltage")}
                      value={(currentBatteryVoltage ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 2, useGrouping: false })}
                      unit="V"
                    />
                  </div>
                </PowerCard>
              )}

              {!solarSystem.publicFlagOnlyProduction && shouldShowGridInfo() && (
                <PowerCard title={t("common.grid")} type="grid">
                  <div className="values-row-4col">
                      <ValueGroup
                        label={t("common.current")}
                        value={formatDefaultValueWithUnitSplit(currentGridWatt ?? 0, "W", 0).value}
                        unit={formatDefaultValueWithUnitSplit(currentGridWatt ?? 0, "W", 0).unit}
                        valueColor={getValueColorGrid(currentGridWatt ?? 0, 5000)}
                        subValue={(currentGridWatt ?? 0) > 0
                          ? t("views.tag_aggregation.consuming")
                          : t("views.tag_aggregation.feeding_in")}
                      />
                    <ValueGroup
                      label={t("components.graph_accordion.grid_consumption")}
                      value={totalGridConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalGridConsumedDay * 1000, "Wh").value : "0"}
                      unit={totalGridConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalGridConsumedDay * 1000, "Wh").unit : "Wh"}
                      subValue={totalGridConsumedPriceDay != undefined ? `${twoDigests(totalGridConsumedPriceDay)} EUR` : undefined}
                    />
                    <ValueGroup
                      label={t("components.graph_accordion.grid_feedin")}
                      value={totalGridFeedInDay != undefined ? formatDefaultValueWithUnitSplit(totalGridFeedInDay * 1000, "Wh").value : "0"}
                      unit={totalGridFeedInDay != undefined ? formatDefaultValueWithUnitSplit(totalGridFeedInDay * 1000, "Wh").unit : "Wh"}
                      subValue={totalGridFeedInPriceDay != undefined ? `${twoDigests(totalGridFeedInPriceDay)} EUR` : undefined}
                    />
                    <ValueGroup
                      label={t("components.graph_accordion.total_overall")}
                      value={totalOverallConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalOverallConsumedDay * 1000, "Wh").value : "0"}
                      unit={totalOverallConsumedDay != undefined ? formatDefaultValueWithUnitSplit(totalOverallConsumedDay * 1000, "Wh").unit : "Wh"}
                      subValue={totalOverallConsumedPriceDay != undefined ? `${twoDigests(totalOverallConsumedPriceDay)} EUR` : undefined}
                    />
                  </div>
                </PowerCard>
              )}

            </div>
            </>
          </AccordionDetails>
        </Accordion>
      )}
    </div>
  );
}
