import React, {useEffect, useState} from "react";

export default function StartPage(){
  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>Hallo and Welcome to your Manage Page for your Solar systems</h1>
        <img className={"Start Image"} src={require("../../img/energy-1322810_1920.jpg")} height={200} width={300} />
  </div>
}
