"use client";
import { useEffect, useRef, useState } from "react";
import { api, ApiError, errorMessage } from "@/lib/api";
import type { Scope } from "@/lib/hooks";
import { Button, Icon, Notice } from "./ui";

type Turn = { role: "user" | "assistant"; content: string };
type Plan = {
  reply: string;
  draft: Record<string, any>;
  ready: boolean;
  notes: string[];
  issues: { field: string }[];
};
type Pending = { message: string; key: string };
const fieldLabels: Record<string, string> = {
  title: "Mission name",
  brief: "Purpose",
  starts_at: "Starts",
  ends_at: "Ends",
  capacity: "Volunteers needed",
  meeting_point: "Meeting point",
  required_skills: "Skills needed",
  longitude: "Map location",
  latitude: "Map location",
};
const starters = [
  "Plan a community preparedness visit",
  "Organise a relief distribution",
  "Coordinate an emergency response",
];

export default function MissionChat({
  scope,
  done,
}: {
  scope: Scope;
  done: () => void;
}) {
  const [message, setMessage] = useState("");
  const [history, setHistory] = useState<Turn[]>([]);
  const [pending, setPending] = useState<Pending | null>(null);
  const [result, setResult] = useState<Plan | null>(null);
  const [busy, setBusy] = useState(false),
    [saving, setSaving] = useState(false),
    [review, setReview] = useState(false);
  const [error, setError] = useState("");
  const lock = useRef(false),
    transcript = useRef<HTMLDivElement>(null),
    composer = useRef<HTMLTextAreaElement>(null);
  const createKey = useRef(crypto.randomUUID());
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  useEffect(() => {
    const el = transcript.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [history, pending, busy, review]);
  useEffect(() => {
    const el = composer.current;
    if (el) {
      el.style.height = "auto";
      el.style.height = Math.min(el.scrollHeight, 144) + "px";
    }
  }, [message]);
  async function send(retry?: Pending) {
    const outgoing = retry ?? {
      message: message.trim(),
      key: crypto.randomUUID(),
    };
    if (lock.current || !outgoing.message) return;
    lock.current = true;
    setBusy(true);
    setError("");
    setPending(outgoing);
    setMessage("");
    setReview(false);
    try {
      const answer = await api(
        scope.sentinel + "/conversations/mission",
        {
          message: outgoing.message,
          history: history.slice(-20),
          draft: result?.draft || {},
          timezone,
        },
        "POST",
        undefined,
        outgoing.key,
      );
      setHistory((previous) => [
        ...previous,
        { role: "user", content: outgoing.message },
        { role: "assistant", content: answer.reply },
      ]);
      setResult(answer);
      setPending(null);
      createKey.current = crypto.randomUUID();
    } catch (e) {
      setError(errorMessage(e));
      // Uncertain network failures retain their key so a completed answer can be replayed.
      if (
        e instanceof ApiError &&
        [
          "AGENT_UNAVAILABLE",
          "ANALYSIS_FAILED",
          "ANALYSIS_INTERRUPTED",
          "META_NOT_CONFIGURED",
          "MODEL_UNAVAILABLE",
          "META_ACCESS_DENIED",
        ].includes(e.code)
      )
        setPending({ ...outgoing, key: crypto.randomUUID() });
    } finally {
      lock.current = false;
      setBusy(false);
      composer.current?.focus();
    }
  }
  async function create() {
    if (lock.current || !result?.ready || pending || message.trim()) return;
    lock.current = true;
    setSaving(true);
    setError("");
    try {
      await api(
        scope.ops + "/commands",
        { action: "CREATE_MISSION", mission: result.draft },
        "POST",
        undefined,
        createKey.current,
      );
      done();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      lock.current = false;
      setSaving(false);
    }
  }
  return (
    <section className="mission-chat" aria-label="Mission planning chat">
      <div className="mission-chat-context">
        <span>
          <Icon name="missions" size={17} /> Mission planner
        </span>
        <span>Review before creating</span>
      </div>
      <div className="mission-chat-transcript" ref={transcript}>
        <div
          role="log"
          aria-label="Mission conversation"
          aria-live="polite"
          aria-relevant="additions text"
        >
          <div className="mission-message assistant">
            <span className="mission-speaker">Mission planner</span>
            <p>What are you planning, and what should the team achieve?</p>
            <p className="mission-message-hint">
              Tell me what you know. We’ll work through the details together.
            </p>
          </div>
          {!history.length && !pending && (
            <div className="mission-starters" aria-label="Ways to start">
              {starters.map((text) => (
                <button
                  type="button"
                  key={text}
                  onClick={() => {
                    setMessage(text);
                    composer.current?.focus();
                  }}
                >
                  {text}
                  <Icon name="arrow" size={16} />
                </button>
              ))}
            </div>
          )}
          {history.map((turn, i) => (
            <div className={"mission-message " + turn.role} key={i}>
              <span className="mission-speaker">
                {turn.role === "user" ? "You" : "Mission planner"}
              </span>
              <p>{turn.content}</p>
            </div>
          ))}
          {pending && (
            <div className="mission-message user">
              <span className="mission-speaker">You</span>
              <p>{pending.message}</p>
            </div>
          )}
          {busy && (
            <div className="mission-thinking" role="status">
              <span className="mission-thinking-dots" aria-hidden>
                <i />
                <i />
                <i />
              </span>
              Working on your plan…
            </div>
          )}
        </div>
        {result && !pending && (
          <div className="mission-draft">
            <button
              type="button"
              className="mission-draft-toggle"
              onClick={() => setReview(!review)}
              aria-expanded={review}
            >
              <Icon name="file" />
              <span>
                <strong>{result.draft.title || "Your mission draft"}</strong>
                <small>
                  {result.ready
                    ? "Ready for your review"
                    : "Taking shape as we talk"}
                </small>
              </span>
              <span>
                {review
                  ? "Hide"
                  : result.ready
                    ? "Review mission"
                    : "View draft"}
              </span>
            </button>
            {review && (
              <div className="mission-draft-content">
                <dl>
                  {Object.entries(fieldLabels)
                    .filter(
                      ([key]) =>
                        key !== "longitude" &&
                        key !== "latitude" &&
                        result.draft[key] != null,
                    )
                    .map(([key, label]) => (
                      <div key={key}>
                        <dt>{label}</dt>
                        <dd>
                          {["starts_at", "ends_at"].includes(key)
                            ? new Date(result.draft[key]).toLocaleString(
                                undefined,
                                { dateStyle: "medium", timeStyle: "short" },
                              )
                            : Array.isArray(result.draft[key])
                              ? result.draft[key].join(", ") ||
                                "No specific skills"
                              : String(result.draft[key])}
                        </dd>
                      </div>
                    ))}
                </dl>
                {result.draft.latitude != null &&
                  result.draft.longitude != null && (
                    <p className="mission-review-note">
                      Map location: {result.draft.latitude},{" "}
                      {result.draft.longitude}
                    </p>
                  )}
                <p className="mission-review-note">
                  Times shown in {timezone}.
                </p>
                {!!result.notes?.length && (
                  <ul>
                    {result.notes.map((note, i) => (
                      <li key={i}>{note}</li>
                    ))}
                  </ul>
                )}
                {!!result.issues?.length && (
                  <p>
                    Still to confirm:{" "}
                    {[
                      ...new Set(
                        result.issues.map(
                          (i) => fieldLabels[i.field] || "mission details",
                        ),
                      ),
                    ].join(", ")}
                    .
                  </p>
                )}
                {result.ready && (
                  <>
                    <p className="mission-review-note">
                      Check the details above. Creating this mission does not
                      assign or dispatch volunteers.
                    </p>
                    <Button
                      variant="gold"
                      disabled={saving || !!message.trim()}
                      onClick={create}
                    >
                      {saving
                        ? "Creating mission…"
                        : "Confirm and create mission"}
                    </Button>
                    {!!message.trim() && (
                      <p className="mission-review-note">
                        Send your changes before creating the mission.
                      </p>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </div>
      <div className="mission-compose-area">
        {error && (
          <div className="mission-chat-error">
            <Notice error>{error}</Notice>
            {pending && (
              <div className="mission-retry">
                <Button disabled={busy} onClick={() => send(pending)}>
                  Try again
                </Button>
                <Button
                  disabled={busy}
                  onClick={() => {
                    setMessage(pending.message);
                    setPending(null);
                    setError("");
                    composer.current?.focus();
                  }}
                >
                  Edit message
                </Button>
              </div>
            )}
          </div>
        )}
        <form
          className="mission-composer"
          onSubmit={(e) => {
            e.preventDefault();
            void send();
          }}
        >
          <textarea
            ref={composer}
            aria-label="Message the mission planner"
            placeholder={
              pending
                ? "Retry or edit your message above"
                : "Tell me about the mission…"
            }
            rows={2}
            value={message}
            maxLength={4000}
            disabled={busy || saving || !!pending}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => {
              if (
                e.key === "Enter" &&
                !e.shiftKey &&
                !e.nativeEvent.isComposing
              ) {
                e.preventDefault();
                void send();
              }
            }}
          />
          <Button
            type="submit"
            variant="primary"
            icon="arrow"
            aria-label="Send message"
            disabled={busy || saving || !!pending || !message.trim()}
          />
        </form>
        <div className="mission-composer-help">
          <span>Enter to send · Shift + Enter for a new line</span>
          <span>{message.length}/4,000</span>
        </div>
      </div>
    </section>
  );
}
