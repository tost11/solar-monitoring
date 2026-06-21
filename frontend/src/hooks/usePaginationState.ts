import { useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

interface UsePaginationStateOptions {
  defaultPage?: number;
  defaultSize?: number;
  defaultSortBy?: string;
  defaultSortOrder?: "asc" | "desc";
  allowedSizes?: number[];
  allowedSortFields?: string[];
}

interface PaginationState {
  page: number;
  size: number;
  sortBy: string;
  sortOrder: string;
  totalElements: number;
  totalPages: number;
}

interface PaginationActions {
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  setSortBy: (sortBy: string) => void;
  setSortOrder: (sortOrder: string) => void;
  setTotalElements: (total: number) => void;
  setTotalPages: (total: number) => void;
  handlePageChange: (event: unknown, newPage: number) => void;
  handleSizeChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  handleSortChange: (sortBy: string, sortOrder: string) => void;
  updateUrl: (page: number, size: number, sortBy: string, sortOrder: string) => void;
}

export function usePaginationState(
  options: UsePaginationStateOptions = {}
): [PaginationState, PaginationActions] {
  const {
    defaultPage = 0,
    defaultSize = 15,
    defaultSortBy = "name",
    defaultSortOrder = "asc",
    allowedSizes = [5, 10, 15, 20, 25, 30],
    allowedSortFields = [],
  } = options;

  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const urlPage = parseInt(searchParams.get("page") || "0", 10);
  const urlSize = parseInt(searchParams.get("size") || defaultSize.toString(), 10);
  const urlSortBy = searchParams.get("sortBy") || defaultSortBy;
  const urlSortOrder = searchParams.get("sortOrder") || defaultSortOrder;

  const initialPage = urlPage >= 0 ? urlPage : defaultPage;
  const initialSize = allowedSizes.includes(urlSize) ? urlSize : defaultSize;
  const initialSortBy =
    allowedSortFields.length === 0 || allowedSortFields.includes(urlSortBy)
      ? urlSortBy
      : defaultSortBy;
  const initialSortOrder = ["asc", "desc"].includes(urlSortOrder)
    ? urlSortOrder
    : defaultSortOrder;

  const [page, setPage] = useState(initialPage);
  const [size, setSize] = useState(initialSize);
  const [sortBy, setSortBy] = useState(initialSortBy);
  const [sortOrder, setSortOrder] = useState(initialSortOrder);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const updateUrl = useCallback(
    (page: number, size: number, sortBy: string, sortOrder: string) => {
      const params = new URLSearchParams(window.location.search);
      params.set("page", page.toString());
      params.set("size", size.toString());
      params.set("sortBy", sortBy);
      params.set("sortOrder", sortOrder);
      navigate({ search: params.toString() }, { replace: true });
    },
    [navigate]
  );

  const handlePageChange = useCallback(
    (event: unknown, newPage: number) => {
      setPage(newPage);
      updateUrl(newPage, size, sortBy, sortOrder);
    },
    [size, sortBy, sortOrder, updateUrl]
  );

  const handleSizeChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const newSize = parseInt(event.target.value, 10);
      setSize(newSize);
      setPage(0);
      updateUrl(0, newSize, sortBy, sortOrder);
    },
    [sortBy, sortOrder, updateUrl]
  );

  const handleSortChange = useCallback(
    (newSortBy: string, newSortOrder: string) => {
      setSortBy(newSortBy);
      setSortOrder(newSortOrder);
      setPage(0);
      updateUrl(0, size, newSortBy, newSortOrder);
    },
    [size, updateUrl]
  );

  const state: PaginationState = {
    page,
    size,
    sortBy,
    sortOrder,
    totalElements,
    totalPages,
  };

  const actions: PaginationActions = {
    setPage,
    setSize,
    setSortBy,
    setSortOrder,
    setTotalElements,
    setTotalPages,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    updateUrl,
  };

  return [state, actions];
}
