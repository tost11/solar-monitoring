import React, {useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {
  clearViewTokens,
  getViewTokens,
  patchViewToken,
  removeViewToken,
  ViewTokenEntry
} from "../api/viewTokens";
import {validateViewToken, ViewTokenValidateResponse} from "../api/SolarSystemAPI";
import {useTranslation} from "react-i18next";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import DeleteIcon from "@mui/icons-material/Delete";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import CheckIcon from "@mui/icons-material/Check";
import CancelIcon from "@mui/icons-material/Cancel";
import moment from "moment";

export default function ViewTokensView() {
  const {t} = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const highlight = searchParams.get("highlight");

  const [entries, setEntries] = useState<string[]>(() => Object.keys(getViewTokens()));
  const [addSystemId, setAddSystemId] = useState("");
  const [addPassword, setAddPassword] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const [validatingAll, setValidatingAll] = useState(false);

  const refresh = () => setEntries(Object.keys(getViewTokens()));
  const entry = (systemId: string): ViewTokenEntry => getViewTokens()[systemId];

  const applyValidation = (systemId: string, res: ViewTokenValidateResponse) => {
    const patch: Partial<ViewTokenEntry> = {valid: res.valid};
    if (res.name) patch.name = res.name;
    patchViewToken(systemId, patch);
  };

  const handleValidate = async (systemId: string) => {
    const e = entry(systemId);
    if (!e || busy || validatingAll) return;
    setBusy(systemId);
    try {
      applyValidation(systemId, await validateViewToken(systemId, e.password));
    } catch (_ex) {
      patchViewToken(systemId, {valid: false});
    } finally {
      refresh();
      setBusy(null);
    }
  };

  const handleValidateAll = async () => {
    if (validatingAll || busy) return;
    setValidatingAll(true);
    const ids = Object.keys(getViewTokens());
    for (const id of ids) {
      const e = entry(id);
      if (!e) continue;
      try {
        applyValidation(id, await validateViewToken(id, e.password));
      } catch (_ex) {
        patchViewToken(id, {valid: false});
      }
      refresh();
    }
    setValidatingAll(false);
  };

  const handleRemove = (systemId: string) => {
    removeViewToken(systemId);
    refresh();
  };

  const handleClearAll = () => {
    clearViewTokens();
    refresh();
  };

  const handleAdd = () => {
    if (!addSystemId.trim() || !addPassword) return;
    navigate("/share/" + encodeURIComponent(addSystemId.trim()) + "/" + encodeURIComponent(addPassword));
  };

  const statusCell = (e: ViewTokenEntry) => {
    if (e.valid === true) {
      return <Chip icon={<CheckIcon/>} label={t("views.view_tokens.status_valid")} color="success" size="small"/>;
    }
    if (e.valid === false) {
      return <Chip icon={<CancelIcon/>} label={t("views.view_tokens.status_invalid")} color="error" size="small"/>;
    }
    return <Typography variant="body2" color="textSecondary">{t("views.view_tokens.status_unknown")}</Typography>;
  };

  return (
    <div style={{padding: "20px", maxWidth: "1000px", margin: "0 auto"}}>
      <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap"}}>
        <div>
          <h2 style={{margin: 0}}>{t("views.view_tokens.title")}</h2>
          <Typography color="textSecondary" sx={{mb: 0}}>
            {t("views.view_tokens.text")}
          </Typography>
        </div>
        <Button
          variant="contained"
          onClick={handleValidateAll}
          disabled={entries.length === 0 || validatingAll || !!busy}
          startIcon={validatingAll ? <CircularProgress size={18}/> : undefined}
        >
          {t("views.view_tokens.validate_all")}
        </Button>
      </div>

      <TableContainer component={Card} sx={{mt: 2}}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t("views.view_tokens.table_system")}</TableCell>
              <TableCell>{t("views.view_tokens.table_expires")}</TableCell>
              <TableCell>{t("views.view_tokens.table_status")}</TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {entries.map((systemId) => {
              const e = entry(systemId);
              const expired = e.expiresAt && new Date(e.expiresAt) < new Date();
              return (
                <TableRow key={systemId} sx={{backgroundColor: systemId === highlight ? "rgba(25,118,210,0.10)" : "transparent"}}>
                  <TableCell>
                    <Typography variant="body2">{e.name || systemId}</Typography>
                    {e.name && <Typography variant="caption" color="textSecondary">{systemId}</Typography>}
                  </TableCell>
                  <TableCell>
                    {e.expiresAt
                      ? expired
                        ? <Chip label={t("views.view_tokens.expired")} color="error" size="small"/>
                        : moment(e.expiresAt).format("YYYY-MM-DD HH:mm")
                      : t("views.view_tokens.never")}
                  </TableCell>
                  <TableCell>{statusCell(e)}</TableCell>
                  <TableCell>
                    <Button
                      variant="text"
                      size="small"
                      onClick={() => handleValidate(systemId)}
                      disabled={busy !== null || validatingAll}
                      startIcon={busy === systemId ? <CircularProgress size={14}/> : undefined}
                    >
                      {t("views.view_tokens.validate")}
                    </Button>
                    <IconButton onClick={() => navigate("/detailDashboard/" + systemId)} size="small"
                                title={t("views.view_tokens.open")}>
                      <OpenInNewIcon/>
                    </IconButton>
                    <IconButton onClick={() => handleRemove(systemId)} size="small"
                                title={t("views.view_tokens.remove")}>
                      <DeleteIcon/>
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
            {entries.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  <Typography variant="body2" color="textSecondary">
                    {t("views.view_tokens.empty")}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {entries.length > 0 && (
        <Button color="error" variant="text" onClick={handleClearAll} sx={{mt: 1}} disabled={validatingAll || !!busy}>
          {t("views.view_tokens.clear_all")}
        </Button>
      )}

      <Divider sx={{my: 3}}/>

      <Typography variant="h6">{t("views.view_tokens.add_title")}</Typography>
      <div style={{display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px", maxWidth: "440px"}}>
        <TextField
          label={t("views.view_tokens.add_system_id")}
          value={addSystemId}
          onChange={(e) => setAddSystemId(e.target.value)}
          fullWidth
        />
        <TextField
          label={t("views.view_tokens.add_password")}
          value={addPassword}
          onChange={(e) => setAddPassword(e.target.value)}
          type="password"
          fullWidth
        />
        <Button onClick={handleAdd} disabled={!addSystemId.trim() || !addPassword} variant="contained">
          {t("views.view_tokens.add_button")}
        </Button>
      </div>
    </div>
  );
}
