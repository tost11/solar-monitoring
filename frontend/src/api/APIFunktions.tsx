import {useContext} from "react";
import {UserContext} from "../UserContext";

export function doRequest<T>(path: string, method: string): (body?: any) => Promise<T> {
  const login = useContext(UserContext);
  return async function (body: any): Promise<T> {
    let header;
    let init;
    if (login !== null) {
      header = {
        'Content-Type': 'application/json',
        'Authorization': ('Bearer ' + login.jwt)
      }
    } else {
      header = {
        'Content-Type': 'application/json'
      }
    }
    if (method.toLocaleUpperCase() !== "GET") {
      init = {
        method: method,
        body: JSON.stringify(body),
        headers: header
      }
    } else {
      init = {
        method: method,
        headers: header
      }
    }
    let resp = await fetch(path,init);
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
