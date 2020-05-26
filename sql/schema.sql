CREATE SCHEMA bitcoin;

-- https://developer.bitcoin.org/reference/rpc/getblock.html
CREATE TABLE bitcoin.blocks (
  hash bytea NOT NULL PRIMARY KEY,
  -- confirmations
  size integer NOT NULL,
  stripped_size integer NOT NULL,
  weight integer NOT NULL,
  height integer NOT NULL,
  version integer NOT NULL,
  -- versionHex
  -- merkleroot
  time timestamp without time zone NOT NULL,
  -- mediantime
  nonce bytea NOT NULL,
  bits bytea NOT NULL,
  difficulty integer NOT NULL,
  -- chainwork
  -- nTx integer NOT NULL,
  previous_block_hash bytea NOT NULL,
  nextblockhash bytea
);

-- https://developer.bitcoin.org/reference/rpc/getrawtransaction.html
CREATE TABLE bitcoin.transactions (
  hash bytea NOT NULL PRIMARY KEY,
  -- hex
  -- txid
  size integer NOT NULL,
  vsize integer NOT NULL,
  weight integer NOT NULL,
  version integer NOT NULL,
  -- locktime
  block_hash bytea NOT NULL,
  -- confirmations
  -- block_time
  time timestamp without time zone NOT NULL
);

CREATE SCHEMA ethereum;

CREATE TABLE ethereum.blocks (
  number text NOT NULL PRIMARY KEY,
  hash text NOT NULL
);

CREATE TABLE ethereum.transactions (
  hash text NOT NULL PRIMARY KEY
);

