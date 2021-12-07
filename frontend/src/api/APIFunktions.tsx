export function getRequest(path:string,responseCallback:(resp:Response)=>void){
  console.log(localStorage.getItem("jwt"))
  fetch(path, {
      method: 'GET',
      headers:{
        'Content-Type': 'application/json',
        "Authorization": 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FhcyIsImV4cCI6MzY1ODkzODk5NDM5OTM0NSwiaWF0IjoxNjM4ODY1ODY3fQ.6UeCoLQA1XdiyycCpuIUYYKzxFWi-ToYctkjJc7_e6o'
      }
    }
  ).then((resp)=>errorHandler(resp,responseCallback))
}
export function postRequest(path:string,body:any,responseCallback:(resp:Response)=>void){
return fetch(path,
  {method:"GET",
    body:body,
    headers:{
      'Content-Type': 'application/json',
      "Authorization": 'Bearer ' + localStorage.getItem("jwt"),
  }
  }

).then((resp)=>errorHandler(resp,responseCallback))
}
export function delRequest(){

}

function errorHandler(response:Response,responseCallback:(resp:Response)=>void){
  if (!response.ok ) {
    response.json().then((responseBody:any) => {
      console.log(responseBody)
    })
  } else {
    responseCallback(response);
  }
}
