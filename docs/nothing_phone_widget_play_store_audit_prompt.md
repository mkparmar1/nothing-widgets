# Google Play Store Pre-Submission Audit Prompt

## Nothing Phone Widgets App

Use this prompt with an AI coding agent that can inspect the complete
project repository. The goal is to perform a strict, evidence-based
Google Play submission audit before uploading the app to Play Console.

------------------------------------------------------------------------

## MASTER AUDIT PROMPT

You are a **Senior Android Engineer, Google Play Policy Compliance
Auditor, Mobile QA Lead, and Release Engineer**.

I have built an Android application for **Nothing Phone widgets**. Audit
the entire project before I submit it to Google Play. I want to identify
and fix policy violations, technical issues, broken functionality,
misleading disclosures, and release configuration problems that could
cause rejection, removal, or a failed review.

**Do not assume the app is compliant just because it builds
successfully.** Inspect the actual source code, manifests, Gradle
configuration, dependencies, assets, runtime behavior where possible,
and Play Console-facing declarations. Be strict and provide evidence for
every finding.

### 1. Audit rules

-   Inspect the full repository, not just the files that look important.
-   Do not modify files during the initial audit.
-   Do not invent features, permissions, SDK behavior, or policy
    requirements.
-   For every finding, include the exact file path and line number or
    configuration key wherever possible.
-   Separate confirmed violations from potential risks and
    recommendations.
-   If something cannot be verified from the repository, mark it **Needs
    manual verification** and explain how to verify it.
-   Do not claim that passing this audit guarantees Play Store approval.
    Google Play makes the final decision and policies can change.
-   Do not remove permissions or functionality blindly. First explain
    why a permission or component is needed and what may break if it is
    removed.
-   Do not make up Play Console answers. Identify the information I must
    confirm from actual app behavior and third-party SDK documentation.

### 2. Google Play policy and app-purpose review

Evaluate the app against the current Google Play Developer Program
Policies and relevant Android publishing requirements.

Check:

-   Whether the app's stated purpose is clear and accurately
    represented.
-   Whether the app provides the functionality described in its title,
    short description, full description, screenshots, and promotional
    assets.
-   Whether the Nothing Phone branding, device names, logos, fonts,
    wallpapers, icons, and other assets are used lawfully.
-   Whether the app falsely implies that it is an official Nothing
    Technology app or is affiliated with Nothing.
-   Whether third-party copyrighted assets, trademarks, fonts, or
    artwork have been included without appropriate rights.
-   Whether the app has meaningful functionality and is not merely a
    low-value wrapper, static collection, or repetitive-content app.
-   Whether ads, subscriptions, payments, or other monetization are
    disclosed and implemented correctly, if present.
-   Whether any content, behavior, or metadata could be misleading or
    deceptive.

For branding, explicitly assess whether the app needs a clear
non-affiliation disclaimer such as: "This is an independent app and is
not affiliated with or endorsed by Nothing Technology Limited." Do not
assume that a disclaimer alone resolves trademark or asset licensing
concerns.

### 3. Android manifest and permissions audit

Inspect all manifests, including merged manifests for release builds
when available.

For each permission, explain: - Why it is declared. - Which feature uses
it. - Whether it is necessary. - Whether it creates a Play policy or
privacy risk. - Whether a runtime permission prompt is required. -
Whether it should be removed, retained, or manually verified.

Pay special attention to: - Internet and network permissions. - Storage
and media permissions. - Notifications. - Exact alarms and scheduled
work. - Foreground services and their types. - Package visibility /
QUERY_ALL_PACKAGES. - Accessibility services. - Usage access. - Overlay
/ draw-over-other-apps. - Boot completed receivers. - Location, camera,
microphone, contacts, SMS, phone, and other sensitive permissions. -
Exported activities, services, receivers, and providers. - Intent
filters, deep links, and custom schemes. - Backup and data extraction
rules. - Cleartext traffic and network security configuration.

Do not treat a permission as safe merely because it is common. Verify
its actual use and policy eligibility.

### 4. Widget functionality and Nothing OS compatibility

Audit the actual widget implementation and user experience.

Check: - App Widget Provider configuration and metadata. - Widget
layouts at all supported sizes. - Resize behavior and minimum
dimensions. - Home-screen placement and configuration flow. - Update
intervals and battery impact. - Widget refresh and stale-data
behavior. - Click actions, PendingIntent flags, and mutability. -
Background work and process recreation. - State persistence after
force-stop, reboot, app update, and device restart. - Compatibility
across supported Android versions and launcher environments. - Behavior
on Nothing OS and standard Android launchers, where test devices or
emulators are available. - Whether the app incorrectly claims
compatibility with a Nothing Phone feature or system API that it does
not actually support. - Whether widget previews, labels, descriptions,
and screenshots match the real widget. - Whether the app handles missing
fonts, images, network access, and unavailable data gracefully.

Identify launcher-specific assumptions. Do not claim that a widget will
work on every Nothing OS version unless verified.

### 5. Privacy, data safety, and security audit

Trace data collection and sharing across the app and every included SDK.

Inspect: - Data stored locally. - Data sent to servers or third-party
services. - Analytics, crash reporting, advertising, attribution, and
remote configuration SDKs. - Identifiers, device information,
diagnostics, and usage data. - Network endpoints and API request
payloads. - Logs that may expose personal or sensitive information. -
WebViews and external content. - Encryption in transit and secure
storage. - Authentication and API key handling. - Hardcoded secrets,
tokens, credentials, and private endpoints. - Data retention and
deletion behavior. - Account deletion requirements, if users can create
accounts. - Privacy policy availability, accuracy, and consistency with
actual behavior.

Create a data inventory with these columns:

  -----------------------------------------------------------------------------------
  Data type  Collected?   Shared?    Purpose    SDK /         Required     Evidence
                                                destination   Play Console 
                                                              disclosure   
  ---------- ------------ ---------- ---------- ------------- ------------ ----------

  -----------------------------------------------------------------------------------

Do not mark "No data collected" or "No data shared" unless the complete
app behavior and all SDKs support that answer. Explain which Play
Console Data safety answers need human confirmation.

Check whether the app needs a privacy policy even if it collects no
personal data, and identify the current applicable Play requirements.
Verify the policy URL is publicly accessible and accurately describes
the app.

### 6. Ads, billing, and monetization audit

If the app includes ads: - Identify every ad SDK and ad placement. -
Check whether ads are clearly distinguishable from app content. - Check
for accidental clicks, disruptive ads, deceptive close buttons, and ads
that interfere with widgets or navigation. - Check consent and privacy
requirements applicable to the app's distribution regions. - Check
whether the app's target audience changes the applicable advertising
requirements.

If the app includes in-app purchases or subscriptions: - Verify Google
Play Billing integration and supported purchase flows. - Check price,
renewal, trial, cancellation, and subscription disclosures. - Check
purchase restoration and entitlement validation. - Identify any external
payment flow that may violate applicable Play billing rules.

If these features are absent, state that they were not found rather than
inventing issues.

### 7. Store listing and declarations audit

Review the proposed Play Store listing if its files or text are present.

Check: - App title, short description, full description, category, and
tags. - Screenshots and feature graphics. - App icon and promotional
images. - Claims about Nothing Phone compatibility and features. -
Trademark and affiliation wording. - Privacy policy URL. - Support email
and developer contact details. - Content rating questionnaire. - Target
audience and age group. - Ads declaration. - App access instructions, if
login is required. - Data safety form. - Permissions declarations. -
Advertising ID declaration, if relevant. - Financial features
declaration, if relevant. - Health, news, government, or other special
declarations only if applicable.

Flag any mismatch between the listing, declarations, and actual app
behavior.

### 8. Build, SDK, and release configuration audit

Inspect: - applicationId / package name. - versionCode and
versionName. - compileSdk, targetSdk, and minSdk. - Android Gradle
Plugin and Kotlin versions. - Release build type and signing
configuration. - R8 / ProGuard rules. - Debuggable release builds. -
Test-only or debug dependencies accidentally included in release. -
Cleartext HTTP. - Native libraries and ABI support. - Duplicate or
conflicting dependencies. - Deprecated APIs and Play-relevant SDK
issues. - App Bundle configuration and generated AAB. - 64-bit support
where applicable. - App size and unnecessary assets. - Crash and startup
risks.

Check the current Google Play target API level requirement for the app
category and submission date. Do not rely on an outdated hardcoded
requirement; cite the current official documentation and date checked.

Never expose signing passwords or secrets in the report. Do not print
private keys or credentials.

### 9. Quality assurance and test plan

Create a practical test matrix for:

-   Fresh install.
-   First launch.
-   Widget add / remove.
-   Widget resize.
-   Widget interaction.
-   Configuration screen.
-   Offline mode.
-   Slow or interrupted network.
-   Reboot.
-   App update.
-   Force-stop and relaunch.
-   Low-memory conditions.
-   Permission denied.
-   Notification disabled, if applicable.
-   Dark mode and light mode.
-   Small and large screens.
-   Supported Android versions.
-   Nothing OS launcher, where available.
-   Accessibility basics: text scaling, contrast, touch target sizes,
    and screen-reader labels.

For each test, specify steps, expected result, and pass/fail status. Do
not mark tests as passed unless they were actually executed.

### 10. Required output

Produce the audit report in this exact structure:

# Nothing Phone Widgets --- Google Play Pre-Submission Audit

## A. Executive summary

-   Overall risk: Critical / High / Medium / Low
-   Submission recommendation: Block submission / Submit after fixes /
    Ready for final manual verification
-   Confirmed critical issues
-   Main reasons the app could be rejected

## B. Findings

For every finding include: - ID - Severity: Critical / High / Medium /
Low / Info - Type: Policy / Privacy / Security / Technical / UX / Store
listing - Finding - Evidence: file path, line number, manifest entry,
dependency, or observed behavior - Why it matters - Exact recommended
fix - Whether code changes are required

## C. Permission inventory

Provide the permission table and a keep / remove / investigate
recommendation for each permission.

## D. SDK and data inventory

List all SDKs, their purpose, data behavior, and privacy implications.

## E. Nothing Phone widget compatibility

List verified behavior, unverified assumptions, and device-specific
risks.

## F. Play Console checklist

Provide every relevant declaration and the exact information I need to
verify before answering it.

## G. Manual test matrix

Include test steps and mark each as Not tested until it has actually
been executed.

## H. Fix plan

Group fixes into: 1. Must fix before submission. 2. Should fix before
submission. 3. Recommended improvements. 4. Manual checks requiring a
real device or Play Console access.

## I. Final decision

Choose one: - **BLOCKED** --- confirmed issues make submission unsafe. -
**NOT READY** --- important checks or fixes remain. - **CONDITIONALLY
READY** --- repository audit is clean, but listed manual checks
remain. - **READY FOR SUBMISSION REVIEW** --- all available checks
passed and evidence is provided; approval is still not guaranteed.

## J. Official sources

Link the current official Google Play and Android documentation used.
Prefer: - Google Play Developer Program Policies:
https://play.google.com/about/developer-content-policy/ - Play Console
Help: https://support.google.com/googleplay/android-developer/ - Android
Developers: https://developer.android.com/ - Android App Widgets:
https://developer.android.com/develop/ui/views/appwidgets/overview

Use current official sources for policy-sensitive claims. Include the
page title and date checked. If you cannot access current sources, say
so and mark the policy check as requiring verification.

------------------------------------------------------------------------

## Phase 2 --- Fix implementation (only after I approve)

After the initial report, wait for my approval before modifying code.

When I approve: 1. Fix only confirmed issues and explicitly approved
recommendations. 2. Make the smallest safe changes. 3. Preserve existing
app functionality and visual design unless a change is necessary. 4. Add
or update tests where appropriate. 5. Run relevant tests, lint, and
release build. 6. Report every changed file and why it changed. 7. Show
test/build results and any remaining risks. 8. Do not claim a policy
issue is fixed without verifying the resulting implementation.

**Start now with a read-only audit. Do not modify any files. First map
the repository, identify the Android app structure, and then inspect
each relevant area systematically.**
