# Releasing to Maven Central

`coinwaka-java` publishes to Maven Central through the Sonatype **Central Portal**.

## One-time setup

1. **Account:** sign in at https://central.sonatype.com (GitHub login is fine).
2. **Namespace:** register `com.coinwaka`. The portal shows a DNS **TXT** record to
   add to `coinwaka.com` to prove ownership; add it and click Verify.
3. **GPG key** (Central requires signed artifacts):
   ```bash
   gpg --gen-key
   gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
   ```
4. **Portal token:** Account → Generate User Token. Put it in `~/.m2/settings.xml`:
   ```xml
   <servers>
     <server>
       <id>central</id>
       <username>TOKEN_USERNAME</username>
       <password>TOKEN_PASSWORD</password>
     </server>
   </servers>
   ```

## Cut a release

```bash
mvn -P release clean deploy
```

The `release` profile attaches sources + javadoc jars, GPG-signs everything, and
publishes via the `central-publishing-maven-plugin` (`autoPublish=true` sends it
straight to Central). Plain `mvn test` / CI never touches these plugins.

Then `com.coinwaka:coinwaka-java:1.0.0` is installable:

```xml
<dependency>
  <groupId>com.coinwaka</groupId>
  <artifactId>coinwaka-java</artifactId>
  <version>1.0.0</version>
</dependency>
```
