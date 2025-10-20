---
description: "Perform a comprehensive code review"
tools: ["changes"]
model: "GPT-4o"
---

You are reviewing a Git diff. Only read and comment on files returned by the `changes` tool.

- Start with: `changes list --scope=staged,unstaged`
- Fetch file contents via: `changes read --path <file>`
- Do not open or reference any file outside the changes list.
  Produce a per-file review with findings, risks, and fixes.
