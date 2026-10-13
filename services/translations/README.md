# Optional translations backend

Own code: GPL-3.0-or-later. Node 22+, npm ci, npm test. Existing Netlify Blobs
dependency is backend-only; see package-lock and THIRD-PARTY-NOTICES.
Configure ADMIN_TOKEN and DOWNLOAD_SIGNING_KEY only as server environment values.
No secret belongs in this tree, public pages, netlify.toml or Android build.
Native /api/translations/access returns a one-hour signed anonymous session.
No stable user/install ID, purchase proof or Google SDK. Browser-origin/header
checks are convenience restrictions, not unforgeable client authentication.
POST ticket requires an approved patch and matching source hash; download requires
the bound bearer plus the 120-second HMAC URL. Rate-limit rule must be confirmed
in Netlify postprocessing logs after deployment; distributed enforcement may lag.
Anonymous endpoints may be mimicked: do not claim guaranteed app exclusivity.
