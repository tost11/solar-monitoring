import React from "react";
import {Colors} from "./utils/ColorUtils";

interface DataValueProps {
  label: string;
  value: string;
  unit?: string;
  subValue?: string;
  barPercent?: number;
  barColor?: string;
  valueColor?: string;
  labelMinHeight?: number;
}

export function DataValue({
  label,
  value,
  unit,
  subValue,
  barPercent,
  barColor,
  valueColor,
  labelMinHeight = 33
}: DataValueProps) {
  return (
    <div className="data-value">
      <span
        className="data-value__label"
        style={{ minHeight: `${labelMinHeight}px` }}
      >
        {label}
      </span>
      <span>
        <span
          className="data-value__value"
          style={valueColor ? { color: valueColor } : undefined}
        >
          {value}
        </span>
        {unit && (
          <span
            className="data-value__unit"
            style={valueColor ? { color: valueColor } : undefined}
          >
            {unit}
          </span>
        )}
      </span>
      {barPercent != null && (
        <div className="data-value__bar">
          <div
            className="data-value__bar-fill"
            style={{
              width: `${Math.min(100, Math.max(0, barPercent))}%`,
              background: barColor || Colors.productionGreen
            }}
          />
        </div>
      )}
      {subValue && (
        <span className="data-value__subvalue">
          {subValue}
        </span>
      )}
    </div>
  );
}
