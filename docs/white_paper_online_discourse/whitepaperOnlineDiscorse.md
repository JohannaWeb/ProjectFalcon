# White Paper: Identity-Driven Discourse Systems (IDDS) 2.1

**Author:** Johanna Almeida  
**Date:** March 2026  
**Category:** Systems Architecture / Social Dynamics  

## 1. Abstract
The Identity-Driven Discourse System (IDDS) maps the transition of online communication from rational exchange to non-recoverable conflict. This paper details the activation of identity layers and the resulting defensive mechanisms that collapse discourse. By quantifying these transitions, we can design AI-driven moderation tools that identify and mitigate escalation before it reaches a terminal state. Version 2.1 introduces the $D_{flag}$ modifier, Moral Protective Framing (MPF), and Adversarial Seeding detection.

## 2. Taxonomy of Identity Layers
IDDS categorizes identity into three compounding layers that define an individual's "threat surface" in conversation:

- **Personal Identity (P):** Encompasses lived experiences, self-perception, and individual history.
- **Ideological Identity (I):** Belief systems, socio-political frameworks, and core values.
- **Group Identity (G):** External affiliations such as nationality, ethnicity, or cultural belonging.

### 2.1 Voluntary Disclosure vs Forced Exposure
- **Voluntary Disclosure:** Identity shared freely in safe/neutral context. Does not inherently trigger escalation.
- **Forced Exposure:** Identity surfaced by an adversarial actor as a target. Activates all three layers simultaneously and sets $D_{flag} = 1$ by definition.

## 3. The Escalation Formula
Conflict intensity is a product of environmental and identity factors. The probability of escalation follows a revised model:

`P(E) = σ(Σ ωᵢ · Aᵢ · D_flag + S · TopicMultiplier)`

Where:
- **$A_i$:** Activation of layers (P, I, G).
- **$D_{flag}$:** Disagreement flag (0 or 1). Escalation probability is amplified by identity only when active disagreement is present.
- **$S$:** TopicMultiplier for high-stakes subjects.

## 4. Discourse State Transition Model
Online interactions move through a predictable degradation:

1. **Neutral:** Fact-based inquiry or sharing.
2. **Disagreement:** Divergent viewpoints without personal friction. Sets $D_{flag} = 1$.
3. **Identity Activation (Floating Modifier):** 
    - If $D_{flag} = 1$: Identity activation leads to **Escalation**.
    - If $D_{flag} = 0$: Identity activation remains **Neutral**.
4. **Personalization:** Arguments target the individual's character.
5. **Ad Hominem:** Rational engagement is replaced by direct attacks.
6. **Dogpile (Non-recoverable):** Collective hostility occurs.

## 5. Defensive Mechanisms
- **Victim Labeling & Minimization:** Reframing power dynamics.
- **National Defense:** Leveraging group identity to shield against critique.
- **Competence Attacks:** Questioning intelligence/authority.
- **Moral Protective Framing (MPF):** Using a third-party vulnerability (children, nation) to justify aggression.
    - `MPF_flag = 1` if: Third-party vulnerability invoked AND Target is a specific group AND $D_{flag} = 1$.

## 6. Systemic Amplifiers
- **Anonymity:** Removes social cost.
- **Platform Redirect:** Users bypass "Silence" (blocking) by broadcasting conflict to their own timelines, resetting decay.
- **Adversarial Seeding:** Posts designed to provoke identity activation. Thread is "born escalated" ($D_{flag} = 1$ at $T=0$).

---

# IDDS 2.1 — Session Notes & Addendums

## Addendum 3 — Silence Bypass via Platform Redirect
Modified mechanism: Silence terminates the loop **only within the original thread**. Cross-thread propagation detection is required for Sovereign Moderation.

## Addendum 4 — De-escalation Artifacts
Humor posts, memes, and non-sequiturs function as **local state resets**. Non-threatening content breaks the reinforcement loop by requiring no defensive response.

## Addendum 5 — Moral Protective Framing (MPF)
Definition: Use of a third-party vulnerability (children, family, nation) as ethical cover to justify escalatory behavior toward a target. It allows actors to jump from Disagreement directly to Threats of Violence while appearing to remain in Neutral.

## Addendum 6 — Adversarial Seeding
Detection signals: High-identity content + open engagement prompt ("what do you think?") + no prior context + high TopicMultiplier.

## Addendum 7 — Positive Reinforcement
Direct acknowledgment or validation can resolve the identity threat driving escalation. High-risk if perceived as insincere (Competence Defense trigger).

## Addendum 8 — Transient Dogpile Groups
Groups that reach Dogpile state, dissolve, and reform against new targets. They carry "escalation momentum" as their $D_{flag}$ never fully resets.

---

## Dataset Notes
- Manual labeling across 16 screenshots (Portuguese/English).
- Validated across Threads, Reddit, WhatsApp.
- **Labeling Schema:** `post_id, anon_user_id, text, local_state, global_state, d_flag, mpf_flag, topic_multiplier, language, thread_id`

## Open Questions for 2.1
- Formalize $\omega_i$ weight learning from labeled data.
- How to detect cross-thread propagation at scale.
- Reversibility — formal conditions for de-escalation transitions.
- MPF detection model — separate classifier needed.
- Integration path into Juntos / Falcon Sovereign Moderation layer.