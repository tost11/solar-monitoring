import {Button, FormControl, InputLabel, MenuItem, Select} from "@mui/material";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import React, {useState} from "react";
import {useTranslation} from "react-i18next";

interface GraphFilterListProps{
  filters: string[],
  onAdd: (filter:string)=>void,
  onDelete: (filter:string)=>void,
  loading: boolean,
  availableFilters: string[]
}

export default function GraphFilterList({filters, onAdd, onDelete, loading, availableFilters}:GraphFilterListProps) {

  const { t } = useTranslation()
  const [selectedFilter, setSelectedFilter] = useState<string>("")

  const unusedFilters = availableFilters.filter(f => !filters.includes(f));

  return <div>
    <h4>{t("views.create_system.graph_filters_existing")}</h4>
    {filters.length > 0 && <div style={{display: "flex", flexDirection: "row", flexWrap: "wrap", gap: "8px", marginTop: "10px"}}>
      {filters.map((filter,i)=><div key={i} style={{
        display: "flex",
        alignItems: "center",
        backgroundColor: "#e3f2fd",
        borderRadius: "16px",
        padding: "4px 8px 4px 12px",
        border: "1px solid #90caf9"
      }}>
        <span style={{marginRight: "4px"}}>{t("graph_filter." + filter)}</span>
        <IconButton size="small" disabled={loading} onClick={()=>onDelete(filter)} style={{padding: "4px"}}>
          <DeleteIcon fontSize="small"/>
        </IconButton>
      </div>)}
    </div>
    }

    <h4>{t("views.create_system.graph_filters_add")}</h4>
    <div className="defaultFlex" style={{alignItems: "center"}}>
      <FormControl style={{minWidth: "250px"}}>
        <InputLabel>{t("views.create_system.graph_filters_filter_name")}</InputLabel>
        <Select
          value={selectedFilter}
          label={t("views.create_system.graph_filters_filter_name")}
          onChange={(e) => setSelectedFilter(e.target.value)}
          disabled={loading || unusedFilters.length === 0}
        >
          {unusedFilters.map((filter) => (
            <MenuItem key={filter} value={filter}>
              {t("graph_filter." + filter)}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <Button
        disabled={!selectedFilter || loading}
        variant="contained"
        onClick={() => {
          onAdd(selectedFilter);
          setSelectedFilter("");
        }}
      >
        {t("views.create_system.graph_filters_add_button")}
      </Button>
    </div>
  </div>
}
