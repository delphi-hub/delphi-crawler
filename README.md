# Delphi Crawler

The automated crawling and processing engine for the Delphi platform.

|branch | status | codacy |
| :---: | :---: | :---: |
| master | <img src="https://travis-ci.org/delphi-hub/delphi-crawler.svg?branch=master"> | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d52f09343249401f829585f6edcf6a32)](https://www.codacy.com/app/bhermann/delphi-crawler?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=delphi-hub/delphi-crawler&amp;utm_campaign=Badge_Grade)|
| develop | <img src="https://travis-ci.org/delphi-hub/delphi-crawler.svg?branch=develop"> | |

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

## Community

Feel welcome to join our chatroom on Gitter: [![Join the chat at https://gitter.im/delphi-hub/delphi](https://badges.gitter.im/delphi-hub/delphi.svg)](https://gitter.im/delphi-hub/delphi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## Contributing

Before contributing, please read our [Code of Conduct](docs/CODE_OF_CONDUCT.md).


## License

The Delphi crawler is open source and available under Apache 2 License.