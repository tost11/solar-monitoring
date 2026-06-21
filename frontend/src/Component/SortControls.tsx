import React from "react";
import { Box, FormControl, InputLabel, MenuItem, Select } from "@mui/material";
import type { SelectChangeEvent } from "@mui/material/Select";
import { useTranslation } from "react-i18next";

export interface SortField {
  value: string;
  label: string;
}

interface SortControlsProps {
  sortBy: string;
  sortOrder: string;
  onSortChange: (sortBy: string, sortOrder: string) => void;
  allowedFields?: SortField[];
}

export default function SortControls({
  sortBy,
  sortOrder,
  onSortChange,
  allowedFields,
}: SortControlsProps) {
  const { t } = useTranslation();

  const defaultFields: SortField[] = [
    { value: "name", label: t("components.sort_controls.options.name") },
    { value: "dayproduction", label: t("components.sort_controls.options.dayproduction") },
    { value: "dayconsumption", label: t("components.sort_controls.options.dayconsumption") },
    { value: "currentproduction", label: t("components.sort_controls.options.currentproduction") },
    { value: "currentconsumption", label: t("components.sort_controls.options.currentconsumption") },
    { value: "currentgrid", label: t("components.sort_controls.options.currentgrid") },
    { value: "efficiency", label: t("components.sort_controls.options.efficiency") },
    { value: "online", label: t("components.sort_controls.options.online") },
  ];

  const fields = allowedFields || defaultFields;

  const handleSortByChange = (event: SelectChangeEvent<string>) => {
    onSortChange(event.target.value, sortOrder);
  };

  const handleSortOrderChange = (event: SelectChangeEvent<string>) => {
    onSortChange(sortBy, event.target.value);
  };

  return (
    <Box sx={{ display: "flex", gap: 2, padding: "1rem", flexWrap: "wrap" }}>
      <FormControl size="small" style={{ minWidth: 200 }}>
        <InputLabel>{t("components.sort_controls.sort_by")}</InputLabel>
        <Select value={sortBy} label={t("components.sort_controls.sort_by")} onChange={handleSortByChange}>
          {fields.map((field) => (
            <MenuItem key={field.value} value={field.value}>
              {field.label}
            </MenuItem>
          ))}
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
