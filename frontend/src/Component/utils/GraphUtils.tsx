
export function formatDefaultValueWithUnit(value:number,unit?:string):string {

  if(!unit){
    return ""+value;
  }

  var res = "";
  var un = unit;
  if (value > 1000) {
    value = value / 1000;
      un = "K" + unit
  }
  if (value > 1000) {
    value = value / 1000;
    un = "M" + unit
  }
  res = "" + value.toLocaleString('de-DE', {
    maximumFractionDigits: 2,
    useGrouping: false
  })
  res += un ? un : ""
  return res
}