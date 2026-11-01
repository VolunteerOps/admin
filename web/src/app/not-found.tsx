import Link from "next/link";
export default function NotFound() {
  return (
    <main className="auth-form" id="main">
      <span className="eyebrow">ResQOps / 404</span>
      <h1>This page has moved.</h1>
      <p>Return to your coordination workspace.</p>
      <Link className="button primary" href="/">
        Open workspace
      </Link>
    </main>
  );
}
