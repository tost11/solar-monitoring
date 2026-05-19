import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
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
  Typography,
} from "@mui/material";
import { formatDefaultValueWithUnit } from "../Component/utils/GraphUtils";
import { useTranslation } from "react-i18next";
import RefreshStatusIndicator from "../Component/RefreshStatusIndicator";

interface SystemContributionCardProps {
  system: SystemContributionDTO;
  onClick: () => void;
}

function SystemContributionCard({
  system,
  onClick,
}: SystemContributionCardProps) {
  return (
    <Card onClick={onClick} style={{ cursor: "pointer", marginBottom: "1rem" }}>
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
              label={system.isOnline ? "Online" : "Offline"}
              color={system.isOnline ? "success" : "error"}
              size="small"
            />
          </div>

          {/* Day Production */}
          <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
            <Typography variant="caption" color="textSecondary">
              Day Production
            </Typography>
            <Typography variant="body1" style={{ fontWeight: 500 }}>
              {formatDefaultValueWithUnit(system.dayProducedKWH, "kWh")}
            </Typography>
            <Typography
              variant="caption"
              style={{ color: "#1976d2", fontWeight: 500 }}
            >
              {system.dayProductionPercentage.toFixed(1)}%
            </Typography>
          </div>

          {/* Day Consumption */}
          {system.dayConsumedKWH !== undefined &&
            system.dayConsumedKWH !== null && (
              <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
                <Typography variant="caption" color="textSecondary">
                  Day Consumption
                </Typography>
                <Typography variant="body1" style={{ fontWeight: 500 }}>
                  {formatDefaultValueWithUnit(system.dayConsumedKWH, "kWh")}
                </Typography>
                {system.dayConsumptionPercentage !== undefined &&
                  system.dayConsumptionPercentage !== null && (
                    <Typography
                      variant="caption"
                      style={{ color: "#d32f2f", fontWeight: 500 }}
                    >
                      {system.dayConsumptionPercentage.toFixed(1)}%
                    </Typography>
                  )}
              </div>
            )}

          {/* Current Production */}
          <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
            <Typography variant="caption" color="textSecondary">
              Current Production
            </Typography>
            <Typography variant="body1" style={{ fontWeight: 500 }}>
              {formatDefaultValueWithUnit(system.currentProduction, "W")}
            </Typography>
            <Typography
              variant="caption"
              style={{ color: "#1976d2", fontWeight: 500 }}
            >
              {system.currentProductionPercentage.toFixed(1)}%
            </Typography>
          </div>

          {/* Current Consumption */}
          {system.currentConsumption !== undefined &&
            system.currentConsumption !== null && (
              <div style={{ minWidth: "120px", flex: "0 0 120px" }}>
                <Typography variant="caption" color="textSecondary">
                  Current Consumption
                </Typography>
                <Typography variant="body1" style={{ fontWeight: 500 }}>
                  {formatDefaultValueWithUnit(system.currentConsumption, "W")}
                </Typography>
                {system.currentConsumptionPercentage !== undefined &&
                  system.currentConsumptionPercentage !== null && (
                    <Typography
                      variant="caption"
                      style={{ color: "#d32f2f", fontWeight: 500 }}
                    >
                      {system.currentConsumptionPercentage.toFixed(1)}%
                    </Typography>
                  )}
              </div>
            )}

          {/* Grid */}
          {system.currentGrid !== undefined && system.currentGrid !== null && (
            <div style={{ minWidth: "100px", flex: "0 0 100px" }}>
              <Typography variant="caption" color="textSecondary">
                Grid
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
                {system.currentGrid > 0 ? "↓ Consuming" : "↑ Feeding In"}
              </Typography>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export default function TagAggregationView() {
  const { tagId } = useParams<{ tagId: string }>();
  const [data, setData] = useState<TagAggregationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { t } = useTranslation();

  const fetchTagData = async (): Promise<boolean> => {
    if (!tagId) return false;

    try {
      const result = await apiGetTagAggregation(tagId);
      setData(result);
      return true;
    } catch {
      return false;
    }
  };

  useEffect(() => {
    if (tagId) {
      setLoading(true);
      apiGetTagAggregation(tagId)
        .then((result) => {
          setData(result);
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [tagId]);

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
        <Typography variant="h5">Tag not found</Typography>
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
      />

      <Card style={{ marginBottom: "1.5rem" }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                Systems Online
              </Typography>
              <Typography variant="h4">
                {data.onlineSystems} / {data.totalSystems}
              </Typography>
            </Grid>
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                Total Day Production
              </Typography>
              <Typography variant="h4">
                {formatDefaultValueWithUnit(data.totalDayProducedKWH, "kWh")}
              </Typography>
            </Grid>
            {data.totalDayConsumedKWH !== null &&
              data.totalDayConsumedKWH !== undefined && (
                <Grid xs={6} md={3}>
                  <Typography variant="caption" color="textSecondary">
                    Total Day Consumption
                  </Typography>
                  <Typography variant="h4">
                    {formatDefaultValueWithUnit(
                      data.totalDayConsumedKWH,
                      "kWh",
                    )}
                  </Typography>
                </Grid>
              )}
            <Grid xs={6} md={3}>
              <Typography variant="caption" color="textSecondary">
                Current Production
              </Typography>
              <Typography variant="h4">
                {formatDefaultValueWithUnit(data.totalCurrentProduction, "W")}
              </Typography>
            </Grid>
            {data.totalCurrentConsumption !== null &&
              data.totalCurrentConsumption !== undefined && (
                <Grid xs={6} md={3}>
                  <Typography variant="caption" color="textSecondary">
                    Current Consumption
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
                    Current Grid
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
                    {data.totalCurrentGrid > 0 ? "Consuming" : "Feeding In"}
                  </Typography>
                </Grid>
              )}
          </Grid>
        </CardContent>
      </Card>

      <Typography variant="h5" style={{ marginBottom: "1rem" }}>
        System Contributions
      </Typography>
      {data.systems.map((system) => (
        <SystemContributionCard
          key={system.id}
          system={system}
          onClick={() => navigate(`/dd/${system.id}`)}
        />
      ))}
    </div>
  );
}
