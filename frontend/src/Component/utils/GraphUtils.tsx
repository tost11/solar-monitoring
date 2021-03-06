
export function formatDefaultValueWithUnit(value:number,unit?:string):string {

  if(!unit){
    return ""+value
  }

  let fak = value < 0 ? -1 : 1
  value = value * fak

  let res = "";
  let un = unit
  if (value > 1000) {
    value = value / 1000
      un = "K" + unit
  }
  if (value > 1000) {
    value = value / 1000
    un = "M" + unit
  }

  value = value * fak

  res = "" + value.toLocaleString('de-DE', {
    maximumFractionDigits: 2,
    useGrouping: false
  })
  res += un ? un : ""
  return res
}

const graphColours =["#8884d8","#ec0f0f","#68e522","#1259d5","#800080","#ff4000","#ff4000"]

export function getGraphColourByIndex(index:number):string{
  if(index < graphColours.length){
    return graphColours[index];
  }
  return graphColours[0];
}
