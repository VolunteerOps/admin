"use client";
import { useEffect, useState } from "react";
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
} from "firebase/auth";
import { usePathname, useRouter } from "next/navigation";
import Link from "next/link";
import { EntryVisual } from "./EntryExperience";
import { identity, errorMessage } from "@/lib/api";
import { Brand, Button, Contours, Icon, Notice } from "./ui";
export default function Auth({ onSignedIn }: { onSignedIn: () => void }) {
  const path=usePathname(),router=useRouter();
  const [mode, setMode] = useState(path==="/signup"?"register":path==="/forgot-password"?"recover":"login");
  useEffect(()=>setMode(path==="/signup"?"register":path==="/forgot-password"?"recover":"login"),[path]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const auth = await identity();
      if (mode === "recover") {
        await sendPasswordResetEmail(auth, email);
        setNotice(
          "If this address has an account, a reset link will arrive shortly.",
        );
      } else if (mode === "register") {
        const result = await createUserWithEmailAndPassword(
          auth,
          email,
          password,
        );
        await updateProfile(result.user, { displayName: name });
        sessionStorage.setItem("resq-setup-pending",result.user.uid);
        await sendEmailVerification(result.user);
        onSignedIn();
        router.replace("/overview");
      } else {
        await signInWithEmailAndPassword(auth, email, password);
        onSignedIn();
        router.replace("/overview");
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main id="main" className="entry-shell reference-auth">
      <EntryVisual />
      <section className="entry-content">
        <div className="entry-top">
          <Link href="/welcome">ResQOps / For coordinators</Link>
          <span>
            <Icon name="shield" size={16} /> ResQOps workspace
          </span>
        </div>
        <div className="entry-auth-form">
          <span className="eyebrow">
            {mode === "recover"
              ? "ACCOUNT RECOVERY"
              : mode === "register"
                ? "JOIN RESQOPS"
                : "WELCOME BACK"}
          </span>
          <h2>
            {mode === "recover"
              ? "Forgot your password?"
              : mode === "register"
                ? "Better response starts with you."
                : "Your team is counting on you."}
          </h2>
          <p>
            {mode === "recover"
              ? "We’ll send a reset link to your email."
              : mode === "register"
                ? "Your workspace administrator will approve access."
                : "Sign in to your coordination workspace."}
          </p>
          <form onSubmit={submit}>
            {mode === "register" && (
              <label>
                Full name
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoComplete="name"
                />
              </label>
            )}
            <label>
              Email address
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@organisation.org"
                autoComplete="username"
                required
              />
            </label>
            {mode !== "recover" && (
              <label>
                Password
                <div className="password-input">
                  <input
                    aria-label="Password"
                    type={show ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete={
                      mode === "register" ? "new-password" : "current-password"
                    }
                    required
                    minLength={mode === "register" ? 8 : 1}
                  />
                  <button type="button" aria-label={show?'Hide password':'Show password'} onClick={() => setShow(!show)}>
                    {show ? "Hide" : "Show"}
                  </button>
                </div>
              </label>
            )}
            {mode === "login" && (
              <button
                type="button"
                className="text-button forgot"
                onClick={() => {
                  router.push("/forgot-password");
                  setError("");
                }}
              >
                Forgot password?
              </button>
            )}
            {error && <Notice error>{error}</Notice>}
            {notice && <Notice>{notice}</Notice>}
            <Button variant="primary" disabled={busy} type="submit">
              {busy
                ? "Please wait…"
                : mode === "recover"
                  ? "Send reset link"
                  : mode === "register"
                    ? "Create account"
                    : "Sign in"}
              <Icon name="arrow" />
            </Button>
          </form>
          <div className="auth-switch">
            {mode === "login" ? (
              <>
                New to ResQOps?{" "}
                <button
                  className="text-button"
                  onClick={() => {
                    router.push("/signup");
                    setError("");
                  }}
                >
                  Create an account
                </button>
              </>
            ) : (
              <button
                className="text-button"
                onClick={() => {
                  router.push("/login");
                  setError("");
                  setNotice("");
                }}
              >
                Back to sign in
              </button>
            )}
          </div>
        </div>
        <footer className="auth-footer">
          Preparedness starts with people.
          <span>© {new Date().getFullYear()} ResQOps</span>
        </footer>
      </section>
    </main>
  );
}
