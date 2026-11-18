import admin from "firebase-admin";

type WhereOp = "==" | ">=" | "<" | "<=" | ">";

export interface RecordedQuery {
  collectionPath: string;
  filters: Array<{ field: string; op: WhereOp; value: unknown }>;
  limitCount?: number;
}

const SERVER_TIMESTAMP_SENTINEL = admin.firestore.FieldValue.serverTimestamp();

function isServerTimestampValue(value: unknown): boolean {
  if (!value || typeof value !== "object") {
    return false;
  }
  const maybeFv = value as { isEqual?: (other: unknown) => boolean };
  return typeof maybeFv.isEqual === "function" && maybeFv.isEqual(SERVER_TIMESTAMP_SENTINEL);
}

function toComparableValue(value: unknown): unknown {
  if (value instanceof Date) {
    return value.getTime();
  }
  if (value && typeof value === "object") {
    const maybeTs = value as { toMillis?: () => number; toDate?: () => Date };
    if (typeof maybeTs.toMillis === "function") {
      return maybeTs.toMillis();
    }
    if (typeof maybeTs.toDate === "function") {
      return maybeTs.toDate().getTime();
    }
  }
  return value;
}

function cloneAndResolveValue(value: unknown, nowMs: number): unknown {
  if (isServerTimestampValue(value)) {
    return admin.firestore.Timestamp.fromDate(new Date(nowMs));
  }
  if (value instanceof admin.firestore.Timestamp) {
    return new admin.firestore.Timestamp(value.seconds, value.nanoseconds);
  }
  if (value instanceof Date) {
    return new Date(value.getTime());
  }
  if (Array.isArray(value)) {
    return value.map((item) => cloneAndResolveValue(item, nowMs));
  }
  if (value && typeof value === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(value)) {
      if (v !== undefined) {
        out[k] = cloneAndResolveValue(v, nowMs);
      }
    }
    return out;
  }
  return value;
}

function cloneData<T>(value: T): T {
  if (value instanceof admin.firestore.Timestamp) {
    return new admin.firestore.Timestamp(value.seconds, value.nanoseconds) as unknown as T;
  }
  if (value instanceof Date) {
    return new Date(value.getTime()) as unknown as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => cloneData(item)) as unknown as T;
  }
  if (value && typeof value === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(value)) {
      out[k] = cloneData(v);
    }
    return out as T;
  }
  return value;
}

function getNestedField(obj: Record<string, unknown>, fieldPath: string): unknown {
  const parts = fieldPath.split(".");
  let curr: unknown = obj;
  for (const part of parts) {
    if (!curr || typeof curr !== "object") {
      return undefined;
    }
    curr = (curr as Record<string, unknown>)[part];
  }
  return curr;
}

function setNestedField(obj: Record<string, unknown>, fieldPath: string, value: unknown): void {
  const parts = fieldPath.split(".");
  let curr: Record<string, unknown> = obj;
  for (let i = 0; i < parts.length - 1; i++) {
    const key = parts[i];
    if (!curr[key] || typeof curr[key] !== "object" || Array.isArray(curr[key])) {
      curr[key] = {};
    }
    curr = curr[key] as Record<string, unknown>;
  }
  curr[parts[parts.length - 1]] = value;
}

export class MockDocumentSnapshot {
  readonly id: string;
  readonly ref: MockDocumentReference;
  private readonly rawData: Record<string, unknown> | undefined;

  constructor(
    id: string,
    ref: MockDocumentReference,
    rawData: Record<string, unknown> | undefined
  ) {
    this.id = id;
    this.ref = ref;
    this.rawData = rawData ? cloneData(rawData) : undefined;
  }

  get exists(): boolean {
    return this.rawData !== undefined;
  }

  data(): Record<string, unknown> | undefined {
    return this.rawData ? cloneData(this.rawData) : undefined;
  }
}

export class MockQuerySnapshot {
  readonly docs: MockDocumentSnapshot[];

  constructor(docs: MockDocumentSnapshot[]) {
    this.docs = docs;
  }

  get empty(): boolean {
    return this.docs.length === 0;
  }

  get size(): number {
    return this.docs.length;
  }
}

export class MockDocumentReference {
  readonly id: string;
  readonly path: string;
  private readonly db: MockFirestore;

  constructor(db: MockFirestore, path: string) {
    this.db = db;
    this.path = path;
    const parts = path.split("/");
    this.id = parts[parts.length - 1];
  }

  collection(subName: string): MockCollectionReference {
    return new MockCollectionReference(this.db, `${this.path}/${subName}`);
  }

  async get(): Promise<MockDocumentSnapshot> {
    return this.db.readDocSnapshot(this);
  }

  async set(data: Record<string, unknown>): Promise<void> {
    this.db.writeDocSet(this.path, data);
  }

  async update(data: Record<string, unknown>): Promise<void> {
    this.db.writeDocUpdate(this.path, data);
  }
}

export class MockQuery {
  protected readonly db: MockFirestore;
  protected readonly collectionPath: string;
  protected readonly filters: Array<{ field: string; op: WhereOp; value: unknown }>;
  protected readonly limitCount?: number;

  constructor(
    db: MockFirestore,
    collectionPath: string,
    filters: Array<{ field: string; op: WhereOp; value: unknown }> = [],
    limitCount?: number
  ) {
    this.db = db;
    this.collectionPath = collectionPath;
    this.filters = filters;
    this.limitCount = limitCount;
  }

  where(field: string, op: WhereOp, value: unknown): MockQuery {
    return new MockQuery(
      this.db,
      this.collectionPath,
      [...this.filters, { field, op, value }],
      this.limitCount
    );
  }

  limit(count: number): MockQuery {
    return new MockQuery(this.db, this.collectionPath, this.filters, count);
  }

  async get(): Promise<MockQuerySnapshot> {
    return this.db.executeQuery(this.collectionPath, this.filters, this.limitCount);
  }
}

export class MockCollectionReference extends MockQuery {
  constructor(db: MockFirestore, collectionPath: string) {
    super(db, collectionPath, [], undefined);
  }

  doc(id?: string): MockDocumentReference {
    const docId = id ?? this.db.nextAutoId();
    return new MockDocumentReference(this.db, `${this.collectionPath}/${docId}`);
  }
}

interface QueuedWrite {
  type: "set" | "update";
  path: string;
  data: Record<string, unknown>;
}

export class MockTransaction {
  private readonly db: MockFirestore;
  private hasWritten = false;
  readonly writes: QueuedWrite[] = [];

  constructor(db: MockFirestore) {
    this.db = db;
  }

  async get(target: MockDocumentReference | MockQuery): Promise<unknown> {
    if (this.hasWritten) {
      throw new Error(
        "Firestore transactions require all reads to be executed before all writes."
      );
    }
    if (target instanceof MockDocumentReference) {
      return this.db.readDocSnapshot(target);
    }
    return target.get();
  }

  set(ref: MockDocumentReference, data: Record<string, unknown>): this {
    this.hasWritten = true;
    this.writes.push({ type: "set", path: ref.path, data });
    return this;
  }

  update(ref: MockDocumentReference, data: Record<string, unknown>): this {
    this.hasWritten = true;
    this.writes.push({ type: "update", path: ref.path, data });
    return this;
  }
}

export class MockFirestore {
  private readonly store = new Map<string, Record<string, unknown>>();
  private autoIdCounter = 0;
  private readonly nowFn: () => number;
  readonly recordedQueries: RecordedQuery[] = [];

  constructor(nowFn: () => number = () => Date.now()) {
    this.nowFn = nowFn;
  }

  asFirestore(): FirebaseFirestore.Firestore {
    return this as unknown as FirebaseFirestore.Firestore;
  }

  seed(path: string, data: Record<string, unknown>): void {
    this.store.set(path, cloneAndResolveValue(data, this.nowFn()) as Record<string, unknown>);
  }

  getRaw(path: string): Record<string, unknown> | undefined {
    const data = this.store.get(path);
    return data ? cloneData(data) : undefined;
  }

  listCollection(collectionPath: string): Array<{ id: string; data: Record<string, unknown> }> {
    const prefix = `${collectionPath}/`;
    const results: Array<{ id: string; data: Record<string, unknown> }> = [];
    for (const [docPath, data] of this.store.entries()) {
      if (docPath.startsWith(prefix)) {
        const remainder = docPath.slice(prefix.length);
        if (!remainder.includes("/")) {
          results.push({ id: remainder, data: cloneData(data) });
        }
      }
    }
    return results;
  }

  nextAutoId(): string {
    this.autoIdCounter += 1;
    return `mock_id_${this.autoIdCounter}`;
  }

  collection(name: string): MockCollectionReference {
    return new MockCollectionReference(this, name);
  }

  readDocSnapshot(ref: MockDocumentReference): MockDocumentSnapshot {
    return new MockDocumentSnapshot(ref.id, ref, this.store.get(ref.path));
  }

  writeDocSet(path: string, data: Record<string, unknown>): void {
    const resolved = cloneAndResolveValue(data, this.nowFn()) as Record<string, unknown>;
    this.store.set(path, resolved);
  }

  writeDocUpdate(path: string, data: Record<string, unknown>): void {
    const existing = this.store.get(path);
    if (!existing) {
      throw new Error(`NOT_FOUND: Document does not exist at ${path}`);
    }
    const copy = cloneData(existing);
    for (const [key, val] of Object.entries(data)) {
      if (val === undefined) {
        continue;
      }
      const resolvedVal = cloneAndResolveValue(val, this.nowFn());
      if (key.includes(".")) {
        setNestedField(copy, key, resolvedVal);
      } else {
        copy[key] = resolvedVal;
      }
    }
    this.store.set(path, copy);
  }

  executeQuery(
    collectionPath: string,
    filters: Array<{ field: string; op: WhereOp; value: unknown }>,
    limitCount?: number
  ): MockQuerySnapshot {
    this.recordedQueries.push({
      collectionPath,
      filters: filters.map((f) => ({ ...f })),
      limitCount,
    });

    const entries = this.listCollection(collectionPath);
    const matched: MockDocumentSnapshot[] = [];

    for (const entry of entries) {
      let passes = true;
      for (const filter of filters) {
        const docVal = toComparableValue(getNestedField(entry.data, filter.field));
        const targetVal = toComparableValue(filter.value);
        if (filter.op === "==") {
          if (docVal !== targetVal) {
            passes = false;
            break;
          }
        } else if (filter.op === ">=") {
          if (typeof docVal !== "number" || typeof targetVal !== "number" || docVal < targetVal) {
            passes = false;
            break;
          }
        } else if (filter.op === "<") {
          if (typeof docVal !== "number" || typeof targetVal !== "number" || docVal >= targetVal) {
            passes = false;
            break;
          }
        } else if (filter.op === "<=") {
          if (typeof docVal !== "number" || typeof targetVal !== "number" || docVal > targetVal) {
            passes = false;
            break;
          }
        } else if (filter.op === ">") {
          if (typeof docVal !== "number" || typeof targetVal !== "number" || docVal <= targetVal) {
            passes = false;
            break;
          }
        }
      }

      if (passes) {
        const ref = new MockDocumentReference(this, `${collectionPath}/${entry.id}`);
        matched.push(new MockDocumentSnapshot(entry.id, ref, entry.data));
        if (limitCount !== undefined && matched.length >= limitCount) {
          break;
        }
      }
    }

    return new MockQuerySnapshot(matched);
  }

  async runTransaction<T>(fn: (tx: MockTransaction) => Promise<T>): Promise<T> {
    const tx = new MockTransaction(this);
    const result = await fn(tx);
    // Chỉ commit khi fn hoàn tất không ném lỗi (giả lập ACID rollback khi lỗi).
    for (const w of tx.writes) {
      if (w.type === "set") {
        this.writeDocSet(w.path, w.data);
      } else {
        this.writeDocUpdate(w.path, w.data);
      }
    }
    return result;
  }
}
