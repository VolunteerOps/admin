export const areas = [
  { name: "Overview", icon: "overview", pages: [["overview", "Overview"], ["operations-map", "Operations map"]] },
  {
    name: "Missions",
    icon: "missions",
    pages: [
      ["missions", "Missions"],
      ["resource-planning", "Resource planning"],
      ["applications", "Applications"],
      ["assignments", "Assignments"],
      ["reports", "Field reports"],
      ["hours", "Contributions"],
      ["deployment-map", "Deployment map"],
    ],
  },
  {
    name: "People",
    icon: "people",
    pages: [
      ["people", "People"],
      ["training", "Training"],
      ["learning", "Training reviews"],
      ["qualifications", "Qualifications"],
    ],
  },
  {
    name: "Sentinel",
    icon: "sentinel",
    pages: [
      ["sentinel", "Radar"],
      ["weather-analysis", "Weather analysis"],
      ["specialised", "Specialised planning"],
      ["agent-runs", "Agent runs"],
      ["assistance", "Assistance"],
    ],
  },
  {
    name: "Communications",
    icon: "messages",
    pages: [
      ["inbox", "Conversations"],
      ["bulletins", "Bulletins"],
    ],
  },
  {
    name: "Resources",
    icon: "resources",
    pages: [
      ["resources", "Inventory"],
      ["stock-history", "Stock history"],
    ],
  },
  {
    name: "Administration",
    icon: "settings",
    pages: [
      ["settings", "Workspace"],
      ["governance", "Governance"],
      ["security", "Dispatch controls"],
      ["activity", "Audit trail"],
      ["insights", "Insights"],
      ["team-pro", "Sponsorship"],
      ["platform", "Platform"],
    ],
  },
];
export const kinds: Record<string, string> = {
  missions: "MISSION",
  applications: "APPLICATION",
  assignments: "ASSIGNMENT",
  reports: "REPORT",
  hours: "HOURS",
  training: "COURSE",
  learning: "ENROLLMENT",
  qualifications: "CREDENTIAL",
  inbox: "MESSAGE",
  bulletins: "BULLETIN",
  resources: "INVENTORY",
  "stock-history": "STOCK_LEDGER",
  activity: "EVENT",
  support: "SUPPORT",
  "deployment-map": "MISSION",
};
export const titles: Record<string, string> = {
  ...Object.fromEntries(areas.flatMap((a) => a.pages)),
  profile: "My account",
  privacy: "Privacy",
  securityAccount: "Account security",
  invitations: "Workspace access",
  pro: "ResQ Pro",
  support: "Help & support",
};
export type Row = {
  id: string;
  kind: string;
  version: number;
  ownerId?: number;
  parentId?: string;
  updatedAt?: string;
  payload: Record<string, any>;
  [key: string]: any;
};
export const label = (s: unknown) =>
  String(s ?? "")
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/^./, (c) => c.toUpperCase());
export const recordTitle = (r: Row) =>
  r.payload?.title ||
  r.payload?.mission_title ||
  r.payload?.course_title ||
  r.payload?.skill ||
  r.payload?.action ||
  r.payload?.summary ||
  label(r.kind);
export const date = (v: unknown) =>
  v
    ? new Intl.DateTimeFormat("en-KE", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      }).format(new Date(String(v)))
    : "—";
