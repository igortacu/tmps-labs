import test from 'node:test';
import assert from 'node:assert/strict';

import { BackendUnavailableError } from '../dist/domain/inference.js';
import { GatewayPipelineBuilder } from '../dist/creation/gateway-pipeline-builder.js';
import { BackendGroup } from '../dist/patterns/composite/backend-group.js';
import { SimulatedBackend } from '../dist/patterns/composite/simulated-backend.js';
import { AuditDecorator } from '../dist/patterns/decorator/audit-decorator.js';
import { RetryDecorator } from '../dist/patterns/decorator/retry-decorator.js';
import { AccessDeniedError, AllowlistPolicy, AuthorizationProxy } from '../dist/patterns/proxy/authorization-proxy.js';

const request = { clientId: 'alice', prompt: 'hello' };

test('Composite: leaf and group expose the same complete contract', () => {
  const leaf = new SimulatedBackend('a', '[A]');
  const group = new BackendGroup('cluster').add(leaf);
  assert.deepEqual(leaf.complete(request), group.complete(request));
  assert.equal(leaf.calls, 2);
});

test('Composite: nested groups fall back to another child', () => {
  const failing = new SimulatedBackend('a', '[A]', 1);
  const backup = new SimulatedBackend('b', '[B]');
  const region = new BackendGroup('region').add(failing);
  const root = new BackendGroup('root').add(region).add(backup);
  assert.equal(root.complete(request).provider, 'b');
  assert.equal(failing.calls, 1);
  assert.equal(backup.calls, 1);
});

test('Composite: removing a child and exhausting children reports unavailability', () => {
  const leaf = new SimulatedBackend('a', '[A]');
  const group = new BackendGroup('empty').add(leaf).remove(leaf);
  assert.throws(() => group.complete(request), BackendUnavailableError);
});

test('Decorator: retry wrapper succeeds after temporary failure', () => {
  const leaf = new SimulatedBackend('a', '[A]', 1);
  const service = new RetryDecorator(leaf, 2);
  assert.equal(service.complete(request).provider, 'a');
  assert.equal(leaf.calls, 2);
});

test('Decorator: audit wrapper records success and failure without changing the response', () => {
  const events = [];
  const leaf = new SimulatedBackend('a', '[A]', 1);
  const service = new AuditDecorator(leaf, { record: (event) => events.push(event) });
  assert.throws(() => service.complete(request), BackendUnavailableError);
  const response = service.complete(request);
  assert.deepEqual(response, { text: '[A] hello', provider: 'a' });
  assert.deepEqual(events.map((event) => event.outcome), ['failure', 'success']);
});

test('Decorator: audit and retry can be stacked in either order', () => {
  for (const auditOutside of [true, false]) {
    const events = [];
    const leaf = new SimulatedBackend('a', '[A]', 1);
    const sink = { record: (event) => events.push(event) };
    const service = auditOutside
      ? new AuditDecorator(new RetryDecorator(leaf, 2), sink)
      : new RetryDecorator(new AuditDecorator(leaf, sink), 2);
    assert.equal(service.complete(request).provider, 'a');
    assert.equal(events.at(-1).outcome, 'success');
    assert.equal(leaf.calls, 2);
  }
});

test('Proxy: unauthorized calls never reach the real service', () => {
  const leaf = new SimulatedBackend('a', '[A]');
  const proxy = new AuthorizationProxy(leaf, new AllowlistPolicy(new Set(['bob'])));
  assert.throws(() => proxy.complete(request), AccessDeniedError);
  assert.equal(leaf.calls, 0);
});

test('Proxy: authorized calls return the real service response unchanged', () => {
  const expected = { text: 'exact response', provider: 'real' };
  let calls = 0;
  const realService = { complete: () => { calls++; return expected; } };
  const proxy = new AuthorizationProxy(realService, new AllowlistPolicy(new Set(['alice'])));
  assert.strictEqual(proxy.complete(request), expected);
  assert.equal(calls, 1);
});

test('Proxy: a new access policy works without changing the proxy', () => {
  const policy = { allows: (clientId) => clientId.startsWith('a') };
  const proxy = new AuthorizationProxy(new SimulatedBackend('a', '[A]'), policy);
  assert.equal(proxy.complete(request).provider, 'a');
  assert.throws(() => proxy.complete({ ...request, clientId: 'bob' }), AccessDeniedError);
});

test('Integration: Builder connects Proxy, Decorators, and Composite in one path', () => {
  const a = new SimulatedBackend('a', '[A]', 1);
  const b = new SimulatedBackend('b', '[B]', 1);
  const region = new BackendGroup('region').add(a).add(b);
  const events = [];
  const gateway = new GatewayPipelineBuilder()
    .addBackend(region)
    .decorate((inner) => new RetryDecorator(inner, 2))
    .decorate((inner) => new AuditDecorator(inner, { record: (event) => events.push(event) }))
    .withPolicy(new AllowlistPolicy(new Set(['alice'])))
    .build();
  assert.equal(gateway.complete(request).provider, 'a');
  assert.deepEqual([a.calls, b.calls], [2, 1]);
  assert.deepEqual(events.map((event) => event.outcome), ['success']);
  assert.throws(() => gateway.complete({ ...request, clientId: 'bob' }), AccessDeniedError);
  assert.deepEqual([a.calls, b.calls], [2, 1]);
});

test('Integration: Builder accepts a new decorator without modification', () => {
  class UppercaseDecorator {
    constructor(inner) { this.inner = inner; }
    complete(request) {
      const response = this.inner.complete(request);
      return { ...response, text: response.text.toUpperCase() };
    }
  }
  const gateway = new GatewayPipelineBuilder()
    .addBackend(new SimulatedBackend('a', '[A]'))
    .decorate((inner) => new UppercaseDecorator(inner))
    .withPolicy({ allows: () => true })
    .build();
  assert.equal(gateway.complete(request).text, '[A] HELLO');
});
