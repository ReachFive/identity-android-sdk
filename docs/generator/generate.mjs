#!/usr/bin/env node
/**
 * Prototype: emit Antora AsciiDoc from a Kotlin function signature + KDoc.
 *
 * Signatures, types, and nested model fields come from source.
 * Descriptions come from KDoc. Signature names are AsciiDoc xrefs
 * (`<<email>>`) resolved by `[subs="normal"]` against param anchors.
 * Types are plain text in the listing (not backticks): site CSS sets
 * `.doc pre code { display: block }`, so an inner `<code>` per type
 * stacked each token on its own line.
 *
 * This is a one-method probe (PasswordAuth.loginWithPassword), not the
 * eventual Dokka plugin.
 */

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(__dirname, '../..')
const SOURCE = path.join(
  REPO_ROOT,
  'sdk-core/src/main/java/co/reachfive/identity/sdk/core/PasswordClient.kt',
)
const MODEL_ROOT = path.join(REPO_ROOT, 'sdk-core/src/main/java')
const OUT_DIR = path.join(REPO_ROOT, 'docs/modules/ROOT/pages')
const METHOD = 'loginWithPassword'
const MAX_NEST = 0
const SKIP_TYPES = new Set([
  'Activity',
  'Context',
  'Intent',
  'View',
  'Exception',
  'Uri',
  'String',
  'Int',
  'Boolean',
  'Long',
  'Double',
  'Float',
  'Unit',
  'Any',
  'Nothing',
  'List',
  'Collection',
  'Set',
  'Map',
  'Array',
  'MutableList',
  'Sequence',
  'Callback',
  'Success',
  'Failure',
])

function main() {
  const source = fs.readFileSync(SOURCE, 'utf8')
  const method = extractMethod(source, METHOD)
  if (!method) {
    console.error(`Could not find fun ${METHOD} in ${SOURCE}`)
    process.exit(1)
  }
  const models = indexDataClasses(MODEL_ROOT)
  const adoc = renderMethodPage(method, models)
  fs.mkdirSync(OUT_DIR, { recursive: true })
  const outFile = path.join(OUT_DIR, `${METHOD}.adoc`)
  fs.writeFileSync(outFile, adoc)
  console.log(`Wrote ${path.relative(REPO_ROOT, outFile)}`)
}

function extractMethod(source, name) {
  const ifaceIndex = source.indexOf('interface PasswordAuth')
  if (ifaceIndex === -1) return null
  const ifaceSource = source.slice(ifaceIndex)
  const funRe = new RegExp(`fun\\s+${name}\\s*\\(`)
  const funMatch = funRe.exec(ifaceSource)
  if (!funMatch) return null

  const absFunIndex = ifaceIndex + funMatch.index
  const kdoc = parseKdoc(extractPrecedingKdoc(source, absFunIndex))
  const { params } = parseParamList(source, absFunIndex + funMatch[0].length - 1)
  const successType = inferSuccessType(params)

  return {
    name,
    kdoc,
    params,
    successType,
    sourcePath: path.relative(REPO_ROOT, SOURCE),
  }
}

function extractPrecedingKdoc(source, index) {
  const before = source.slice(0, index)
  const kdocEnd = before.lastIndexOf('*/')
  if (kdocEnd === -1) return ''
  const between = before.slice(kdocEnd + 2)
  if (!isOnlyAnnotationsAndComments(between)) return ''
  const kdocStart = before.lastIndexOf('/**', kdocEnd)
  if (kdocStart === -1) return ''
  return source.slice(kdocStart + 3, kdocEnd)
}

function isOnlyAnnotationsAndComments(text) {
  const without = text
    .replace(/\/\/[^\n]*/g, '')
    .replace(/@[A-Za-z.]+\([^)]*\)/gs, '')
    .replace(/@[A-Za-z.]+/g, '')
    .trim()
  return without.length === 0
}

function parseKdoc(raw) {
  if (!raw) return { description: '', params: {} }
  const lines = raw.split('\n').map((line) => line.replace(/^\s*\*\s?/, '').trimEnd())

  const paramDescriptions = {}
  const descriptionLines = []
  let currentParam = null

  for (const line of lines) {
    const paramMatch = line.match(/^@param\s+(\w+)\s+(.*)$/)
    if (paramMatch) {
      currentParam = paramMatch[1]
      paramDescriptions[currentParam] = paramMatch[2].trim()
      continue
    }
    if (line.startsWith('@')) {
      currentParam = null
      continue
    }
    if (currentParam) {
      paramDescriptions[currentParam] = `${paramDescriptions[currentParam]} ${line.trim()}`.trim()
      continue
    }
    descriptionLines.push(line.trim())
  }

  const description = descriptionLines.join('\n').replace(/\n{3,}/g, '\n\n').trim()
  return { description, params: paramDescriptions }
}

function parseParamList(source, openParenIndex) {
  let depth = 0
  let genericDepth = 0
  let i = openParenIndex
  for (; i < source.length; i++) {
    const ch = source[i]
    if (ch === '(') depth++
    else if (ch === ')') {
      depth--
      if (depth === 0) break
    } else if (ch === '<') genericDepth++
    else if (ch === '>' && genericDepth > 0) genericDepth--
  }
  const inner = stripComments(source.slice(openParenIndex + 1, i))
  const parts = splitTopLevel(inner, ',')
  const params = parts
    .map((part) => part.trim())
    .filter(Boolean)
    .map(parseParam)
  return { params, closeIndex: i }
}

function stripComments(text) {
  return text.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '')
}

function splitTopLevel(text, sep) {
  const parts = []
  let buf = ''
  let paren = 0
  let generic = 0
  for (const ch of text) {
    if (ch === '(') paren++
    else if (ch === ')') paren--
    else if (ch === '<') generic++
    else if (ch === '>') generic--
    if (ch === sep && paren === 0 && generic === 0) {
      parts.push(buf)
      buf = ''
    } else {
      buf += ch
    }
  }
  if (buf.trim()) parts.push(buf)
  return parts
}

function parseParam(text) {
  const deprecated = text.match(/@Deprecated\("([^"]+)"\)/)
  const stripped = text
    .replace(/@[A-Za-z.]+\([^)]*\)\s*/gs, '')
    .replace(/@[A-Za-z.]+\s*/g, '')
    .replace(/\b(?:val|var)\s+/g, '')
    .trim()
  const eq = indexOfTopLevel(stripped, '=')
  const nameAndType = (eq === -1 ? stripped : stripped.slice(0, eq)).trim()
  const defaultValue = eq === -1 ? null : stripped.slice(eq + 1).trim()
  const colon = nameAndType.indexOf(':')
  return {
    name: (colon === -1 ? nameAndType : nameAndType.slice(0, colon)).trim(),
    type: (colon === -1 ? '' : nameAndType.slice(colon + 1)).trim(),
    defaultValue,
    optional: defaultValue !== null,
    deprecated: deprecated ? deprecated[1] : null,
  }
}

function indexOfTopLevel(text, sep) {
  let paren = 0
  let generic = 0
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (ch === '(') paren++
    else if (ch === ')') paren--
    else if (ch === '<') generic++
    else if (ch === '>') generic--
    if (ch === sep && paren === 0 && generic === 0) return i
  }
  return -1
}

function inferSuccessType(params) {
  const success = params.find((p) => p.name === 'success')
  const match = success?.type.match(/^Success<(.+)>$/)
  return match ? match[1].trim() : 'Unit'
}

function unwrapType(type) {
  let t = type.replace(/@[A-Za-z.]+(?:\([^)]*\))?\s*/g, '').replace(/\?$/, '').trim()
  const gen = t.match(/^(?:Success|Failure|List|Collection|Set|MutableList)\s*<(.+)>$/)
  if (gen) return unwrapType(gen[1])
  return t
}

function isCallbackType(type) {
  return /^(Success|Failure)</.test(type.replace(/\?$/, '').trim())
}

function walkKtFiles(dir, acc = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name)
    if (ent.isDirectory()) walkKtFiles(p, acc)
    else if (ent.name.endsWith('.kt')) acc.push(p)
  }
  return acc
}

function indexDataClasses(root) {
  const map = new Map()
  for (const file of walkKtFiles(root)) {
    const source = fs.readFileSync(file, 'utf8')
    const re = /data class (\w+)\s*\(/g
    let match
    while ((match = re.exec(source))) {
      map.set(match[1], { file, source })
    }
  }
  return map
}

function parseDataClass(models, name) {
  const entry = models.get(name)
  if (!entry) return null
  const marker = `data class ${name}(`
  const start = entry.source.indexOf(marker)
  if (start === -1) return null
  const openParen = start + marker.length - 1
  const kdoc = parseKdoc(extractPrecedingKdoc(entry.source, start))
  const { params, closeIndex } = parseParamList(entry.source, openParen)
  let searchFrom = openParen
  const fields = params.map((p) => {
    const slice = entry.source.slice(searchFrom, closeIndex)
    const re = new RegExp(`\\b(?:val|var)\\s+${p.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*:`)
    const match = re.exec(slice)
    const abs = match ? searchFrom + match.index : searchFrom
    searchFrom = abs + 1
    const fieldKdoc = parseKdoc(extractPrecedingKdoc(entry.source, abs))
    return {
      ...p,
      description: fieldKdoc.description,
    }
  })
  return { name, description: kdoc.description, fields }
}

function kdocToAdoc(text) {
  return text
    .replace(/\[([^\]]+)\]/g, '`$1`')
    .replace(/`{2,}/g, '`')
}

function describeField(field) {
  const parts = []
  if (field.description) parts.push(kdocToAdoc(field.description))
  else parts.push('_Missing KDoc._')
  if (field.deprecated) parts.push(`Deprecated:: ${field.deprecated}`)
  return parts.join('\n\n')
}

function renderNestedFields(fields, typeName, models, depth) {
  const rows = fields.map((field) => {
    const opt = field.optional ? ' (optional)' : ''
    const nested = expandIfModel(field.type, models, depth + 1, `${typeName}-${field.name}`)
    const desc = [describeField(field), nested].filter(Boolean).join('\n\n')
    return `![[${typeName}-${field.name}, ${field.name}]]${field.name} \`${field.type}\`${opt} ! ${desc}`
  })
  return `[cols="1,3a"]\n!===\n${rows.join('\n')}\n!===`
}

function expandIfModel(type, models, depth, _anchorPrefix) {
  if (depth > MAX_NEST) return ''
  if (isCallbackType(type)) return ''
  const base = unwrapType(type)
  if (!base || SKIP_TYPES.has(base) || !models.has(base)) return ''
  const model = parseDataClass(models, base)
  if (!model) return ''
  return renderNestedFields(model.fields, model.name, models, depth)
}

function renderMethodPage(method, models) {
  const signatureLines = [
    `client.${method.name}(`,
    ...method.params.map((p, i) => {
      const comma = i < method.params.length - 1 ? ',' : ''
      const defaultBit = p.optional ? ` = ${p.defaultValue}` : ''
      return `  <<${p.name}>>: ${p.type}${defaultBit}${comma}`
    }),
    ')',
  ]

  const paramRows = method.params
    .map((p) => {
      const desc = method.kdoc.params[p.name]
        ? kdocToAdoc(method.kdoc.params[p.name])
        : '_Missing `@param` in KDoc._'
      const opt = p.optional ? ' (optional)' : ''
      const nested = isCallbackType(p.type) ? '' : expandIfModel(p.type, models, 0, p.name)
      const cell = [desc, nested].filter(Boolean).join('\n\n')
      return `|[[${p.name}, ${p.name}]]${p.name} \`${p.type}\`${opt} | ${cell}`
    })
    .join('\n')

  const about = kdocToAdoc(method.kdoc.description) || '_Missing KDoc description._'
  const successModel = parseDataClass(models, method.successType)
  const responseBody = successModel
    ? `The \`success\` callback receives <<${successModel.name}>>.

[[${successModel.name}]]
=== ${successModel.name}

${kdocToAdoc(successModel.description) || ''}

[cols="1a,2a"]
|===
${successModel.fields
  .map((field) => {
    const opt = field.optional ? ' (optional)' : ''
    const nested = expandIfModel(field.type, models, 0, `${successModel.name}-${field.name}`)
    const cell = [describeField(field), nested].filter(Boolean).join('\n\n')
    return `|[[${successModel.name}-${field.name}, ${field.name}]]${field.name} \`${field.type}\`${opt}
| ${cell}`
  })
  .join('\n')}
|===`
    : `The \`success\` callback receives \`${method.successType}\`.`

  return `// GENERATED from ${method.sourcePath} — do not edit.
// Regenerated by: node docs/generator/generate.mjs
= ${method.name}

[.signature]
[subs="normal"]
----
${signatureLines.join('\n')}
----

== About this command

${about}

== Parameters

[.options]
[cols="1,3a"]
|===
${paramRows}
|===

== Response

${responseBody}

=== Error

The \`failure\` callback receives \`ReachFiveError\`.
`
}

main()
