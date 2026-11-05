"use client";
import { DraftScope } from "./Forms";
import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname,useRouter } from "next/navigation";
import {
  onAuthStateChanged,
  sendEmailVerification,
  signOut,
  type User,
} from "firebase/auth";
import { api, identity, errorMessage } from "@/lib/api";
import { areas, titles } from "@/lib/catalog";
import { useData, setCacheIdentity, invalidateData, type Scope } from "@/lib/hooks";
import { Brand, Button, Icon, Loading, Notice, lockPageScroll } from "./ui";
import ViewBoundary from "./ViewBoundary";
import NavigationTabs from "./NavigationTabs";
import Auth from "./Auth";
import EntryLanding from "./EntryExperience";
import AccountSetup from "./AccountSetup";
import ProExperience from "./ProExperience";
import Promotions from "./Promotions";
import Preparedness from "./Preparedness";
import Notifications from "./Notifications";
import TopbarAccount from "./TopbarAccount";
import Overview from "./Overview";
import Operations from "./Operations";
import Sentinel from "./Sentinel";
import Administration from "./Administration";
import Account from "./Account";
import Governance from "./Governance";
import Reauthenticate from "./Reauthenticate";
import { canCoordinate } from "@/lib/business";
import { Fragment } from "react";
export default function Portal() {
  const router=useRouter();
  const path = usePathname().split("/").filter(Boolean)[0] || "overview";
  const [user, setUser] = useState<User | null>(null),
    [ready, setReady] = useState(false),
    [revision, setRevision] = useState(0),
    [failure, setFailure] = useState(""),
    [org, setOrg] = useState(""),
    [region, setRegion] = useState(""),
    [menu, setMenu] = useState(false),
    [dark, setDark] = useState(false);
  const [verifyNotice, setVerifyNotice] = useState("");
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const update = () => setScrolled(window.scrollY > 4);
    update(); window.addEventListener("scroll", update, { passive: true });
    return () => window.removeEventListener("scroll", update);
  }, []);
  useEffect(() => {
    setDark(localStorage.getItem("resq-theme") === "dark");
    let stop = () => {};
    let disposed = false;
    identity()
      .then((auth) => {
        if (disposed) return;
        stop = onAuthStateChanged(auth, (u) => {
          setCacheIdentity(u?.uid || "");
          setUser(u);
          setReady(true);
          setRevision((v) => v + 1);
        });
      })
      .catch((e) => {
        setFailure(errorMessage(e));
        setReady(true);
      });
    return () => {
      disposed = true;
      stop();
    };
  }, []);
  useEffect(() => setMenu(false), [path]);
  useEffect(() => menu ? lockPageScroll() : undefined, [menu]);
  useEffect(() => { document.documentElement.classList.toggle("dark", dark); }, [dark]);
  const refresh = () => { invalidateData(); setRevision((v) => v + 1); };
  const verified = !!user?.emailVerified;
  useEffect(()=>{if(verified&&["login","signup","forgot-password"].includes(path))router.replace("/overview");},[verified,path,router]);
  const me = useData(verified ? "/api/auth/me" : null, revision);
  const [booting, setBooting] = useState(false);
  useEffect(() => {
    if (!verified) return;
    let live = true;
    setBooting(true);
    api("/api/auth/firebase/bootstrap", {
      full_name: user?.displayName || "ResQ coordinator",
    })
      .then(() => {
        if (live) refresh();
      })
      .catch((e) => {
        if (live) setFailure(errorMessage(e));
      })
      .finally(() => {
        if (live) setBooting(false);
      });
    return () => {
      live = false;
    };
  }, [user?.uid, verified]);
  const memberships = (me.data?.memberships || []).filter((m: any) =>
    canCoordinate(m.role, true, m.access === "ALLOWED", m.organization_id),
  );
  useEffect(() => {
    if (!me.data) return;
    if (!memberships.some((m: any) => m.organization_id === org)) {
      setOrg(memberships[0]?.organization_id || "");
      setRegion("");
    }
  }, [me.data]);
  const regions = useData(
    org ? `/api/organizations/${org}/sentinel/regions` : null,
    revision,
  );
  useEffect(() => {
    if (regions.data && !regions.data.some((r: any) => r.id === region))
      setRegion(regions.data[0]?.id || "");
  }, [regions.data]);
  const scope: Scope = {
    org,
    region,
    role: memberships.find((m: any) => m.organization_id === org)?.role || "",
    userId: Number(me.data?.user?.id || 0),
    ops: `/api/organizations/${org}/regions/${region}/operations`,
    sentinel: `/api/organizations/${org}/sentinel/regions/${region}`,
    safety: `/api/organizations/${org}/regions/${region}/safety`,
    refresh,
  };
  if (["welcome","onboarding"].includes(path)) return <EntryLanding screen={path==="welcome"?"splash":"onboarding"}/>;
  if (!ready)
    return (
      <main id="main" className="welcome-card">
        <Brand />
        <h1>Opening your workspace</h1>
        <Loading />
      </main>
    );
  if (!user)
    return (
      <>
        <Auth onSignedIn={refresh} />
        {failure && (
          <div className="auth-service-notice">
            <Notice error>{failure}</Notice>
          </div>
        )}
      </>
    );
  if (!verified)
    return (
      <main id="main" className="welcome-card">
        <Brand />
        <h1>Check your inbox.</h1>
        <p>
          Verify <strong>{user.email}</strong> to continue to ResQOps.
        </p>
        <Notice>
          {verifyNotice || "Follow the verification link, then return here."}
        </Notice>
        <div className="action-list">
          <Button
            variant="primary"
            onClick={async () => {
              await user.reload();
              setUser((await identity()).currentUser);
              refresh();
            }}
          >
            I’ve verified my email
          </Button>
          <Button
            onClick={async () => {
              try {
                await sendEmailVerification(user);
                setVerifyNotice("A new verification email has been sent.");
              } catch (e) {
                setVerifyNotice(errorMessage(e));
              }
            }}
          >
            Send a new email
          </Button>
          <Button onClick={async () => signOut(await identity())}>
            Sign out
          </Button>
        </div>
      </main>
    );
  if(path==="setup" && (booting||!me.data)) return <main id="main" className="welcome-card"><Loading/></main>;
  if(path==="setup" || (!booting&&me.data&&typeof window!=="undefined"&&sessionStorage.getItem('resq-setup-pending')===user.uid))return <AccountSetup user={user} scope={scope} onDone={refresh}/>;
  const area = areas.find((a) => a.pages.some((p) => p[0] === path));
  const account = [
    "profile",
    "privacy",
    "securityAccount",
    "invitations",
    "pro",
  ].includes(path);
  const title = titles[path] || "Page not found";
  return (
    <DraftScope.Provider value={[user.uid,org,region].join(":")}><div className={"app " + (dark ? "dark" : "")}>
      <Reauthenticate />
      <aside className={"sidebar " + (menu ? "open" : "")}>
        <Link href="/overview" aria-label="ResQOps home">
          <Brand />
        </Link>
        <button
          className="mobile-close button"
          aria-label="Close navigation"
          onClick={() => setMenu(false)}
        >
          <Icon name="close" />
        </button>
        <span className="eyebrow">Coordination</span>
        <nav aria-label="Main navigation">
          {areas.filter(a=>a.name!=="Administration").map((a,i) => <Fragment key={a.name}>{a.name==="Field reports"&&<span className="eyebrow sidebar-section">Plan & improve</span>}<Link className={"nav-link "+(area===a?"active":"")} href={"/"+a.pages[0][0]} aria-current={area===a?"page":undefined}><Icon name={a.icon}/>{a.name}</Link></Fragment>)}
        </nav>
        <div className="sidebar-bottom"><Link className="sidebar-pro" href="/pro"><span className="sidebar-pro-mark"><Icon name="shield" size={24}/></span><span><strong>ResQOps <em>PRO</em></strong><small>Explore your Pro benefits</small></span></Link></div>
      </aside>
      <div className="main-column">
        <header className={"topbar" + (scrolled ? " is-scrolled" : "")}>
          <div className="workspace-switch">
            <Button
              className="mobile-menu"
              icon="menu"
              aria-label="Open navigation"
              onClick={() => setMenu(true)}
            />
            <Icon name="missions" />
            <select
              aria-label="Workspace"
              value={org}
              onChange={(e) => {
                setOrg(e.target.value);
                setRegion("");
              }}
            >
              {!memberships.length && <option value="">My workspace</option>}
              {memberships.map((m: any) => (
                <option key={m.organization_id} value={m.organization_id}>
                  {m.organization_name}
                </option>
              ))}
            </select>
          </div>
          <div className="topbar-right">
            <label>
              <Icon name="pin" size={16} />
              <select
                aria-label="Region"
                value={region}
                onChange={(e) => setRegion(e.target.value)}
              >
                {!regions.data?.length && <option value="">{regions.loading ? "Loading regions…" : "No region"}</option>}
                {regions.data?.map((r: any) => (
                  <option value={r.id} key={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
            <Button
              icon={dark ? "sun" : "moon"}
              variant="quiet"
              aria-label="Switch theme"
              onClick={() => {
                setDark(!dark);
                localStorage.setItem("resq-theme", !dark ? "dark" : "light");
              }}
            />
            <TopbarAccount user={user} onLogout={async()=>{await signOut(await identity());setOrg("");setRegion("");setFailure("");}}/>
          </div>
        </header>
        <main id="main" className="content">
          <header className="page-heading">
            <div>
              <h1>{title}</h1>
            </div>
            <div className="heading-actions">
              <Button
                icon="refresh"
                onClick={refresh}
                aria-label="Refresh page"
              >
                Refresh
              </Button>
              {path === "overview" && (
                <Link className="button primary" href="/missions?create=1">
                  <Icon name="plus" size={18} />
                  Plan a mission
                </Link>
              )}
            </div>
          </header>
          <div className={area && area.pages.length > 1 ? "page-folder" : "page-view"}>
          {area && area.pages.length > 1 && (
            <NavigationTabs pages={area.pages} current={path} label="Section navigation" />
          )}
          <div className="page-folder-content">
          {(failure || me.error || regions.error) && (
            <Notice error>{failure || me.error || regions.error}</Notice>
          )}
          <Fragment key={user.uid + ":" + org + ":" + region + ":" + path}><ViewBoundary>
            {path==="pro"||path==="pro-guide"?<ProExperience guide={path==="pro-guide"} revision={revision}/>:path==="promotions"?<Promotions revision={revision} refresh={refresh}/>:account ? (
              <Account
                page={path}
                scope={scope}
                user={user}
                revision={revision}
              />
            ) : path === "platform" ? (
              <Administration
                page={path}
                scope={scope}
                revision={revision}
                platform={
                  me.data?.platform_grant === "TECH_ADMIN" ||
                  me.data?.platform_access === "ALLOWED"
                }
              />
            ) : (me.loading && !me.data) || booting || (memberships.length > 0 && !org) || (org && !regions.data && regions.loading) || (regions.data?.length > 0 && !region) ? (
              <Loading />
            ) : !org ? (
              <section className="welcome-card">
                <span className="eyebrow">YOU’RE SIGNED IN</span>
                <h1>Your account is ready.</h1>
                <p>
                  Your workspace membership hasn’t been approved yet. Check your
                  invitations or contact the workspace owner.
                </p>
                <div className="action-list">
                  <Link className="button primary" href="/invitations">
                    Check invitations
                    <Icon name="arrow" />
                  </Link>
                  <Link className="button" href="/profile">
                    My account
                  </Link>
                  {me.data?.platform_access === "ALLOWED" && (
                    <Link className="button" href="/platform">
                      Set up workspace
                    </Link>
                  )}
                </div>
              </section>
            ) : !region && !["settings","people","governance","team-pro"].includes(path) ? (
              <section className="panel">
                <div className="empty">
                  <h2>Set your operating region.</h2>
                  <p>Add a region to connect missions, people and Sentinel.</p>
                  <Link className="button primary" href="/settings">
                    Configure region
                  </Link>
                </div>
              </section>
            ) : path === "campaigns" ? <Preparedness scope={scope} revision={revision}/> : path === "notifications" ? <Notifications scope={scope} revision={revision}/> : path === "governance" ? (
              <Governance scope={scope} revision={revision} />
            ) : path === "overview" ? (
              <Overview scope={scope} revision={revision} />
            ) : [
                "sentinel",
                "weather-analysis",
                "resource-planning",
                "specialised",
                "agent-runs",
                "operations-map",
                "assistance",
                "deployment-map",
              ].includes(path) ? (
              <Sentinel page={path} scope={scope} revision={revision} />
            ) : [
                "settings",
                "security",
                "insights",
                "team-pro",
                "people",
              ].includes(path) ? (
              <Administration page={path} scope={scope} revision={revision} />
            ) : (
              <Operations page={path} scope={scope} revision={revision} />
            )}
          </ViewBoundary></Fragment>
          </div></div>
        </main>
      </div>
    </div></DraftScope.Provider>
  );
}
