import { Button } from "./ui";

export default function PlanEditor({
  value,
  onChange,
  snapshot,
}: {
  value: any;
  onChange: (v: any) => void;
  snapshot: any;
}) {
  const candidates = (snapshot?.candidates || []).filter(
    (c: any) => c.eligible,
  );
  const resources = snapshot?.resources || [];
  function update(index: number, change: any) {
    onChange({
      ...value,
      allocations: value.allocations.map((a: any, i: number) =>
        i === index ? { ...a, ...change } : a,
      ),
    });
  }
  const unused = candidates.filter(
    (c: any) => !value.allocations.some((a: any) => a.user_id === c.user_id),
  );
  return (
    <div className="stack">
      <label>
        Proposal summary
        <textarea
          required
          maxLength={3000}
          value={value.summary}
          onChange={(e) => onChange({ ...value, summary: e.target.value })}
        />
      </label>
      {value.allocations.map((a: any, i: number) => (
        <section className="panel panel-body" key={i}>
          <div className="panel-head">
            <h3>Allocation {i + 1}</h3>
            <Button
              type="button"
              onClick={() =>
                onChange({
                  ...value,
                  allocations: value.allocations.filter(
                    (_: any, j: number) => j !== i,
                  ),
                })
              }
            >
              Remove allocation
            </Button>
          </div>
          <label>
            Eligible responder
            <select
              value={a.user_id}
              onChange={(e) => update(i, { user_id: Number(e.target.value) })}
            >
              {candidates
                .filter(
                  (c: any) =>
                    c.user_id === a.user_id ||
                    !value.allocations.some(
                      (x: any) => x.user_id === c.user_id,
                    ),
                )
                .map((c: any) => (
                  <option value={c.user_id} key={c.user_id}>
                    Responder {c.user_id}
                  </option>
                ))}
            </select>
          </label>
          <label>
            Allocation rationale
            <textarea
              required
              maxLength={800}
              value={a.reason}
              onChange={(e) => update(i, { reason: e.target.value })}
            />
          </label>
          {a.supplies.map((s: any, j: number) => {
            const resource = resources.find((r: any) => r.id === s.resource_id);
            const other = value.allocations.reduce(
              (sum: number, row: any, n: number) =>
                sum +
                row.supplies.reduce(
                  (v: number, x: any, m: number) =>
                    v +
                    (x.resource_id === s.resource_id && (n !== i || m !== j)
                      ? x.quantity
                      : 0),
                  0,
                ),
              0,
            );
            return (
              <div className="panel panel-body" key={s.resource_id}>
                <strong>{resource?.title || "Resource"}</strong>
                <small>
                  {resource?.location} ·{" "}
                  {Math.max(0, (resource?.available || 0) - other)} available
                  after other allocations
                </small>
                <label>
                  Quantity
                  <input
                    type="number"
                    required
                    min={1}
                    max={Math.max(0, (resource?.available || 0) - other)}
                    step={1}
                    value={s.quantity}
                    onChange={(e) =>
                      update(i, {
                        supplies: a.supplies.map((x: any, n: number) =>
                          n === j
                            ? { ...x, quantity: Number(e.target.value) }
                            : x,
                        ),
                      })
                    }
                  />
                </label>
                <label>
                  Equipment rationale
                  <input
                    required
                    maxLength={500}
                    value={s.reason}
                    onChange={(e) =>
                      update(i, {
                        supplies: a.supplies.map((x: any, n: number) =>
                          n === j ? { ...x, reason: e.target.value } : x,
                        ),
                      })
                    }
                  />
                </label>
                <Button
                  type="button"
                  onClick={() =>
                    update(i, {
                      supplies: a.supplies.filter(
                        (_: any, n: number) => n !== j,
                      ),
                    })
                  }
                >
                  Remove supply
                </Button>
              </div>
            );
          })}
          <label>
            Add equipment
            <select
              value=""
              onChange={(e) => {
                if (e.target.value)
                  update(i, {
                    supplies: [
                      ...a.supplies,
                      { resource_id: e.target.value, quantity: 1, reason: "" },
                    ],
                  });
              }}
            >
              <option value="">Select available equipment</option>
              {resources
                .filter(
                  (r: any) =>
                    r.available > 0 &&
                    !a.supplies.some((s: any) => s.resource_id === r.id),
                )
                .map((r: any) => (
                  <option value={r.id} key={r.id}>
                    {r.title} · {r.location}
                  </option>
                ))}
            </select>
          </label>
        </section>
      ))}
      <Button
        type="button"
        icon="plus"
        disabled={
          !unused.length ||
          value.allocations.length >= 20 ||
          value.allocations.length >=
            Math.max(
              0,
              (snapshot?.mission?.capacity || 0) -
                (snapshot?.mission?.assigned_count || 0),
            )
        }
        onClick={() =>
          onChange({
            ...value,
            allocations: [
              ...value.allocations,
              { user_id: unused[0].user_id, reason: "", supplies: [] },
            ],
          })
        }
      >
        Add eligible responder
      </Button>
      {["shortages", "decisions"].map((k) => (
        <label key={k}>
          {k === "shortages" ? "Remaining shortages" : "Coordinator decisions"}
          <textarea
            value={value[k].join("\n")}
            onChange={(e) =>
              onChange({
                ...value,
                [k]: e.target.value.split("\n").slice(0, 20),
              })
            }
          />
          <small>One item per line.</small>
        </label>
      ))}
    </div>
  );
}
