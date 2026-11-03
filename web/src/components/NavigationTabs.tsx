"use client";
import { useEffect, useRef } from "react";
import Link from "next/link";

export default function NavigationTabs({ pages, current, label }: {
  pages: string[][];
  current: string;
  label: string;
}) {
  const rail = useRef<HTMLElement>(null);
  useEffect(() => {
    const container = rail.current;
    const active = container?.querySelector<HTMLElement>('[aria-current="page"]');
    if (!container || !active) return;
    const bounds = container.getBoundingClientRect();
    const item = active.getBoundingClientRect();
    // Reveal the selected tab without moving the page or its vertical scroll.
    if (item.left < bounds.left + 36 || item.right > bounds.right - 36) {
      container.scrollLeft += item.left - bounds.left - (bounds.width - item.width) / 2;
    }
  }, [current]);
  return <nav ref={rail} className="tabs" aria-label={label}>
    {pages.map(([id, name]) => <Link key={id} href={"/" + id}
      className={current === id ? "active" : ""}
      aria-current={current === id ? "page" : undefined}>
      <span className="tab-label">{name}</span>
    </Link>)}
  </nav>;
}
