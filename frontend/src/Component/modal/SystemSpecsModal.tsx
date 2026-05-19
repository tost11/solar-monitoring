import React from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle, Button, Typography} from '@mui/material';
import {useTranslation} from "react-i18next";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";

interface SystemSpecsModalProps {
  open: boolean;
  onClose: () => void;
  maxInstalledSolarPower?: number;
  maxInverterOutputPower?: number;
}

export default function SystemSpecsModal({open, onClose, maxInstalledSolarPower, maxInverterOutputPower}: SystemSpecsModalProps) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {t("components.system_specs_modal.title")}
      </DialogTitle>
      <DialogContent>
        {maxInstalledSolarPower && (
          <Typography paragraph>
            <strong>{t("components.system_specs_modal.max_installed_solar_power")}:</strong> {formatDefaultValueWithUnit(maxInstalledSolarPower, "W", 0, true)}
          </Typography>
        )}
        {maxInverterOutputPower && (
          <Typography paragraph>
            <strong>{t("components.system_specs_modal.max_inverter_output_power")}:</strong> {formatDefaultValueWithUnit(maxInverterOutputPower, "W", 0, true)}
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          {t("common.close")}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
