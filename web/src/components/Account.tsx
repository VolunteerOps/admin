"use client";
import { UserFacingError } from "@/lib/friendly-errors";
import { useState } from "react";
import Link from "next/link";
import {
  sendPasswordResetEmail,
  verifyBeforeUpdateEmail,
  signOut,
  type User,
} from "firebase/auth";
import { api, identity, errorMessage } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date, label } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Loading, Notice } from "./ui";
import { Form, opts, type Field } from "./Forms";
import { Evidence } from "./Sentinel";
import AccountAccess from "./AccountAccess";
const tabs = [
  ["profile", "Personal profile"],
  ["privacy", "Privacy & data"],
  ["securityAccount", "Account security"],
  ["invitations", "Workspace access"],
  ["pro", "ResQ Pro"],
];
export default function Account({
  page,
  scope,
  user,
  revision,
}: {
  page: string;
  scope: Scope;
  user: User;
  revision: number;
}) {
  const home = useData("/api/me/home", revision),
    invitations = useData(
      page === "invitations" ? "/api/me/invitations" : null,
      revision,
    ),
    workspaces = useData(
      page === "invitations" ? "/api/me/workspaces" : null,
      revision,
    ),
    shares = useData(
      page === "privacy" ? "/api/me/profile-shares" : null,
      revision,
    ),
    subscription = useData(
      page === "pro" ? "/api/me/subscription" : null,
      revision,
    ),
    usage = useData(page === "pro" ? "/api/me/usage" : null, revision);
  const [notice, setNotice] = useState(""),
    [error, setError] = useState(""),
    [join, setJoin] = useState<any>(null),
    [deleting, setDeleting] = useState(false),
    [share, setShare] = useState("");
  const profile = home.data?.profile;
  async function run(action: () => Promise<void>) {
    setError("");
    setNotice("");
    try {
      await action();
      scope.refresh();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <div className="account-layout">
      <nav className="account-nav" aria-label="Account navigation">
        {tabs.map(([id, name]) => (
          <Link
            className={page === id ? "active" : ""}
            key={id}
            href={"/" + id}
          >
            {name}
          </Link>
        ))}
      </nav>
      <div className="account-content">
        {(error || home.error) && <Notice error>{error || home.error}</Notice>}
        {notice && <Notice>{notice}</Notice>}
        {home.loading ? (
          <Loading />
        ) : page === "profile" ? (
          <section className="panel">
            <div className="panel-head">
              <h2>Your profile</h2>
              <span className="avatar">
                {(user.displayName || user.email || "R")
                  .slice(0, 2)
                  .toUpperCase()}
              </span>
            </div>
            <div className="panel-body">
              <p>
                Choose what coordinators should know about you. Contact details
                stay private.
              </p>
              <Form
                key={profile?.version || 0}
                fields={[
                  {
                    key: "display_name",
                    label: "Display name",
                    value: profile?.displayName || user.displayName || "",
                    min: 2,
                    max: 120,
                    full: true,
                  },
                  {
                    key: "biography",
                    label: "About you",
                    type: "textarea",
                    required: false,
                    max: 1000,
                    value: profile?.biography,
                    full: true,
                  },
                  {
                    key: "region",
                    label: "General location",
                    required: false,
                    max: 80,
                    value: profile?.region,
                  },
                  {
                    key: "availability",
                    label: "Availability",
                    options: opts([
                      "UNSPECIFIED",
                      "WEEKDAYS",
                      "WEEKENDS",
                      "FLEXIBLE",
                      "UNAVAILABLE",
                    ]),
                    value: profile?.availability || "UNSPECIFIED",
                  },
                  {
                    key: "skills",
                    label: "Skills",
                    required: false,
                    value: profile?.skills?.join(", "),
                    hint: "Separate with commas. Qualifications are verified separately.",
                    full: true,
                  },
                  {
                    key: "phone",
                    label: "Private phone",
                    required: false,
                    max: 40,
                    value: profile?.privateDetails?.phone,
                  },
                  {
                    key: "emergency_contact",
                    label: "Private emergency contact",
                    required: false,
                    max: 500,
                    value: profile?.privateDetails?.emergency_contact,
                  },
                ]}
                onSubmit={async (v) => {
                  const { phone, emergency_contact, skills, ...rest } = v;
                  await api(
                    "/api/me/profile",
                    {
                      ...rest,
                      expected_version: profile?.version ?? 0,
                      skills: (skills || "")
                        .split(",")
                        .map((s: string) => s.trim())
                        .filter(Boolean),
                      private_details: {
                        ...profile?.privateDetails,
                        phone: phone || "",
                        emergency_contact: emergency_contact || "",
                      },
                    },
                    "PUT",
                  );
                  setNotice("Your profile has been saved.");
                  scope.refresh();
                }}
              />
            </div>
          </section>
        ) : page === "privacy" ? (
          <>
            <section className="panel">
              <div className="panel-head">
                <h2>Visibility & discovery</h2>
              </div>
              <div className="panel-body">
                <Form
                  key={home.data?.directory?.version}
                  fields={[
                    {
                      key: "discoverable",
                      label: "Approved coordinators can discover my profile",
                      type: "checkbox",
                      value: home.data?.directory?.discoverable,
                      full: true,
                    },
                    {
                      key: "public_preview",
                      label: "Allow previews through a profile share link",
                      type: "checkbox",
                      value: home.data?.directory?.publicPreview,
                      full: true,
                    },
                    {
                      key: "show_availability",
                      label: "Include my availability preference",
                      type: "checkbox",
                      value: home.data?.directory?.showAvailability,
                      full: true,
                    },
                  ]}
                  submit="Save preferences"
                  onSubmit={async (v) => {
                    await api(
                      "/api/me/directory",
                      { ...v, expected_version: home.data?.directory?.version },
                      "PUT",
                    );
                    setNotice("Visibility preferences saved.");
                    scope.refresh();
                  }}
                />
              </div>
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>Profile sharing</h2>
                <Button
                  onClick={() =>
                    run(async () => {
                      const r = await api("/api/me/profile-shares", {});
                      setShare(r.token);
                    })
                  }
                >
                  Create share code
                </Button>
              </div>
              <div className="panel-body">
                {share && (
                  <label>
                    Private share code
                    <input readOnly value={share} />
                    <small>
                      Share only with the intended recipient. It expires in
                      seven days.
                    </small>
                  </label>
                )}
                {(shares.data?.items || shares.data || []).map((s: any) => (
                  <div className="detail-line" key={s.id}>
                    <span>Expires {date(s.expiresAt)}</span>
                    <Button
                      disabled={!!s.revokedAt}
                      onClick={() =>
                        run(async () => {
                          await api(
                            "/api/me/profile-shares/" + s.id,
                            {},
                            "DELETE",
                          );
                          setShare("");
                        })
                      }
                    >
                      Revoke
                    </Button>
                  </div>
                ))}
              </div>
            </section>
            <section className="panel panel-body">
              <h2>Your data</h2>
              <p>
                Export your profile and consents. Account deletion is reviewed
                separately from leaving a workspace.
              </p>
              <div className="action-list">
                <Button
                  icon="download"
                  onClick={() =>
                    run(async () => {
                      let after: string | undefined;
                      const pages = [];
                      for (let n = 0; n < 100; n++) {
                        const r = await api(
                          "/api/me/export" +
                            (after
                              ? "?after=" + encodeURIComponent(after)
                              : ""),
                        );
                        pages.push(r);
                        if (!r.next_cursor) {
                          const url = URL.createObjectURL(
                            new Blob([JSON.stringify(pages, null, 2)], {
                              type: "application/json",
                            }),
                          );
                          const a = document.createElement("a");
                          a.href = url;
                          a.download = "resq-personal-data.json";
                          a.click();
                          URL.revokeObjectURL(url);
                          setNotice(
                            "Export downloaded. Keep the file private.",
                          );
                          return;
                        }
                        after = r.next_cursor;
                      }
                      throw new UserFacingError(
                        "Export exceeds the current page limit. Contact support for a complete export.",
                      );
                    })
                  }
                >
                  Export my data
                </Button>
                <Button variant="danger" onClick={() => setDeleting(true)}>
                  Request deletion
                </Button>
                <Button
                  onClick={() =>
                    run(async () => {
                      await api("/api/me/deletion-request", {}, "DELETE");
                      setNotice("Deletion request cancelled.");
                    })
                  }
                >
                  Cancel deletion request
                </Button>
              </div>
              <Evidence value={home.data?.deletion} />
            </section>
          </>
        ) : page === "securityAccount" ? (
          <>
            <section className="panel">
              <div className="panel-head">
                <h2>Sign-in details</h2>
                <Badge status="VERIFIED" />
              </div>
              <div className="panel-body">
                <label>
                  Email
                  <input readOnly value={user.email || ""} />
                </label>
                <p>
                  Email and password are sufficient for sign-in. Workspace
                  permissions are managed separately.
                </p>
                <Button
                  onClick={() =>
                    run(async () => {
                      await sendPasswordResetEmail(
                        await identity(),
                        user.email!,
                      );
                      setNotice("Password reset email sent.");
                    })
                  }
                >
                  Change password
                </Button>
                <h3 className="section-title">Change email</h3>
                <Form
                  fields={[
                    {
                      key: "email",
                      label: "New email address",
                      type: "email",
                      full: true,
                    },
                  ]}
                  submit="Verify new email"
                  onSubmit={async (v) => {
                    await verifyBeforeUpdateEmail(
                      (await identity()).currentUser!,
                      v.email,
                    );
                    setNotice(
                      "Check the new address for a verification email.",
                    );
                  }}
                />
              </div>
            </section>
            <section className="panel panel-body">
              <h2>Sessions</h2>
              <p>
                End your sessions if you no longer trust a signed-in device.
              </p>
              <Button
                variant="danger"
                onClick={() =>
                  run(async () => {
                    await api("/api/auth/sessions/revoke", {});
                    await signOut(await identity());
                  })
                }
              >
                Revoke sessions & sign out
              </Button>
            </section>
          </>
        ) : page === "invitations" ? (
          <>
            <section className="panel">
              <div className="panel-head">
                <h2>Invitation inbox</h2>
              </div>
              {invitations.error && <Notice error>{invitations.error}</Notice>}
              {invitations.data?.length ? (
                invitations.data.map((i: any) => (
                  <div className="queue-row" key={i.id}>
                    <span>
                      <strong>
                        {i.organization?.displayName || "Workspace invitation"}
                      </strong>
                      <small>
                        {label(i.role)} · Expires {date(i.expiresAt)}
                      </small>
                    </span>
                    {!i.acceptedAt && !i.declinedAt && !i.revokedAt ? (
                      <div className="action-list">
                        <Button
                          variant="primary"
                          onClick={() =>
                            run(async () => {
                              await api(
                                "/api/me/invitations/" + i.id + "/accept",
                                {},
                              );
                              setNotice(
                                "Invitation accepted. Your workspace is now available.",
                              );
                            })
                          }
                        >
                          Accept
                        </Button>
                        <Button
                          onClick={() =>
                            run(async () => {
                              await api(
                                "/api/me/invitations/" + i.id + "/decline",
                                {},
                              );
                            })
                          }
                        >
                          Decline
                        </Button>
                      </div>
                    ) : (
                      <Badge
                        status={
                          i.acceptedAt
                            ? "ACCEPTED"
                            : i.declinedAt
                              ? "DECLINED"
                              : "REVOKED"
                        }
                      />
                    )}
                  </div>
                ))
              ) : (
                <Empty
                  title="No invitations yet"
                  body="Your organisation can invite you using your verified email address."
                />
              )}
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>Open workspaces</h2>
              </div>
              {workspaces.data?.items?.map((w: any) => (
                <div className="queue-row" key={w.id}>
                  <strong>{w.displayName}</strong>
                  <Button onClick={() => setJoin(w)}>Request to join</Button>
                </div>
              ))}
              {!workspaces.data?.items?.length && (
                <Empty
                  title="No public recruitment available"
                  body="An administrator can still invite you directly."
                />
              )}
            </section>
            <section className="panel panel-body">
              <h2>Accept an invitation code</h2>
              <Form
                fields={[
                  {
                    key: "token",
                    label: "Invitation code",
                    min: 43,
                    max: 43,
                    full: true,
                  },
                ]}
                submit="Accept invitation"
                onSubmit={async (v) => {
                  await api("/api/invitations/accept", v);
                  setNotice("Invitation accepted.");
                  scope.refresh();
                }}
              />
            </section>
          </>
        ) : (
          <>
            <section className="hero-band">
              <div>
                <span className="eyebrow">RESQOPS PRO</span>
                <h2>More capacity for your team.</h2>
                <p>
                  Review your entitlement, assistant usage and volunteer
                  sponsorship.
                </p>
              </div>
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>Your subscription</h2>
              </div>
              <div className="panel-body">
                {subscription.error ? (
                  <Notice error>{subscription.error}</Notice>
                ) : (
                  <Evidence value={subscription.data} />
                )}
                <p>
                  Purchase and manage your subscription in the Android app.
                  Entitlements sync to this workspace.
                </p>
                <Link className="button" href="/team-pro">
                  Manage sponsored volunteers
                </Link>
              </div>
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>Usage</h2>
              </div>
              <div className="panel-body">
                {usage.error ? (
                  <Notice error>{usage.error}</Notice>
                ) : (
                  <Evidence value={usage.data} />
                )}
              </div>
            </section>
          </>
        )}
        {["invitations", "privacy"].includes(page) && (
          <AccountAccess
            scope={scope}
            revision={revision}
            privacy={page === "privacy"}
          />
        )}
        {join && (
          <Drawer
            title={"Join " + join.displayName}
            onClose={() => setJoin(null)}
          >
            <Form
              fields={[
                {
                  key: "message",
                  label: "Introduce yourself",
                  type: "textarea",
                  min: 10,
                  max: 1000,
                  full: true,
                },
              ]}
              submit="Request membership"
              onSubmit={async (v) => {
                await api("/api/me/join-requests", {
                  organization_id: join.id,
                  ...v,
                });
                setJoin(null);
                setNotice("Request submitted for review.");
              }}
            />
          </Drawer>
        )}
        {deleting && (
          <Drawer
            title="Request account deletion"
            onClose={() => setDeleting(false)}
          >
            <p>
              This starts a privacy review. Operational records may need to be
              retained under the applicable retention policy.
            </p>
            <Form
              fields={[
                {
                  key: "confirmation",
                  label: "Type DELETE to confirm",
                  full: true,
                },
              ]}
              submit="Request deletion"
              onSubmit={async (v) => {
                if (v.confirmation !== "DELETE")
                  throw new UserFacingError("Type DELETE to confirm.");
                await api("/api/me/deletion-request", {});
                setDeleting(false);
                scope.refresh();
                setNotice("Deletion request submitted.");
              }}
            />
          </Drawer>
        )}
      </div>
    </div>
  );
}
