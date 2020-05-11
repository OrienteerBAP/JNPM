# JNPM
Pure Java API for Node Package Manager
Goal is to provide Java apps flexibility in runtime in comparison with WebJars for working with NPM packages and resources.

Main target capabilities:
- [ ] Equal behaviour of `npm install` and `jnpm install` for global and local scopes
  - [ ] Download packages
  - [ ] Doanload all required dependencies (prod and if needed dev as well)
- [ ] Ability to share and reuse the same repository wtih `npm` during runtime work
- [ ] Easy access to packages resources and transparent downloading of initial packages
