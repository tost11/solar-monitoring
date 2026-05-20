import React from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle, Button, Typography, Chip} from '@mui/material';
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";
import {SolarSystemType} from "../../api/SolarSystemAPI";
import {TagDTO} from "../../api/UserAPIFunctions";
import moment from "moment";

interface SystemSpecsModalProps {
  open: boolean;
  onClose: () => void;
  maxInstalledSolarPower?: number;
  maxInverterOutputPower?: number;
  tags?: TagDTO[];
  buildingDate?: moment.Moment;
  creationDate?: moment.Moment;
  systemType?: SolarSystemType;
}

export default function SystemSpecsModal({
  open,
  onClose,
  maxInstalledSolarPower,
  maxInverterOutputPower,
  tags,
  buildingDate,
  creationDate,
  systemType
}: SystemSpecsModalProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const getSystemTypeTranslationKey = (type: SolarSystemType): string => {
    const typeMap: Record<SolarSystemType, string> = {
      [SolarSystemType.SELFMADE]: 'components.solarsystem.types.selfmade',
      [SolarSystemType.SIMPLE]: 'components.solarsystem.types.simple',
      [SolarSystemType.VERY_SIMPLE]: 'components.solarsystem.types.very-simple',
      [SolarSystemType.GRID]: 'components.solarsystem.types.grid',
      [SolarSystemType.GRID_BATTERY]: 'components.solarsystem.types.grid_battery',
    };
    return typeMap[type] || 'components.solarsystem.types.simple';
  };

  const calculateAge = (creationDate: moment.Moment): string => {
    const now = moment();
    const days = now.diff(creationDate, 'days');

    if (days < 365) {
      return `${days} ${t('components.system_specs_modal.days')}`;
    } else {
      const years = days / 365;
      return `${years.toFixed(1)} ${t('components.system_specs_modal.years')}`;
    }
  };

  const handleTagClick = (tagId: string) => {
    navigate(`/tag/${tagId}`);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {t("components.system_specs_modal.title")}
      </DialogTitle>
      <DialogContent>
        {systemType && (
          <Typography paragraph>
            <strong>{t("components.system_specs_modal.system_type")}:</strong> {t(getSystemTypeTranslationKey(systemType))}
          </Typography>
        )}
        {buildingDate && (
          <Typography paragraph>
            <strong>{t("components.system_specs_modal.building_date")}:</strong> {buildingDate.format('YYYY-MM-DD')}
          </Typography>
        )}
        {creationDate && (
          <Typography paragraph>
            <strong>{t("components.system_specs_modal.system_age")}:</strong> {calculateAge(creationDate)}
          </Typography>
        )}
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
        {tags && tags.length > 0 && (
          <div style={{ marginTop: '1rem' }}>
            <Typography variant="body2" color="textSecondary" style={{ marginBottom: '0.5rem' }}>
              <strong>{t("components.system_specs_modal.tags")}:</strong>
            </Typography>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
              {tags.map((tag) => (
                <Chip
                  key={tag.id}
                  label={tag.name}
                  onClick={() => handleTagClick(tag.id)}
                  style={{ backgroundColor: tag.color, color: 'white', cursor: 'pointer' }}
                  size="small"
                />
              ))}
            </div>
          </div>
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
