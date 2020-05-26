# Ethereum ETL

Bitcoin and Ethereum ETL with:

* cats-effect
* fs2
* http4s

Load to:
* postgresql
* redis

## Bitcoin

```bash
sbt -Dconfig.file=src/main/resources/local.conf runMain io.olownia.BitcoinEtl
```

## Ethereum

```bash
sbt -Dconfig.file=src/main/resources/local.conf runMain io.olownia.EthereumEtl
```
