import React, {useState} from "react";
import {
  CreatedAccessTokenResponseDTO,
  TokenPurpose,
  createSystemToken
} from "../api/SolarSystemAPI";
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  TextField,
  Typography
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import {useTranslation} from "react-i18next";
import {toast} from "react-toastify";

interface TokenSelectionModalProps {
  systemId: string;
  open: boolean;
  onDone: () => void;
}

export default function TokenSelectionModal({systemId, open, onDone}: TokenSelectionModalProps) {
  const {t} = useTranslation();

  const [loading, setLoading] = useState(false);
  const [createdToken, setCreatedToken] = useState<CreatedAccessTokenResponseDTO | null>(null);

  const handleCreate = (purpose: TokenPurpose) => {
    setLoading(true);
    createSystemToken(systemId, {
      name: "Default",
      purpose: purpose,
      expiresAt: undefined
    }).then((res) => {
      setCreatedToken(res);
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleCopyToken = () => {
    if (createdToken?.token) {
      navigator.clipboard.writeText(createdToken.token).then(() => {
        toast.success(t("views.edit_system.tokens_copied_clipboard"));
      });
    }
  };

  const handleDismiss = () => {
    setCreatedToken(null);
    onDone();
  };

  // If a token was just created, show the Token Created dialog
  if (createdToken) {
    return (
      <Dialog open={open} disableEscapeKeyDown maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.tokens_created_title")}</DialogTitle>
        <DialogContent>
          <Typography color="warning.main" sx={{marginBottom: "16px"}}>
            {t("views.edit_system.tokens_created_message")}
          </Typography>
          <TextField
            value={createdToken.token || ""}
            fullWidth
            multiline
            slotProps={{
              input: {
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={handleCopyToken} edge="end">
                      <ContentCopyIcon/>
                    </IconButton>
                  </InputAdornment>
                )
              }
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDismiss} variant="contained">
            {t("views.edit_system.tokens_copied")}
          </Button>
        </DialogActions>
      </Dialog>
    );
  }

  // Otherwise, show the token selection options
  return (
    <Dialog open={open} disableEscapeKeyDown maxWidth="sm" fullWidth>
      <DialogTitle>{t("views.create_system.token_selection_title")}</DialogTitle>
      <DialogContent>
        <Typography sx={{marginBottom: "16px"}}>
          {t("views.create_system.token_selection_message")}
        </Typography>
        <div style={{display: "flex", flexDirection: "column", gap: "12px"}}>
          <Button
            variant="outlined"
            onClick={handleDismiss}
            disabled={loading}
            fullWidth
          >
            {t("views.create_system.token_selection_skip")}
          </Button>
          <Button
            variant="contained"
            onClick={() => handleCreate(TokenPurpose.DATA_PUSH_REST)}
            disabled={loading}
            fullWidth
          >
            <div style={{display: "flex", flexDirection: "column", alignItems: "center"}}>
              <span>{t("views.create_system.token_selection_rest")}</span>
              <Typography variant="caption" color="inherit">
                {t("views.create_system.token_selection_rest_desc")}
              </Typography>
            </div>
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => handleCreate(TokenPurpose.DATA_PUSH_ENCRYPTED)}
            disabled={loading}
            fullWidth
          >
            <div style={{display: "flex", flexDirection: "column", alignItems: "center"}}>
              <span>{t("views.create_system.token_selection_encrypted")}</span>
              <Typography variant="caption" color="inherit">
                {t("views.create_system.token_selection_encrypted_desc")}
              </Typography>
            </div>
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
