
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

const graphColours =["blue","green","red","purple","darkorange","brown","magenta","darkblue","darkgreen","darkred","lightpurple","darkcyan","lightbrown","Indigo","Maroon","MediumSpringGreen","Olive","Teal"]

export function getGraphColourByIndex(index:number):string{
  if(index < graphColours.length){
    return graphColours[index];
  }
  return graphColours[0];
}
