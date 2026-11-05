"use client";

export type HarnessData = {
  plan?: { title: string; status: string }[];
  notes?: string[];
  steps?: { tool: string; at?: string; duration_ms?: number; ok: boolean; error?: string }[];
  rounds?: number;
};

export function evidenceStepLabel(name:string){
  const labels:Record<string,string>={analyze_weather_window:'Review weather forecasts',read_local_reports:'Check local reports',read_weather_evidence:'Review weather evidence',read_mission_requirements:'Check mission requirements',find_places:'Find the location',read_candidates:'Check volunteer availability',read_stock:'Check equipment stock',read_commitments:'Check existing commitments',read_mission:'Review the mission',read_weather_assessment:'Review the weather assessment',update_plan:'Update the analysis plan',record_note:'Record a finding'};
  return labels[name] || 'Check supporting information';
}

export function HarnessSteps({ harness }: { harness?: HarnessData | null }) {
  if (!harness || (!harness.steps?.length && !harness.plan?.length && !harness.notes?.length))
    return null;
  return (
    <div aria-label="Analysis progress">
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
            Checks performed
          </h4>
          <ol>
            {harness.steps.map((step, i) => (
              <li key={i}>
                {evidenceStepLabel(step.tool)}
                {step.ok ? " · Check finished" : " · Could not finish"}
              </li>
            ))}
          </ol>
        </>
      )}
    </div>
  );
}
