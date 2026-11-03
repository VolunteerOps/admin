"use client";
import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  ArrowLeft,
  ArrowUpRight,
  ShieldCheck,
  Check,
  CheckCircle2,
  MapPin,
  HeartHandshake,
  Radio,
  Users,
  Boxes,
  Flag,
  ClipboardCheck,
  Sparkles,
} from "lucide-react";
import Link from "next/link";
const intros = [
  {
    label: "SEE THE BIGGER PICTURE",
    title: (
      <>
        A clearer picture.
        <br />A better next step.
      </>
    ),
    description:
      "Bring local reports and early warnings into one view. Understand what is happening, where it matters, and what needs your attention.",
    benefit: "Clear information. Plain language. Less guesswork.",
    icon: Radio,
  },
  {
    label: "BRING PEOPLE TOGETHER",
    title: (
      <>
        The right people.
        <br />
        Working as one.
      </>
    ),
    description:
      "Find available volunteers, match their skills to a mission, and give everyone a clear plan. Spend less time organizing and more time helping.",
    benefit: "One team. Shared purpose. Clear responsibilities.",
    icon: Users,
  },
  {
    label: "PREPARE WITH CONFIDENCE",
    title: (
      <>
        Ready today.
        <br />
        Stronger tomorrow.
      </>
    ),
    description:
      "Keep track of supplies, close readiness gaps, and prepare your response before it is needed. Review every plan before a team moves.",
    benefit: "Helpful intelligence. Human decisions. Always.",
    icon: ShieldCheck,
  },
];
export function EntryBrand({ light = false }: { light?: boolean }) {
  return (
    <Link
      className={"entry-brand " + (light ? "light" : "")}
      href="/welcome"
      aria-label="ResQOps welcome"
    >
      <img src="/logo-monochrome.png" alt="" width={42} height={42} />
      <span>
        ResQ<span>Ops</span>
      </span>
    </Link>
  );
}
export function Artwork({ index }: { index: number }) {
  return (
    <div
      className={"entry-art art-" + index}
      aria-label={
        [
          "Illustration of reports coming together on a radar",
          "Illustration of volunteers coordinated around a mission",
          "Illustration of supplies and a plan ready for review",
        ][index]
      }
      role="img"
    >
      {index === 0 ? (
        <>
          <div className="art-coordinates">
            COAST REGION <span>SCENARIO PREVIEW</span>
          </div>
          <div className="radar-disc">
            <div className="radar-grid" />
            <div className="radar-sweep" />
            <span className="radar-center" />
            <i className="radar-point p1" />
            <i className="radar-point p2" />
            <i className="radar-point p3" />
            <div className="radar-orbit o1" />
            <div className="radar-orbit o2" />
            <div className="radar-orbit o3" />
            <div className="radar-axis x" />
            <div className="radar-axis y" />
          </div>
          <div className="art-mini-card report-card">
            <span className="art-icon">
              <Radio size={21} />
            </span>
            <div>
              <small>FIELD REPORT</small>
              <b>A signal worth a closer look</b>
              <span>Review the source and next steps</span>
            </div>
            <span className="art-status" />
          </div>
          <div className="art-float local-card">
            <MapPin size={17} />
            <span>
              Local context.
              <br />
              <b>A shared understanding.</b>
            </span>
          </div>
          <span className="north-marker">
            N<br />↑
          </span>
        </>
      ) : index === 1 ? (
        <>
          <div className="team-orbit orbit-a" />
          <div className="team-orbit orbit-b" />
          <div className="team-center">
            <Flag size={29} />
            <span>ONE MISSION</span>
            <b>A shared plan</b>
            <small>Everyone knows their part</small>
          </div>
          <div className="team-person tp1">
            <span className="art-avatar av-green">AJ</span>
            <div>
              <b>Amina</b>
              <small>First aid</small>
            </div>
            <CheckCircle2 size={15} />
          </div>
          <div className="team-person tp2">
            <span className="art-avatar av-yellow">DO</span>
            <div>
              <b>David</b>
              <small>Logistics</small>
            </div>
            <CheckCircle2 size={15} />
          </div>
          <div className="team-person tp3">
            <span className="art-avatar av-blue">FA</span>
            <div>
              <b>Fatuma</b>
              <small>Community support</small>
            </div>
            <CheckCircle2 size={15} />
          </div>
          <div className="art-mini-card team-note">
            <ShieldCheck size={21} />
            <div>
              <b>Skills checked. Roles clear.</b>
              <span>You review the team before approval.</span>
            </div>
          </div>
          <div className="art-label">ILLUSTRATIVE TEAM</div>
        </>
      ) : (
        <>
          <div className="readiness-card">
            <div className="readiness-head">
              <span className="art-icon">
                <ClipboardCheck size={21} />
              </span>
              <div>
                <small>PREPAREDNESS CHECKLIST</small>
                <b>Small steps. Stronger readiness.</b>
              </div>
            </div>
            {[
              ["First aid supplies", "Stock reviewed", Boxes],
              ["Team availability", "People confirmed", Users],
              ["Meeting point", "Location reviewed", MapPin],
            ].map(([title, sub, Icon]: any) => (
              <div className="readiness-row" key={title}>
                <Icon size={18} />
                <div>
                  <b>{title}</b>
                  <small>{sub}</small>
                </div>
                <Check size={15} />
              </div>
            ))}
            <div className="readiness-progress">
              <span />
            </div>
            <small className="readiness-caption">
              ONE CLEAR PLAN, READY FOR YOUR REVIEW
            </small>
          </div>
          <div className="art-mini-card review-card">
            <ShieldCheck size={28} />
            <div>
              <small>YOU STAY IN CONTROL</small>
              <b>People make the final call.</b>
              <span>Review first. Then act.</span>
            </div>
          </div>
          <div className="art-spark">
            <Sparkles size={25} />
          </div>
        </>
      )}
    </div>
  );
}
export default function EntryLanding({
  screen,
}: {
  screen: "splash" | "onboarding";
}) {
  const [slide, setSlide] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  function introTo(i: number) {
    setSlide(i);
    requestAnimationFrame(() => heading.current?.focus());
  }
  if (screen === "splash")
    return (
      <main id="main" className="entry-splash">
        <div className="splash-grid" />
        <header>
          <EntryBrand light />
          <span className="splash-header-note">
            FOR THE PEOPLE WHO HELP PEOPLE
          </span>
        </header>
        <div className="splash-center">
          <div className="splash-emblem">
            <div className="splash-ring one" />
            <div className="splash-ring two" />
            <div className="splash-ring three" />
            <ShieldCheck size={67} strokeWidth={1.25} />
            <span className="emblem-dot" />
          </div>
          <div className="splash-wordmark">
            ResQ<span>Ops</span>
          </div>
          <span className="splash-kicker">COORDINATION WITH PURPOSE</span>
          <h1>
            A shared purpose.
            <br />A stronger response.
          </h1>
          <p>
            Bring people, information and resources together.
            <br />
            Be ready for what comes next.
          </p>
          <Link className="entry-btn gold" href="/onboarding#1">
            Get started <ArrowRight size={18} />
          </Link>
          <Link className="splash-login" href="/login">
            Already part of a team?{" "}
            <b>
              Sign in <ArrowUpRight size={13} />
            </b>
          </Link>
        </div>
        <footer>
          <span>
            <ShieldCheck size={14} />
            Built for humanitarian and public service teams
          </span>
          <span>ResQOps · Welcome experience</span>
        </footer>
      </main>
    );

  return (
    <main id="main" className="entry-shell">
      <aside className="entry-visual">
        <EntryBrand light />
        <div className="visual-eyebrow">
          <span />
          CONNECTED PEOPLE. COORDINATED ACTION.
        </div>
        <Artwork index={slide} />
        <div className="visual-caption">
          <span>0{slide + 1}</span>
          <p>
            {
              [
                "Clarity when it matters most.",
                "A team is more than a list of names.",
                "Good preparation makes a difference.",
              ][slide]
            }
          </p>
        </div>
        <div className="visual-foot">PREPARED TOGETHER. STRONGER TOGETHER.</div>
      </aside>
      <section className="entry-content">
        <header className="entry-top">
          <Link className="entry-text-link" href="/login">
            Skip introduction <ArrowRight size={16} />
          </Link>
        </header>
        <div className="intro-content">
          <div className="intro-mobile-art">
            <Artwork index={slide} />
          </div>
          <span className="entry-kicker">{intros[slide].label}</span>
          <h1 ref={heading} tabIndex={-1}>
            {intros[slide].title}
          </h1>
          <p>{intros[slide].description}</p>
          <div className="intro-benefit">
            <ShieldCheck size={18} />
            {intros[slide].benefit}
          </div>
          <div className="intro-actions">
            {slide < 2 ? (
              <button className="entry-btn" onClick={() => introTo(slide + 1)}>
                Continue <ArrowRight size={18} />
              </button>
            ) : (
              <Link className="entry-btn" href="/signup">
                Let’s get you set up <ArrowRight size={18} />
              </Link>
            )}
            {slide > 0 && (
              <button className="entry-back" onClick={() => introTo(slide - 1)}>
                <ArrowLeft size={17} />
                Back
              </button>
            )}
          </div>
          <div className="intro-bottom">
            <div className="intro-dots" aria-label="Onboarding pages">
              {intros.map((_, i) => (
                <button
                  key={i}
                  className={slide === i ? "active" : ""}
                  onClick={() => introTo(i)}
                  aria-label={"Go to onboarding page " + (i + 1)}
                  aria-current={slide === i ? "step" : undefined}
                />
              ))}
            </div>
            <span>0{slide + 1} / 03</span>
          </div>
        </div>
      </section>
    </main>
  );
}
export function EntryVisual() {
  return (
    <aside className="entry-visual">
      <EntryBrand light />
      <div className="auth-side-copy">
        <span className="entry-kicker">WELCOME TO RESQOPS</span>
        <h2>
          Good people.
          <br />
          Working together.
          <br />
          <em>Making a difference.</em>
        </h2>
        <p>
          One place to prepare, coordinate and help your community respond with
          confidence.
        </p>
      </div>
      <div className="auth-art">
        <div className="community-orbit">
          <div className="community-heart">
            <HeartHandshake size={48} strokeWidth={1.2} />
          </div>
          <div className="community-chip cc1">
            <Users size={18} />
            People
          </div>
          <div className="community-chip cc2">
            <Radio size={18} />
            Clarity
          </div>
          <div className="community-chip cc3">
            <Boxes size={18} />
            Readiness
          </div>
        </div>
      </div>
      <div className="auth-side-bottom">
        <ShieldCheck size={22} />
        <p>
          Designed for humanitarian teams
          <br />
          and public service coordinators.
        </p>
      </div>
      <div className="visual-foot">PREPARED TOGETHER. STRONGER TOGETHER.</div>
    </aside>
  );
}
