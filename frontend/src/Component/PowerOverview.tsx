import React from "react";
import {GraphDataObject} from "./../../api/GraphAPI";
import {SolarSystemDTO} from "./../../api/SolarSystemAPI";
import {formatDefaultValueWithUnitSplit} from "./utils/GraphUtils";
import {useTranslation} from "react-i18next";
import {
  getValueColorProduction,
  getValueColorConsumption,
  getValueColorSOC,
  getValueColorGrid,
  getValueColorBatteryWatt,
} from "./utils/ColorUtils";

interface PowerOverviewProps {
  graphData: GraphDataObject
  solarSystem: SolarSystemDTO
}

function PowerCard({title, borderColor, children}: {title: string, borderColor: string, children: React.ReactNode}) {
  return (
    <div
      style={{
        background: "white",
        borderRadius: "12px",
        padding: "20px 24px",
        borderLeft: `4px solid ${borderColor}`,
        borderRight: `4px solid ${borderColor}`,
        boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
        position: "relative",
        overflow: "hidden",
        minHeight: "unset",
        flex: "1 1 280px",
        minWidth: "0",
      }}
    >
      <div style={{
        fontSize: "13px",
        fontWeight: 500,
        color: borderColor,
        textTransform: "uppercase",
        letterSpacing: "0.5px",
        marginBottom: "16px",
      }}>
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
    <div style={{ display: "flex", flexDirection: "column" }}>
      <span style={{ fontSize: "11px", color: "#888", textTransform: "uppercase", marginBottom: "4px" }}>
        {label}
      </span>
      <span>
        <span style={{ fontSize: "28px", fontWeight: 700, color: valueColor }}>{value}</span>
        {unit && <span style={{ fontSize: "14px", fontWeight: 400, marginLeft: "4px", color: valueColor || "#666" }}>{unit}</span>}
      </span>
      {barPercent != null && (
        <div style={{
          marginTop: "6px",
          height: "4px",
          background: "#e0e0e0",
          borderRadius: "2px",
          overflow: "hidden",
        }}>
          <div style={{
            height: "100%",
            width: `${Math.min(100, Math.max(0, barPercent))}%`,
            background: barColor || "#43a047",
            borderRadius: "2px",
          }} />
        </div>
      )}
      {subValue && (
        <span style={{ fontSize: "12px", color: "#666", marginTop: "4px" }}>
          {subValue}
        </span>
      )}
    </div>
  );
}

export default function PowerOverview({graphData, solarSystem}: PowerOverviewProps) {
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

  // Current values from MongoDB cache
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

  const valuesRowStyle: React.CSSProperties = {
    display: "grid",
    gridTemplateColumns: "1fr 1fr 1fr",
    gap: "16px",
  };

  const valuesRowFullStyle: React.CSSProperties = {
    display: "grid",
    gridTemplateColumns: "1fr 1fr 1fr 1fr",
    gap: "16px",
  };

  const offlineStyle = isOnline === false ? { opacity: 0.4 } : {};

  return (
    <>
      {isOnline === false && (
        <div style={{
          textAlign: "center",
          padding: "8px 0",
          color: "#999",
          fontSize: "13px",
        }}>
          {t("common.offline")}
        </div>
      )}
      <div className="powerOverviewGrid" style={{
        display: "flex",
        flexWrap: "wrap",
        gap: "16px",
        ...offlineStyle,
      }}>

      {/* Production Card */}
      <PowerCard title={t("components.graph_accordion.production")} borderColor="#43a047">
        <div style={valuesRowStyle}>
          <ValueGroup
            label={t("common.current")}
            value={(currentInputWatt ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 0, useGrouping: false })}
            unit="W"
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
          <ValueGroup
            label={t("common.total")}
            value={formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").value}
            unit={formatDefaultValueWithUnitSplit(totalProduced * 1000, "Wh").unit}
            subValue={totalPrice != undefined ? `${twoDigests(totalPrice)} EUR` : undefined}
          />
        </div>
      </PowerCard>

      {/* Consumption Card (non-grid) */}
      {!shouldShowGridInfo() && (
        <PowerCard title={t("components.graph_accordion.consumption")} borderColor="#1e88e5">
          <div style={valuesRowStyle}>
            <ValueGroup
              label={t("common.current")}
              value={(currentOutputWatt ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 0, useGrouping: false })}
              unit="W"
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
            <ValueGroup
              label={t("common.total")}
              value={totalConsumed != undefined ? formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").value : "0"}
              unit={totalConsumed != undefined ? formatDefaultValueWithUnitSplit(totalConsumed * 1000, "Wh").unit : "Wh"}
              subValue={totalConsumedPrice != undefined ? `${twoDigests(totalConsumedPrice)} EUR` : undefined}
            />
          </div>
        </PowerCard>
      )}

      {/* Battery Card */}
      {!solarSystem.publicFlagOnlyProduction &&
       (solarSystem.type === "SELFMADE" || solarSystem.type === "GRID_BATTERY") && (
        <PowerCard title={t("common.battery")} borderColor="#8e24aa">
          <div style={valuesRowStyle}>
            <ValueGroup
              label={t("common.current")}
              value={`${(currentBatteryWatt ?? 0) < 0 ? "-" : ""}${Math.abs(currentBatteryWatt ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 0, useGrouping: false })}`}
              unit="W"
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
            />
            <ValueGroup
              label={t("components.graph_accordion.battery_label_voltage")}
              value={(currentBatteryVoltage ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 2, useGrouping: false })}
              unit="V"
            />
          </div>
        </PowerCard>
      )}

      {/* Grid Card */}
      {shouldShowGridInfo() && (
        <PowerCard title={t("common.grid")} borderColor="#fb8c00">
          <div style={valuesRowFullStyle}>
              <ValueGroup
                label={t("common.current")}
                value={(currentGridWatt ?? 0).toLocaleString('de-DE', { maximumFractionDigits: 0, useGrouping: false })}
                unit="W"
                valueColor={getValueColorGrid(currentGridWatt ?? 0)}
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
  );
}
