import React from "react";
import TablePagination from "@mui/material/TablePagination";
import { useTranslation } from "react-i18next";

interface PaginatedListProps<T> {
  items: T[];
  totalElements: number;
  page: number;
  size: number;
  onPageChange: (event: unknown, newPage: number) => void;
  onSizeChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  renderItem: (item: T, index: number) => React.ReactNode;
  rowsPerPageOptions?: number[];
  containerStyle?: React.CSSProperties;
}

export default function PaginatedList<T>({
  items,
  totalElements,
  page,
  size,
  onPageChange,
  onSizeChange,
  renderItem,
  rowsPerPageOptions = [5, 10, 15, 20, 25, 30],
  containerStyle,
}: PaginatedListProps<T>) {
  const { t } = useTranslation();

  return (
    <div style={containerStyle}>
      <div>
        {items.map((item, index) => (
          <React.Fragment key={index}>{renderItem(item, index)}</React.Fragment>
        ))}
      </div>

      {totalElements > 0 && (
        <div style={{ display: "flex", justifyContent: "center", marginTop: "1rem" }}>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={onPageChange}
            rowsPerPage={size}
            onRowsPerPageChange={onSizeChange}
            rowsPerPageOptions={rowsPerPageOptions}
            labelRowsPerPage={t("components.pagination.rows_per_page")}
          />
        </div>
      )}
    </div>
  );
}
