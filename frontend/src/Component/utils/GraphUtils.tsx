
export function formatDefaultValueWithUnit(value:number, unit?:string, digits?:number, fixedDigits?:boolean):string {

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
    if(fixedDigits !== true && digits !== undefined && digits !== null){
      digits += 3;
    }
  }
  if (value > 1000) {
    value = value / 1000
    un = "M" + unit
    if(fixedDigits !== true && digits !== undefined && digits !== null){
      digits += 3;
    }
  }

  value = value * fak

  res = "" + value.toLocaleString('de-DE', {
    maximumFractionDigits: digits != undefined ? digits : 2,
    useGrouping: false
  })
  res += un ? un : ""
  return res
}

const graphColours =["blue","green","red","purple","darkorange","brown","magenta","darkblue","darkgreen","darkred","steelblue","darkcyan","coral","Indigo","Maroon","MediumSpringGreen","Olive","Teal"]

export function getGraphColourByIndex(index:number):string{
  if(index < graphColours.length){
    return graphColours[index];
  }
  return graphColours[0];
}


/*export const addUtcOffsetToTime = (date:Moment,timezone:string,add:boolean,)=>{
  var utcOffset = moment().tz(timezone).utcOffset();
  //utcOffset -= moment(date).utcOffset();
  if(add) {
    return date.add(utcOffset, "minutes")
  }else{
    return date.subtract(utcOffset, "minutes")
  }
}*/
