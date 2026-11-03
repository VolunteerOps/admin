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
      ["hours", "Contributions"],
      ["deployment-map", "Deployment map"],
    ],
  },
  {
    name: "People",
    icon: "people",
    pages: [
      ["people", "People"],
    ],
  },
  {
    name: "Sentinel",
    icon: "sentinel",
    pages: [
      ["sentinel", "Disaster radar"],
      ["weather-analysis", "Sources & coverage"],
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
  {name:"Field reports",icon:"file",pages:[["reports","Field reports"]]},
  {name:"Preparedness",icon:"shield",pages:[["campaigns","Preparedness campaigns"]]},
  {name:"Training",icon:"people",pages:[["training","Courses"],["learning","Training reviews"],["qualifications","Qualifications"]]},
  {name:"Reports & impact",icon:"overview",pages:[["insights","Reports & impact"]]},
  {
    name: "Administration",
    icon: "settings",
    pages: [
      ["settings", "Workspace"],
      ["governance", "Governance"],
      ["security", "Dispatch controls"],
      ["activity", "Audit trail"],
      ["team-pro", "Sponsorship"],
      ["platform", "Platform"],
      ["promotions", "Promotions"],
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
  pro: "ResQOps Pro",
  "pro-guide": "Your Pro tools",
  notifications: "Notifications",
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
