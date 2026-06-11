# libs/

Local AAR files used as flat-directory dependencies.

## audioswitch-release-v1.2.0-internal-1.aar

**Upstream:** https://github.com/GetStream/android-audioswitch

**Why a local AAR?**
The existing upstream version had a bug that required a fix directly in the library.
Rather than publishing a fork, it is shipped as a local AAR since this is a short-lived workaround —
it will be removed in the v2 release.
