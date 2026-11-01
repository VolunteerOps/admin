"use client";
import { useEffect, useRef, type ReactNode } from "react";
import {
  House,
  Compass,
  UsersThree,
  Broadcast,
  ChatsCircle,
  Package,
  GearSix,
  ArrowRight,
  ArrowUpRight,
  Plus,
  MagnifyingGlass,
  ArrowClockwise,
  X,
  List,
  SignOut,
  Sun,
  Moon,
  MapPin,
  WarningCircle,
  CheckCircle,
  CaretRight,
  Bell,
  ShieldCheck,
  FileText,
  Clock,
  Heart,
  Download,
  DotsThree,
  Check,
  ArrowLeft,
} from "@phosphor-icons/react";
const icons: Record<string, typeof House> = {
  overview: House,
  missions: Compass,
  people: UsersThree,
  sentinel: Broadcast,
  messages: ChatsCircle,
  resources: Package,
  settings: GearSix,
  arrow: ArrowRight,
  external: ArrowUpRight,
  plus: Plus,
  search: MagnifyingGlass,
  refresh: ArrowClockwise,
  close: X,
  menu: List,
  logout: SignOut,
  sun: Sun,
  moon: Moon,
  pin: MapPin,
  warning: WarningCircle,
  check: CheckCircle,
  chevron: CaretRight,
  bell: Bell,
  shield: ShieldCheck,
  file: FileText,
  clock: Clock,
  heart: Heart,
  download: Download,
  more: DotsThree,
  tick: Check,
  back: ArrowLeft,
};
export function Icon({ name, size = 20 }: { name: string; size?: number }) {
  const Component = icons[name] || FileText;
  return <Component size={size} weight="regular" aria-hidden />;
}
export function Brand() {
  return (
    <span className="brand">
      <span className="brand-symbol">
        r<span>+</span>
      </span>
      <span>
        ResQ<span className="brand-gold">Ops.</span>
      </span>
    </span>
  );
}
export function Button({
  children,
  icon,
  variant = "",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: string;
  variant?: string;
}) {
  return (
    <button
      {...props}
      className={"button " + variant + " " + (props.className || "")}
    >
      {icon && <Icon name={icon} size={18} />} {children}
    </button>
  );
}
export function Badge({ status }: { status: unknown }) {
  const s = String(status || "Recorded");
  return (
    <span
      className={
        "badge " +
        (/ACTIVE|APPROVED|COMPLETED|RESOLVED|VERIFIED|PUBLISHED|ALLOWED/.test(s)
          ? "positive"
          : /URGENT|CANCELLED|REVOKED|ACTION_REQUIRED|FAILED/.test(s)
            ? "negative"
            : /PENDING|DRAFT|STAGED|WAIT|OPEN/.test(s)
              ? "pending"
              : "")
      }
    >
      <i />
      {s.toLowerCase().replace(/_/g, " ")}
    </span>
  );
}
export function Empty({
  title = "Nothing here yet",
  body,
  action,
}: {
  title?: string;
  body?: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <Icon name="compass" size={30} />
      </span>
      <h3>{title}</h3>
      {body && <p>{body}</p>}
      {action}
    </div>
  );
}
export function Notice({
  children,
  error = false,
}: {
  children: ReactNode;
  error?: boolean;
}) {
  return (
    <div
      role={error ? "alert" : "status"}
      className={"notice " + (error ? "error" : "")}
    >
      <Icon name={error ? "warning" : "check"} />
      <span>{children}</span>
    </div>
  );
}
export function Loading() {
  return (
    <div aria-label="Loading workspace" className="skeletons">
      <div />
      <div />
      <div />
    </div>
  );
}
export function Drawer({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
    const unlock = lockPageScroll();
    return () => { ref.current?.close(); unlock(); };
  }, []);
  return (
    <dialog
      className="drawer"
      ref={ref}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="drawer-body">
        <header>
          <div>
            <span className="eyebrow">Workspace / Review</span>
            <h2>{title}</h2>
          </div>
          <Button aria-label="Close panel" icon="close" onClick={onClose} />
        </header>
        {children}
      </div>
    </dialog>
  );
}
export function Contours() {
  return (
    <svg className="contours" viewBox="0 0 600 600" fill="none" aria-hidden>
      {Array.from({ length: 23 }, (_, i) => (
        <ellipse
          key={i}
          cx="300"
          cy="300"
          rx={70 + i * 9}
          ry={80 + i * 7}
          transform={`rotate(${i * 7} 300 300)`}
          stroke="currentColor"
          strokeWidth=".7"
        />
      ))}
    </svg>
  );
}

let scrollLocks = 0;
let previousOverflow = "";
export function lockPageScroll() {
  if (scrollLocks++ === 0) { previousOverflow = document.documentElement.style.overflow; document.documentElement.style.overflow = "hidden"; }
  let released = false;
  return () => { if (!released) { released = true; if (--scrollLocks === 0) document.documentElement.style.overflow = previousOverflow; } };
}
