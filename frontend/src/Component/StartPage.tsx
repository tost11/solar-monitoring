import React, {useContext} from "react";
import {UserContext} from "../context/UserContext";

export default function StartPage(){
  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>Hello and Welcome to your Solar-Monitoring</h1>
        <img className={"Start Image"} src={require("../../img/energy-1322810_1920.jpg")} height={200} width={300} />
  </div>
}
