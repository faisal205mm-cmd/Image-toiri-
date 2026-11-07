const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read sales_records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("sales_records").get());
});

test("Unauthenticated user: cannot create sale record", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("sales_records").doc("INV-001").set({
    invoiceNumber: "INV-001",
    totalAmount: 1200,
    userId: ALICE_UID
  }));
});

test("Authenticated user: can create own sale record with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("sales_records").doc("INV-001").set({
    invoiceNumber: "INV-001",
    totalAmount: 1200,
    paidAmount: 1000,
    dueAmount: 200,
    userId: ALICE_UID
  }));
});

test("Authenticated user: cannot read another user's sale record", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sales_records").doc("INV-BOB").set({
      invoiceNumber: "INV-BOB",
      totalAmount: 500,
      userId: BOB_UID
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("sales_records").doc("INV-BOB").get());
});

test("Authenticated user: can create own monthly finance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("monthly_finances").doc("FIN-001").set({
    type: "EXPENSE",
    title: "Fabric Purchase",
    amount: 3500,
    userId: ALICE_UID
  }));
});

test("Authenticated user: cannot delete another user's finance", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("monthly_finances").doc("FIN-BOB").set({
      type: "INCOME",
      title: "Store Sale",
      amount: 4000,
      userId: BOB_UID
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("monthly_finances").doc("FIN-BOB").delete());
});

test("Authenticated user: can query own sales_records with where('userId', '==', uid)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("sales_records").where("userId", "==", ALICE_UID).get());
});

test("Authenticated user: can query own monthly_finances with where('userId', '==', uid)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("monthly_finances").where("userId", "==", ALICE_UID).get());
});

test("Authenticated user: cannot read another user's finance record", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("monthly_finances").doc("FIN-BOB-2").set({
      type: "INCOME",
      title: "Store Sale",
      amount: 4000,
      userId: BOB_UID
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("monthly_finances").doc("FIN-BOB-2").get());
});
