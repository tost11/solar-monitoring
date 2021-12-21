import { useContext } from "react";
import { UserContext } from "../UserContext";

export function doRequest<T>(path:string,method:string):(body:any)=>Promise<T>{
  const login = useContext(UserContext);
  return async function(body:any): Promise<T> {
    let header;
    if (login !== null) {
      header = {
        'Content-Type': 'application/json',
        'Authorization': ('Bearer ' + login.jwt)
      }
    }else{
      header = {
        'Content-Type': 'application/json'
      }
    }

    let resp = await fetch(path,
        {
          method: method,
          body: JSON.stringify(body),
          headers: header
        }
    );
    let res = errorHandler(resp);
    return await res.json();
  }
}

function errorHandler(response: Response): Response {
  if (!response.ok) {
    throw response;
  } else {
    return response
  }
}
