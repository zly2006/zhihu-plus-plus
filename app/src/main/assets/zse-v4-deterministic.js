#!/usr/bin/env node
'use strict';

const fs = require('fs');
const vm = require('vm');
const path = require('path');

function buildDate(nowMs) {
  const RealDate = Date;
  function MockDate(...args) {
    if (args.length === 0) {
      return new RealDate(nowMs);
    }
    return new RealDate(...args);
  }
  MockDate.now = () => nowMs;
  MockDate.UTC = RealDate.UTC;
  MockDate.parse = RealDate.parse;
  MockDate.prototype = RealDate.prototype;
  return MockDate;
}

function buildMath(nowMs) {
  const math = Object.create(Math);
  const r = ((nowMs % 127) + 127) % 127;
  math.random = () => r / 127;
  return math;
}

function encryptDeterministic(input, nowMs, zsePath) {
  const jsPath = zsePath || path.join(__dirname, 'zse-v4.js');
  const code = fs.readFileSync(jsPath, 'utf8');

  const ctx = {
    console: { log: () => {} },
    Date: buildDate(nowMs),
    Math: buildMath(nowMs),
    atob: (s) => Buffer.from(s, 'base64').toString('binary'),
  };

  vm.createContext(ctx);
  vm.runInContext(code, ctx);
  return ctx.exports.encrypt(input);
}

if (require.main === module) {
  const input = process.argv[2] || '';
  const nowMs = Number(process.argv[3] || Date.now());
  const zsePath = process.argv[4];
  const out = encryptDeterministic(input, nowMs, zsePath);
  process.stdout.write(out);
}

module.exports = { encryptDeterministic };
