import "dotenv/config";
import { createServer } from "./server";
import { buildContainer } from "./composition/container";

const MFA_ENCRYPTION_KEY_BYTES = 32;

const port = Number(process.env.PORT ?? 3000);
const jwtSecret = process.env.JWT_SECRET;
const mfaEncryptionKeyHex = process.env.MFA_ENCRYPTION_KEY;

if (!jwtSecret) {
  throw new Error("JWT_SECRET environment variable is required.");
}

if (!mfaEncryptionKeyHex) {
  throw new Error("MFA_ENCRYPTION_KEY environment variable is required.");
}

const mfaEncryptionKey = Buffer.from(mfaEncryptionKeyHex, "hex");
if (mfaEncryptionKey.length !== MFA_ENCRYPTION_KEY_BYTES) {
  throw new Error(
    `MFA_ENCRYPTION_KEY must be a ${MFA_ENCRYPTION_KEY_BYTES}-byte key encoded as ${MFA_ENCRYPTION_KEY_BYTES * 2} hex characters.`,
  );
}

const container = buildContainer({
  databasePath: process.env.DATABASE_PATH ?? "financepulse.sqlite",
  jwtSecret,
  mfaEncryptionKey,
});

const app = createServer(container);

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`FinancePulse Engine backend listening on port ${port}`);
});
