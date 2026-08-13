import React from "react";

interface DataCardProps {
  title: string;
  type: 'production' | 'consumption' | 'battery' | 'grid';
  children: React.ReactNode;
  minWidth?: number;
  showHeader?: boolean;
}

export function DataCard({title, type, children, minWidth = 280, showHeader = true}: DataCardProps) {
  return (
    <div
      className={`data-card data-card--${type}`}
      style={minWidth !== 280 ? { flex: `1 1 ${minWidth}px`, minWidth: 0 } : undefined}
    >
      {showHeader && (
        <div className={`data-card__title data-card__title--${type}`}>
          {title}
        </div>
      )}
      {!showHeader && (
        <div className={`data-card__header data-card__header--${type}`}>
          {title}
        </div>
      )}
      {children}
    </div>
  );
}
