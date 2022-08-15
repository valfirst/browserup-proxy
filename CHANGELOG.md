# Changelog

# [Unreleased - 2.2.3-SNAPSHOT]

# [2.2.2]
## Changed
### Dependencies
- Bump Selenium from `4.2.2` to `4.3.0` (https://github.com/valfirst/browserup-proxy/pull/110)
- Bump LittleProxy from `2.0.9` to `2.0.11` (https://github.com/valfirst/browserup-proxy/pull/115, https://github.com/valfirst/browserup-proxy/pull/122) (incl. fix for a memory leak)
- Bump Bouncy Castle from `1.70` to `1.71` (https://github.com/valfirst/browserup-proxy/pull/117)
- Bump Swagger from `2.2.1` to `2.2.2` (https://github.com/valfirst/browserup-proxy/pull/119)

# [2.2.1]
## Changed
### Dependencies
- Bump Selenium from `4.1.4` to `4.2.2` (https://github.com/valfirst/browserup-proxy/pull/99)
- Bump Swagger from `2.2.0` to `2.2.1` (https://github.com/valfirst/browserup-proxy/pull/106)
- Bump Netty from `4.1.77.Final` to `4.1.79.Final` (https://github.com/valfirst/browserup-proxy/pull/107, https://github.com/valfirst/browserup-proxy/pull/114)
- Bump Jetty from `9.4.46.v20220331` to `9.4.48.v20220622` (https://github.com/valfirst/browserup-proxy/pull/108)

# [2.2.0]

## Added
- Add ability to get deep copy of HAR object (https://github.com/valfirst/browserup-proxy/pull/85)
- Add ability to find the most recent entry in HAR: `HarLog#findMostRecentEntry()` (https://github.com/valfirst/browserup-proxy/pull/86)
- Add ability to convert HAR to byte array (https://github.com/valfirst/browserup-proxy/pull/90)

## Changed
### Dependencies
- Bump `mitmproxy` from `5.3.0` to `6.0.2` (https://github.com/valfirst/browserup-proxy/pull/94)
- Bump `okhttp` from `4.9.3` to `4.10.0` (https://github.com/valfirst/browserup-proxy/pull/100)

## Fixed
- Make sure default values from HAR entities satisfies specification (https://github.com/valfirst/browserup-proxy/pull/84)

  http://www.softwareishard.com/blog/har-12-spec/#request:
  - headersSize [number] - Set to -1 if the info is not available.
  - bodySize [number] - Set to -1 if the info is not available.

  http://www.softwareishard.com/blog/har-12-spec/#response:
  - headersSize [number]* - Set to -1 if the info is not available.
  - bodySize [number] - Set to -1 if the info is not available.

- Fix `Falcon` deprecation warnings (https://github.com/valfirst/browserup-proxy/pull/101)

# [2.1.5]
- Bump Log4J from `2.17.1` to `2.17.2`
- Bump Jackson from `2.13.1` to `2.13.3`
- Bump Awaitility from `4.1.1` to `4.2.0`
- Bump Guava from `31.0.1-jre` to `31.1-jre`
- Bump Swagger from `2.1.13` to `2.2.0`
- Bump Jetty from `9.4.35.v20201120` to `9.4.46.v20220331`
- Bump Netty from `4.1.74.Final` to `4.1.77.Final`
- Upgrade to LittleProxy `2.0.9`
- Upgrade to an actively maintained [LittleProxy](https://github.com/LittleProxy/LittleProxy) fork
- Bump Selenium from `3.141.59` to `4.1.4`
- Use the `CONNECT` method URI as host detection fallback
- Bump dnsjava from `3.5.0` to `3.5.1`
- Drop Javassist dependency: Javassist dependency was added in order to improve Netty performance, however Netty dropped Javassist support a long time ago
- Optimize logging performance

# [2.1.4]
- Bump SLF4J from `1.7.32` to `1.7.36`
- Bump Netty from `4.1.72.Final` to `4.1.74.Final`
- Bump dnsjava from `3.4.3` to `3.5.0`
- Bump Swagger from `2.1.12` to `2.1.13`

# [2.1.3] - The first release from this fork.
### Maven group is `com.github.valfirst.browserup-proxy`
#### The reason of forking can be found [here](https://github.com/browserup/browserup-proxy/issues/388#issuecomment-996277034).
TBD

# [2.1.2]
TBD

# [2.1.1]
TBD

# [2.1.0]
TBD

# [2.0.1]
TBD

# [2.0.0]
- Performance, Page and Network assertions. The proxy now lets you "assert" over the REST API about the recent HTTP traffic. If you are familiar with HAR files, this lets you skip handling them directly for most use-cases. Some highlights (See the rest in: https://github.com/valfirst/browserup-proxy/commit/889aeda6d27b05b50714b754f6e43b3a600e6d9b):
    - assertMostRecentResponseTimeLessThanOrEqual
    - assertResponseTimeLessThanOrEqual
    - assertMostRecentResponseContentContains
    - assertMostRecentResponseContentMatches
    - assertAnyUrlContentLengthLessThanOrEquals
    - assertAnyUrlContentMatches
    - assertAnyUrlContentDoesNotContain
    - assertAnyUrlResponseHeaderContains
    - assertResponseStatusCode
    - assertMostRecentResponseContentLengthLessThanOrEqual
- Fix compatibility with the HAR viewer by setting correct defaults per the HAR spec
- Update Netty to the latest version
- Merge in contribution from @jrgp to allow upstream proxy connections to utilize HTTPS.
- Default to the step name "Default" when requests come through and no page is set yet.

# [1.2.1]
- No changes, binaries compiled for Java 8+.

# [1.2.0]
- Add much-needed handling of Brotli Compression. Brotli has become a popular alternative to GZIP compression scheme, and is utilized all over the web by websites including Google and Facebook. The proxy can now decompress and recognize brotli.
- Add recognition for variant (versioned) JSON content type strings. Previously, response bodies for JSON content types with content types like  "application/something-v1+json"  would not be captured. Now they will be.
- Fix a credentials leak where the basic auth header was being added to non-connect request types.
- Dependency updates

# [1.1.0]
- ZIP distribution with launch scripts, SSL certificates and keys
- Dependency updates

# [1.0.0]
- Initial fork based on BrowserMob Proxy
- HTTP/2 support via Netty 4.1.34 upgrade
- Java 11 support
- Upgrades to dependencies (mockito, etc)
- Upgrade to an actively maintained, [LittleProxy](https://github.com/mrog/LittleProxy) fork
- Switch to Gradle
- Import a new, better HAR reader from https://github.com/sdstoehr/har-reader
- Extend the har reader with filtering/finding capabilities
- Modify every existing file by adding a header to ensure compliance with Apache License
- Rename our fork to our own name, BrowserUp, as we will be investing in it heavily.
    We have no relation to BrowserMob, which was a company acquired by Neustar in 2010.
- Updates to the Readme to remove legacy proxyserver information
