import Portal from "@/components/Portal";
import "./globals.css";
import "./entry.css";
import "./pro.css";
import "./reference-experience.css";
import type { Metadata } from "next";
export const metadata: Metadata = {
  title: {
    default: "ResQOps · Coordination workspace",
    template: "%s · ResQOps",
  },
  description: "Coordinate people, missions and preparedness.",
  robots: { index: false, follow: false },
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <a className="skip-link" href="#main">
          Skip to content
        </a>
        <Portal />{children}
      </body>
    </html>
  );
}
