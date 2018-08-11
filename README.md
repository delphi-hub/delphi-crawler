# Delphi Crawler

The automated crawling and processing engine for the Delphi platform.

We are currently in pre-alpha state! There is no release and the code in
this repository is purely experimental!

|branch | status | codacy | snyk |
| :---: | :---: | :---: | :---: |  
| master | [![Build Status](https://travis-ci.org/delphi-hub/delphi-crawler.svg?branch=master)](https://travis-ci.org/delphi-hub/delphi-crawler) | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d52f09343249401f829585f6edcf6a32)](https://www.codacy.com/app/bhermann/delphi-crawler?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=delphi-hub/delphi-crawler&amp;utm_campaign=Badge_Grade)| [![Known Vulnerabilities](https://snyk.io/test/github/delphi-hub/delphi-crawler/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/delphi-hub/delphi-crawler/?targetFile=build.sbt) |
| develop | [![Build Status](https://travis-ci.org/delphi-hub/delphi-crawler.svg?branch=develop)](https://travis-ci.org/delphi-hub/delphi-crawler)  | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d52f09343249401f829585f6edcf6a32?branch=develop)](https://www.codacy.com/app/bhermann/delphi-crawler?branch=develop&amp;utm_source=github.com&amp;utm_medium=referral&amp;utm_content=delphi-hub/delphi-crawler&amp;utm_campaign=Badge_Grade) | [![Known Vulnerabilities](https://snyk.io/test/github/delphi-hub/delphi-crawler/develop/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/delphi-hub/delphi-crawler/develop/?targetFile=build.sbt) |

## What is the crawler component?

Delphi's crawler component is automatically scanning repositories of code
for new identifiable artifacts to index.
It the pushes these artifacts on its work queue and eventually processes them.
During processing metrics for that artifact are collected and stored.

## How does the crawler component work?

It searches new code artifact from Maven central, downloads necessary files,
and pushes them through the Hermes metrics tool.
The results will then be indexed into an elasticsearch database.

## How can I use the crawler component?

If you just wish to query the results, maybe the public instance at
https://delphi.cs.uni-paderborn.de is the right choice for you.

If you want to create your own index, you can start the crawler directly
using sbt in the project folder:

```
sbt run
```

It expects a running instance of elasticsearch on port 9200 on the same machine.
You can override this default by setting the environment variable `DELPHI_ELASTIC_URI` to connect to your instance.
The URI format is `elasticsearch://<host>:<port>`.

## Community

Feel welcome to join our chatroom on Gitter: [![Join the chat at https://gitter.im/delphi-hub/delphi](https://badges.gitter.im/delphi-hub/delphi.svg)](https://gitter.im/delphi-hub/delphi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## Contributing

Contributions are *very* welcome!

Before contributing, please read our [Code of Conduct](CODE_OF_CONDUCT.md).

Refer to the [Contribution Guide](CONTRIBUTING.md) for details about the workflow.
We use Pull Requests to collect contributions. Especially look out for "help wanted" issues
[![GitHub issues by-label](https://img.shields.io/github/issues/delphi-hub/delphi-crawler/help%20wanted.svg)](https://github.com/delphi-hub/delphi-crawler/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22),
but feel free to work on other issues as well.
You can ask for clarification in the issues directly, or use our Gitter
chat for a more interactive experience.

[![GitHub issues](https://img.shields.io/github/issues/delphi-hub/delphi-crawler.svg)](https://github.com/delphi-hub/delphi-crawler/issues)


## License

The Delphi crawler is open source and available under Apache 2 License.

[![GitHub license](https://img.shields.io/github/license/delphi-hub/delphi-crawler.svg)](https://github.com/delphi-hub/delphi-crawler/blob/master/LICENSE)
