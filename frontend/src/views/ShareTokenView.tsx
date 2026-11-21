import React, {useEffect, useRef, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {resolveShareLink, validateViewToken} from "../api/SolarSystemAPI";
import {findViewTokenBySecret, setViewToken} from "../api/viewTokens";
import {useTranslation} from "react-i18next";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CircularProgress from "@mui/material/CircularProgress";
import Typography from "@mui/material/Typography";

type Status = "loading" | "success" | "error";

export default function ShareTokenView() {
  const {t} = useTranslation();
  const {systemId, secret} = useParams();
  const navigate = useNavigate();

  const [status, setStatus] = useState<Status>("loading");
  const [resolvedSystemId, setResolvedSystemId] = useState<string | null>(null);
  const attempted = useRef(false);

  useEffect(() => {
    if (!systemId || !secret || attempted.current) return;
    attempted.current = true;

    const activate = async () => {
      // If this device already activated this exact link and still holds its token,
      // just validate it instead of re-resolving (so the link's usage count is not reduced).
      const existing = findViewTokenBySecret(systemId, secret);
      if (existing) {
        try {
          const v = await validateViewToken(systemId, existing.password);
          if (v.valid) {
            setResolvedSystemId(systemId);
            setStatus("success");
            return;
          }
        } catch (_ex) {
          // fall through to resolve below
        }
      }

      try {
        const res = await resolveShareLink(systemId, secret);
        if (res.valid && res.password) {
          const id = res.systemId || systemId;
          setViewToken(id, res.password, res.expiresAt, secret);
          setResolvedSystemId(id);
          setStatus("success");
        } else {
          setStatus("error");
        }
      } catch (_ex) {
        setStatus("error");
      }
    };

    activate();
  }, [systemId, secret]);

  return (
    <div style={{display: "flex", justifyContent: "center", alignItems: "center", minHeight: "70vh"}}>
      <Card sx={{maxWidth: 480, width: "100%"}}>
        <CardContent>
          {status === "loading" && (
            <div style={{display: "flex", flexDirection: "column", alignItems: "center", gap: "16px", py: "16px"}}>
              <Typography variant="h5" align="center" sx={{mb: 0}}>
                {t("views.share_link.title")}
              </Typography>
              <CircularProgress/>
              <Typography variant="body1" color="textSecondary" align="center">
                {t("views.share_link.checking")}
              </Typography>
            </div>
          )}

          {status === "success" && (
            <div style={{display: "flex", flexDirection: "column", alignItems: "center", gap: "20px", py: "16px"}}>
              <Typography variant="h5" align="center" sx={{mb: 0}}>
                {t("views.share_link.title")}
              </Typography>
              <Typography color="success.main" align="center">
                {t("views.share_link.success")}
              </Typography>
              <div style={{display: "flex", flexDirection: "column", gap: "12px", width: "100%"}}>
                <Button
                  variant="contained"
                  size="large"
                  fullWidth
                  onClick={() => resolvedSystemId && navigate("/detailDashboard/" + resolvedSystemId)}
                >
                  {t("views.share_link.go_system")}
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  fullWidth
                  onClick={() => resolvedSystemId && navigate("/view-tokens?highlight=" + encodeURIComponent(resolvedSystemId))}
                >
                  {t("views.share_link.go_tokens")}
                </Button>
              </div>
            </div>
          )}

          {status === "error" && (
            <div style={{display: "flex", flexDirection: "column", alignItems: "center", gap: "20px", py: "16px"}}>
              <Typography variant="h5" align="center" sx={{mb: 0}}>
                {t("views.share_link.error_title")}
              </Typography>
              <Typography color="error.main" align="center">
                {t("views.share_link.error")}
              </Typography>
              <Button variant="outlined" size="large" fullWidth onClick={() => navigate("/")}>
                {t("views.share_link.continue_home")}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
