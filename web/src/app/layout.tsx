import {Suspense} from "react";
import Portal from "@/components/Portal";
import "./globals.css";
import "./entry.css";
import "./pro.css";
import "./reference-experience.css";
import "./workspace.css";
import type { Metadata } from "next";
export const metadata: Metadata = {
  title: {
    default: "ResQOps · Coordination workspace",
    template: "%s · ResQOps",
  },
  description: "Coordinate people, missions and preparedness.",
  robots: { index: false, follow: false },
  icons: { icon: [{ url: "/logo-monochrome.png", type: "image/png" }], apple: "/logo-monochrome.png" },
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <a className="skip-link" href="#main">
          Skip to content
        </a>
        <Suspense fallback={<main>Opening your workspace…</main>}><Portal /></Suspense>{children}
      </body>
    </html>
  );
}
