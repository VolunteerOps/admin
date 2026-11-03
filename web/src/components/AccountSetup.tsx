"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { updateProfile, type User } from "firebase/auth";
import { api, errorMessage } from "@/lib/api";
import { useData, invalidateData, type Scope } from "@/lib/hooks";
import { EntryBrand } from "./EntryExperience";
import { Button, Icon, Loading, Notice } from "./ui";
const stages = [
  "About you",
  "Your organization",
  "Your coverage",
  "Your preferences",
];
export default function AccountSetup({
  user,
  scope,
  onDone,
}: {
  user: User;
  scope: Scope;
  onDone: () => void;
}) {
  const router = useRouter(),
    saved = useData("/api/me/portal-preferences"),
    workspaces = useData("/api/me/workspaces");
  const regions = useData(
    scope.org ? `/api/organizations/${scope.org}/sentinel/regions` : null,
  );
  const [version, setVersion] = useState(0),
    [step, setStep] = useState(0),
    [form, setForm] = useState({
      name: user.displayName || "",
      role: "Coordinator",
      organization: "",
      coverage: "",
      briefings: true,
      reminders: true,
      completed: false,
      pro_tour_completed: false,
    }),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [done, setDone] = useState(false),
    [joinMode, setJoinMode] = useState("later"),
    [workspace, setWorkspace] = useState(""),
    [token, setToken] = useState(""),
    [message, setMessage] = useState(""),
    [domain, setDomain] = useState(""),
    [orgDone, setOrgDone] = useState(false);
  useEffect(() => {
    if (saved.data) {
      setVersion(saved.data.version);
      if (saved.data.payload) {
        const p = saved.data.payload;
        setForm({
          name: p.name,
          role: p.role,
          organization: p.organization,
          coverage: p.coverage,
          briefings: p.briefings,
          reminders: p.reminders,
          completed: p.completed,
          pro_tour_completed: p.pro_tour_completed === true,
        });
        setStep(Math.min(p.step || 0, 3));
      }
    }
  }, [saved.data]);
  async function next(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (step === 0) {
        await api("/api/auth/profile", { full_name: form.name }, "PATCH");
        await updateProfile(user, { displayName: form.name });
      }
      if (step === 1 && !orgDone) {
        if (joinMode === "join")
          await api("/api/me/join-requests", {
            organization_id: workspace,
            message,
          });
        if (joinMode === "invite")
          await api("/api/invitations/accept", { token });
        if (joinMode === "create")
          await api("/api/me/partnerships", {
            name: form.organization,
            domain,
            message,
          });
        setOrgDone(true);
      }
      const result = await api(
        "/api/me/portal-preferences",
        {
          ...form,
          expected_version: version,
          step: step + 1,
          completed: step === 3,
        },
        "PUT",
      );
      setVersion(result.version);
      invalidateData();
      if (step === 3) {
        sessionStorage.removeItem("resq-setup-pending");
        setDone(true);
        onDone();
      } else setStep(step + 1);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main id="main" className="entry-shell setup-shell">
      <aside className="entry-visual setup-visual">
        <EntryBrand light />
        <div className="setup-side-copy">
          <span className="entry-kicker">LET’S MAKE THIS YOURS</span>
          <h2>
            A few details.
            <br />A better starting point.
          </h2>
          <p>
            Set up your profile around your team and the communities you serve.
          </p>
        </div>
        <ol className="setup-nav">
          {stages.map((s, i) => (
            <li
              key={s}
              className={
                done || i < step ? "complete" : i === step ? "current" : ""
              }
            >
              <button
                disabled={i >= step || busy || done}
                onClick={() => setStep(i)}
              >
                <span>{i < step || done ? "✓" : i + 1}</span>
                <div>
                  <b>{s}</b>
                  <small>
                    {
                      [
                        "How your team will know you",
                        "Where you work and how you’ll join",
                        "The communities you support",
                        "Updates that work for you",
                      ][i]
                    }
                  </small>
                </div>
              </button>
            </li>
          ))}
        </ol>
        <div className="setup-side-note">
          <Icon name="shield" />
          <div>
            <b>Access is always reviewed.</b>
            <p>
              Profile details do not grant access to private workspace
              information.
            </p>
          </div>
        </div>
      </aside>
      <section className="entry-content">
        <header className="entry-top">
          <Link href="/invitations" className="entry-text-link">
            Workspace access <Icon name="arrow" />
          </Link>
        </header>
        {saved.loading && !saved.data ? (
          <Loading />
        ) : done ? (
          <div className="setup-form">
            <span className="entry-kicker">YOUR PROFILE IS READY</span>
            <h1>A good place to start.</h1>
            <p>
              Your preferences are saved. Workspace owners review membership
              requests separately.
            </p>
            <Button
              variant="primary"
              onClick={() => router.replace("/overview")}
            >
              Open my workspace <Icon name="arrow" />
            </Button>
          </div>
        ) : (
          <form className="setup-form" onSubmit={next}>
            <span className="entry-kicker">STEP {step + 1} OF 4</span>
            <h1>
              {
                [
                  "Let’s start with you.",
                  "Find your people.",
                  "Where do you make a difference?",
                  "Make it work for you.",
                ][step]
              }
            </h1>
            <p>
              {
                [
                  "A name your team recognizes, and the work you do.",
                  "Join an existing team or apply for a new workspace.",
                  "Your preferred coverage helps describe the communities you serve. Workspace permissions remain separate.",
                  "Choose the updates you want to see in your notification feed.",
                ][step]
              }
            </p>
            {(error || saved.error) && (
              <Notice error>{error || saved.error}</Notice>
            )}
            {step === 0 && (
              <>
                <label>
                  Full name
                  <input
                    required
                    minLength={2}
                    maxLength={120}
                    autoComplete="name"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                  />
                </label>
                <label>
                  Your job or responsibility
                  <input
                    maxLength={100}
                    value={form.role}
                    onChange={(e) => setForm({ ...form, role: e.target.value })}
                  />
                </label>
                <small>
                  This describes your work; it does not change your account
                  permissions.
                </small>
              </>
            )}
            {step === 1 && (
              <>
                <label>
                  How will you join?
                  <select
                    value={joinMode}
                    onChange={(e) => {
                      setJoinMode(e.target.value);
                      setOrgDone(false);
                    }}
                  >
                    <option value="later">I’ll arrange access later</option>
                    <option value="join">Request to join a workspace</option>
                    <option value="invite">I have an invitation</option>
                    <option value="create">
                      Apply for a new organization workspace
                    </option>
                  </select>
                </label>
                {joinMode === "join" && (
                  <label>
                    Available workspaces
                    <select
                      required
                      value={workspace}
                      onChange={(e) => {
                        setWorkspace(e.target.value);
                        setOrgDone(false);
                        setForm({
                          ...form,
                          organization:
                            workspaces.data?.items?.find(
                              (w: any) => w.id === e.target.value,
                            )?.displayName || "",
                        });
                      }}
                    >
                      <option value="">Choose a workspace</option>
                      {workspaces.data?.items?.map((w: any) => (
                        <option key={w.id} value={w.id}>
                          {w.displayName || w.display_name}
                        </option>
                      ))}
                    </select>
                    {workspaces.error && (
                      <Notice error>{workspaces.error}</Notice>
                    )}
                    <small>
                      If your workspace is not listed, ask its owner for an
                      invitation.
                    </small>
                  </label>
                )}
                {joinMode === "invite" && (
                  <label>
                    Invitation code
                    <input
                      required
                      value={token}
                      onChange={(e) => {
                        setToken(e.target.value);
                        setOrgDone(false);
                      }}
                      autoComplete="off"
                    />
                  </label>
                )}
                {joinMode === "create" && (
                  <>
                    <label>
                      Organization name
                      <input
                        required
                        minLength={2}
                        maxLength={160}
                        value={form.organization}
                        onChange={(e) =>
                          setForm({ ...form, organization: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      Organization domain
                      <input
                        required
                        value={domain}
                        onChange={(e) => setDomain(e.target.value)}
                        placeholder="organisation.org"
                      />
                    </label>
                  </>
                )}
                {["join", "create"].includes(joinMode) && (
                  <label>
                    Tell the reviewer about your work
                    <textarea
                      required
                      minLength={10}
                      maxLength={1000}
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                    />
                  </label>
                )}
                {orgDone && joinMode !== "later" && (
                  <Notice>Your access request has been recorded.</Notice>
                )}
              </>
            )}
            {step === 2 && (
              <>
                <label>
                  Preferred city, district or region
                  <input
                    list="setup-regions"
                    value={form.coverage}
                    onChange={(e) =>
                      setForm({ ...form, coverage: e.target.value })
                    }
                    maxLength={300}
                    placeholder="Mombasa, Kenya"
                  />
                  <datalist id="setup-regions">
                    {regions.data?.map((r: any) => (
                      <option key={r.id} value={r.name} />
                    ))}
                  </datalist>
                </label>
                <div className="setup-coverage-note">
                  <Icon name="pin" />
                  <p>
                    This is your profile’s coverage preference. Administrators
                    configure the workspace’s operating boundaries in Settings.
                  </p>
                </div>
              </>
            )}
            {step === 3 && (
              <div className="setup-preferences">
                <label className="check">
                  <input
                    type="checkbox"
                    checked={form.briefings}
                    onChange={(e) =>
                      setForm({ ...form, briefings: e.target.checked })
                    }
                  />
                  <span>
                    <strong>Preparedness updates</strong>
                    <small>
                      Include campaign activity in my notifications.
                    </small>
                  </span>
                </label>
                <label className="check">
                  <input
                    type="checkbox"
                    checked={form.reminders}
                    onChange={(e) =>
                      setForm({ ...form, reminders: e.target.checked })
                    }
                  />
                  <span>
                    <strong>Training updates</strong>
                    <small>
                      Include course and qualification activity in my
                      notifications.
                    </small>
                  </span>
                </label>
                <p>
                  Mission and response updates remain visible. These preferences
                  do not enable email or push messages.
                </p>
              </div>
            )}
            <div className="form-actions">
              {step > 0 && (
                <Button disabled={busy} onClick={() => setStep(step - 1)}>
                  Back
                </Button>
              )}
              <Button
                type="submit"
                variant="primary"
                disabled={busy || !!saved.error}
              >
                {busy
                  ? "Saving…"
                  : step === 3
                    ? "Finish setup"
                    : "Save and continue"}
                <Icon name="arrow" />
              </Button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}
