import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../..')
const generate = spawnSync(process.execPath, [path.join(__dirname, 'generate.mjs')], {
  encoding: 'utf8',
})
if (generate.status !== 0) {
  console.error(generate.stderr || generate.stdout)
  process.exit(generate.status ?? 1)
}

const adoc = fs.readFileSync(
  path.join(repoRoot, 'docs/modules/ROOT/pages/loginWithPassword.adoc'),
  'utf8',
)

const signature = adoc.split('== About this command')[0]

const checks = [
  ['title', /^= loginWithPassword$/m],
  ['KDoc lead sentence', /Authenticate the user with the specified identifier/],
  ['signature xref for origin', /<<origin>>: String\? = null/],
  ['signature xref for email', /<<email>>: String\? = null/],
  ['origin @param description', /reporting only/],
  ['password is required (no default)', /<<password>>: String,/],
  ['signature uses subs=normal', /\[subs="normal"\]/],
  ['signature does not wrap types in backticks', () => !signature.includes('`')],
  ['generated banner', /GENERATED from sdk-core/],
  ['LoginMfaConf nested activity', /LoginMfaConf-activity/],
  ['LoginMfaConf activity KDoc', /Android activity used to continue the MFA step-up flow/],
  ['deprecated redirectUri', /will be removed in a future release/],
  ['AuthToken heading', /^=== AuthToken$/m],
  ['AuthToken accessToken field', /AuthToken-accessToken/],
  ['OpenIdUser nested preferredUsername', /OpenIdUser-preferredUsername/],
  ['OpenIdUser email does not steal param anchor', /\[\[OpenIdUser-email, email\]\]/],
  ['does not expand Address inside OpenIdUser', (hay) => !/Address-formatted/.test(hay)],
  ['single top-level email param anchor', (hay) => (hay.match(/\[\[email, email\]\]/g) || []).length === 1],
  ['does not expand Activity', (hay) => !/android\.app\.Activity|WindowManager/.test(hay)],
]

let failed = 0
for (const [label, re] of checks) {
  const haystack = String(label).startsWith('signature') ? signature : adoc
  const ok = typeof re === 'function' ? re(adoc) : re.test(haystack)
  if (ok) {
    console.log(`pass  ${label}`)
  } else {
    console.error(`FAIL  ${label}`)
    failed++
  }
}

if (failed > 0) {
  process.exit(1)
}
console.log('All generator checks passed.')
