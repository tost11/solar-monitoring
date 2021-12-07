import { useContext } from "react";
import { UserContext } from "../UserContext";

export async function getRequest(path:string):Promise<Response>{
  console.log(localStorage.getItem("jwt"))
 let resp= await fetch(path, {
      method: 'GET',
      headers:{
        'Content-Type': 'application/json',
        "Authorization": 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FhcyIsImV4cCI6MzY1ODkzODk5NDM5OTM0NSwiaWF0IjoxNjM4ODY1ODY3fQ.6UeCoLQA1XdiyycCpuIUYYKzxFWi-ToYctkjJc7_e6o'
      }
    }
  )
  return errorHandler(resp)

}
export async function postRequest(path:string,body:any):Promise<Response>{
  const login = await useContext(UserContext);
  let header= {  'Content-Type': 'application/json',
    'Authorization' : 'Bearer '+ "",}
  if(login!==null){
    header= {
      'Content-Type': 'application/json',
      'Authorization' : 'Bearer '+ login.jwt,
    }
  }
  let resp = await fetch(path,
  {method:"Post",
    body:body,
    headers:header}



)
  return errorHandler(resp)
}
export function delRequest(){

}

function errorHandler(response:Response):Response{
  if (!response.ok ) {
      throw new Error("Request fail "+ response.status)
  } else {
    return response
  }
}
