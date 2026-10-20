export type DocRow = {
  id: number;
  name: string;
  category: string;
  content_type: string;
  size_bytes: number;
  created_at: string;
};

export type Grant = {
  user_id: number;
  email: string;
  permission: "READ" | "EDIT";
  granted_by: number;
};

export type AuditRow = {
  id: number;
  created_at: string;
  actor: string | null;
  action: string;
  document_id: number | null;
  outcome: "ALLOWED" | "DENIED";
  detail: string | null;
};

export type ChainResult = {
  intact: boolean;
  rowsChecked: number;
  firstBadId: number | null;
};