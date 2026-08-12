export const Colors = {
  productionGreen: '#2e7d32',
  consumptionRed: '#d32f2f',
  batteryOrange: '#f57c00',
  gridOrange: '#fb8c00',
  consumptionBlue: '#1e88e5',
  batteryPurple: '#8e24aa',
  black: 'black',
};

export function getGradientColor(
  percent: number,
  startHue: number,
  endHue: number,
  saturation: number = 70,
  lightness: number = 45
): string {
  const clamped = Math.max(0, Math.min(100, percent));
  const hue = startHue + (clamped / 100) * (endHue - startHue);
  return `hsl(${hue.toFixed(0)}, ${saturation}%, ${lightness}%)`;
}

export function getValueColorProduction(value: number, maxValue: number): string {
  if (Math.abs(value) < 10) return 'black';
  const percent = (value / maxValue) * 100;
  return getGradientColor(percent, 0, 120, 70, 45);
}

export function getValueColorConsumption(value: number, maxValue: number): string {
  if (Math.abs(value) < 10) return 'black';
  const percent = (value / maxValue) * 100;
  return getGradientColor(percent, 120, 0, 70, 45);
}

export function getValueColorSOC(value: number, max: number = 100): string {
  const percent = (value / max) * 100;
  return getGradientColor(percent, 0, 120, 70, 45);
}

export function getValueColorGrid(value: number): string {
  if (value > 10) return Colors.consumptionRed;
  if (value < -10) return Colors.productionGreen;
  return Colors.black;
}

export function getValueColorBatteryWatt(value: number): string {
  if (Math.abs(value) < 10) return Colors.black;
  if (value > 0) return Colors.productionGreen;
  return Colors.batteryOrange;
}
