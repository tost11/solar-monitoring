import React from 'react';

interface ProductionSparklineProps {
  productionCurve: (number | null)[] | null;
  width?: number;
  height?: number;
}

export default function ProductionSparkline({productionCurve, width = 150, height = 40}: ProductionSparklineProps) {
  if (!productionCurve || productionCurve.length === 0) {
    return null;
  }

  const values = productionCurve;
  const hasData = values.some(v => v !== null && v !== undefined);
  if (!hasData) {
    return null;
  }

  const max = Math.max(...values.filter(v => v !== null && v !== undefined) as number[]);
  if (max <= 0) {
    return null;
  }

  const padding = 2;
  const chartHeight = height - padding * 2;
  const step = width / (values.length - 1);

  const segments: string[] = [];
  let currentSegment: string[] = [];

  for (let i = 0; i < values.length; i++) {
    const v = values[i];
    if (v === null || v === undefined) {
      if (currentSegment.length > 1) {
        segments.push(currentSegment.join(' '));
      }
      currentSegment = [];
    } else {
      const x = i * step;
      const y = padding + chartHeight - (v / max) * chartHeight;
      if (currentSegment.length === 0) {
        currentSegment.push(`M${x.toFixed(1)},${y.toFixed(1)}`);
      } else {
        currentSegment.push(`L${x.toFixed(1)},${y.toFixed(1)}`);
      }
    }
  }
  if (currentSegment.length > 1) {
    segments.push(currentSegment.join(' '));
  }

  if (segments.length === 0) {
    return null;
  }

  const linePath = segments.join(' ');

  let areaPath = '';
  for (const seg of segments) {
    const commands = seg.split(' ');
    const first = commands[0].replace('M', '');
    const last = commands[commands.length - 1].replace('L', '');
    const [firstX] = first.split(',');
    const [lastX] = last.split(',');
    areaPath += `M${firstX},${height - padding} ${seg.replace('M', 'L')} L${lastX},${height - padding} Z `;
  }

  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={{display:'block',maxWidth:'100%',height:'auto'}} width={width} height={height}>
      <rect x={0} y={0} width={width} height={height} fill="#e8e8e8" rx={2} />
      <path d={areaPath} fill="#4caf50" fillOpacity={0.3} stroke="none" />
      <path d={linePath} fill="none" stroke="#4caf50" strokeWidth={1.5} />
    </svg>
  );
}
