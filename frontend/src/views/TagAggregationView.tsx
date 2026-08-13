import React, { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  apiGetTagAggregation,
  SystemContributionDTO,
  TagAggregationDTO,
} from "../api/SolarSystemAPI";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import Typography from "@mui/material/Typography";
import { formatDefaultValueWithUnit } from "../Component/utils/GraphUtils";
import { Colors } from "../Component/utils/ColorUtils";
import RefreshStatusIndicator from "../Component/RefreshStatusIndicator";
import SortControls, { SortField } from "../Component/SortControls";
import { useTranslation } from "react-i18next";
import { usePaginationState } from "../hooks/usePaginationState";
import PaginatedList from "../Component/PaginatedList";

interface SystemContributionCardProps {
  system: SystemContributionDTO;
  onClick: () => void;
  totals: {
    totalDayProducedKWH: number;
    totalDayConsumedKWH: number;
    totalCurrentProduction: number;
    totalCurrentConsumption: number;
  };
  isGrayedOut?: boolean;
}

function SystemContributionCard({
  system,
  onClick,
  totals,
  isGrayedOut,
}: SystemContributionCardProps) {
  const { t } = useTranslation();

  return (
    <Card
      onClick={onClick}
      style={{
        cursor: "pointer",
        marginBottom: "1rem",
        opacity: isGrayedOut ? 0.5 : 1
      }}
    >
      <CardContent>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "5px",
            flexWrap: "wrap",
          }}
        >
          {/* System Name - Fixed Width */}
          <div style={{ minWidth: "200px", flex: "0 0 200px" }}>
            <Typography variant="h6" style={{ marginBottom: "0.25rem" }}>
              {system.name}
            </Typography>
            <Chip
              label={system.isOnline ? t("common.online") : t("common.offline")}
              color={system.isOnline ? (system.currentProduction > 0 ? "success" : "warning") : "error"}
              size="small"
            />
          </div>

          {/* Day Production */}
          <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
            <Typography variant="caption" color="textSecondary">
              {t("views.tag_aggregation.day_production")}
            </Typography>
            <Typography variant="body1" style={{ fontWeight: 500 }}>
              {formatDefaultValueWithUnit(system.dayProducedKWH * 1000, "Wh")}
            </Typography>
            <Typography
              variant="caption"
              style={{ color: "#1976d2", fontWeight: 500 }}
            >
              {totals.totalDayProducedKWH > 0
                ? ((system.dayProducedKWH / totals.totalDayProducedKWH) * 100).toFixed(1)
                : "0.0"}%
            </Typography>
          </div>

          {/* Day Consumption */}
          {system.dayConsumedKWH !== undefined &&
            system.dayConsumedKWH !== null && (
              <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
                <Typography variant="caption" color="textSecondary">
                  {t("views.tag_aggregation.day_consumption")}
                </Typography>
                <Typography variant="body1" style={{ fontWeight: 500 }}>
                  {formatDefaultValueWithUnit(system.dayConsumedKWH * 1000, "Wh")}
                </Typography>
                {totals.totalDayConsumedKWH > 0 && (
                  <Typography
                    variant="caption"
                    style={{ color: "#ff8c00", fontWeight: 500 }}
                  >
                    {((system.dayConsumedKWH! / totals.totalDayConsumedKWH) * 100).toFixed(1)}%
                  </Typography>
                )}
              </div>
            )}

          {/* Current Production */}
          <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
            <Typography variant="caption" color="textSecondary">
              {t("views.tag_aggregation.current_production")}
            </Typography>
            <Typography variant="body1" style={{ fontWeight: 500 }}>
              {formatDefaultValueWithUnit(system.currentProduction, "W")}
              {system.maxInstalledSolarPower && system.maxInstalledSolarPower > 0 && (
                <span style={{ color: Colors.productionGreen, marginLeft: "8px" }}>
                  ({((system.currentProduction / system.maxInstalledSolarPower) * 100).toFixed(1)}%)
                </span>
              )}
            </Typography>
            <Typography
              variant="caption"
              style={{ color: "#1976d2", fontWeight: 500 }}
            >
              {totals.totalCurrentProduction > 0
                ? ((system.currentProduction / totals.totalCurrentProduction) * 100).toFixed(1)
                : "0.0"}%
            </Typography>
          </div>

          {/* Current Consumption */}
          {system.currentConsumption !== undefined &&
            system.currentConsumption !== null && (
              <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
                <Typography variant="caption" color="textSecondary">
                  {t("views.tag_aggregation.current_consumption")}
                </Typography>
                <Typography variant="body1" style={{ fontWeight: 500 }}>
                  {formatDefaultValueWithUnit(system.currentConsumption, "W")}
                </Typography>
                {totals.totalCurrentConsumption > 0 && (
                  <Typography
                    variant="caption"
                    style={{ color: "#ff8c00", fontWeight: 500 }}
                  >
                    {((system.currentConsumption! / totals.totalCurrentConsumption) * 100).toFixed(1)}%
                  </Typography>
                )}
              </div>
            )}

          {/* Grid */}
          {system.currentGrid !== undefined && system.currentGrid !== null && (
            <div style={{ minWidth: "100px", flex: "0 0 100px" }}>
              <Typography variant="caption" color="textSecondary">
                {t("common.grid")}
              </Typography>
              <Typography
                variant="body1"
                style={{
                  fontWeight: 500,
                  color: system.currentGrid > 0 ? Colors.consumptionRed : Colors.productionGreen,
                }}
              >
                {formatDefaultValueWithUnit(Math.abs(system.currentGrid), "W")}
              </Typography>
              <Typography
                variant="caption"
                style={{
                  color: system.currentGrid > 0 ? Colors.consumptionRed : Colors.productionGreen,
                  fontWeight: 500,
                }}
              >
                {system.currentGrid > 0 ? t("views.tag_aggregation.consuming_arrow") : t("views.tag_aggregation.feeding_in_arrow")}
              </Typography>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export default function TagAggregationView() {
  const { t } = useTranslation();
  const { tagId } = useParams<{ tagId: string }>();
  const navigate = useNavigate();

  const [data, setData] = useState<TagAggregationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [listLoading, setListLoading] = useState(false);

  const [paginationState, paginationActions] = usePaginationState({
    defaultPage: 0,
    defaultSize: 15,
    defaultSortBy: "name",
    defaultSortOrder: "asc",
    allowedSizes: [5, 10, 15, 20, 25, 30],
    allowedSortFields: [
      "name",
      "dayproduction",
      "dayconsumption",
      "currentproduction",
      "currentconsumption",
      "currentgrid",
      "efficiency",
      "online",
    ],
  });

  const sortFields: SortField[] = [
    { value: "name", label: t("components.sort_controls.options.name") },
    { value: "dayproduction", label: t("components.sort_controls.options.dayproduction") },
    { value: "dayconsumption", label: t("components.sort_controls.options.dayconsumption") },
    { value: "currentproduction", label: t("components.sort_controls.options.currentproduction") },
    { value: "currentconsumption", label: t("components.sort_controls.options.currentconsumption") },
    { value: "currentgrid", label: t("components.sort_controls.options.currentgrid") },
    { value: "efficiency", label: t("components.sort_controls.options.efficiency") },
    { value: "online", label: t("components.sort_controls.options.online") },
  ];

  const refreshIndicatorKey = `${tagId}-${paginationState.page}-${paginationState.size}-${paginationState.sortBy}-${paginationState.sortOrder}`;

  const fetchTagData = useCallback(async (): Promise<boolean> => {
    if (!tagId) return false;

    try {
      const { page, size, sortBy, sortOrder } = paginationState;
      const result = await apiGetTagAggregation(tagId, page, size, sortBy, sortOrder);
      setData(result);
      paginationActions.setTotalElements(result.systems.totalElements);
      paginationActions.setTotalPages(result.systems.totalPages);
      return true;
    } catch {
      return false;
    }
  }, [tagId, paginationState, paginationActions]);

  useEffect(() => {
    if (tagId) {
      if (data === null) {
        setLoading(true);
      } else {
        setListLoading(true);
      }

      apiGetTagAggregation(
        tagId,
        paginationState.page,
        paginationState.size,
        paginationState.sortBy,
        paginationState.sortOrder
      )
        .then((result) => {
          setData(result);
          paginationActions.setTotalElements(result.systems.totalElements);
          paginationActions.setTotalPages(result.systems.totalPages);
        })
        .catch(() => {})
        .finally(() => {
          setLoading(false);
          setListLoading(false);
        });
    }
  }, [
    tagId,
    paginationState.page,
    paginationState.size,
    paginationState.sortBy,
    paginationState.sortOrder,
  ]);

  const hasValueForSort = (system: SystemContributionDTO, sortBy: string): boolean => {
    switch (sortBy.toLowerCase()) {
      case "dayproduction":
        return system.dayProducedKWH > 0;
      case "dayconsumption":
        return system.dayConsumedKWH !== undefined && system.dayConsumedKWH !== null && system.dayConsumedKWH > 0;
      case "currentproduction":
        return system.currentProduction > 0;
      case "currentconsumption":
        return system.currentConsumption !== undefined && system.currentConsumption !== null && system.currentConsumption > 0;
      case "currentgrid":
        return system.currentGrid !== undefined && system.currentGrid !== null && system.currentGrid !== 0;
      case "efficiency":
        return system.maxInstalledSolarPower !== undefined &&
               system.maxInstalledSolarPower !== null &&
               system.maxInstalledSolarPower > 0;
      case "name":
      case "online":
        return true;
      default:
        return true;
    }
  };

  if (loading) {
    return (
      <div
        className="defaultFlowColumn"
        style={{
          alignItems: "center",
          justifyContent: "center",
          minHeight: "50vh",
        }}
      >
        <CircularProgress />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="defaultFlowColumn">
        <Typography variant="h5">{t("views.tag_aggregation.not_found")}</Typography>
      </div>
    );
  }

  return (
    <div className="defaultFlowColumn" style={{ padding: "1rem" }}>
      <div
        style={{
          backgroundColor: data.tag.color,
          padding: "1rem",
          borderRadius: "8px",
          marginBottom: "1rem",
        }}
      >
        <Typography variant="h4" style={{ color: "white" }}>
          {data.tag.name}
        </Typography>
      </div>

      <RefreshStatusIndicator
        key={refreshIndicatorKey}
        fetchCallback={fetchTagData}
        normalInterval={300000}
        errorInterval={60000}
        skipInitialFetch={true}
      />

      <Card style={{ marginBottom: "1.5rem" }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                {t("views.tag_aggregation.systems_online")}
              </Typography>
              <Typography variant="h4">
                {data.onlineSystems} / {data.totalSystems}
              </Typography>
            </Grid>
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                {t("common.total")} {t("views.tag_aggregation.day_production")}
              </Typography>
              <Typography variant="h4">
                {formatDefaultValueWithUnit(data.totalDayProducedKWH * 1000, "Wh")}
              </Typography>
            </Grid>
            {data.totalDayConsumedKWH !== null &&
              data.totalDayConsumedKWH !== undefined && (
                <Grid xs={6} md={3}>
                  <Typography variant="caption" color="textSecondary">
                    {t("common.total")} {t("views.tag_aggregation.day_consumption")}
                  </Typography>
                  <Typography variant="h4">
                    {formatDefaultValueWithUnit(
                      data.totalDayConsumedKWH * 1000,
                      "Wh",
                    )}
                  </Typography>
                </Grid>
              )}
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                {t("views.tag_aggregation.current_production")}
              </Typography>
              <Typography variant="h4">
                {formatDefaultValueWithUnit(data.totalCurrentProduction, "W")}
              </Typography>
            </Grid>
            {data.totalCurrentConsumption !== null &&
              data.totalCurrentConsumption !== undefined && (
                <Grid xs={6} md={3}>
                  <Typography variant="caption" color="textSecondary">
                    {t("views.tag_aggregation.current_consumption")}
                  </Typography>
                  <Typography variant="h4">
                    {formatDefaultValueWithUnit(
                      data.totalCurrentConsumption,
                      "W",
                    )}
                  </Typography>
                </Grid>
              )}
            {data.totalCurrentGrid !== null &&
              data.totalCurrentGrid !== undefined && (
                <Grid xs={6} md={3}>
                  <Typography variant="caption" color="textSecondary">
                    {t("views.tag_aggregation.current_grid")}
                  </Typography>
                  <Typography
                    variant="h4"
                    color={data.totalCurrentGrid > 0 ? "error" : "success"}
                  >
                    {formatDefaultValueWithUnit(
                      Math.abs(data.totalCurrentGrid),
                      "W",
                    )}
                  </Typography>
                  <Typography variant="caption">
                    {data.totalCurrentGrid > 0 ? t("views.tag_aggregation.consuming") : t("views.tag_aggregation.feeding_in")}
                  </Typography>
                </Grid>
              )}
          </Grid>
        </CardContent>
      </Card>

      <SortControls
        sortBy={paginationState.sortBy}
        sortOrder={paginationState.sortOrder}
        onSortChange={paginationActions.handleSortChange}
        allowedFields={sortFields}
      />

      <Typography variant="h5" style={{ marginBottom: "1rem" }}>
        {t("views.tag_aggregation.system_contributions")}
      </Typography>

      {listLoading ? (
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            minHeight: "200px",
          }}
        >
          <CircularProgress />
        </div>
      ) : (
        <PaginatedList
          items={data.systems.content}
          totalElements={paginationState.totalElements}
          page={paginationState.page}
          size={paginationState.size}
          onPageChange={paginationActions.handlePageChange}
          onSizeChange={paginationActions.handleSizeChange}
          rowsPerPageOptions={[5, 10, 15, 20, 25, 30]}
          renderItem={(system) => {
            const isGrayedOut =
              paginationState.sortBy && !hasValueForSort(system, paginationState.sortBy);

            return (
              <SystemContributionCard
                key={system.id}
                system={system}
                onClick={() => navigate(`/dd/${system.id}`)}
                totals={{
                  totalDayProducedKWH: data.totalDayProducedKWH,
                  totalDayConsumedKWH: data.totalDayConsumedKWH,
                  totalCurrentProduction: data.totalCurrentProduction,
                  totalCurrentConsumption: data.totalCurrentConsumption,
                }}
                isGrayedOut={isGrayedOut}
              />
            );
          }}
        />
      )}
    </div>
  );
}
