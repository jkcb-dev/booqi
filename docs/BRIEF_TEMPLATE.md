# Agent brief template

Keep briefs short: the repo holds the rules, the brief holds only this ticket's decisions.

```
Implement issue #N on jkcb-dev/booqi as the <role> role.
Worktree: work ONLY in /Users/joco03/projekt-ia-p/Booqi-wt/N (branch <branch>, from origin/main,
local.properties copied); start every command with `cd <that path> &&`. Don't touch other worktrees.
Read: .claude/agents/booqi-<role>.md, docs/DEVELOPMENT.md, issue #N, <the doc § it implements>.
Understand the code with graphify first (docs/DEVELOPMENT.md § graphify), then read only what you need.
Decisions already made (follow them): <bullets — this is where Opus adds value>.
Out of scope / don't touch: <files other parallel tickets own>.
Done: `scripts/verify.sh <modules>` prints RESULT: OK; PR to main with `Closes #N`; report < 200 words
(what you built, deviations, real counts from verify.sh, PR URL).
```
