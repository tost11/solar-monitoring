import React, { useEffect, useRef, useState } from "react";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import moment from "moment";
import { useTranslation } from "react-i18next";
import { Colors } from "./utils/ColorUtils";

interface RefreshStatusIndicatorProps {
  fetchCallback: () => Promise<boolean>;
  normalInterval?: number;
  errorInterval?: number;
  staleThresholdMinutes?: number;
  skipInitialFetch?: boolean;
}

export default function RefreshStatusIndicator({
  fetchCallback,
  normalInterval = 300000,
  errorInterval = 60000,
  staleThresholdMinutes = 10,
  skipInitialFetch = false,
}: RefreshStatusIndicatorProps) {
  const { t } = useTranslation();
  const [lastRefreshed, setLastRefreshed] = useState<moment.Moment | null>(
    null,
  );
  const [currentRefreshInterval, setCurrentRefreshInterval] =
    useState<number>(normalInterval);
  const [, forceUpdate] = useState(0);

  const refreshTimer = useRef<NodeJS.Timeout>();
  const statusUpdateTimer = useRef<NodeJS.Timeout>();

  const scheduleNextRefresh = (delayMs: number) => {
    setCurrentRefreshInterval(delayMs);

    if (refreshTimer.current) {
      clearTimeout(refreshTimer.current);
    }

    refreshTimer.current = setTimeout(async () => {
      const success = await fetchCallback();
      if (success) {
        setLastRefreshed(moment());
      }
      const nextDelay = success ? normalInterval : errorInterval;
      scheduleNextRefresh(nextDelay);
    }, delayMs);
  };

  useEffect(() => {
    if (!skipInitialFetch) {
      fetchCallback().then((success) => {
        if (success) {
          setLastRefreshed(moment());
        }
        scheduleNextRefresh(normalInterval);
      });
    } else {
      // Skip initial fetch - parent component's useEffect handles initial data load
      // Set lastRefreshed to now since parent is fetching the data
      setLastRefreshed(moment());
      // Only schedule the first refresh interval
      scheduleNextRefresh(normalInterval);
    }

    return () => {
      if (refreshTimer.current) {
        clearTimeout(refreshTimer.current);
      }
    };
  }, [skipInitialFetch]);

  useEffect(() => {
    statusUpdateTimer.current = setInterval(() => {
      forceUpdate((prev) => prev + 1);
    }, 60000);

    return () => {
      if (statusUpdateTimer.current) {
        clearInterval(statusUpdateTimer.current);
      }
    };
  }, []);

  const getRefreshStatus = () => {
    if (!lastRefreshed) {
      return {
        text: t("components.refresh_status.loading"),
        backgroundColor: "transparent",
        textColor: "textSecondary" as const,
      };
    }

    const minutesAgo = moment().diff(lastRefreshed, "minutes");
    const isStale = minutesAgo >= staleThresholdMinutes;

    let timeText;
    if (minutesAgo < 1) {
      timeText = t("components.refresh_status.just_now");
    } else if (minutesAgo === 1) {
      timeText = t("components.refresh_status.one_minute_ago");
    } else {
      timeText = t("components.refresh_status.minutes_ago", { count: minutesAgo });
    }

    return {
      text: t("components.refresh_status.last_refreshed", { time: timeText }),
      backgroundColor: isStale ? "#ffcdd2" : "#c8e6c9",
      textColor: isStale ? Colors.consumptionRed : Colors.productionGreen,
    };
  };

  const refreshStatus = getRefreshStatus();

  return (
    <>
      <style>{`
        @keyframes progressBar {
          from { width: 0%; }
          to { width: 100%; }
        }
      `}</style>

      <Card
        style={{
          marginBottom: "1rem",
          backgroundColor: refreshStatus.backgroundColor,
          position: "relative",
          overflow: "hidden",
        }}
      >
        <CardContent style={{ paddingBottom: "8px" }}>
          <Typography
            variant="body1"
            style={{ color: refreshStatus.textColor }}
          >
            {refreshStatus.text}
          </Typography>
        </CardContent>
        {lastRefreshed && (
          <div
            style={{
              position: "absolute",
              bottom: 0,
              left: 0,
              height: "3px",
              backgroundColor: "#1976d2",
              animation: `progressBar ${currentRefreshInterval}ms linear`,
              animationFillMode: "forwards",
              width: "0%",
            }}
            key={lastRefreshed.valueOf()}
          />
        )}
      </Card>
    </>
  );
}
