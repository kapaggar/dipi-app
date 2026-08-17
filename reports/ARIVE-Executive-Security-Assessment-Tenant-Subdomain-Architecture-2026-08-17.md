# ARIVE Platform Security Assessment

## Tenant Subdomain Architecture (`*.myarive.com`)

---

| Field | Detail |
|-------|--------|
| **Document classification** | Confidential — Executive Distribution |
| **Prepared for** | ARIVE Leadership (Executive Team) |
| **Assessment date** | 17 August 2026 |
| **Assessment type** | Architecture review, external reconnaissance, tenant configuration analysis |
| **Scope** | `*.myarive.com` multi-tenant subdomain provisioning, DNS/TLS/CDN routing, authentication boundaries, and phishing/abuse exposure |
| **Methodology** | Non-intrusive external validation; review of tenant configuration export (~10,190 records); controlled HTTP/TLS/DNS probes; client application bundle analysis |
| **Overall risk rating** | **High** (with one **Critical** conditional finding pending cookie-scope verification) |

---

## 1. Executive Summary

ARIVE operates a wildcard tenant namespace in which each loan officer (LO) organization receives a branded URL of the form `{label}.myarive.com`. External assessment confirms that **any subdomain label**—whether assigned to a real tenant, a reserved system term, or a nonexistent organization—**resolves publicly, presents a valid TLS certificate, and serves an identical production web application**.

This architecture creates a **first-party abuse surface**: a malicious or compromised LO account does not need to spoof ARIVE infrastructure. The attacker operates *on* ARIVE infrastructure with a hostname that borrowers, realtors, and staff reasonably interpret as official.

### Key conclusions

1. **The primary risk is architectural, not cosmetic.** Individual subdomain names in a tenant list are indicators of a deeper provisioning and routing model that lacks reserved-word enforcement and server-side tenant validation at the edge.

2. **High-value system labels are either already claimed by tenants or live and unclaimed.** Labels such as `apply`, `signup`, `gateway`, and `arive` are in active use. Critical infrastructure labels including `login`, `admin`, `api`, and `secure` are **not assigned in the tenant database but are live on the internet today**, creating a race-to-register vulnerability.

3. **Phishing under a legitimate ARIVE certificate is structurally enabled.** An attacker with LO provisioning access can direct victims to `{convincing-label}.myarive.com` and deliver the authentic ARIVE user interface over HTTPS.

4. **One conditional finding could elevate impact to Critical.** If authentication cookies are scoped to the parent domain (`.myarive.com`) rather than host-only, cross-tenant session exposure becomes possible. This was not verifiable without authorized authenticated testing and must be treated as the highest-priority validation item.

### Recommended executive actions (30-day horizon)

| Priority | Action | Owner (suggested) |
|----------|--------|-------------------|
| P0 | Authorize and execute cross-tenant cookie/session test with two disposable tenants | CISO / Engineering |
| P0 | Implement reserved-word denylist at `losUrl` provisioning and block changes to high-risk labels | Product + Platform |
| P0 | Deploy edge tenant-existence validation (404/403 for unknown or reserved labels) | Platform / Cloud Engineering |
| P1 | Conduct backfill review of platform-brand and system-word subdomains already assigned | Security + Legal + Customer Success |
| P1 | Constrain `losUrl` to approved patterns; restrict or gate off-platform URLs | Product + API |
| P2 | Establish ongoing monitoring and abuse-reporting workflow for tenant URL policy violations | Security Operations |

---

## 2. Background and Business Context

ARIVE provides mortgage origination software to loan officers and their organizations. Tenant-branded URLs (`*.myarive.com`) serve as the primary entry point for LO staff, processors, borrowers (POS portal), and partners.

Users naturally infer trust from:

- The **ARIVE brand** and familiar application interface
- A **valid HTTPS padlock** on `*.myarive.com`
- **Official-looking hostname labels** such as `apply`, `login`, `secure`, or `arive`

When any participant can obtain a subdomain label without policy enforcement, the platform inadvertently becomes a **trusted delivery channel for social engineering**, with potential regulatory and reputational consequences under GLBA, state privacy laws, and industry fraud reporting obligations.

---

## 3. Scope and Methodology

### 3.1 In scope

- Public DNS, TLS, and HTTP behavior for `*.myarive.com`
- CloudFront/S3 delivery pattern for tenant hostnames
- Tenant `losUrl` configuration patterns (full export: **10,190** records)
- Targeted review of flagged high-risk tenants (30-record triage sample)
- Production SPA configuration and authentication integration (Auth0 via `auth.lendwize.io`)
- Separately hosted API subdomains (`ppeapi.myarive.com`, `losapi.myarive.com`, etc.)

### 3.2 Out of scope (requires authorized engagement)

- Authenticated penetration testing
- Source code and backend API authorization review
- Auth0 tenant configuration audit
- Email template and notification pipeline review
- Legal review of tenant contracts and Acceptable Use Policy enforcement

### 3.3 Methods

| Method | Application |
|--------|-------------|
| DNS enumeration | Confirmed wildcard resolution via CloudFront (`d20r2fh8inux71.cloudfront.net`) |
| HTTP/TLS probing | Compared response headers, body size, and ETag across reserved, tenant, and nonexistent labels |
| Certificate inspection | Verified Subject Alternative Name coverage: `myarive.com`, `*.myarive.com` |
| Configuration analysis | Parsed tenant database export for `losUrl` patterns, reserved-word collisions, off-platform URLs |
| Client bundle review | Identified hostname handling, Auth0 integration, and special-case logic for `login` subdomain |

### 3.4 Limitations

- Testing was **read-only** and **unauthenticated**. Cookie scope, API authorization boundaries, and post-login tenant isolation could not be conclusively determined.
- Findings reflect conditions observed on **17 August 2026**; live infrastructure may change.
- Legitimate businesses may hold coincidentally sensitive-sounding names (e.g., `trust.myarive.com` — Trust Mortgage LLC). Risk assessment distinguishes **malicious intent** from **trust confusion**.

---

## 4. Architecture Summary (Current State)

```
                    ┌─────────────────────────────────────┐
                    │  *.myarive.com  (wildcard DNS)      │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │  Amazon CloudFront (single pattern)   │
                    │  Valid TLS: *.myarive.com           │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │  Amazon S3 — single Angular SPA     │
                    │  Same 55,374-byte shell all hosts   │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │  Client-side tenant resolution      │
                    │  Auth0 @ auth.lendwize.io           │
                    │  APIs @ *api.myarive.com            │
                    └─────────────────────────────────────┘
```

**Observed behavior:** `login.myarive.com`, `admin.myarive.com`, `apply.myarive.com`, `nonexistent-tenant-xyz12345.myarive.com`, and all tested labels returned **HTTP 200** with **identical content** (ETag: `ba8735ad2d6636d5cc2c92fb9082ec98`).

**Implication:** The edge layer does not distinguish official system infrastructure from tenant-chosen or unprovisioned hostnames.

---

## 5. Findings

Findings are rated using qualitative severity aligned to ISO 27005 / NIST SP 800-30 conventions.

| ID | Finding | Severity | Status |
|----|---------|----------|--------|
| F-01 | Wildcard subdomain serves live application without tenant validation | **High** | Confirmed |
| F-02 | No reserved-word enforcement on tenant URL provisioning | **High** | Confirmed |
| F-03 | Platform-brand and system-word subdomains assigned to tenants | **High** | Confirmed |
| F-04 | Critical infrastructure labels live but unassigned (`login`, `admin`, `api`, etc.) | **High** | Confirmed |
| F-05 | Application contains special logic for `login` subdomain without reservation | **High** | Confirmed |
| F-06 | Off-platform `losUrl` values accepted in configuration | **Medium** | Confirmed (n=2) |
| F-07 | Authentication cookie domain scope unknown; potential cross-tenant exposure | **Critical (conditional)** | Requires test |
| F-08 | Weak content security posture on tenant-facing SPA | **Medium** | Confirmed |
| F-09 | Typosquatting within namespace (`arise.myarive.com`) | **Low** | Confirmed |

---

### F-01: Wildcard Subdomain Serves Live Application Without Tenant Validation

**Severity:** High  
**Confidence:** High

Any `{label}.myarive.com` hostname returns the production SPA shell with HTTP 200, including labels with no corresponding tenant record. Users and security tools cannot rely on DNS resolution or TLS presence as indicators of a legitimate, provisioned tenant.

**Business impact:** Enables ghost URLs, post-offboarding confusion, and attacker-controlled labels that appear equally valid to end users.

**Evidence:** Nonexistent label `nonexistent-tenant-xyz12345.myarive.com` returned 200, content-length 55374, identical ETag to `apply.myarive.com`.

---

### F-02: No Reserved-Word Enforcement on Tenant URL Provisioning

**Severity:** High  
**Confidence:** High

Analysis of 10,190 tenant records identified **16 bare reserved-word collisions** where the subdomain label exactly matches a term commonly associated with platform infrastructure or security functions:

| Subdomain | Organization (from export) | Org ID |
|-----------|------------------------------|--------|
| `apply.myarive.com` | UW Funding Inc. | 1747 |
| `signup.myarive.com` | SIGNUP ORG | 39 |
| `gateway.myarive.com` | Gateway Financial Group | 1360 |
| `loan.myarive.com` | California Realty and Mortgage | 966 |
| `myloan.myarive.com` | AAA Lending LLC | 10437 |
| `welcome.myarive.com` | Welcome Mortgage | 3289 |
| `arive.myarive.com` | Market Place Org for LP (Do not Change) | 14078 |
| `mortgage.myarive.com` | Castle Home Loans | 897 |
| `trust.myarive.com` | Trust Mortgage LLC | 9453 |
| `aml.myarive.com` | American Mortgage Lenders, Corp | 6382 |
| `lender.myarive.com` | Exclusive Mortgage Company | 3669 |
| `investor.myarive.com` | Investor First Lending | 9686 |
| `crm.myarive.com` | Castle Rock Mortgage LLC | 8801 |
| `link.myarive.com` | First Link Mortgage | 6735 |
| `cookie.myarive.com` | COOKIE HOME LOANS LLC | 830 |
| `star.myarive.com` | Star Mortgage and Finance LLC | 1521 |

Additional platform-brand variants include `ariveprocessingtest`, `ctcarive`, `dansarive`, and others.

**Business impact:** Borrowers receiving links to `apply.myarive.com` or `signup.myarive.com` have no reliable method to distinguish a specific LO tenant from an official ARIVE system endpoint.

---

### F-03: Platform-Brand Subdomain Assigned to Tenant

**Severity:** High  
**Confidence:** High

`arive.myarive.com` is assigned to org ID 14078. A hostname of this form strongly implies vendor-operated infrastructure rather than a customer organization.

**Business impact:** Support fraud, fake billing or compliance notices, and staff-targeted phishing with elevated credibility.

---

### F-04: Critical Infrastructure Labels Live but Unassigned

**Severity:** High  
**Confidence:** High

The following labels were **not present** in the tenant configuration export but **resolved and served the live SPA** at time of assessment:

`login`, `admin`, `api`, `secure`, `portal`, `support`, `verify`, `billing`, `pay`, `mail`, `www`, `status`, `dashboard`, `oauth`, `sso`, `signin`

**Business impact:** Any party able to provision `losUrl` can preemptively claim these labels before ARIVE deploys official system services, creating operational lock-in and security ambiguity.

---

### F-05: Application Special-Cases `login` Subdomain Without Reservation

**Severity:** High  
**Confidence:** High

Production client code includes explicit handling: when the first subdomain label equals `login` and an unauthorized response includes a `loginUrl`, the browser is redirected to that URL. This indicates **product intent** to use `login.myarive.com` as infrastructure—yet the label is not reserved in provisioning controls.

**Business impact:** Race condition between platform engineering plans and tenant self-service URL selection.

---

### F-06: Off-Platform `losUrl` Values Accepted

**Severity:** Medium  
**Confidence:** High

Two tenants in the export point `losUrl` to external domains:

| Organization | Org ID | losUrl |
|--------------|--------|--------|
| Garden State Home Loans | 11 | `https://portal.gardenstateloans.com` |
| Pacaso | 3492 | `https://los.pacaso.com` |

**Business impact:** If `losUrl` is rendered in ARIVE-branded email, UI, or redirects, a tenant could direct users to an attacker-controlled external site while maintaining the trust relationship established through ARIVE communications.

---

### F-07: Authentication Cookie Domain Scope Unknown (Conditional Critical)

**Severity:** Critical (if confirmed) / High (if host-only)  
**Confidence:** Low (not externally observable)

The application uses Auth0 (`auth.lendwize.io`) with SPA cookie storage supporting an optional `cookieDomain` parameter. If session or transaction cookies are set with `Domain=.myarive.com`, **every subdomain inherits cookie visibility**, enabling potential cross-tenant session leakage and CSRF-class attacks.

**Business impact:** Cross-tenant data exposure, account compromise across organizational boundaries, regulatory breach notification triggers.

**Required validation:** Authorized test with two isolated test tenants—login to Tenant A, visit Tenant B without re-authentication, observe cookie `Domain` attribute and API/data access.

---

### F-08: Limited Security Headers on Tenant-Facing Application

**Severity:** Medium  
**Confidence:** High

Observed Content-Security-Policy: `frame-ancestors 'none'` with telemetry reporting. No Strict-Transport-Security header observed on probe. Third-party scripts include customer-support and analytics integrations (e.g., Intercom, Freshworks, Appcues, Microsoft Clarity).

**Business impact:** Expanded attack surface for script injection if combined with other vulnerabilities; reduced defense-in-depth on borrower-facing flows.

---

### F-09: Typosquatting Within Namespace

**Severity:** Low  
**Confidence:** High

`arise.myarive.com` (Arise Lending LLC, org ID 2325) is visually confusable with `arive` at edit distance 1. Appears to be a legitimate business; noted for partner/staff targeting scenarios.

---

## 6. Threat Scenarios and Exploitation Paths

The following scenarios are **feasible under the current architecture** and do not require sophisticated infrastructure attacks.

### Scenario A — Borrower Phishing on Authentic ARIVE Infrastructure

| Step | Description |
|------|-------------|
| 1 | Attacker obtains LO account (fraudulent signup, insider, or compromised credentials) |
| 2 | Configures or uses a convincing `losUrl` (e.g., `secure-upload.myarive.com`, or an already-confusing label such as `apply.myarive.com`) |
| 3 | Sends email/SMS to borrowers: "Complete your secure document upload" |
| 4 | Victim observes valid TLS on `*.myarive.com` and authentic ARIVE interface |
| 5 | Attacker harvests credentials, PII, or redirects to wire-fraud follow-up |

**Detection difficulty:** High. Indicators of compromise are not traditional phishing domains; they are legitimate first-party hosts.

---

### Scenario B — Infrastructure Label Squatting

| Step | Description |
|------|-------------|
| 1 | Attacker registers `losUrl` = `https://login.myarive.com` before ARIVE reserves it |
| 2 | Platform cannot deploy centralized login without tenant conflict or legal/commercial dispute |
| 3 | Users visiting `login.myarive.com` receive tenant-controlled experience |

**Current state:** Label is live and unassigned—window is open.

---

### Scenario C — Vendor Impersonation via `arive.myarive.com`

| Step | Description |
|------|-------------|
| 1 | Attacker (or compromised tenant admin of org 14078) sends "ARIVE security update" link |
| 2 | Target LO staff believe they are interacting with the platform vendor |
| 3 | Credential or MFA fatigue attacks against administrative users |

---

### Scenario D — Cross-Tenant Session Abuse (Conditional)

| Step | Description |
|------|-------------|
| 1 | User authenticates at `tenantA.myarive.com` |
| 2 | User clicks link to `tenantB.myarive.com` (attacker-controlled label) |
| 3 | If cookies are parent-scoped, session artifacts are sent to Tenant B origin |
| 4 | Potential unauthorized actions if server-side tenant binding is insufficient |

**Status:** Unverified. Treat as Critical until disproven.

---

## 7. Risk Assessment Summary

| Risk category | Likelihood | Impact | Residual risk |
|---------------|------------|--------|---------------|
| First-party borrower phishing | High | High | **High** |
| Infrastructure label squatting | Medium | High | **High** |
| Cross-tenant session exposure | Unknown | Critical | **Critical (conditional)** |
| Vendor impersonation (`arive.*`) | Medium | High | **High** |
| Off-platform redirect abuse | Low (current n=2) | Medium | **Medium** |
| Typosquatting | Low | Medium | **Low** |

### What DMARC does and does not protect

`myarive.com` publishes DMARC `p=reject`, which materially reduces **email spoofing of `@myarive.com` addresses**. It does **not** mitigate **hyperlink phishing** to valid `*.myarive.com` URLs sent from arbitrary external mail domains—a primary abuse vector in this model.

---

## 8. Recommendations

### 8.1 Immediate (0–30 days)

#### R-01: Execute authorized cookie and tenant-isolation test
- Provision two disposable test tenants
- Document Auth0 cookie attributes (`Domain`, `Secure`, `HttpOnly`, `SameSite`)
- Attempt cross-subdomain session reuse and API calls
- **Success criteria:** Confirmed host-only cookies and server-side tenant enforcement on all API paths

#### R-02: Implement reserved-word denylist at provisioning
Enforce at `losUrl` create/update in API and admin tooling. Seed list (minimum):

```
login, logon, signin, auth, sso, oauth, secure, security, account, accounts,
admin, portal, support, verify, billing, pay, payment, payments, api, app, apps,
mail, www, my, id, system, status, dashboard, console, gateway, signup, apply,
esign, docs, arive, myarive, lendwize, help, download, mobile, static, cdn,
dev, staging, test, prod, internal, official, secureupload, identity, reset,
password, mfa, wire, paymentportal, paynow, nmls, consumer, gov, fha, va, usda
```

Apply pattern rules: block labels starting with `arive-`, ending in `-login`, and homoglyph variants.

#### R-03: Deploy edge tenant validation
Implement CloudFront Function or Lambda@Edge to:
- Return **404** for hostnames with no provisioned tenant
- Return **403** for reserved labels not explicitly mapped to platform services
- Maintain cached allowlist synchronized from org configuration service

#### R-04: Emergency review of P1 subdomain assignments
Prioritize executive decision on:
- `arive.myarive.com` (platform brand)
- `apply.myarive.com`, `signup.myarive.com`, `gateway.myarive.com` (system semantics)
- Document remediation: rename, reclaim, or formal exception with compensating controls

---

### 8.2 Short-term (30–90 days)

#### R-05: Architectural separation of platform vs tenant hostnames
Long-term target: platform services on dedicated hosts (e.g., `login.arive.com`, `app.arive.com`) and tenant URLs on a non-confusable namespace (e.g., `{tenant}.lo.arive.com`).

#### R-06: Constrain `losUrl` input
- Allow only `https://{label}.myarive.com` matching registered slug
- Block or require security review for external URLs
- If external URLs permitted: mandatory interstitial warning displaying final destination domain

#### R-07: Borrower-facing trust indicators
Display immutable org legal name, NMLS ID, and registered portal URL on authentication and document-upload screens. Warn on hostname/org mismatch.

#### R-08: Security headers and CSP hardening
Deploy HSTS (includeSubDomains after validation), tighten CSP on borrower flows, and minimize third-party script exposure on POS portal paths.

#### R-09: Monitoring and alerting
Alert on: new reserved-pattern `losUrl`, Levenshtein distance ≤1 to `arive`, off-platform URL changes, and rapid subdomain relabeling.

---

### 8.3 Governance and compliance (ongoing)

#### R-10: Update Acceptable Use Policy and onboarding
Prohibit misleading subdomain selection. Include examples of prohibited labels. Enable contractual suspension for phishing tenants.

#### R-11: Establish abuse reporting channel
Publish `report-abuse@arive.com` (or equivalent) with defined SLAs for phishing tenant investigation and takedown.

#### R-12: Executive metrics dashboard
Track: count of reserved-word tenants, unvalidated hostname request rate (post-fix), abuse reports, mean time to suspend abusive tenant.

---

## 9. Suggested Remediation Roadmap

```
Phase 1 (Week 1-2)     Phase 2 (Week 3-6)        Phase 3 (Month 2-3)
─────────────────     ──────────────────        ───────────────────
Cookie/session test   Edge tenant validation    Architecture split
Reserved-word block   P1 subdomain backfill     Borrower trust UX
Executive decisions   losUrl constraints        Monitoring + AUP
```

---

## 10. Conclusion

The `*.myarive.com` tenant subdomain model provides valuable white-label branding but, in its current form, **conflates customer-chosen hostnames with potential platform infrastructure** and **serves a live application for every conceivable label without edge validation**. This is not primarily a problem of a few unfortunate tenant names—it is a **systemic trust-boundary issue** that enables credible phishing, impersonation, and (pending verification) possible cross-tenant session exposure.

Remediation should prioritize:

1. **Validating authentication cookie scope**
2. **Stopping further reserved-label provisioning**
3. **Rejecting unknown hostnames at the CDN edge**
4. **Executive remediation of already-assigned high-risk labels**

With these controls, the tenant naming problem largely ceases to grow—and ARIVE restores a meaningful distinction between *official platform infrastructure* and *customer-branded portals*.

---

## Appendix A: Assessment Evidence Summary

| Observation | Result |
|-------------|--------|
| Wildcard DNS target | `d20r2fh8inux71.cloudfront.net` |
| TLS SAN | `myarive.com`, `*.myarive.com` |
| SPA response size (all tested hosts) | 55,374 bytes |
| SPA ETag (all tested hosts) | `ba8735ad2d6636d5cc2c92fb9082ec98` |
| Tenant records analyzed | 10,190 |
| Numeric subdomain labels | 5,627 (55.2%) |
| Named subdomain labels | 4,563 (44.8%) |
| Off-platform losUrl count | 2 |
| DMARC policy | `p=reject` |
| Auth0 domain (from client config) | `auth.lendwize.io` |
| API hosts (from client config) | `ppeapi.myarive.com`, `losapi.myarive.com`, `leadapi.myarive.com`, others |

---

## Appendix B: High-Priority Triage Sample (from detailed review)

| Priority | Category | Subdomain | Notes |
|----------|----------|-----------|-------|
| P1 | Platform brand | `arive` | Impersonates operator |
| P1 | Reserved system word | `apply`, `signup`, `gateway`, `loan`, `myloan`, `welcome` | Reads as official infra |
| P1 | Platform brand variants | `ariveprocessingtest`, `ctcarive`, `dansarive`, etc. | Brand confusion |
| P2 | Off-platform losUrl | `portal.gardenstateloans.com`, `los.pacaso.com` | External redirect risk |
| P2 | Typosquat | `arise` | Edit distance 1 from `arive` |
| P3 | System-word substring | `applynow`, `bsmlogin`, `docverifyprocessing`, etc. | Lower priority |

---

## Appendix C: Recommended Authorized Follow-On Assessments

1. **Authenticated penetration test** — cross-tenant API authorization, IDOR, and session management
2. **Auth0 configuration review** — callback URLs, cookie policy, organization/connection mapping
3. **Email and notification pipeline review** — all surfaces rendering `losUrl`
4. **Red team exercise** — simulated borrower phishing using test tenant on approved label
5. **Legal/compliance review** — tenant suspension authority, breach notification playbooks

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-17 | Security Assessment Team | Initial executive release |

---

*This document contains confidential security information intended for ARIVE executive and authorized personnel only. Distribution outside the organization requires written approval.*
