import React, { useMemo } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatDefaultValueWithUnit } from "./utils/GraphUtils";

interface CombinedProductionGraphProps {
  curve: (number | null)[];
}

interface DataPoint {
  time: string;
  watts: number | null;
}

export default function CombinedProductionGraph({ curve }: CombinedProductionGraphProps) {
  const data: DataPoint[] = useMemo(() => {
    return curve.map((value, i) => ({
      time: `${String(Math.floor(i / 4)).padStart(2, "0")}:${String((i % 4) * 15).padStart(2, "0")}`,
      watts: value,
    }));
  }, [curve]);

  const tickValues = useMemo(
    () => [0, 24, 48, 72, 95].map((i) => data[i]?.time ?? ""),
    [data]
  );

  return (
    <ResponsiveContainer width="100%" height={180}>
      <AreaChart data={data} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
        <defs>
          <linearGradient id="productionGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#4caf50" stopOpacity={0.4} />
            <stop offset="100%" stopColor="#4caf50" stopOpacity={0.05} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
        <XAxis
          dataKey="time"
          ticks={tickValues}
          tick={{ fontSize: 11 }}
          tickFormatter={(v) => v}
          interval="preserveStartEnd"
        />
        <YAxis
          tick={{ fontSize: 11 }}
          tickFormatter={(v) => formatDefaultValueWithUnit(v, "W")}
          width={55}
        />
        <Tooltip
          formatter={(value: number) => [formatDefaultValueWithUnit(value, "W"), "Production"]}
          labelFormatter={(label) => `Time: ${label}`}
        />
        <Area
          type="monotone"
          dataKey="watts"
          stroke="#4caf50"
          strokeWidth={2}
          fill="url(#productionGradient)"
          connectNulls={false}
          dot={false}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
