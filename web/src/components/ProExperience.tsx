"use client";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  CheckCheck,
  ChevronRight,
  FileText,
  Flag,
  HeartHandshake,
  Layers,
  LockKeyhole,
  Radio,
  ShieldCheck,
  Sparkles,
  Users,
  X,
  Boxes,
  BookOpen,
} from "lucide-react";
import { api, errorMessage } from "@/lib/api";
import { Notice, lockPageScroll } from "./ui";
import PromoCode from "./PromoCode";
export const proFeatures = [
  {
    id: "briefing",
    title: "Understand the day",
    label: "AI briefings",
    description:
      "Turn scattered reports into a clear summary of what needs attention.",
    icon: Radio,
    prompt:
      "What needs our attention today? Summarize the evidence and what is still unknown.",
  },
  {
    id: "planning",
    title: "Find a starting point",
    label: "Response planning",
    description:
      "Get a first draft of a response plan, ready for your team to review.",
    icon: Flag,
    prompt:
      "Help me prepare the Tana River response. Suggest a draft plan and review steps.",
  },
  {
    id: "resources",
    title: "Spot the gaps earlier",
    label: "Resource insights",
    description:
      "Ask where people and supplies may fall short before you make a decision.",
    icon: Boxes,
    prompt:
      "Where are our resource gaps? Use saved mission and inventory records.",
  },
  {
    id: "reports",
    title: "Make reports easier to use",
    label: "Report summaries",
    description:
      "Bring field observations together and see what still needs checking.",
    icon: FileText,
    prompt:
      "Summarize the field reports and distinguish reported facts from missing evidence.",
  },
];
export function ProBadge() {
  return (
    <span className="pro-badge">
      <Sparkles size={11} />
      PRO
    </span>
  );
}
export function ProGate({
  feature = "briefing",
  onClose,
}: {
  feature?: string;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const item = proFeatures.find((f) => f.id === feature) || proFeatures[0];
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const element = dialog.current;
    element?.showModal();
    const unlock = lockPageScroll();
    return () => {
      unlock();
      element?.close();
      previous?.focus();
    };
  }, []);
  return (
    <dialog
      className="pro-gate"
      ref={dialog}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      aria-labelledby="pro-gate-title"
    >
      <button
        className="pro-close"
        onClick={onClose}
        aria-label="Close upgrade screen"
      >
        <X size={20} />
      </button>
      <div className="pro-gate-art">
        <ProBadge />
        <div className="pro-orbit">
          <span />
          <span />
          <div>
            <item.icon size={38} strokeWidth={1.3} />
          </div>
          <i>
            <Sparkles size={16} />
          </i>
        </div>
        <p>MORE CLARITY. MORE TIME FOR PEOPLE.</p>
      </div>
      <div className="pro-gate-content">
        <span className="pro-eyebrow">{item.label} · ResQOps Pro</span>
        <h2 id="pro-gate-title">
          {item.title}.<br />
          With a little help from AI.
        </h2>
        <p>{item.description} All AI-powered features are included in Pro.</p>
        <ul className="pro-checks">
          <li>
            <Check size={16} />
            Ask Sentinel in everyday language
          </li>
          <li>
            <Check size={16} />
            Review the evidence behind suggestions
          </li>
          <li>
            <Check size={16} />
            Keep every decision in your hands
          </li>
        </ul>
        <Link className="pro-button" href={"/pro?feature=" + item.id}>
          Explore ResQOps Pro <ArrowRight size={17} />
        </Link>
        <button className="pro-text" onClick={onClose}>
          Keep using Standard
        </button>
        <small>Payment and eligibility are checked before activation.</small>
      </div>
    </dialog>
  );
}
function Brand() {
  return (
    <Link className="pro-brand" href="/overview">
      <ShieldCheck size={28} strokeWidth={1.5} />
      <span>
        ResQ<span>Ops</span>
      </span>
      <ProBadge />
    </Link>
  );
}
function BriefingArt({ kind = 0 }: { kind?: number }) {
  return (
    <div
      className={"pro-art pro-art-" + kind}
      aria-label="Illustrative Pro feature preview"
    >
      <div className="art-orbit orbit-a" />
      <div className="art-orbit orbit-b" />
      <div className="art-satellite">
        <Sparkles size={22} />
      </div>
      <div className="pro-evidence">
        <span>
          <Radio size={14} />
          FIELD REPORT
        </span>
        <b>Coastal rainfall</b>
        <small>Practice observation · Kilifi</small>
        <div className="evidence-lines">
          <i />
          <i />
          <i />
        </div>
      </div>
      <div className="pro-example">
        <div className="example-top">
          <span className="pro-ai-icon">
            <Sparkles size={20} />
          </span>
          <div>
            <b>
              {
                [
                  "Your morning, made clearer",
                  "A plan you can build on",
                  "A clearer view of your supplies",
                ][kind]
              }
            </b>
            <small>ASK SENTINEL · ILLUSTRATIVE EXAMPLE</small>
          </div>
          <span className="example-dot" />
        </div>
        {kind === 0 ? (
          <>
            <div className="example-item">
              <span className="example-number">01</span>
              <div>
                <b>Start with the latest reports</b>
                <p>
                  Review the coastal rainfall observation with your field team.
                </p>
              </div>
            </div>
            <div className="example-item">
              <span className="example-number">02</span>
              <div>
                <b>Check what is still missing</b>
                <p>Confirm local conditions before changing a response plan.</p>
              </div>
            </div>
            <div className="example-source">
              <FileText size={13} />
              Reports → Evidence → Your review
            </div>
          </>
        ) : kind === 1 ? (
          <>
            <div className="example-item">
              <CheckCheck size={20} />
              <div>
                <b>1. Confirm the need</b>
                <p>Review reports with the local coordinator.</p>
              </div>
            </div>
            <div className="example-item">
              <Users size={20} />
              <div>
                <b>2. Review the team</b>
                <p>Check availability, skills and safe access.</p>
              </div>
            </div>
            <div className="example-source">
              <ShieldCheck size={13} />
              Draft only · You approve the next step
            </div>
          </>
        ) : (
          <>
            <div className="supply-line">
              <span>Water kits</span>
              <b>Review stock</b>
            </div>
            <div className="supply-track">
              <i />
            </div>
            <div className="supply-line">
              <span>People & equipment</span>
              <b>Check availability</b>
            </div>
            <div className="supply-track second">
              <i />
            </div>
            <p className="example-resource-note">
              Compare saved needs with available supplies before making
              assignments.
            </p>
            <div className="example-source">
              <Boxes size={13} />
              Illustration · Not an inventory assessment
            </div>
          </>
        )}
      </div>
      <div className="pro-human">
        <span>
          <ShieldCheck size={17} />
        </span>
        <div>
          <b>You stay in control</b>
          <small>Every suggestion needs your review.</small>
        </div>
        <Check size={17} />
      </div>
    </div>
  );
}
const guideSteps = [
  {
    title: "Start with a clearer picture.",
    description:
      "Open Ask Sentinel from your overview or radar. Ask for a briefing or a summary of field reports in your own words.",
    tip: "Try asking",
    prompt: "What needs our attention today?",
    where: "Overview & Sentinel",
    icon: Radio,
  },
  {
    title: "Turn a question into a first draft.",
    description:
      "Ask for help preparing a response. Review the evidence, check what is missing and adapt the draft with your team.",
    tip: "Try asking",
    prompt: "Help me prepare the Tana River response.",
    where: "Missions",
    icon: Flag,
  },
  {
    title: "Check the gaps. Then decide.",
    description:
      "Ask Sentinel to compare saved needs with available supplies. Confirm quantities and team availability before assigning anything.",
    tip: "Try asking",
    prompt: "Where are our resource gaps?",
    where: "Resources",
    icon: Boxes,
  },
];
export default function ProExperience({
  guide = false,
  revision = 0,
}: {
  guide?: boolean;
  revision?: number;
}) {
  const [screen, setScreen] = useState<"plans" | "review" | "guide" | "done">(
      guide ? "guide" : "plans",
    ),
    [step, setStep] = useState(0),
    [pro, setPro] = useState(false),
    [loaded, setLoaded] = useState(false),
    [failure, setFailure] = useState(""),
    [promotion, setPromotion] = useState<any>(null),
    [checkoutBusy, setCheckoutBusy] = useState(false),
    [checkoutNotice, setCheckoutNotice] = useState(""),
    [feature, setFeature] = useState("briefing");
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    if (!guide && new URLSearchParams(location.search).get("review") === "1")
      setScreen("review");
    const f = new URLSearchParams(location.search).get("feature");
    if (proFeatures.some((x) => x.id === f)) {
      setFeature(f!);
      if (guide) setStep(f === "planning" ? 1 : f === "resources" ? 2 : 0);
    }
    api("/api/me/subscription")
      .then((j) => setPro(j.resqops_pro === true))
      .catch((e) => setFailure(errorMessage(e)))
      .finally(() => setLoaded(true));
  }, [guide, revision]);
  function change(next: typeof screen) {
    setScreen(next);
    window.scrollTo({ top: 0, behavior: "instant" });
    setTimeout(() => heading.current?.focus(), 0);
  }
  function advance(n: number) {
    setStep(n);
    setTimeout(() => heading.current?.focus(), 0);
  }
  async function finish() {
    try {
      const pref = await api("/api/me/portal-preferences");
      if (pref.payload) {
        const p = pref.payload;
        await api(
          "/api/me/portal-preferences",
          {
            expected_version: pref.version,
            name: p.name,
            role: p.role,
            organization: p.organization,
            coverage: p.coverage,
            briefings: p.briefings,
            reminders: p.reminders,
            step: p.step,
            completed: p.completed,
            pro_tour_completed: true,
          },
          "PUT",
        );
      }
    } catch (e) {
      setFailure(errorMessage(e));
      return;
    }
    change("done");
  }
  async function checkout() {
    setCheckoutBusy(true);
    setFailure("");
    setCheckoutNotice("");
    try {
      if (promotion?.percent === 100) {
        const result = await api("/api/billing/promotions/redeem", {
          code: promotion.code,
        });
        if (!result.activated) {
          setCheckoutNotice("This promotion has already ended.");
          return;
        }
        setPro(true);
        change("guide");
        return;
      }
      const result = await api(
        "/api/billing/web-checkout",
        promotion ? { code: promotion.code } : {},
      );
      if (result.url) window.location.assign(result.url);
      else
        setCheckoutNotice(
          "Online checkout is not available yet. You can keep using Standard or redeem an eligible no-charge promotion.",
        );
    } catch (e) {
      setFailure(errorMessage(e));
    } finally {
      setCheckoutBusy(false);
    }
  }
  const current = guideSteps[step];
  return (
    <div className="pro-shell">
      {failure && <Notice error>{failure}</Notice>}
      <header className="pro-header">
        <Brand />
        <Link className="pro-back" href="/overview">
          <ArrowLeft size={15} />
          Back to workspace
        </Link>
      </header>
      {screen === "plans" ? (
        <>
          <section className="pro-main">
            <div className="pro-intro">
              <span className="pro-eyebrow">
                <span className="pro-tiny-star">✦</span> A LITTLE EXTRA SUPPORT.
                A BIGGER DIFFERENCE.
              </span>
              <h1 ref={heading} tabIndex={-1}>
                More clarity.
                <br />
                <em>More time for people.</em>
              </h1>
              <p>
                Meet ResQOps Pro. Your AI companion for making sense of
                information, preparing a response and keeping your team a step
                ahead.
              </p>
              <div className="pro-intro-proof">
                <span>
                  <ShieldCheck size={16} />
                  You make the decisions
                </span>
                <span>
                  <Sparkles size={16} />
                  Muse Spark & Muse Wonder
                </span>
              </div>
              <Link className="pro-text" href="/pro-guide">
                Take a quick feature tour <ArrowUpRight size={15} />
              </Link>
            </div>
            <BriefingArt />
            <section
              className="pro-plans"
              aria-label="Compare Standard and Pro"
            >
              <div className="standard-plan">
                <div className="plan-top">
                  <Layers size={21} />
                  <span>{pro ? "CORE WORKSPACE" : "YOUR CURRENT PLAN"}</span>
                </div>
                <h2>ResQOps Standard</h2>
                <p>The essentials for everyday coordination.</p>
                <ul className="pro-checks">
                  <li>
                    <Check size={16} />
                    Missions & volunteer coordination
                  </li>
                  <li>
                    <Check size={16} />
                    Resources, reports & preparedness
                  </li>
                  <li>
                    <Check size={16} />
                    Radar & source evidence
                  </li>
                </ul>
                <Link className="pro-standard-link" href="/overview">
                  Continue with Standard <ArrowRight size={16} />
                </Link>
              </div>
              <div className="premium-plan">
                <div className="plan-top">
                  <Sparkles size={21} />
                  <span>ALL AI-POWERED FEATURES</span>
                  <ProBadge />
                </div>
                <h2>ResQOps Pro</h2>
                <p>Everything in Standard, with a helping hand from AI.</p>
                <div className="premium-benefits">
                  {proFeatures.map((f) => (
                    <div key={f.id}>
                      <Check size={15} />
                      <span>{f.label}</span>
                    </div>
                  ))}
                </div>
                <div className="pro-purchase-row">
                  <div>
                    <b>{pro ? "Pro is active" : "Choose your next step"}</b>
                    <small>
                      {pro
                        ? "Your subscription is verified."
                        : "Review payment or enter a promotion."}
                    </small>
                  </div>
                  <button
                    className="pro-button gold"
                    disabled={!loaded}
                    onClick={() => change(pro ? "guide" : "review")}
                  >
                    {!loaded
                      ? "Checking plan…"
                      : pro
                        ? "Explore your Pro tools"
                        : "Review upgrade"}
                    <ArrowRight size={17} />
                  </button>
                </div>
              </div>
            </section>
            <section className="pro-features">
              <div className="pro-section-title">
                <span className="pro-eyebrow">
                  LESS TIME PIECING THINGS TOGETHER
                </span>
                <h2>A helping hand, wherever you need it.</h2>
                <p>
                  One assistant. Four useful ways to move your work forward.
                </p>
              </div>
              <div className="pro-feature-grid">
                {proFeatures.map((f) => (
                  <article key={f.id}>
                    <span className="pro-feature-icon">
                      <f.icon size={21} />
                    </span>
                    <h3>{f.title}</h3>
                    <p>{f.description}</p>
                    <Link href={"/pro-guide?feature=" + f.id}>
                      See how it works <ArrowUpRight size={14} />
                    </Link>
                  </article>
                ))}
              </div>
            </section>
            <section className="pro-reassurance">
              <HeartHandshake size={32} strokeWidth={1.4} />
              <div>
                <h3>Technology supports the work. People lead it.</h3>
                <p>
                  Pro helps you prepare and understand. It does not approve
                  missions, dispatch teams or send messages.
                </p>
              </div>
            </section>
            <section className="pro-faq">
              <h2>A few things you might be wondering</h2>
              {[
                [
                  "What stays in Standard?",
                  "Your core coordination tools: missions, volunteers, resources, radar, source evidence, reports, training and preparedness. AI assistance is the Pro upgrade.",
                ],
                [
                  "Does Pro make decisions for me?",
                  "No. Suggestions are drafts for you to check. Your team stays responsible for verifying conditions and approving actions.",
                ],
                [
                  "Can I subscribe today?",
                  "Paid subscriptions require a connected checkout. You can also redeem a valid no-charge promotion. Viewing the tour never activates Pro.",
                ],
                [
                  "Will Pro turn on live emergency data?",
                  "No. Pro access and data connections are separate. Sentinel reports missing or unavailable sources. A subscription never makes unavailable forecasts appear.",
                ],
              ].map(([q, a]) => (
                <details key={q}>
                  <summary>
                    {q}
                    <ChevronRight size={16} />
                  </summary>
                  <p>{a}</p>
                </details>
              ))}
            </section>
          </section>
        </>
      ) : screen === "review" ? (
        <section className="pro-review">
          <button className="pro-text" onClick={() => change("plans")}>
            <ArrowLeft size={15} />
            Back to plans
          </button>
          <div className="review-heading">
            <span className="pro-feature-icon">
              <Sparkles size={27} />
            </span>
            <span className="pro-eyebrow">YOUR NEXT STEP</span>
            <h1 ref={heading} tabIndex={-1}>
              A little more support.
              <br />
              For the work that matters.
            </h1>
            <p>Here’s what ResQOps Pro brings to your workspace.</p>
          </div>
          <div className="review-grid">
            <div className="review-includes">
              <h2>All your AI tools, together.</h2>
              {proFeatures.map((f) => (
                <div key={f.id}>
                  <span className="pro-feature-icon">
                    <f.icon size={19} />
                  </span>
                  <div>
                    <h3>{f.label}</h3>
                    <p>{f.description}</p>
                  </div>
                  <Check size={17} />
                </div>
              ))}
            </div>
            <aside className="review-order">
              <div className="row-between">
                <h2>ResQOps Pro</h2>
                <ProBadge />
              </div>
              <p>AI assistance for your coordination workspace.</p>
              <dl>
                <div>
                  <dt>Subscription price</dt>
                  <dd>
                    {promotion?.percent === 100
                      ? "Covered by promotion"
                      : "Shown at secure checkout"}
                  </dd>
                </div>
                <div>
                  <dt>Billing & renewal</dt>
                  <dd>
                    {promotion?.percent === 100
                      ? `${promotion.days} days · no renewal`
                      : "Review before payment"}
                  </dd>
                </div>
                <div>
                  <dt>Payment today</dt>
                  <dd>
                    {promotion?.percent === 100
                      ? "No charge"
                      : promotion
                        ? `${promotion.percent}% discount at checkout`
                        : "Review before payment"}
                  </dd>
                </div>
              </dl>
              <PromoCode onApplied={setPromotion} />
              <button
                className="pro-button"
                disabled={checkoutBusy}
                onClick={() => void checkout()}
              >
                <LockKeyhole size={16} />
                {checkoutBusy
                  ? "Checking…"
                  : promotion?.percent === 100
                    ? "Activate promotional access"
                    : "Continue to secure checkout"}
              </button>
              {checkoutNotice && <Notice>{checkoutNotice}</Notice>}
              <p className="review-note">
                Payment details are handled by the payment provider. Partial
                discounts unlock Pro only after confirmed payment.
              </p>
              <div className="review-divider">
                <span>TAKE A LOOK INSIDE</span>
              </div>
              <Link
                className="pro-button outline"
                href={"/pro-guide?feature=" + feature}
              >
                Preview the Pro welcome tour <ArrowRight size={16} />
              </Link>
              <small>
                The tour shows examples. It does not unlock AI access.
              </small>
            </aside>
          </div>
          <Link className="pro-text" href="/overview">
            Continue using Standard <ArrowRight size={15} />
          </Link>
        </section>
      ) : screen === "guide" ? (
        <section className="pro-guide">
          <div className="guide-art-panel">
            <span className="pro-eyebrow">
              {pro ? "YOUR PRO TOOLS" : "A LOOK INSIDE PRO"}
            </span>
            <BriefingArt kind={step} />
            <div className="guide-caption">
              <span>0{step + 1}</span>
              <p>
                {
                  [
                    "A clearer picture of what matters.",
                    "A starting point for a thoughtful response.",
                    "The right questions before the next move.",
                  ][step]
                }
              </p>
            </div>
          </div>
          <section className="guide-copy">
            <div className="guide-top">
              <span>
                {pro ? "PRO GUIDE" : "PREVIEW TOUR"} · {step + 1} OF 3
              </span>
              <button className="pro-text" onClick={finish}>
                Skip tour <ArrowRight size={14} />
              </button>
            </div>
            <div className="guide-copy-center">
              <span className="pro-feature-icon">
                <current.icon size={25} />
              </span>
              <h1 ref={heading} tabIndex={-1}>
                {current.title}
              </h1>
              <p>{current.description}</p>
              <div className="guide-prompt">
                <span>
                  <Sparkles size={15} />
                  {current.tip}
                </span>
                <blockquote>“{current.prompt}”</blockquote>
              </div>
              <div className="guide-location">
                <BookOpen size={17} />
                <span>
                  Find it in <b>{current.where}</b>
                </span>
              </div>
              <div className="guide-review">
                <ShieldCheck size={17} />
                Read the sources. Check what is missing. Make the final
                decision.
              </div>
              {!pro && (
                <div className="guide-preview-note">
                  Preview only · Pro access is not activated. The examples are
                  illustrations, not AI-generated advice.
                </div>
              )}
            </div>
            <div className="guide-controls">
              <div className="guide-dots">
                {guideSteps.map((s, i) => (
                  <button
                    key={s.title}
                    aria-label={"Go to guide step " + (i + 1)}
                    aria-current={step === i ? "step" : undefined}
                    className={step === i ? "active" : ""}
                    onClick={() => advance(i)}
                  />
                ))}
              </div>
              {step > 0 && (
                <button className="pro-text" onClick={() => advance(step - 1)}>
                  <ArrowLeft size={16} />
                  Back
                </button>
              )}
              <button
                className="pro-button"
                onClick={() => (step === 2 ? finish() : advance(step + 1))}
              >
                {step === 2 ? "Finish guide" : "Next"}
                <ArrowRight size={17} />
              </button>
            </div>
          </section>
        </section>
      ) : (
        <section className="pro-finish">
          <div className="pro-finish-icon">
            <CheckCheck size={42} strokeWidth={1.5} />
            <Sparkles size={19} />
          </div>
          <span className="pro-eyebrow">
            {pro ? "READY WHEN YOU ARE" : "PRO TOUR COMPLETE"}
          </span>
          <h1 ref={heading} tabIndex={-1}>
            {pro
              ? "Your next step, a little clearer."
              : "A little help. A lot of possibility."}
          </h1>
          <p>
            {pro
              ? "Your Pro tools are available across the workspace. Start with a question and review every suggestion."
              : "You’ve seen how Pro can support your team. Your current plan has not changed, and no payment has been taken."}
          </p>
          <div className="finish-tools">
            {proFeatures.map((f) => (
              <div key={f.id}>
                <f.icon size={18} />
                <span>{f.label}</span>
                {pro ? <Check size={15} /> : <LockKeyhole size={14} />}
              </div>
            ))}
          </div>
          <Link
            className="pro-button"
            href={pro ? "/sentinel?ask=1" : "/overview"}
          >
            {pro ? "Open Ask Sentinel" : "Back to my workspace"}
            <ArrowRight size={18} />
          </Link>
          <button
            className="pro-text"
            onClick={() => {
              setStep(0);
              change("guide");
            }}
          >
            Replay the quick guide
          </button>
          <small>You can find this guide again in your ResQOps Pro page.</small>
        </section>
      )}
      <footer className="pro-footer">
        <span>
          <ShieldCheck size={14} />
          Built for the people who help people.
        </span>
        <span>{pro ? "ResQOps Pro" : "ResQOps · Standard & Pro"}</span>
      </footer>
    </div>
  );
}
