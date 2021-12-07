import React from "react";
import {createSystem,getSystem} from "./SolarSystemAPI";
import Button from '@mui/material/Button';
import { useState } from "react";
import SystemComponent from "./SystemComponent";

export default function App() {

  return <div>
    <SystemComponent/>

  </div>
}
