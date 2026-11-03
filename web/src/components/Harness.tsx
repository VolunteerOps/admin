"use client";

export type HarnessData = {
  plan?: { title: string; status: string }[];
  notes?: string[];
  steps?: { tool: string; at?: string; duration_ms?: number; ok: boolean; error?: string }[];
  rounds?: number;
};

export function HarnessSteps({ harness }: { harness?: HarnessData | null }) {
  if (!harness || (!harness.steps?.length && !harness.plan?.length && !harness.notes?.length))
    return null;
  return (
    <div aria-label="Agent harness">
      {!!harness.plan?.length && (
        <>
          <h4 className="section-title">Agent plan</h4>
          <ul>
            {harness.plan.map((step, i) => (
              <li key={i}>
                {step.status === "done" ? "✓" : step.status === "in_progress" ? "▶" : "○"}{" "}
                {step.title}
              </li>
            ))}
          </ul>
        </>
      )}
      {!!harness.notes?.length && (
        <>
          <h4 className="section-title">Working notes</h4>
          <ul>
            {harness.notes.map((note, i) => (
              <li key={i}>{note}</li>
            ))}
          </ul>
        </>
      )}
      {!!harness.steps?.length && (
        <>
          <h4 className="section-title">
            Tool steps{harness.rounds ? ` · ${harness.rounds} rounds` : ""}
          </h4>
          <ol>
            {harness.steps.map((step, i) => (
              <li key={i}>
                <code>{step.tool}</code>
                {step.ok ? "" : ` — failed${step.error ? ": " + step.error : ""}`}
                {typeof step.duration_ms === "number" ? ` · ${step.duration_ms} ms` : ""}
              </li>
            ))}
          </ol>
        </>
      )}
    </div>
  );
}
