# Vendored Apache Thrift Java library

This directory contains a vendored copy of the [Apache Thrift](https://github.com/apache/thrift)
Java runtime library (`lib/java/src/main/java/org/apache/thrift`).

## How it differs from upstream

Two kinds of changes are applied on top of the pristine upstream sources.

### 1. Mechanical transformation (applied to every vendor drop)

* The `org.apache.thrift` package is renamed to `com.jetbrains.cef.remote.thrift`
  to avoid clashing with a "real" Thrift dependency that might be present on the classpath.
* `org.slf4j.Logger` / `org.slf4j.LoggerFactory` are replaced with
  `com.jetbrains.cef.remote.thrift.Logger` / `LoggerFactory` — lightweight local
  substitutes (backed by `org.cef.misc.CefLog`) that live only on the main branch
  (`Logger.java`, `LoggerFactory.java`; they are not part of the vendor branch).
* Only a curated subset of upstream files is vendored: the servlet-, SASL- and
  HTTP-client-based transports/servers are intentionally excluded.

### 2. Local patches (NOTICE: these files were changed by JetBrains)

* `server/TThreadPoolServer.java` — JBR-9328: catch `Throwable` (not just `Exception`)
  in the worker loop and log it.
* `partial/ThriftMetadata.java` — replaced `org.apache.commons.lang3.StringUtils.repeat`
  with plain `String.repeat` (commons-lang3 is not vendored).
* `partial/Validate.java` — replaced `org.apache.commons.lang3.Validate` calls with
  plain `IllegalArgumentException` / `IllegalStateException` throws.

Keep this list up to date when adding new patches.

## How updates from upstream are arranged

The repository uses the classic **vendor branch + subtree merge** workflow:

* The orphan branch `thrift-vendor` holds *only* the mechanically transformed upstream
  sources (its tree root corresponds to this directory). One commit per upstream version.
* The main branch carries the local patches on top and is connected to `thrift-vendor`
  by merge commits, so every upgrade is a normal git 3-way merge: files you never
  patched are fast-forwarded to the new upstream version silently, and conflicts appear
  only where a local patch and an upstream change genuinely overlap.
* Enable `git config rerere.enabled true` once per clone so that resolved conflicts are
  remembered and replayed automatically on subsequent upgrades.

## Updating to a new upstream version

```sh
# 0. one-time per clone
git config rerere.enabled true

# 1. switch to the vendor branch in a separate worktree
git worktree add ../thrift-vendor thrift-vendor
cd ../thrift-vendor

# 2. download and extract the new upstream release
V=0.23.0   # target version
curl -L -o /tmp/thrift-$V.tar.gz https://github.com/apache/thrift/archive/refs/tags/v$V.tar.gz
tar -xzf /tmp/thrift-$V.tar.gz -C /tmp
SRC=/tmp/thrift-$V/lib/java/src/main/java/org/apache/thrift

# 3. update the files we vendor (only files already tracked; new upstream files
#    are not picked up automatically — review them and `git add` deliberately)
git ls-files | while read f; do
  if [ -f "$SRC/$f" ]; then cp "$SRC/$f" "$f"; else echo "REMOVED upstream: $f"; fi
done

# 3a. list upstream files that are not vendored yet; vendor (cp + transform +
#     git add) the ones the updated code needs — compile errors after the merge
#     will point at them. E.g. 0.19 -> 0.23 requires adding
#     transport/SocketAddressProvider.java and transport/TNonblockingSSLSocket.java,
#     while the servlet/SASL/HTTP files stay excluded.
comm -23 <(cd "$SRC" && find . -name '*.java' | sed 's|^\./||' | sort) \
         <(git ls-files '*.java' | sort)

# 4. apply the mechanical transformation
git ls-files -z | xargs -0 sed -i '' \
  -e 's/org\.apache\.thrift/com.jetbrains.cef.remote.thrift/g' \
  -e 's/org\.slf4j/com.jetbrains.cef.remote.thrift/g'

# 5. commit the vendor drop
git commit -am "thrift vendor: import Apache Thrift $V"
cd - && git worktree remove ../thrift-vendor

# 6. merge into the main branch
git merge -X subtree=java/com/jetbrains/cef/remote/thrift thrift-vendor
# resolve conflicts (expected only in the locally patched files listed above),
# then verify the vendored library still compiles and commit the merge
```

Notes:

* If upstream *removed* a file (step 3 prints `REMOVED upstream:`), `git rm` it on the
  vendor branch before committing.
* If upstream code starts referencing dependencies this project does not vendor
  (e.g. `org.apache.commons.*`, including fully-qualified references without an
  `import`), adapt the file with a local patch on the main branch after the merge and
  add it to the patch list above.
* Regenerate the RPC code (`thrift_codegen`, `remote/gen-cpp`) with the
  matching Thrift compiler version.
