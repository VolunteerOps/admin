import type {NextConfig} from 'next';
const backend=process.env.RESQ_BACKEND_ORIGIN || 'https://resq-api-staging.up.railway.app';
if(!/^https:\/\/[a-z0-9.-]+(?::\d+)?$/i.test(backend)&&!/^http:\/\/127\.0\.0\.1:\d+$/.test(backend))throw Error('Invalid backend origin');
// Weather tools can run for three minutes; the default rewrite proxy cuts off at 30 seconds.
const config:NextConfig={output:'standalone',poweredByHeader:false,experimental:{proxyTimeout:240000},async rewrites(){return [{source:'/api/:path*',destination:backend+'/api/:path*'},{source:'/sentinel/config',destination:backend+'/sentinel/config'},{source:'/sentinel/map-widget-config',destination:backend+'/sentinel/map-widget-config'}];},async headers(){return [{source:'/:path*',headers:[{key:'X-Content-Type-Options',value:'nosniff'},{key:'Referrer-Policy',value:'same-origin'},{key:'X-Frame-Options',value:'DENY'},{key:'Cache-Control',value:'no-store'}]}];}};
export default config;
