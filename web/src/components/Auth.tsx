"use client";
import { useState } from "react";
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
} from "firebase/auth";
import { identity, errorMessage } from "@/lib/api";
import { Brand, Button, Contours, Icon, Notice } from "./ui";
export default function Auth({ onSignedIn }: { onSignedIn: () => void }) {
  const [mode, setMode] = useState("login");
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
        await sendEmailVerification(result.user);
        onSignedIn();
      } else {
        await signInWithEmailAndPassword(auth, email, password);
        onSignedIn();
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main id="main" className="auth-layout">
      <section className="auth-story">
        <Brand />
        <div className="story-copy">
          <span className="eyebrow">THE COORDINATION WORKSPACE</span>
          <h1>
            People ready.
            <br />
            Response together.
          </h1>
          <p>
            One place to prepare your team,
            <br />
            coordinate missions and act with clarity.
          </p>
          <div className="story-mark">
            <span />
            <span />
            <span />
            <span />
            <span />
            <span />
            <span />
            <span />
          </div>
        </div>
        <Contours />
        <footer>
          <span>Built for the people who show up.</span>
          <span>ResQOps / Kenya</span>
        </footer>
      </section>
      <section className="auth-panel">
        <div className="auth-top">
          <span>For coordinators</span>
          <span>
            <Icon name="shield" size={16} /> ResQOps workspace
          </span>
        </div>
        <div className="auth-form">
          <span className="eyebrow">
            {mode === "recover"
              ? "ACCOUNT RECOVERY"
              : mode === "register"
                ? "JOIN RESQOPS"
                : "WELCOME BACK"}
          </span>
          <h2>
            {mode === "recover"
              ? "Reset your password"
              : mode === "register"
                ? "Create your account"
                : "Let’s get to work."}
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
                  setMode("recover");
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
                    setMode("register");
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
                  setMode("login");
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
