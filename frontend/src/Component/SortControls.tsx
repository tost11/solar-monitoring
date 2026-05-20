import React from "react";
import { Box, FormControl, InputLabel, MenuItem, Select } from "@mui/material";
import type { SelectChangeEvent } from "@mui/material/Select";
import { useTranslation } from "react-i18next";

interface SortControlsProps {
  sortBy: string;
  sortOrder: string;
  onSortChange: (sortBy: string, sortOrder: string) => void;
}

export default function SortControls({
  sortBy,
  sortOrder,
  onSortChange,
}: SortControlsProps) {
  const { t } = useTranslation();
  const handleSortByChange = (event: SelectChangeEvent<string>) => {
    onSortChange(event.target.value, sortOrder);
  };

  const handleSortOrderChange = (event: SelectChangeEvent<string>) => {
    onSortChange(sortBy, event.target.value);
  };

  return (
    <Box display="flex" gap={2} padding="1rem" flexWrap="wrap">
      <FormControl size="small" style={{ minWidth: 200 }}>
        <InputLabel>{t("components.sort_controls.sort_by")}</InputLabel>
        <Select value={sortBy} label={t("components.sort_controls.sort_by")} onChange={handleSortByChange}>
          <MenuItem value="name">{t("components.sort_controls.options.name")}</MenuItem>
          <MenuItem value="dayproduction">{t("components.sort_controls.options.dayproduction")}</MenuItem>
          <MenuItem value="dayconsumption">{t("components.sort_controls.options.dayconsumption")}</MenuItem>
          <MenuItem value="currentproduction">{t("components.sort_controls.options.currentproduction")}</MenuItem>
          <MenuItem value="currentconsumption">{t("components.sort_controls.options.currentconsumption")}</MenuItem>
          <MenuItem value="currentgrid">{t("components.sort_controls.options.currentgrid")}</MenuItem>
          <MenuItem value="efficiency">{t("components.sort_controls.options.efficiency")}</MenuItem>
          <MenuItem value="online">{t("components.sort_controls.options.online")}</MenuItem>
        </Select>
      </FormControl>

      {sortBy && (
        <FormControl size="small" style={{ minWidth: 120 }}>
          <InputLabel>{t("components.sort_controls.order")}</InputLabel>
          <Select value={sortOrder} label={t("components.sort_controls.order")} onChange={handleSortOrderChange}>
            <MenuItem value="asc">{t("components.sort_controls.ascending")}</MenuItem>
            <MenuItem value="desc">{t("components.sort_controls.descending")}</MenuItem>
          </Select>
        </FormControl>
      )}
    </Box>
  );
}
