import {toast} from "react-toastify";
import {viewTokenForPath} from "./viewTokens";

function buildHeader(path: string) {
  const header: Record<string, string> = {
    'Content-Type': 'application/json'
  };
  const viewToken = viewTokenForPath(path);
  if (viewToken) {
    header['X-View-Token'] = viewToken;
  }
  return header;
}

export async function doRequest<T>(path: string, method: string,body?: any):Promise<T> {
    let header;
    let init;
    header = buildHeader(path)
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
    if (!resp.ok) {
      try{
        let data = await resp.json();
        toast.error(data.error)
      }catch (_ex){
        toast.error('Error on fetching data')
      }
      throw resp;
    }
    return await resp.json();
}


export async function doRequestNoBody(path: string, method: string,body?: any) {
  let header;
  let init;
  header = buildHeader(path)
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
  if (!resp.ok) {
    try{
      let data = await resp.json();
      toast.error(data.error)
    }catch (_ex){
      toast.error('Error on fetching data')
    }
    throw resp;
  }
}

