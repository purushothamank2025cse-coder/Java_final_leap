---
schema: clarification-answers/v1
status: submitted
generated_at: "2026-09-28T09:41:51.878Z"
scope: [frontend, backend, generic]
questions_file: clarification-questions.json
---

# Rearchitecture Clarification Answers

## 🖥️ Frontend

- **F1** Should the redesign keep the existing static HTML/CSS and vanilla JavaScript, or move to a frontend framework?
  - answer: Keep static HTML/CSS and vanilla JavaScript
  - source: user
- **F2** Which component or styling approach should be used for the refreshed UI?
  - answer: Tailwind CSS
  - source: user
- **F3** Should I use the existing Carepoint UI and the previously shared healthcare landing-page screenshot as visual references, or do you have another screenshot or mockup to follow?
  - answer: more attractive doctor website theme u refer and do the professional best ui
  - source: user
- **F4** What design system should guide the new professional theme? Include preferred brand colors, clinic name/logo, and any visual elements to keep or avoid.
  - answer: change color so that it look clean and elegant proffessional look
  - source: user
- **F5** What accessibility standard should the UI target?
  - answer: WCAG 2.1 AA
  - source: user
- **F6** Which browsers should the redesigned clinic site support?
  - answer: Modern Chrome, Edge, Firefox, and Safari
  - source: user
- **F7** How should the UI adapt to mobile, tablet, and desktop screens?
  - answer: Responsive mobile, tablet, and desktop
  - source: user
- **F8** Which UI languages should be included?
  - answer: preserve current locales
  - source: user
- **F9** What frontend state-management approach do you prefer?
  - answer: Keep the current vanilla JavaScript approach
  - source: user
- **F10** How should public, sign-in, sign-up, and admin pages be routed?
  - answer: de-facto router for selected framework
  - source: user

## ⚙️ Backend

- **B1** Which backend framework and version should remain or be targeted?
  - answer: Keep Spring Boot 4.1.1 and Java 17
  - source: user
- **B2** How should the existing clinic API endpoints and their behavior be handled while adding authentication?
  - answer: Preserve endpoint contracts and protect them with authorization
  - source: user
- **B3** How should authentication accounts be added to the existing MySQL database and clinic data?
  - answer: Add account tables in-place and preserve existing clinic records
  - source: user
- **B4** Which sign-up and admin access policy do you want? Public signup must not let users grant themselves admin privileges.
  - answer: preserve existing auth mechanism; detect from codebase
  - source: user
- **B5** Are there specific availability, response-time, or traffic requirements?
  - answer: not now needed
  - source: user

## 📋 General

- **G1** What should be true for this work to be considered complete?
  - answer: Professional, distinctive, responsive UI across public, sign-in, sign-up, and admin pages; secure login and sign-up; only authorized admins can access dashboard and clinic-management data; existing clinic workflows and records preserved; Lombok used where it improves Java clarity; tests pass.
  - source: user
- **G2** Are there any features or changes you want explicitly excluded from this work?
  - answer: infer from project structure
  - source: user
- **G3** How should existing tests be handled?
  - answer: must pass
  - source: user
- **G5** List any additional requirements, exclusions, dependencies, compliance rules, or operational constraints not covered above.
  - answer: None beyond the decisions listed in this specification.
  - source: user
