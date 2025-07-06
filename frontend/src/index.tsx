import React from "react";
import ReactDOM from "react-dom";
import App from "./App";
import { Suspense } from 'react';

const rootElement = document.getElementById("root");

// import i18n (needs to be bundled ;))
import './i18n';

ReactDOM.render(
  <React.StrictMode>
    <Suspense fallback="...is loading">
      <App/>
    </Suspense>
  </React.StrictMode>,
  rootElement
);
