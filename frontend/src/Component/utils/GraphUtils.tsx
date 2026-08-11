
export interface FormattedValue {
  value: string
  unit: string
}

export function formatDefaultValueWithUnitSplit(
  value: number,
  unit?: string,
  digits?: number,
  fixedDigits?: boolean
): FormattedValue {
  if (!unit) {
    return { value: "" + value, unit: "" };
  }

  let fak = value < 0 ? -1 : 1;
  value = value * fak;

  let un = unit;

  if (value > 1000) {
    value = value / 1000;
    un = "k" + unit;
    if (fixedDigits !== true && digits !== undefined && digits !== null) {
      digits += 3;
    }
  }
  if (value > 1000) {
    value = value / 1000;
    un = "M" + unit;
    if (fixedDigits !== true && digits !== undefined && digits !== null) {
      digits += 3;
    }
  }
  if (value > 1000) {
    value = value / 1000;
    un = "G" + unit;
    if (fixedDigits !== true && digits !== undefined && digits !== null) {
      digits += 3;
    }
  }
  if (value > 1000) {
    value = value / 1000;
    un = "T" + unit;
    if (fixedDigits !== true && digits !== undefined && digits !== null) {
      digits += 3;
    }
  }

  value = value * fak;

  const formatted = value.toLocaleString('de-DE', {
    maximumFractionDigits: digits != undefined ? digits : 2,
    useGrouping: false
  });

  return { value: formatted, unit: un };
}

export function formatDefaultValueWithUnit(
  value: number,
  unit?: string,
  digits?: number,
  fixedDigits?: boolean
): string {
  const result = formatDefaultValueWithUnitSplit(value, unit, digits, fixedDigits);
  return result.value + result.unit;
}

const graphColours =["blue","green","red","purple","darkorange","brown","magenta","darkblue","darkgreen","darkred","steelblue","darkcyan","coral","Indigo","Maroon","MediumSpringGreen","Olive","Teal"]

export function getGraphColourByIndex(index:number):string{
  if(index < graphColours.length){
    return graphColours[index];
  }
  return graphColours[0];
}
