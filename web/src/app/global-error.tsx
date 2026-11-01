"use client";
export default function GlobalError({reset}:{error:Error;reset:()=>void}) {
 return <html lang="en"><body style={{margin:0,background:'#f3f6fa',color:'#102642',fontFamily:'Arial,sans-serif'}}><main style={{maxWidth:560,margin:'15vh auto',padding:32}}><h1>We could not open your workspace</h1><p style={{lineHeight:1.6}}>Your saved records have not changed. Reload the workspace to try again. If this continues, contact support and tell them which page you were opening.</p><button style={{padding:'12px 20px',background:'#2563eb',color:'#fff',border:0,borderRadius:8,cursor:'pointer'}} onClick={reset}>Try again</button><a href="/overview" style={{marginLeft:24,color:'#07336a'}}>Return to overview</a></main></body></html>;
}
