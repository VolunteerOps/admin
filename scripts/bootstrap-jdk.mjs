// Render static-site images do not guarantee Java. Fetch an official Temurin JDK
// and verify its SHA-256 from the HTTPS release metadata before extracting it.
import {createHash} from 'node:crypto';
import {createReadStream, createWriteStream} from 'node:fs';
import {mkdir, mkdtemp, writeFile, stat} from 'node:fs/promises';
import {resolve, join} from 'node:path';
import {Readable} from 'node:stream';
import {pipeline} from 'node:stream/promises';
import {execFileSync} from 'node:child_process';
if (process.platform !== 'linux' || process.arch !== 'x64') throw new Error('Automatic JDK bootstrap supports Render Linux x64; configure JAVA_HOME on other platforms');
const base = resolve('.render-tools');
await mkdir(base, {recursive: true});
const signal = () => AbortSignal.timeout(300_000);
const metadataResponse = await fetch('https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse', {signal: signal()});
if (!metadataResponse.ok) throw new Error('JDK release metadata request failed: ' + metadataResponse.status);
const assets = await metadataResponse.json();
const asset = assets.find(row => row.binary?.package?.link && row.version?.major === 21);
const pkg = asset?.binary?.package;
if (!pkg || !/^[a-f0-9]{64}$/i.test(pkg.checksum) || !Number.isSafeInteger(pkg.size) || pkg.size < 1_000_000 || pkg.size > 500_000_000) throw new Error('Invalid official JDK metadata');
const source = new URL(pkg.link);
if (source.protocol !== 'https:' || source.hostname !== 'github.com' || !source.pathname.startsWith('/adoptium/temurin21-binaries/releases/download/')) throw new Error('Unexpected JDK download origin');
const target = join(base, 'jdk21-' + pkg.checksum.slice(0, 16));
try {
  if ((await stat(join(target, '.verified'))).isFile() && (await stat(join(target, 'bin', 'java'))).isFile()) { console.log(target); process.exit(0); }
} catch {}
const download = await mkdtemp(join(base, 'download-'));
const archive = join(download, 'jdk.tar.gz');
const response = await fetch(source, {signal: signal()});
if (!response.ok || !response.body) throw new Error('JDK archive request failed: ' + response.status);
await pipeline(Readable.fromWeb(response.body), createWriteStream(archive));
if ((await stat(archive)).size !== pkg.size) throw new Error('Incomplete JDK archive');
const hash = createHash('sha256');
for await (const chunk of createReadStream(archive)) hash.update(chunk);
if (hash.digest('hex') !== pkg.checksum.toLowerCase()) throw new Error('JDK checksum mismatch');
await mkdir(target, {recursive: true});
execFileSync('tar', ['-xzf', archive, '-C', target, '--strip-components=1'], {stdio: 'inherit'});
if (!(await stat(join(target, 'bin', 'java'))).isFile()) throw new Error('JDK extraction failed');
await writeFile(join(target, '.verified'), pkg.checksum + '\n');
console.log(target);
