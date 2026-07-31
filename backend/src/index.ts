import "dotenv/config";
import { createServer } from "./server";
import { buildContainer } from "./composition/container";

const port = Number(process.env.PORT ?? 3000);
const jwtSecret = process.env.JWT_SECRET;

if (!jwtSecret) {
  throw new Error("JWT_SECRET environment variable is required.");
}

const container = buildContainer({
  databasePath: process.env.DATABASE_PATH ?? "financepulse.sqlite",
  jwtSecret,
});

const app = createServer(container);

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`FinancePulse Engine backend listening on port ${port}`);
});
