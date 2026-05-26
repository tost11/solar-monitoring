import React, { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  apiGetTagAggregation,
  SystemContributionDTO,
  TagAggregationDTO,
} from "../api/SolarSystemAPI";
import {
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  TablePagination,
  Typography,
} from "@mui/material";
import { formatDefaultValueWithUnit } from "../Component/utils/GraphUtils";
import RefreshStatusIndicator from "../Component/RefreshStatusIndicator";
import SortControls from "../Component/SortControls";
import { useTranslation } from "react-i18next";

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
              color={system.isOnline ? "success" : "error"}
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
                <span style={{ color: "#2e7d32", marginLeft: "8px" }}>
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
                  color: system.currentGrid > 0 ? "#d32f2f" : "#2e7d32",
                }}
              >
                {formatDefaultValueWithUnit(Math.abs(system.currentGrid), "W")}
              </Typography>
              <Typography
                variant="caption"
                style={{
                  color: system.currentGrid > 0 ? "#d32f2f" : "#2e7d32",
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
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [data, setData] = useState<TagAggregationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [listLoading, setListLoading] = useState(false);

  // Parse URL parameters with validation
  const urlPage = parseInt(searchParams.get("page") || "0", 10);
  const urlSize = parseInt(searchParams.get("size") || "15", 10);
  const urlSortBy = searchParams.get("sortBy") || "name";
  const urlSortOrder = searchParams.get("sortOrder") || "asc";

  // Validate page (must be >= 0)
  const initialPage = urlPage >= 0 ? urlPage : 0;

  // Validate size (must be one of allowed values)
  const allowedSizes = [5, 10, 15, 20, 25, 30];
  const initialSize = allowedSizes.includes(urlSize) ? urlSize : 15;

  // Validate sortBy (must be one of allowed options)
  const allowedSortBy = ["name", "dayproduction", "dayconsumption", "currentproduction", "currentconsumption", "currentgrid", "efficiency", "online"];
  const initialSortBy = allowedSortBy.includes(urlSortBy) ? urlSortBy : "name";

  // Validate sortOrder (must be asc or desc)
  const initialSortOrder = (urlSortOrder === "asc" || urlSortOrder === "desc") ? urlSortOrder : "asc";

  const [page, setPage] = useState(initialPage);
  const [size, setSize] = useState(initialSize);
  const [sortBy, setSortBy] = useState<string>(initialSortBy);
  const [sortOrder, setSortOrder] = useState<string>(initialSortOrder);

  const refFilters = useRef({ page, size, sortBy, sortOrder });

  const updateUrl = (newPage: number, newSize: number, newSortBy: string, newSortOrder: string) => {
    navigate({
      pathname: location.pathname,
      search: `?page=${newPage}&size=${newSize}&sortBy=${newSortBy}&sortOrder=${newSortOrder}`,
    }, { replace: true });
  };

  const fetchTagData = async (): Promise<boolean> => {
    if (!tagId) return false;

    try {
      const { page, size, sortBy, sortOrder } = refFilters.current;
      const result = await apiGetTagAggregation(tagId, page, size, sortBy, sortOrder);
      setData(result);
      return true;
    } catch {
      return false;
    }
  };

  useEffect(() => {
    refFilters.current = { page, size, sortBy, sortOrder };

    if (tagId) {
      if (data === null) {
        setLoading(true);
      } else {
        setListLoading(true);
      }

      apiGetTagAggregation(tagId, page, size, sortBy, sortOrder)
        .then((result) => {
          setData(result);
        })
        .catch(() => {})
        .finally(() => {
          setLoading(false);
          setListLoading(false);
        });
    }
  }, [tagId, page, size, sortBy, sortOrder]);

  const handlePageChange = (event: unknown, newPage: number) => {
    setPage(newPage);
    updateUrl(newPage, size, sortBy, sortOrder);
  };

  const handleSizeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newSize = parseInt(event.target.value, 10);
    setSize(newSize);
    setPage(0);
    updateUrl(0, newSize, sortBy, sortOrder);
  };

  const handleSortChange = (newSortBy: string, newSortOrder: string) => {
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    setPage(0);
    updateUrl(0, size, newSortBy, newSortOrder);
  };

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
        sortBy={sortBy}
        sortOrder={sortOrder}
        onSortChange={handleSortChange}
      />

      <TablePagination
        component="div"
        count={data.systems.totalElements}
        page={data.systems.page}
        onPageChange={handlePageChange}
        rowsPerPage={data.systems.size}
        onRowsPerPageChange={handleSizeChange}
        rowsPerPageOptions={[5, 10, 15, 20, 25, 30]}
        labelRowsPerPage={t("components.pagination.rows_per_page")}
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
        data.systems.content.map((system) => {
          const isGrayedOut = sortBy && !hasValueForSort(system, sortBy);

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
        })
      )}
    </div>
  );
}
