import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { Suspense } from 'react';

const rootElement = document.getElementById("root");

// import i18n (needs to be bundled ;))
import './i18n';

const root = createRoot(rootElement!);
root.render(
  <React.StrictMode>
    <Suspense fallback="...is loading">
      <App/>
    </Suspense>
  </React.StrictMode>
);
