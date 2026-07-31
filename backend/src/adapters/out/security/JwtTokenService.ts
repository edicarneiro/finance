import jwt from "jsonwebtoken";
import { TokenPayload, TokenService } from "../../../application/ports/TokenService";

const ACCESS_TOKEN_TTL_SECONDS = 15 * 60;

export class JwtTokenService implements TokenService {
  constructor(private readonly secret: string) {}

  issue(userId: string): string {
    return jwt.sign({}, this.secret, {
      subject: userId,
      expiresIn: ACCESS_TOKEN_TTL_SECONDS,
    });
  }

  verify(token: string): TokenPayload | null {
    try {
      const decoded = jwt.verify(token, this.secret);

      if (typeof decoded === "object" && typeof decoded.sub === "string") {
        return { userId: decoded.sub };
      }

      return null;
    } catch {
      return null;
    }
  }
}
