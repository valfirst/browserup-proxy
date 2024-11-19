# Changelog

# [Unreleased - 3.0.0-SNAPSHOT]
## Breaking chnages
- Har log filtering logic is moved out of model class into a separate utility class
  - `c.b.h.m.HarLog.findMostRecentEntry()` is replaced by `c.b.h.f.HarLogFilter.findMostRecentEntry(HarLog)`
  - `c.b.h.m.HarLog.findMostRecentEntry(Pattern)` is replaced by `c.b.h.f.HarLogFilter.findMostRecentEntry(HarLog, Pattern)`
  - `c.b.h.m.HarLog.findEntries(Pattern)` is replaced by `c.b.h.f.HarLogFilter.findEntries(HarLog, Pattern)`
- [browserup-proxy-rest] Improved the result returned by `/proxy/{port}/har/mostRecentEntry` if no HAR entries are available:
  &nbsp;             | Old behaviour                                        | New behaviour
  ------------------ | ---------------------------------------------------- | -------------
  **Status code**    | `200`                                                | `204`
  **Response body**  | HAR entry with required fields having default values | No content
- Remove copy-pasted HAR models and migrate back to the original model (https://github.com/sdstoehr/har-reader)
  Removed entity                                                         | Replacement
  ---------------------------------------------------------------------- | ------------------------------------------------------------------
  `com.browserup.harreader.model.HarCache`                               | `de.sstoehr.harreader.model.HarCache`
  `com.browserup.harreader.model.HarContent`                             | `de.sstoehr.harreader.model.HarContent`
  `com.browserup.harreader.model.HarCookie`                              | `de.sstoehr.harreader.model.HarCookie`
  `com.browserup.harreader.model.HarCreatorBrowser`                      | `de.sstoehr.harreader.model.HarCreatorBrowser`
  `com.browserup.harreader.model.HarHeader`                              | `de.sstoehr.harreader.model.HarHeader`
  `com.browserup.harreader.model.HarPage`                                | `de.sstoehr.harreader.model.HarPage`
  `com.browserup.harreader.model.HarPageTiming`                          | `de.sstoehr.harreader.model.HarPageTiming`
  `com.browserup.harreader.model.HarPostData`                            | `de.sstoehr.harreader.model.HarPostData`
  `com.browserup.harreader.model.HarPostDataParam`                       | `de.sstoehr.harreader.model.HarPostDataParam`
  `com.browserup.harreader.model.HarQueryParam`                          | `de.sstoehr.harreader.model.HarQueryParam`
  `com.browserup.harreader.model.HarRequest`                             | `de.sstoehr.harreader.model.HarRequest`
  `com.browserup.harreader.model.HarResponse`                            | `de.sstoehr.harreader.model.HarResponse`
  `com.browserup.harreader.model.HttpMethod`                             | `de.sstoehr.harreader.model.HttpMethod`
  `com.browserup.harreader.model.HttpStatus`                             | `de.sstoehr.harreader.model.HttpStatus`

# [2.2.19]
## Changed
### Dependencies
- Bump Netty from `4.1.112.Final` to `4.1.113.Final` (https://github.com/valfirst/browserup-proxy/pull/404)
- Bump SLF4J from `2.0.13` to `2.0.16` (https://github.com/valfirst/browserup-proxy/pull/393)
- Bump Log4J from `2.23.1` to `2.24.0` (https://github.com/valfirst/browserup-proxy/pull/403)
- Bump dnsjava from `3.6.0` to `3.6.1` (https://github.com/valfirst/browserup-proxy/pull/386)
- Bump Apache Commons Lang from `3.15.0` to `3.17.0` (https://github.com/valfirst/browserup-proxy/pull/392, https://github.com/valfirst/browserup-proxy/pull/400)
- Bump Swagger from `2.2.22` to `2.2.23` (https://github.com/valfirst/browserup-proxy/pull/398)
- Bump Jetty from `9.4.54.v20240208` to `9.4.56.v20240826` (https://github.com/valfirst/browserup-proxy/pull/406)
- Bump Jersey from `2.43` to `2.45` (https://github.com/valfirst/browserup-proxy/pull/390, https://github.com/valfirst/browserup-proxy/pull/405)
- Bump Guava from `33.2.1-jre` to `33.3.0-jre` (https://github.com/valfirst/browserup-proxy/pull/396)
- Bump Awaitility from `4.2.1` to `4.2.2` (https://github.com/valfirst/browserup-proxy/pull/395)

# [2.2.18]
## Changed
### Dependencies
- Bump Netty from `4.1.111.Final` to `4.1.112.Final` (https://github.com/valfirst/browserup-proxy/pull/381)
- Bump dnsjava from `3.5.3` to `3.6.0` (https://github.com/valfirst/browserup-proxy/pull/385)
- Bump Jackson from `2.17.1` to `2.17.2` (https://github.com/valfirst/browserup-proxy/pull/380)
- Bump Apache Commons Lang from `3.14.0` to `3.15.0` (https://github.com/valfirst/browserup-proxy/pull/382)


# [2.2.17]
## Changed
### Dependencies
- Bump Netty from `4.1.107.Final` to `4.1.111.Final` (https://github.com/valfirst/browserup-proxy/pull/352, https://github.com/valfirst/browserup-proxy/pull/359, https://github.com/valfirst/browserup-proxy/pull/369, https://github.com/valfirst/browserup-proxy/pull/372)
- Bump Bouncy Castle from `1.77` to `1.78.1` (https://github.com/valfirst/browserup-proxy/pull/356, https://github.com/valfirst/browserup-proxy/pull/360)
- Bump SLF4J from `2.0.12` to `2.0.13` (https://github.com/valfirst/browserup-proxy/pull/358)
- Bump Swagger from `2.2.20` to `2.2.22` (https://github.com/valfirst/browserup-proxy/pull/353, https://github.com/valfirst/browserup-proxy/pull/367)
- Bump Jackson from `2.17.0` to `2.17.1` (https://github.com/valfirst/browserup-proxy/pull/362)
- Bump Jetty from `9.4.53.v20231009` to `9.4.54.v20240208` (https://github.com/valfirst/browserup-proxy/pull/377)
- Bump Jersey from `2.40` to `2.43` (https://github.com/valfirst/browserup-proxy/pull/357, https://github.com/valfirst/browserup-proxy/pull/378)
- Bump Guava from `33.1.0-jre` to `33.2.1-jre` (https://github.com/valfirst/browserup-proxy/pull/363, https://github.com/valfirst/browserup-proxy/pull/372)

# [2.2.16]
## Changed
### Dependencies
- Bump HAR reader from `2.2.1` to `2.3.0` (https://github.com/valfirst/browserup-proxy/pull/309)
- Bump Netty from `4.1.101.Final` to `4.1.107.Final` (https://github.com/valfirst/browserup-proxy/pull/319, https://github.com/valfirst/browserup-proxy/pull/330, https://github.com/valfirst/browserup-proxy/pull/338)
- Bump SLF4J from `2.0.9` to `2.0.12` (https://github.com/valfirst/browserup-proxy/pull/326, https://github.com/valfirst/browserup-proxy/pull/328, , https://github.com/valfirst/browserup-proxy/pull/335)
- Bump Log4J from `2.21.1` to `2.23.1` (https://github.com/valfirst/browserup-proxy/pull/308, https://github.com/valfirst/browserup-proxy/pull/327, https://github.com/valfirst/browserup-proxy/pull/340, , https://github.com/valfirst/browserup-proxy/pull/346)
- Bump Swagger from `2.2.19` to `2.2.20` (https://github.com/valfirst/browserup-proxy/pull/323)
- Bump Jackson from `2.16.0` to `2.17.0` (https://github.com/valfirst/browserup-proxy/pull/324, https://github.com/valfirst/browserup-proxy/pull/348, , https://github.com/valfirst/browserup-proxy/pull/350)
- Bump Apache Commons Lang from `3.13.0` to `3.14.0` (https://github.com/valfirst/browserup-proxy/pull/257)
- Bump Guava from `32.1.3-jre` to `33.1.0-jre` (https://github.com/valfirst/browserup-proxy/pull/322, https://github.com/valfirst/browserup-proxy/pull/351)
- Bump Awaitility from `4.2.0` to `4.2.1` (https://github.com/valfirst/browserup-proxy/pull/349)

# [2.2.15]
## Changed
- Use native Java to manage OS processes (https://github.com/valfirst/browserup-proxy/pull/306)

  `org.zeroturnaround:zt-exec` dependency is dropped.

### Dependencies
- Bump Netty from `4.1.100.Final` to `4.1.101.Final` (https://github.com/valfirst/browserup-proxy/pull/297)
- Bump `okhttp` from `4.11.0` to `4.12.0` (https://github.com/valfirst/browserup-proxy/pull/290)
- Bump Log4J from `2.20.0` to `2.21.1` (https://github.com/valfirst/browserup-proxy/pull/289, https://github.com/valfirst/browserup-proxy/pull/292)
- Bump Swagger from `2.2.17` to `2.2.19` (https://github.com/valfirst/browserup-proxy/pull/291, https://github.com/valfirst/browserup-proxy/pull/296)
- Bump dnsjava from `3.5.2` to `3.5.3` (https://github.com/valfirst/browserup-proxy/pull/298)
- Bump Bouncy Castle from `1.76` to `1.77` (https://github.com/valfirst/browserup-proxy/pull/300)
- Bump Jackson from `2.15.3` to `2.16.0` (https://github.com/valfirst/browserup-proxy/pull/299)

## Fixed
- Fix scope of `org.slf4j:jcl-over-slf4j` dependency: do not add it as `compile`-scope dependency (https://github.com/valfirst/browserup-proxy/pull/303)

  Users should decide which logging framework to use and how to bridge Jakarta Commons Logging (JCL).

# [2.2.14]
## Changed
### Dependencies
- Bump LittleProxy from `2.0.20` to `2.0.22` (https://github.com/valfirst/browserup-proxy/pull/277, https://github.com/valfirst/browserup-proxy/pull/279, https://github.com/valfirst/browserup-proxy/pull/285)
- Bump Netty from `4.1.97.Final` to `4.1.100.Final` (https://github.com/valfirst/browserup-proxy/pull/274, https://github.com/valfirst/browserup-proxy/pull/276)
- Bump Jetty from `9.4.51.v20230217` to `9.4.53.v20231009` (https://github.com/valfirst/browserup-proxy/pull/281)
- Bump Swagger from `2.2.15` to `2.2.17` (https://github.com/valfirst/browserup-proxy/pull/273, https://github.com/valfirst/browserup-proxy/pull/286)
- Bump Jackson from `2.15.2` to `2.15.3` (https://github.com/valfirst/browserup-proxy/pull/288)
- Bump Guava from `32.1.2-jre` to `32.1.3-jre` (https://github.com/valfirst/browserup-proxy/pull/284)
- Bump Selenium from `4.12.1` to `4.13.0` (https://github.com/valfirst/browserup-proxy/pull/275)

# [2.2.13]
## Changed
### Dependencies
- Bump LittleProxy from `2.0.19` to `2.0.20` (https://github.com/valfirst/browserup-proxy/pull/272)
- Bump SLF4J from `2.0.7` to `2.0.9` (https://github.com/valfirst/browserup-proxy/pull/267)
- Bump Selenium from `4.11.0` to `4.12.1` (https://github.com/valfirst/browserup-proxy/pull/265, https://github.com/valfirst/browserup-proxy/pull/271)

# [2.2.12]
## Changed
### Dependencies
- Bump Netty from `4.1.95.Final` to `4.1.97.Final` (https://github.com/valfirst/browserup-proxy/pull/258, https://github.com/valfirst/browserup-proxy/pull/264)
- Bump Bouncy Castle from `1.75` to `1.76` (https://github.com/valfirst/browserup-proxy/pull/256)
- Bump Apache Commons Lang from `3.12.0` to `3.13.0` (https://github.com/valfirst/browserup-proxy/pull/257)
- Bump Guava from `32.1.1-jre` to `32.1.2-jre` (https://github.com/valfirst/browserup-proxy/pull/260)
- Bump Selenium from `4.10.0` to `4.11.0` (https://github.com/valfirst/browserup-proxy/pull/259)

# [2.2.11]
## Changed
### Dependencies
- Bump LittleProxy from `2.0.17` to `2.0.19` (https://github.com/valfirst/browserup-proxy/pull/235, https://github.com/valfirst/browserup-proxy/pull/253)
- Bump Netty from `4.1.92.Final` to `4.1.95.Final` (https://github.com/valfirst/browserup-proxy/pull/236, https://github.com/valfirst/browserup-proxy/pull/246, https://github.com/valfirst/browserup-proxy/pull/252)
- Bump Swagger from `2.2.9` to `2.2.15` (https://github.com/valfirst/browserup-proxy/pull/231, https://github.com/valfirst/browserup-proxy/pull/237, https://github.com/valfirst/browserup-proxy/pull/242, https://github.com/valfirst/browserup-proxy/pull/245, https://github.com/valfirst/browserup-proxy/pull/250)
- Bump Jersey from `2.32` to `2.40` (https://github.com/valfirst/browserup-proxy/pull/255)
- Bump Bouncy Castle from `1.73` to `1.75` (https://github.com/valfirst/browserup-proxy/pull/243, https://github.com/valfirst/browserup-proxy/pull/247)
- Bump Jackson from `2.15.0` to `2.15.2` (https://github.com/valfirst/browserup-proxy/pull/233, https://github.com/valfirst/browserup-proxy/pull/238)
- Bump Guava from `31.1-jre` to `32.1.1-jre` (https://github.com/valfirst/browserup-proxy/pull/234, https://github.com/valfirst/browserup-proxy/pull/241, https://github.com/valfirst/browserup-proxy/pull/249)
- Bump Selenium from `4.9.0` to `4.10.0` (https://github.com/valfirst/browserup-proxy/pull/229, https://github.com/valfirst/browserup-proxy/pull/240)

# [2.2.10]
## Changed
### Dependencies
- Bump LittleProxy from `2.0.16` to `2.0.17` (https://github.com/valfirst/browserup-proxy/pull/218)
- Bump Netty from `4.1.90.Final` to `4.1.92.Final` (https://github.com/valfirst/browserup-proxy/pull/219, https://github.com/valfirst/browserup-proxy/pull/228)
- Bump `okhttp` from `4.10.0` to `4.11.0` (https://github.com/valfirst/browserup-proxy/pull/227)
- Bump Bouncy Castle from `1.72` to `1.73` (https://github.com/valfirst/browserup-proxy/pull/222)
- Bump Jackson from `2.14.2` to `2.15.0` (https://github.com/valfirst/browserup-proxy/pull/226)
- Bump Selenium from `4.8.2` to `4.9.0` (https://github.com/valfirst/browserup-proxy/pull/216, https://github.com/valfirst/browserup-proxy/pull/227)

# [2.2.9]
## Changed
### Dependencies
- Bump LittleProxy from `2.0.15` to `2.0.16` (https://github.com/valfirst/browserup-proxy/pull/207)
- Bump Jetty from `9.4.50.v20221201` to `9.4.51.v20230217` (https://github.com/valfirst/browserup-proxy/pull/208)
- Bump Swagger from `2.2.8` to `2.2.9` (https://github.com/valfirst/browserup-proxy/pull/212)
- Bump SLF4J from `2.0.6` to `2.0.7` (https://github.com/valfirst/browserup-proxy/pull/214)
- Bump Netty from `4.1.89.Final` to `4.1.90.Final` (https://github.com/valfirst/browserup-proxy/pull/213)
- Bump Selenium from `4.8.1` to `4.8.2` (https://github.com/valfirst/browserup-proxy/pull/215)

# [2.2.8]
## Changed
### Dependencies
- Bump Swagger from `2.2.7` to `2.2.8` (https://github.com/valfirst/browserup-proxy/pull/194)
- Bump Netty from `4.1.86.Final` to `4.1.89.Final` (https://github.com/valfirst/browserup-proxy/pull/196, https://github.com/valfirst/browserup-proxy/pull/203, https://github.com/valfirst/browserup-proxy/pull/204)
- Bump Jackson from `2.14.1` to `2.14.2` (https://github.com/valfirst/browserup-proxy/pull/198)
- Bump Selenium from `4.7.2` to `4.8.1` (https://github.com/valfirst/browserup-proxy/pull/197, https://github.com/valfirst/browserup-proxy/pull/205)
- Bump Log4J from `2.19.0` to `2.20.0` (https://github.com/valfirst/browserup-proxy/pull/134)

# [2.2.7]
## Changed
### Dependencies
- Bump Selenium from `4.5.0` to `4.7.2` (https://github.com/valfirst/browserup-proxy/pull/164, https://github.com/valfirst/browserup-proxy/pull/189)
- Bump LittleProxy from `2.0.13` to `2.0.15` (https://github.com/valfirst/browserup-proxy/pull/180, https://github.com/valfirst/browserup-proxy/pull/188)
- Bump httpclient from `4.5.13` to `4.5.14` (https://github.com/valfirst/browserup-proxy/pull/182)
- Bump Jetty from `9.4.49.v20220914` to `9.4.50.v20221201` (https://github.com/valfirst/browserup-proxy/pull/184)
- Bump SLF4J from `2.0.5` to `2.0.6` (https://github.com/valfirst/browserup-proxy/pull/187)
- Bump Netty from `4.1.82.Final` to `4.1.86.Final` (https://github.com/valfirst/browserup-proxy/pull/190)

# [2.2.6]
## Changed
### Dependencies
- Bump Swagger from `2.2.6` to `2.2.7` (https://github.com/valfirst/browserup-proxy/pull/173)
- Bump dnsjava from `3.5.1` to `3.5.2` (https://github.com/valfirst/browserup-proxy/pull/176)
- Bump SLF4J from `2.0.3` to `2.0.5` (https://github.com/valfirst/browserup-proxy/pull/174, https://github.com/valfirst/browserup-proxy/pull/179)

## Fixed
- (Fixes https://github.com/valfirst/browserup-proxy/pull/177) Don't use Jackson BOM as explicit dependency (https://github.com/valfirst/browserup-proxy/pull/178)

# [2.2.5]
## Changed
### Dependencies
- Bump Swagger from `2.2.3` to `2.2.6` (https://github.com/valfirst/browserup-proxy/pull/157, https://github.com/valfirst/browserup-proxy/pull/162)
- Bump Jakson BOM from `2.13.4.20221013` to `2.14.0` (https://github.com/valfirst/browserup-proxy/pull/167)

## Fixed
- (Fixes https://github.com/valfirst/browserup-proxy/pull/160) Downgrade Netty from `4.1.84.Final` to `4.1.82.Final` (https://github.com/valfirst/browserup-proxy/pull/170)

# [2.2.4]
## Changed
### Dependencies
- Bump SLF4J from `2.0.1` to `2.0.3` (https://github.com/valfirst/browserup-proxy/pull/143, https://github.com/valfirst/browserup-proxy/pull/147)
- Bump Bouncy Castle from `1.71.1` to `1.72` (https://github.com/valfirst/browserup-proxy/pull/148)
- Bump Swagger from `2.2.2` to `2.2.3` (https://github.com/valfirst/browserup-proxy/pull/146)
- Bump LittleProxy from `2.0.11` to `2.0.13` (https://github.com/valfirst/browserup-proxy/pull/150)
- Bump Selenium from `4.4.0` to `4.5.0` (https://github.com/valfirst/browserup-proxy/pull/145)
- Revise Jackson dependencies (https://github.com/valfirst/browserup-proxy/pull/153)
- Bump Netty from `4.1.82.Final` to `4.1.84.Final` (https://github.com/valfirst/browserup-proxy/pull/156)
- Bump Jakson BOM from `2.13.4` to `2.13.4.20221013` (https://github.com/valfirst/browserup-proxy/pull/155)

## Deprecated
- Start deprecating copy-pasted HAR reader logic (https://github.com/valfirst/browserup-proxy/pull/154) \
  \
  At some point the full copy of HAR reader library (https://github.com/sdstoehr/har-reader) was added to BrowserUp proxy: browserup#38. That was not a good solution, since the fixes from the original library are not added back, the copy adds extra maintenance effort, etc. The best strategy here is to propose custom changes to the original implementation step by step. \
  This commit starts the deprecation process of copy-pasted as-is HAR reader entities.
  Deprecated entity                                                      | Replacement
  ---------------------------------------------------------------------- | ------------------------------------------------------------------
  `com.browserup.harreader.HarReader`                                    | `de.sstoehr.harreader.HarReader`
  `com.browserup.harreader.HarReaderException`                           | `de.sstoehr.harreader.HarReaderException`
  `com.browserup.harreader.HarReaderMode`                                | `de.sstoehr.harreader.HarReaderMode`
  `com.browserup.harreader.jackson.DefaultMapperFactory`                 | `de.sstoehr.harreader.jackson.DefaultMapperFactory`
  `com.browserup.harreader.jackson.ExceptionIgnoringDateDeserializer`    | `de.sstoehr.harreader.jackson.ExceptionIgnoringDateDeserializer`
  `com.browserup.harreader.jackson.ExceptionIgnoringIntegerDeserializer` | `de.sstoehr.harreader.jackson.ExceptionIgnoringIntegerDeserializer`
  `com.browserup.harreader.jackson.MapperFactory`                        | `de.sstoehr.harreader.jackson.MapperFactory`
  `com.browserup.harreader.model.HarHeader`                              | `de.sstoehr.harreader.model.HarHeader`
  `com.browserup.harreader.model.HarPageTiming`                          | `de.sstoehr.harreader.model.HarPageTiming`
  `com.browserup.harreader.model.HarQueryParam`                          | `de.sstoehr.harreader.model.HarQueryParam`


# [2.2.3]
## Changed
### Dependencies
- Bump Selenium from `4.3.0` to `4.4.0` (https://github.com/valfirst/browserup-proxy/pull/123)
- Bump Bouncy Castle from `1.71` to `1.71.1` (https://github.com/valfirst/browserup-proxy/pull/127)
- Bump Netty from `4.1.79.Final` to `4.1.82.Final` (https://github.com/valfirst/browserup-proxy/pull/128, https://github.com/valfirst/browserup-proxy/pull/132, , https://github.com/valfirst/browserup-proxy/pull/136)
- Bump Jackson from `2.13.3` to `2.13.4` (https://github.com/valfirst/browserup-proxy/pull/129)
- Bump SLF4J from `1.7.36` to `2.0.1` (https://github.com/valfirst/browserup-proxy/pull/138)
- Bump Log4J from `2.18.0` to `2.19.0` (https://github.com/valfirst/browserup-proxy/pull/134)
- Switch to Log4j SLF4J 2.0 API binding to Log4j 2 Core (https://github.com/valfirst/browserup-proxy/pull/140)
- Bump Jetty from `9.4.48.v20220622` to `9.4.49.v20220914` (https://github.com/valfirst/browserup-proxy/pull/141)

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
