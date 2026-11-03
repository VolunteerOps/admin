"use client";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import type { User } from "firebase/auth";
import { Button, Icon, lockPageScroll, Notice } from "./ui";
import { errorMessage } from "@/lib/api";
export default function TopbarAccount({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => Promise<void>;
}) {
  const [open, setOpen] = useState(false),
    [confirm, setConfirm] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const root = useRef<HTMLDivElement>(null),
    trigger = useRef<HTMLButtonElement>(null),
    dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    if (!open) return;
    const click = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    };
    const key = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        trigger.current?.focus();
      }
    };
    document.addEventListener("pointerdown", click);
    document.addEventListener("keydown", key);
    return () => {
      document.removeEventListener("pointerdown", click);
      document.removeEventListener("keydown", key);
    };
  }, [open]);
  useEffect(() => {
    if (!confirm) return;
    const el = dialog.current;
    el?.showModal();
    const unlock = lockPageScroll();
    return () => {
      el?.close();
      unlock();
      trigger.current?.focus();
    };
  }, [confirm]);
  const initials = (user.displayName || user.email || "R")
    .split(/[\s.@]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((s) => s[0])
    .join("")
    .toUpperCase();
  return (
    <>
      <Link
        className="notification-button"
        href="/notifications"
        aria-label="Notifications"
      >
        <Icon name="bell" size={23} />
      </Link>
      <div className="account-menu-wrap" ref={root}>
        <button
          ref={trigger}
          className="top-avatar"
          aria-expanded={open}
          aria-controls="account-menu"
          aria-label="Open account menu"
          onClick={() => setOpen(!open)}
        >
          {initials}
        </button>
        {open && (
          <nav
            id="account-menu"
            className="account-popover"
            aria-label="Account"
          >
            <div className="account-popover-heading">
              <strong>{user.displayName || "Your account"}</strong>
              <small>{user.email}</small>
            </div>
            {[
              ["settings", "Settings", "settings"],
              ["profile", "Profile", "people"],
              ["support", "Help & support", "heart"],
            ].map(([path, title, icon]) => (
              <Link key={path} href={"/" + path} onClick={() => setOpen(false)}>
                <Icon name={icon} />
                {title}
              </Link>
            ))}
            <button
              onClick={() => {
                setOpen(false);
                setConfirm(true);
              }}
            >
              <Icon name="logout" />
              Log out
            </button>
          </nav>
        )}
      </div>
      {confirm && (
        <dialog
          ref={dialog}
          className="logout-dialog"
          onCancel={(e) => {
            e.preventDefault();
            if (!busy) setConfirm(false);
          }}
          aria-labelledby="logout-title"
        >
          <Icon name="logout" size={30} />
          <h2 id="logout-title">Log out of ResQOps?</h2>
          <p>You can sign back in whenever you need to return.</p>
          {error && <Notice error>{error}</Notice>}
          <div className="action-list">
            <Button disabled={busy} onClick={() => setConfirm(false)}>
              Stay signed in
            </Button>
            <Button
              variant="primary"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  await onLogout();
                  setConfirm(false);
                } catch (e) {
                  setError(errorMessage(e));
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? "Logging out…" : "Log out"}
            </Button>
          </div>
        </dialog>
      )}
    </>
  );
}
