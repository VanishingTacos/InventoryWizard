# Security Policy

## Supported Versions

Security fixes are only made for the latest release of InventoryWizard. Older releases are not patched, so please update before reporting.

| Version          | Supported |
| ---------------- | --------- |
| Latest (2.3.x)   | ✅        |
| Older than 2.3.0 | ❌        |

This covers every loader and platform built from this repository: Fabric, Quilt, NeoForge, Forge and the Paper/Spigot plugin.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, pull requests or the Discord server.**

Report it privately through GitHub instead:

1. Go to the [Security tab](https://github.com/VanishingTacos/InventoryWizard/security) of this repository
2. Click **Report a vulnerability**
3. Fill in the form with as much detail as you can

Only the maintainers can see the report, and we can discuss it and work on a fix there in private.

### What to include

A good report helps us confirm and fix the problem quickly. Please include:

- The InventoryWizard version and jar name (for example `InventoryWizard-fabric-2.3.0+1.21.4.jar`)
- The loader or server (Fabric, Quilt, NeoForge, Forge, Paper or Spigot) and its version
- The Minecraft version
- A description of the vulnerability and what an attacker could do with it
- Steps to reproduce it, ideally with a minimal setup (other mods or plugins installed, config used, commands run)
- A proof of concept, logs or screenshots, if you have them
- Any ideas you have for fixing it

### What counts as a vulnerability

Examples of issues we want to hear about privately:

- A player on a Paper/Spigot server being able to duplicate, delete or take items they should not have access to (for example through sorting, locked slots or `/iwiz` commands)
- Sorting or commands that bypass permissions, protection plugins or container access rules
- A client or server crash, freeze or excessive lag that another player can trigger on purpose
- Config or save data handling that allows reading or writing files outside the game directory
- Anything that lets code run that should not

Ordinary bugs, such as items sorting into the wrong order or a button in the wrong place, are not security issues. Please report those on the [issue tracker](https://github.com/VanishingTacos/InventoryWizard/issues) or the Discord.

## What to Expect

InventoryWizard is maintained in spare time, so these are goals rather than guarantees:

- **Acknowledgement** within 7 days of your report
- **Initial assessment** (whether we can reproduce it and how severe it is) within 14 days
- **Updates** as the fix progresses, at least every 14 days
- **A fix release** as soon as practical, depending on severity and complexity

If the report is accepted, we will work on a fix, publish a new release and then publish a security advisory. You will be credited in the advisory unless you would rather stay anonymous. If the report is declined, we will explain why.

## Disclosure Policy

We follow coordinated disclosure. Please:

- Give us a reasonable amount of time to release a fix before sharing details publicly (we aim for 90 days at most from the report)
- Avoid harming players or servers while testing: only test on worlds and servers you own or have permission to test on
- Do not access, change or delete other people's data

We will not take action against anyone who reports a vulnerability in good faith and follows this policy.
